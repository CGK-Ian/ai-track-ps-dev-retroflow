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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
public class RetrospectiveService {

    static final String NOT_A_MEMBER = "Email is not a member of the team";

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
        Team team = findTeam(teamId);
        team.findMemberByEmail(request.email())
            .orElseThrow(() -> new RetroflowException(NOT_A_MEMBER));
        if (retrospectiveRepository.existsByTeam_IdAndStatus(teamId, Retrospective.Status.OPEN)) {
            throw new RetroflowException("Team already has an open retrospective");
        }

        Retrospective retrospective = new Retrospective();
        retrospective.setTeam(team);
        retrospective.setTitle(request.title());
        retrospective.setDate(request.date() != null ? request.date() : LocalDate.now());
        retrospective.setStatus(Retrospective.Status.OPEN);
        return toResponse(retrospectiveRepository.save(retrospective));
    }

    @Transactional(readOnly = true)
    public PagedResponse<RetrospectiveResponse> getRetrospectivesForTeam(
            Long teamId, String email, int pageNumber, int pageSize) {
        findTeam(teamId).findMemberByEmail(email)
            .orElseThrow(() -> new RetroflowException(NOT_A_MEMBER));
        return toPage(retrospectiveRepository.findByTeam_Id(teamId, pageable(pageNumber, pageSize)));
    }

    @Transactional(readOnly = true)
    public PagedResponse<RetrospectiveResponse> queryRetrospectives(
            Long id, String title, RetrospectiveStatus status, int pageNumber, int pageSize) {
        String titlePattern = "%" + (title == null ? "" : escapeLike(title.trim().toLowerCase())) + "%";
        Retrospective.Status entityStatus = status == null ? null : Retrospective.Status.valueOf(status.name());
        return toPage(retrospectiveRepository.search(id, titlePattern, entityStatus, pageable(pageNumber, pageSize)));
    }

    @Transactional(readOnly = true)
    public RetrospectiveResponse getRetrospective(Long id) {
        return toResponse(loadRetrospective(id));
    }

    public void closeRetrospective(Long retrospectiveId, String email) {
        Retrospective retrospective = loadRetrospective(retrospectiveId);
        retrospective.getTeam().findMemberByEmail(email)
            .orElseThrow(() -> new RetroflowException(NOT_A_MEMBER));
        closeRetrospective(retrospectiveId);
    }

    private Team findTeam(Long teamId) {
        return teamRepository.findById(teamId)
            .orElseThrow(() -> new NotFoundException("Team not found: " + teamId));
    }

    private Retrospective loadRetrospective(Long id) {
        return retrospectiveRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Retrospective not found: " + id));
    }

    private static PageRequest pageable(int pageNumber, int pageSize) {
        return PageRequest.of(pageNumber, pageSize, Sort.by("id"));
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static PagedResponse<RetrospectiveResponse> toPage(Page<Retrospective> page) {
        return new PagedResponse<>(page.map(RetrospectiveService::toResponse).getContent(),
            new PagingInfo((int) page.getTotalElements()));
    }

    private static RetrospectiveResponse toResponse(Retrospective retrospective) {
        return new RetrospectiveResponse(retrospective.getId(), retrospective.getTitle(), retrospective.getDate(),
            RetrospectiveStatus.valueOf(retrospective.getStatus().name()), retrospective.getTeam().getId());
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