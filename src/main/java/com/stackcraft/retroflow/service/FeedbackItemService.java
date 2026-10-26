package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.dto.CreateFeedbackItemRequest;
import com.stackcraft.retroflow.dto.FeedbackItemResponse;
import com.stackcraft.retroflow.dto.PagedResponse;
import com.stackcraft.retroflow.dto.PagingInfo;
import com.stackcraft.retroflow.dto.TeamMemberResponse;
import com.stackcraft.retroflow.dto.UpdateFeedbackItemRequest;
import com.stackcraft.retroflow.entity.ActionPriority;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.FeedbackType;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.TeamMember;
import com.stackcraft.retroflow.exception.NotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.FeedbackItemRepository;
import com.stackcraft.retroflow.repository.RetrospectiveRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FeedbackItemService {

    private static final String CLOSED = "Cannot modify a closed retrospective";

    private final RetrospectiveRepository retrospectiveRepository;
    private final FeedbackItemRepository feedbackItemRepository;

    public FeedbackItemService(RetrospectiveRepository retrospectiveRepository,
                               FeedbackItemRepository feedbackItemRepository) {
        this.retrospectiveRepository = retrospectiveRepository;
        this.feedbackItemRepository = feedbackItemRepository;
    }

    public FeedbackItemResponse createFeedbackItem(Long retrospectiveId, CreateFeedbackItemRequest request) {
        Retrospective retrospective = findRetrospective(retrospectiveId);
        TeamMember submitter = requireMember(retrospective, request.email());
        requireOpen(retrospective);

        FeedbackItem item = new FeedbackItem();
        item.setRetrospective(retrospective);
        item.setContent(request.content());
        item.setType(request.type().name());
        item.setSubmitter(submitter);
        item.setSubmittedBy(submitter.getName());
        if (request.type() == FeedbackType.ACTION_ITEM) {
            item.setPriority(request.priority());
        }
        return toResponse(feedbackItemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public PagedResponse<FeedbackItemResponse> listFeedbackItems(
            Long retrospectiveId, ActionPriority priority, Boolean completed, int pageNumber, int pageSize) {
        findRetrospective(retrospectiveId);
        boolean actionItemsOnly = priority != null || completed != null;
        Page<FeedbackItem> page = feedbackItemRepository.search(retrospectiveId, actionItemsOnly, priority,
            completed, PageRequest.of(pageNumber, pageSize, Sort.by("id")));
        return new PagedResponse<>(page.map(FeedbackItemService::toResponse).getContent(),
            new PagingInfo((int) page.getTotalElements()));
    }

    @Transactional(readOnly = true)
    public FeedbackItemResponse getFeedbackItem(Long retrospectiveId, Long feedbackItemId) {
        findRetrospective(retrospectiveId);
        return toResponse(findItem(retrospectiveId, feedbackItemId));
    }

    public FeedbackItemResponse updateFeedbackItem(
            Long retrospectiveId, Long feedbackItemId, UpdateFeedbackItemRequest request) {
        Retrospective retrospective = findRetrospective(retrospectiveId);
        FeedbackItem item = findItem(retrospectiveId, feedbackItemId);
        requireMember(retrospective, request.email());

        if (isActionItem(item)) {
            // Incomplete action items stay editable after the retrospective closes.
            if (item.isCompleted()) {
                throw new RetroflowException("A completed action item cannot be edited");
            }
            if (request.priority() != null) {
                item.setPriority(request.priority());
            }
        } else {
            requireOpen(retrospective);
        }
        item.setContent(request.content());
        return toResponse(feedbackItemRepository.save(item));
    }

    public void deleteFeedbackItem(Long retrospectiveId, Long feedbackItemId, String email) {
        Retrospective retrospective = findRetrospective(retrospectiveId);
        FeedbackItem item = findItem(retrospectiveId, feedbackItemId);
        requireMember(retrospective, email);
        requireOpen(retrospective);
        feedbackItemRepository.delete(item);
    }

    public FeedbackItemResponse completeActionItem(Long retrospectiveId, Long feedbackItemId, String email) {
        Retrospective retrospective = findRetrospective(retrospectiveId);
        FeedbackItem item = findItem(retrospectiveId, feedbackItemId);
        requireMember(retrospective, email);
        if (!isActionItem(item)) {
            throw new RetroflowException("Only action items can be completed");
        }
        if (item.isCompleted()) {
            throw new RetroflowException("Action item is already completed");
        }
        item.setCompleted(true);
        return toResponse(feedbackItemRepository.save(item));
    }

    private Retrospective findRetrospective(Long id) {
        return retrospectiveRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Retrospective not found: " + id));
    }

    private FeedbackItem findItem(Long retrospectiveId, Long feedbackItemId) {
        FeedbackItem item = feedbackItemRepository.findById(feedbackItemId)
            .orElseThrow(() -> new NotFoundException("Feedback item not found: " + feedbackItemId));
        if (!item.getRetrospective().getId().equals(retrospectiveId)) {
            throw new NotFoundException(
                "Feedback item " + feedbackItemId + " not found in retrospective " + retrospectiveId);
        }
        return item;
    }

    private static TeamMember requireMember(Retrospective retrospective, String email) {
        return retrospective.getTeam().findMemberByEmail(email)
            .orElseThrow(() -> new RetroflowException(RetrospectiveService.NOT_A_MEMBER));
    }

    private static void requireOpen(Retrospective retrospective) {
        if (retrospective.getStatus() != Retrospective.Status.OPEN) {
            throw new RetroflowException(CLOSED);
        }
    }

    private static boolean isActionItem(FeedbackItem item) {
        return FeedbackType.ACTION_ITEM.name().equals(item.getType());
    }

    private static FeedbackItemResponse toResponse(FeedbackItem item) {
        TeamMember submitter = item.getSubmitter();
        TeamMemberResponse submittedBy = submitter == null ? null
            : new TeamMemberResponse(submitter.getId(), submitter.getName(), submitter.getEmail());
        boolean action = isActionItem(item);
        return new FeedbackItemResponse(item.getId(), item.getRetrospective().getId(), item.getContent(),
            FeedbackType.valueOf(item.getType()), submittedBy,
            action ? item.getPriority() : null, action ? item.isCompleted() : null);
    }
}
