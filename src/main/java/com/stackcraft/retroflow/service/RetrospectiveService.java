package com.stackcraft.retroflow.service;

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

@Service
public class RetrospectiveService {

    private final TeamRepository teamRepository;
    private final RetrospectiveRepository retrospectiveRepository;
    private final FeedbackItemRepository feedbackItemRepository;
    private final ActionItemRepository actionItemRepository;

    public RetrospectiveService(
            TeamRepository teamRepository,
            RetrospectiveRepository retrospectiveRepository,
            FeedbackItemRepository feedbackItemRepository,
            ActionItemRepository actionItemRepository) {
        this.teamRepository = teamRepository;
        this.retrospectiveRepository = retrospectiveRepository;
        this.feedbackItemRepository = feedbackItemRepository;
        this.actionItemRepository = actionItemRepository;
    }

    @Transactional
    public Retrospective createRetrospective(Long teamId, String title) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RetroflowException("Team not found"));
        if (!retrospectiveRepository.findByTeam_IdAndStatus(teamId, "OPEN").isEmpty()) {
            throw new RetroflowException("A team cannot have two open retrospectives");
        }

        Retrospective retrospective = new Retrospective();
        retrospective.setTeam(team);
        retrospective.setTitle(title);
        retrospective.setStatus("OPEN");
        return retrospectiveRepository.save(retrospective);
    }

    @Transactional
    public Retrospective closeRetrospective(Long retrospectiveId) {
        Retrospective retrospective = getRetrospective(retrospectiveId);
        retrospective.setStatus("CLOSED");
        return retrospectiveRepository.save(retrospective);
    }

    @Transactional
    public FeedbackItem addFeedbackItem(Long retrospectiveId, String content, String type, String submittedBy) {
        Retrospective retrospective = getOpenRetrospective(retrospectiveId);
        FeedbackItem item = new FeedbackItem();
        item.setRetrospective(retrospective);
        item.setContent(content);
        item.setType(type);
        item.setSubmittedBy(submittedBy);
        return feedbackItemRepository.save(item);
    }

    @Transactional
    public ActionItem addActionItem(Long retrospectiveId, String content, String priority, String assignee) {
        Retrospective retrospective = getOpenRetrospective(retrospectiveId);
        ActionItem item = new ActionItem();
        item.setRetrospective(retrospective);
        item.setContent(content);
        item.setPriority(priority);
        item.setAssignee(assignee);
        return actionItemRepository.save(item);
    }

    @Transactional
    public ActionItem completeActionItem(Long actionItemId) {
        ActionItem item = getActionItem(actionItemId);
        item.setCompleted(true);
        return actionItemRepository.save(item);
    }

    @Transactional
    public ActionItem uncompleteActionItem(Long actionItemId) {
        ActionItem item = getActionItem(actionItemId);
        if (item.isCompleted()) {
            throw new RetroflowException("A completed action item cannot be uncompleted");
        }
        item.setCompleted(false);
        return actionItemRepository.save(item);
    }

    private Retrospective getOpenRetrospective(Long retrospectiveId) {
        Retrospective retrospective = getRetrospective(retrospectiveId);
        if (!"OPEN".equals(retrospective.getStatus())) {
            throw new RetroflowException("Cannot modify a closed retrospective");
        }
        return retrospective;
    }

    private Retrospective getRetrospective(Long retrospectiveId) {
        return retrospectiveRepository.findById(retrospectiveId)
                .orElseThrow(() -> new RetroflowException("Retrospective not found"));
    }

    private ActionItem getActionItem(Long actionItemId) {
        return actionItemRepository.findById(actionItemId)
                .orElseThrow(() -> new RetroflowException("Action item not found"));
    }
}