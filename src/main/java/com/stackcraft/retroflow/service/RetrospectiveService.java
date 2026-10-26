package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.dto.CreateRetrospectiveRequest;
import com.stackcraft.retroflow.dto.RetrospectiveResponse;
import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.ActionItemRepository;
import com.stackcraft.retroflow.repository.FeedbackItemRepository;
import com.stackcraft.retroflow.repository.RetrospectiveRepository;
import com.stackcraft.retroflow.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
            .orElseThrow(() -> new RetroflowException("Team not found: " + teamId));
        if (retrospectiveRepository.existsByTeam_IdAndStatus(teamId, Retrospective.Status.OPEN)) {
            throw new RetroflowException("Team already has an open retrospective");
        }
        if (title == null || title.isBlank()) {
            throw new RetroflowException("Retrospective title is required");
        }

        Retrospective retrospective = new Retrospective();
        retrospective.setTeam(team);
        retrospective.setTitle(title);
        retrospective.setStatus(Retrospective.Status.OPEN);
        return retrospectiveRepository.save(retrospective);
    }

    public Retrospective closeRetrospective(Long retrospectiveId) {
        Retrospective retrospective = findRetrospective(retrospectiveId);
        if (retrospective.getStatus() == Retrospective.Status.CLOSED) {
            throw new RetroflowException("Retrospective is already closed");
        }
        retrospective.setStatus(Retrospective.Status.CLOSED);
        return retrospectiveRepository.save(retrospective);
    }

    public FeedbackItem addFeedbackItem(Long retrospectiveId, String content, String type, String submittedBy) {
        Retrospective retrospective = requireOpenRetrospective(retrospectiveId);
        FeedbackItem item = new FeedbackItem();
        item.setRetrospective(retrospective);
        item.setContent(content);
        item.setType(type);
        item.setSubmittedBy(submittedBy);
        return feedbackItemRepository.save(item);
    }

    public ActionItem addActionItem(Long retrospectiveId, String description, String priority, String assignee) {
        Retrospective retrospective = requireOpenRetrospective(retrospectiveId);
        ActionItem item = new ActionItem();
        item.setRetrospective(retrospective);
        item.setDescription(description);
        item.setPriority(priority);
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
            throw new RetroflowException("A completed action item cannot be uncompleted");
        }
        item.setCompleted(false);
        return actionItemRepository.save(item);
    }

    public RetrospectiveResponse createRetrospective(Long teamId, CreateRetrospectiveRequest request) {
        throw new UnsupportedOperationException("Retrospective creation has not been implemented yet");
    }

    public List<RetrospectiveResponse> getRetrospectivesForTeam(Long teamId) {
        throw new UnsupportedOperationException("Retrospective retrieval has not been implemented yet");
    }

    private Retrospective requireOpenRetrospective(Long retrospectiveId) {
        Retrospective retrospective = findRetrospective(retrospectiveId);
        if (retrospective.getStatus() != Retrospective.Status.OPEN) {
            throw new RetroflowException("Cannot modify a closed retrospective");
        }
        return retrospective;
    }

    private Retrospective findRetrospective(Long retrospectiveId) {
        return retrospectiveRepository.findById(retrospectiveId)
            .orElseThrow(() -> new RetroflowException("Retrospective not found: " + retrospectiveId));
    }

    private ActionItem findActionItem(Long actionItemId) {
        return actionItemRepository.findById(actionItemId)
            .orElseThrow(() -> new RetroflowException("Action item not found: " + actionItemId));
    }
}