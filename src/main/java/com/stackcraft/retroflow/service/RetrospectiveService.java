package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.dto.CreateRetrospectiveRequest;
import com.stackcraft.retroflow.dto.PagedResponse;
import com.stackcraft.retroflow.dto.PagingInfo;
import com.stackcraft.retroflow.dto.RetrospectiveResponse;
import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.NotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.ActionItemRepository;
import com.stackcraft.retroflow.repository.FeedbackItemRepository;
import com.stackcraft.retroflow.repository.RetrospectiveRepository;
import com.stackcraft.retroflow.repository.TeamRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class RetrospectiveService {

    private final TeamRepository teamRepository;
    private final RetrospectiveRepository retrospectiveRepository;
    private final FeedbackItemRepository feedbackItemRepository;
    private final ActionItemRepository actionItemRepository;

    public RetrospectiveService(TeamRepository teamRepository,
                                RetrospectiveRepository retrospectiveRepository,
                                FeedbackItemRepository feedbackItemRepository,
                                ActionItemRepository actionItemRepository) {
        this.teamRepository = teamRepository;
        this.retrospectiveRepository = retrospectiveRepository;
        this.feedbackItemRepository = feedbackItemRepository;
        this.actionItemRepository = actionItemRepository;
    }

    public Retrospective createRetrospective(Long teamId, String title) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team " + teamId + ": not found"));
        validateNewRetrospective(team, title);

        Retrospective retrospective = new Retrospective();
        retrospective.setTeam(team);
        retrospective.setTitle(title);
        retrospective.setDate(LocalDate.now());
        retrospective.setStatus(Retrospective.Status.OPEN);
        return retrospectiveRepository.save(retrospective);
    }

    public Retrospective closeRetrospective(Long retrospectiveId) {
        return closeRetrospectiveRecord(retrospectiveId);
    }

    public void closeRetrospective(Long retrospectiveId, String email) {
        Retrospective retrospective = findRetrospective(retrospectiveId);
        requireTeamMember(retrospective.getTeam(), email);
        closeRetrospectiveRecord(retrospective);
    }

    public FeedbackItem addFeedbackItem(Long retrospectiveId, String content, String type, String submittedBy) {
        Retrospective retrospective = requireOpenRetrospective(retrospectiveId);
        validateFeedback(content, type);
        validateSubmitter(retrospectiveId, submittedBy);
        FeedbackItem item = new FeedbackItem();
        item.setRetrospective(retrospective);
        item.setContent(content);
        item.setType(type);
        item.setSubmittedBy(submittedBy);
        return feedbackItemRepository.save(item);
    }

    public ActionItem addActionItem(Long retrospectiveId, String description, String priority, String assignee) {
        Retrospective retrospective = requireOpenRetrospective(retrospectiveId);
        if (description == null || description.isBlank()) {
            throw new RetroflowException("ActionItem new: description is required");
        }
        ActionItem item = new ActionItem();
        item.setRetrospective(retrospective);
        item.setDescription(description);
        item.setPriority(normalizePriority(retrospectiveId, priority));
        item.setAssignee(assignee);
        return actionItemRepository.save(item);
    }

    public ActionItem completeActionItem(Long actionItemId) {
        ActionItem item = findActionItem(actionItemId);
        item.setCompleted(true);
        return actionItemRepository.save(item);
    }

    public ActionItem uncompleteActionItem(Long actionItemId) {
        ActionItem item = findActionItem(actionItemId);
        if (item.isCompleted()) {
            throw new RetroflowException("ActionItem " + actionItemId + ": cannot be uncompleted");
        }
        item.setCompleted(false);
        return actionItemRepository.save(item);
    }

    public RetrospectiveResponse createRetrospective(Long teamId, CreateRetrospectiveRequest request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team " + teamId + ": not found"));
        validateNewRetrospective(team, request.title());
        requireTeamMember(team, request.email());

        Retrospective retrospective = new Retrospective();
        retrospective.setTeam(team);
        retrospective.setTitle(request.title());
        retrospective.setDate(request.date() == null ? LocalDate.now() : request.date());
        retrospective.setStatus(Retrospective.Status.OPEN);
        return toResponse(retrospectiveRepository.save(retrospective));
    }

    @Transactional(readOnly = true)
    public PagedResponse<RetrospectiveResponse> getRetrospectivesForTeam(
            Long teamId, String email, int pageNumber, int pageSize) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Team " + teamId + ": not found"));
        requireTeamMember(team, email);
        Page<Retrospective> page = retrospectiveRepository.findByTeam_Id(teamId, PageRequest.of(pageNumber, pageSize));
        return toPage(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<RetrospectiveResponse> queryRetrospectives(
            Long id, String title, RetrospectiveStatus status, int pageNumber, int pageSize) {
        Retrospective.Status entityStatus = status == null ? null : Retrospective.Status.valueOf(status.name());
        return toPage(retrospectiveRepository.search(id, title, entityStatus, PageRequest.of(pageNumber, pageSize)));
    }

    @Transactional(readOnly = true)
    public RetrospectiveResponse getRetrospective(Long retrospectiveId) {
        return toResponse(findRetrospective(retrospectiveId));
    }

    private void validateNewRetrospective(Team team, String title) {
        if (title == null || title.isBlank()) {
            throw new RetroflowException("Retrospective new: title is required");
        }
        if (title.length() > 255) {
            throw new RetroflowException("Retrospective new: title cannot exceed 255 characters");
        }
        if (retrospectiveRepository.existsByTeam_IdAndStatus(team.getId(), Retrospective.Status.OPEN)) {
            throw new RetroflowException("Team " + team.getId() + ": already has an open retrospective");
        }
    }

    private void validateFeedback(String content, String type) {
        if (content == null || content.isBlank() || content.length() > 5000) {
            throw new RetroflowException("FeedbackItem new: content must be nonblank and at most 5000 characters");
        }
        try {
            com.stackcraft.retroflow.entity.FeedbackType.valueOf(type.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new RetroflowException("FeedbackItem new: type is not supported");
        }
    }

    private void validateSubmitter(Long retrospectiveId, String submitter) {
        if (submitter == null || submitter.isBlank()
                || (!submitter.matches("[A-Za-z]+")
                && !submitter.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
            throw new RetroflowException("FeedbackItem new: submitter must be a member email");
        }
    }

    private String normalizePriority(Long actionItemId, String priority) {
        if (priority == null || priority.isBlank()) {
            throw new RetroflowException("ActionItem " + actionItemId + ": priority is required");
        }
        try {
            com.stackcraft.retroflow.entity.ActionPriority.valueOf(priority.toUpperCase(Locale.ROOT));
            return priority;
        } catch (IllegalArgumentException exception) {
            throw new RetroflowException("ActionItem " + actionItemId + ": priority is not supported");
        }
    }

    private Retrospective requireOpenRetrospective(Long retrospectiveId) {
        Retrospective retrospective = findRetrospective(retrospectiveId);
        if (retrospective.getStatus() != Retrospective.Status.OPEN) {
            throw new RetroflowException("Retrospective " + retrospectiveId + ": cannot modify a closed retrospective");
        }
        return retrospective;
    }

    private void requireTeamMember(Team team, String email) {
        if (email == null || team.getTeamMembers().stream()
                .noneMatch(member -> member.getEmail().equalsIgnoreCase(email))) {
            throw new RetroflowException("Team " + team.getId() + ": email is not a member of the team");
        }
    }

    private Retrospective closeRetrospectiveRecord(Long retrospectiveId) {
        return closeRetrospectiveRecord(findRetrospective(retrospectiveId));
    }

    private Retrospective closeRetrospectiveRecord(Retrospective retrospective) {
        if (retrospective.getStatus() == Retrospective.Status.CLOSED) {
            throw new RetroflowException("Retrospective " + retrospective.getId() + ": already closed");
        }
        retrospective.setStatus(Retrospective.Status.CLOSED);
        return retrospectiveRepository.save(retrospective);
    }

    private Retrospective findRetrospective(Long retrospectiveId) {
        return retrospectiveRepository.findById(retrospectiveId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Retrospective " + retrospectiveId + ": retrospective not found"));
    }

    private ActionItem findActionItem(Long actionItemId) {
        return actionItemRepository.findById(actionItemId)
                .orElseThrow(() -> new EntityNotFoundException("ActionItem " + actionItemId + ": not found"));
    }

    private RetrospectiveResponse toResponse(Retrospective retrospective) {
        RetrospectiveStatus status = RetrospectiveStatus.valueOf(retrospective.getStatus().name());
        return new RetrospectiveResponse(retrospective.getId(), retrospective.getTitle(), retrospective.getDate(),
                status, retrospective.getTeam().getId());
    }

    private PagedResponse<RetrospectiveResponse> toPage(Page<Retrospective> page) {
        List<RetrospectiveResponse> items = page.getContent().stream().map(this::toResponse).toList();
        return new PagedResponse<>(items, new PagingInfo(Math.toIntExact(page.getTotalElements())));
    }
}
