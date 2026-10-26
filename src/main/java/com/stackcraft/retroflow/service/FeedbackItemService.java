package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.dto.CreateFeedbackItemRequest;
import com.stackcraft.retroflow.dto.FeedbackItemResponse;
import com.stackcraft.retroflow.dto.PagedResponse;
import com.stackcraft.retroflow.dto.PagingInfo;
import com.stackcraft.retroflow.dto.TeamMemberResponse;
import com.stackcraft.retroflow.dto.UpdateFeedbackItemRequest;
import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.ActionPriority;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.FeedbackType;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.TeamMember;
import com.stackcraft.retroflow.exception.NotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.ActionItemRepository;
import com.stackcraft.retroflow.repository.FeedbackItemRepository;
import com.stackcraft.retroflow.repository.RetrospectiveRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class FeedbackItemService {

    private final RetrospectiveRepository retrospectiveRepository;
    private final FeedbackItemRepository feedbackItemRepository;
    private final ActionItemRepository actionItemRepository;

    public FeedbackItemService(RetrospectiveRepository retrospectiveRepository,
                               FeedbackItemRepository feedbackItemRepository,
                               ActionItemRepository actionItemRepository) {
        this.retrospectiveRepository = retrospectiveRepository;
        this.feedbackItemRepository = feedbackItemRepository;
        this.actionItemRepository = actionItemRepository;
    }

    public FeedbackItemResponse createFeedbackItem(Long retrospectiveId, CreateFeedbackItemRequest request) {
        validateContent("new", request.content());
        if (request.type() == null) {
            throw new RetroflowException("FeedbackItem new: type is required");
        }
        if (request.type() == FeedbackType.ACTION_ITEM && request.priority() == null) {
            throw new RetroflowException("ActionItem new: priority is required");
        }
        Retrospective retrospective = requireRetrospective(retrospectiveId);
        requireOpen(retrospective);
        TeamMember member = requireMember(retrospective, request.email());
        FeedbackItem item = new FeedbackItem();
        item.setRetrospective(retrospective);
        item.setContent(request.content());
        item.setType(request.type().name());
        item.setSubmittedBy(member.getId().toString());
        item = feedbackItemRepository.save(item);
        ActionItem actionItem = null;
        if (request.type() == FeedbackType.ACTION_ITEM) {
            actionItem = new ActionItem();
            actionItem.setRetrospective(retrospective);
            actionItem.setFeedbackItemId(item.getId());
            actionItem.setDescription(request.content());
            actionItem.setPriority(request.priority().name());
            actionItem.setAssignee(member.getName());
            actionItem = actionItemRepository.save(actionItem);
        }
        return toResponse(item, actionItem);
    }

    @Transactional(readOnly = true)
    public PagedResponse<FeedbackItemResponse> listFeedbackItems(
            Long retrospectiveId, ActionPriority priority, Boolean completed, int pageNumber, int pageSize) {
        requireRetrospective(retrospectiveId);
        List<ActionItem> actions = actionItemRepository.findAllByRetrospective_Id(retrospectiveId);
        List<FeedbackItem> feedbackItems = feedbackItemRepository.findAllByRetrospective_Id(retrospectiveId);
        List<FeedbackItemResponse> matching = new ArrayList<>();
        for (FeedbackItem item : feedbackItems) {
            ActionItem action = actions.stream()
                    .filter(candidate -> item.getId().equals(candidate.getFeedbackItemId()))
                    .findFirst().orElse(null);
            if (matches(item, action, priority, completed)) {
                matching.add(toResponse(item, action));
            }
        }
        matching.sort(Comparator.comparing(FeedbackItemResponse::id));
        int start = Math.min(pageNumber * pageSize, matching.size());
        int end = Math.min(start + pageSize, matching.size());
        return new PagedResponse<>(matching.subList(start, end), new PagingInfo(matching.size()));
    }

    @Transactional(readOnly = true)
    public FeedbackItemResponse getFeedbackItem(Long retrospectiveId, Long feedbackItemId) {
        Retrospective retrospective = requireRetrospective(retrospectiveId);
        FeedbackItem item = findFeedbackItem(retrospectiveId, feedbackItemId);
        ActionItem action = actionItemRepository.findByFeedbackItemId(item.getId()).orElse(null);
        return toResponse(item, action);
    }

    public FeedbackItemResponse updateFeedbackItem(
            Long retrospectiveId, Long feedbackItemId, UpdateFeedbackItemRequest request) {
        Retrospective retrospective = requireRetrospective(retrospectiveId);
        requireMember(retrospective, request.email());
        FeedbackItem item = findFeedbackItem(retrospectiveId, feedbackItemId);
        ActionItem action = actionItemRepository.findByFeedbackItemId(item.getId()).orElse(null);
        if (action == null) {
            requireOpen(retrospective);
        } else {
            if (action.isCompleted()) {
                throw new RetroflowException("ActionItem " + action.getId() + ": completed action item cannot be edited");
            }
            if (request.priority() != null) {
                action.setPriority(request.priority().name());
                actionItemRepository.save(action);
            }
        }
        validateContent(feedbackItemId.toString(), request.content());
        item.setContent(request.content());
        item = feedbackItemRepository.save(item);
        if (action != null) {
            action.setDescription(request.content());
        }
        return toResponse(item, action);
    }

    public void deleteFeedbackItem(Long retrospectiveId, Long feedbackItemId, String email) {
        Retrospective retrospective = requireRetrospective(retrospectiveId);
        requireOpen(retrospective);
        requireMember(retrospective, email);
        FeedbackItem item = findFeedbackItem(retrospectiveId, feedbackItemId);
        actionItemRepository.findByFeedbackItemId(item.getId()).ifPresent(actionItemRepository::delete);
        feedbackItemRepository.delete(item);
    }

    public FeedbackItemResponse completeActionItem(Long retrospectiveId, Long feedbackItemId, String email) {
        Retrospective retrospective = requireRetrospective(retrospectiveId);
        requireMember(retrospective, email);
        FeedbackItem item = findFeedbackItem(retrospectiveId, feedbackItemId);
        ActionItem action = actionItemRepository.findByFeedbackItemId(item.getId())
                .orElseThrow(() -> new RetroflowException(
                        "FeedbackItem " + feedbackItemId + ": only action items can be completed"));
        if (action.isCompleted()) {
            throw new RetroflowException("ActionItem " + action.getId() + ": already completed");
        }
        action.setCompleted(true);
        return toResponse(item, actionItemRepository.save(action));
    }

    private boolean matches(FeedbackItem item, ActionItem action, ActionPriority priority, Boolean completed) {
        if (priority == null && completed == null) {
            return true;
        }
        return action != null
                && (priority == null || priority.name().equals(action.getPriority()))
                && (completed == null || completed == action.isCompleted());
    }

    private FeedbackItemResponse toResponse(FeedbackItem item, ActionItem action) {
        return new FeedbackItemResponse(item.getId(), item.getRetrospective().getId(), item.getContent(),
                FeedbackType.valueOf(item.getType()), memberResponse(item), action == null ? null
                : ActionPriority.valueOf(action.getPriority()), action == null ? null : action.isCompleted());
    }

    private TeamMemberResponse memberResponse(FeedbackItem item) {
        String reference = item.getSubmittedBy();
        return item.getRetrospective().getTeam().getTeamMembers().stream()
                .filter(member -> reference.equals(member.getId().toString()))
                .findFirst()
                .map(member -> new TeamMemberResponse(member.getId(), member.getName(), member.getEmail()))
                .orElseGet(() -> new TeamMemberResponse(null, reference, null));
    }

    private TeamMember requireMember(Retrospective retrospective, String email) {
        return retrospective.getTeam().getTeamMembers().stream()
                .filter(member -> member.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElseThrow(() -> new RetroflowException("Team " + retrospective.getTeam().getId()
                        + ": email is not a member of the team"));
    }

    private void validateContent(String feedbackItemId, String content) {
        if (content == null || content.isBlank() || content.length() > 5000) {
            throw new RetroflowException("FeedbackItem " + feedbackItemId
                    + ": content must be nonblank and at most 5000 characters");
        }
    }

    private void requireOpen(Retrospective retrospective) {
        if (retrospective.getStatus() != Retrospective.Status.OPEN) {
            throw new RetroflowException("Retrospective " + retrospective.getId()
                    + ": cannot modify a closed retrospective");
        }
    }

    private Retrospective requireRetrospective(Long retrospectiveId) {
        return retrospectiveRepository.findById(retrospectiveId)
                .orElseThrow(() -> new NotFoundException("Retrospective " + retrospectiveId + ": not found"));
    }

    private FeedbackItem findFeedbackItem(Long retrospectiveId, Long feedbackItemId) {
        return feedbackItemRepository.findByIdAndRetrospective_Id(feedbackItemId, retrospectiveId)
                .orElseThrow(() -> new NotFoundException("FeedbackItem " + feedbackItemId
                        + ": not found in Retrospective " + retrospectiveId));
    }
}
