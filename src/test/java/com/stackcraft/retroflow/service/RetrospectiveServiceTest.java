package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.dto.CreateRetrospectiveRequest;
import com.stackcraft.retroflow.dto.RetrospectiveResponse;
import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.entity.TeamMember;
import com.stackcraft.retroflow.exception.RetroflowException;
import jakarta.persistence.EntityNotFoundException;
import com.stackcraft.retroflow.repository.ActionItemRepository;
import com.stackcraft.retroflow.repository.FeedbackItemRepository;
import com.stackcraft.retroflow.repository.RetrospectiveRepository;
import com.stackcraft.retroflow.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrospectiveServiceTest {

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private RetrospectiveRepository retrospectiveRepository;
    @Mock
    private FeedbackItemRepository feedbackItemRepository;
    @Mock
    private ActionItemRepository actionItemRepository;

    private RetrospectiveService retrospectiveService;
    private Team team;

    @BeforeEach
    void setUp() {
        retrospectiveService = new RetrospectiveService(
            teamRepository, retrospectiveRepository, feedbackItemRepository, actionItemRepository);
        team = new Team();
        team.setId(23L);
        team.setName("Team-Alpha");
        team.setTeamMembers(List.of(new TeamMember(12L, "Alice", "alice@example.com", team)));
    }

    @Test
    void createRetrospective_setsOpenStatusAndTeamRelationship() {
        when(teamRepository.findById(23L)).thenReturn(Optional.of(team));
        when(retrospectiveRepository.existsByTeam_IdAndStatus(23L, Retrospective.Status.OPEN)).thenReturn(false);
        when(retrospectiveRepository.save(any(Retrospective.class))).thenAnswer(invocation -> {
            Retrospective retrospective = invocation.getArgument(0);
            retrospective.setId(41L);
            return retrospective;
        });

        Retrospective created = retrospectiveService.createRetrospective(23L, "Sprint 1");

        assertThat(created.getId()).isEqualTo(41L);
        assertThat(created.getTitle()).isEqualTo("Sprint 1");
        assertThat(created.getStatus()).isEqualTo(Retrospective.Status.OPEN);
        assertThat(created.getTeam()).usingRecursiveComparison().isEqualTo(team);
        verify(retrospectiveRepository).save(created);
    }

    @Test
    void createRetrospectiveRequest_defaultsDateToTodayAndStatusToOpen() {
        when(teamRepository.findById(23L)).thenReturn(Optional.of(team));
        when(retrospectiveRepository.existsByTeam_IdAndStatus(23L, Retrospective.Status.OPEN)).thenReturn(false);
        when(retrospectiveRepository.save(any(Retrospective.class))).thenAnswer(invocation -> {
            Retrospective retrospective = invocation.getArgument(0);
            retrospective.setId(41L);
            return retrospective;
        });

        RetrospectiveResponse created = retrospectiveService.createRetrospective(
            23L, new CreateRetrospectiveRequest("Sprint 1", null, "alice@example.com"));

        assertThat(created).isEqualTo(new RetrospectiveResponse(
            41L, "Sprint 1", LocalDate.now(), RetrospectiveStatus.OPEN, 23L));
    }

    @Test
    void createRetrospectiveRequest_acceptsPastAndFutureDates() {
        when(teamRepository.findById(23L)).thenReturn(Optional.of(team));
        when(retrospectiveRepository.existsByTeam_IdAndStatus(23L, Retrospective.Status.OPEN)).thenReturn(false);
        when(retrospectiveRepository.save(any(Retrospective.class))).thenAnswer(invocation -> {
            Retrospective retrospective = invocation.getArgument(0);
            retrospective.setId(41L);
            return retrospective;
        });

        for (LocalDate date : new LocalDate[] {
            LocalDate.now().minusDays(10), LocalDate.now(), LocalDate.now().plusDays(10)
        }) {
            RetrospectiveResponse created = retrospectiveService.createRetrospective(
                23L, new CreateRetrospectiveRequest("Sprint 1", date, "alice@example.com"));

            assertThat(created.date()).isEqualTo(date);
            assertThat(created.title()).isEqualTo("Sprint 1");
            assertThat(created.teamId()).isEqualTo(23L);
        }
    }

    @Test
    void createRetrospectiveRequest_acceptsTeamMemberEmail() {
        when(teamRepository.findById(23L)).thenReturn(Optional.of(team));
        when(retrospectiveRepository.existsByTeam_IdAndStatus(23L, Retrospective.Status.OPEN)).thenReturn(false);
        when(retrospectiveRepository.save(any(Retrospective.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RetrospectiveResponse created = retrospectiveService.createRetrospective(
            23L, new CreateRetrospectiveRequest("Sprint 1", LocalDate.of(2026, 10, 26), "ALICE@example.com"));

        assertThat(created.status()).isEqualTo(RetrospectiveStatus.OPEN);
        assertThat(created.date()).isEqualTo(LocalDate.of(2026, 10, 26));
        assertThat(created.teamId()).isEqualTo(23L);
    }

    @Test
    void createRetrospectiveRequest_rejectsEmailNotOnTeam() {
        when(teamRepository.findById(23L)).thenReturn(Optional.of(team));
        when(retrospectiveRepository.existsByTeam_IdAndStatus(23L, Retrospective.Status.OPEN)).thenReturn(false);

        assertThatThrownBy(() -> retrospectiveService.createRetrospective(
            23L, new CreateRetrospectiveRequest("Sprint 1", null, "stranger@example.com")))
            .isInstanceOf(RetroflowException.class);

        verify(retrospectiveRepository, never()).save(any(Retrospective.class));
    }

    @Test
    void createRetrospective_rejectsMissingTeam() {
        when(teamRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> retrospectiveService.createRetrospective(404L, "Sprint 1"))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Team 404: not found");
        verify(retrospectiveRepository, never()).save(any(Retrospective.class));
    }

    @Test
    void createRetrospective_rejectsBlankTitle() {
        when(teamRepository.findById(23L)).thenReturn(Optional.of(team));

        assertThatThrownBy(() -> retrospectiveService.createRetrospective(23L, " \t "))
            .isInstanceOf(RetroflowException.class)
            .hasMessageContaining("title is required");
        verify(retrospectiveRepository, never()).save(any(Retrospective.class));
    }

    @Test
    void createRetrospective_acceptsUnicodeTitle() {
        when(teamRepository.findById(23L)).thenReturn(Optional.of(team));
        when(retrospectiveRepository.existsByTeam_IdAndStatus(23L, Retrospective.Status.OPEN)).thenReturn(false);
        when(retrospectiveRepository.save(any(Retrospective.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Retrospective created = retrospectiveService.createRetrospective(23L, "振り返り");

        assertThat(created.getTitle()).isEqualTo("振り返り");
        assertThat(created.getTeam()).usingRecursiveComparison().isEqualTo(team);
        assertThat(created.getStatus()).isEqualTo(Retrospective.Status.OPEN);
    }

    @Test
    void createRetrospective_acceptsUnicodeTitleAt255Characters() {
        when(teamRepository.findById(23L)).thenReturn(Optional.of(team));
        when(retrospectiveRepository.existsByTeam_IdAndStatus(23L, Retrospective.Status.OPEN)).thenReturn(false);
        when(retrospectiveRepository.save(any(Retrospective.class))).thenAnswer(invocation -> invocation.getArgument(0));
        String title = "界".repeat(255);

        Retrospective created = retrospectiveService.createRetrospective(23L, title);

        assertThat(created.getTitle()).isEqualTo(title).hasSize(255);
        assertThat(created.getTeam()).usingRecursiveComparison().isEqualTo(team);
    }

    @Test
    void createRetrospective_rejectsTitlesLongerThan255Characters() {
        when(teamRepository.findById(23L)).thenReturn(Optional.of(team));

        assertThatThrownBy(() -> retrospectiveService.createRetrospective(23L, "A".repeat(256)))
            .isInstanceOf(RetroflowException.class);
        verify(retrospectiveRepository, never()).save(any(Retrospective.class));
    }

    @Test
    void createRetrospective_rejectsASecondOpenRetrospectiveForTheTeam() {
        when(teamRepository.findById(23L)).thenReturn(Optional.of(team));
        when(retrospectiveRepository.existsByTeam_IdAndStatus(23L, Retrospective.Status.OPEN)).thenReturn(true);

        assertThatThrownBy(() -> retrospectiveService.createRetrospective(23L, "Sprint 2"))
            .isInstanceOf(RetroflowException.class)
            .hasMessageContaining("already has an open retrospective");
        verify(retrospectiveRepository, never()).save(any(Retrospective.class));
    }

    @Test
    void closeRetrospective_closesAnOpenRetrospectiveWithoutItems() {
        Retrospective open = retrospective(Retrospective.Status.OPEN);
        when(retrospectiveRepository.findById(41L)).thenReturn(Optional.of(open));
        when(retrospectiveRepository.save(open)).thenReturn(open);

        Retrospective closed = retrospectiveService.closeRetrospective(41L);

        assertThat(closed).isSameAs(open);
        assertThat(closed.getId()).isEqualTo(41L);
        assertThat(closed.getTeam()).usingRecursiveComparison().isEqualTo(team);
        assertThat(closed.getStatus()).isEqualTo(Retrospective.Status.CLOSED);
        verify(retrospectiveRepository).save(open);
    }

    @Test
    void closeRetrospective_rejectsAnAlreadyClosedRetrospective() {
        when(retrospectiveRepository.findById(41L))
            .thenReturn(Optional.of(retrospective(Retrospective.Status.CLOSED)));

        assertThatThrownBy(() -> retrospectiveService.closeRetrospective(41L))
            .isInstanceOf(RetroflowException.class)
            .hasMessageContaining("already closed");
        verify(retrospectiveRepository, never()).save(any(Retrospective.class));
    }

    @Test
    void closeRetrospective_reportsMissingRetrospectiveAsNotFound() {
        when(retrospectiveRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> retrospectiveService.closeRetrospective(404L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Retrospective 404: retrospective not found");
        verify(retrospectiveRepository, never()).save(any(Retrospective.class));
    }

    @Test
    void addFeedbackItem_persistsAllFieldsAndParentRelationship() {
        when(retrospectiveRepository.findById(41L))
            .thenReturn(Optional.of(retrospective(Retrospective.Status.OPEN)));
        when(feedbackItemRepository.save(any(FeedbackItem.class))).thenAnswer(invocation -> {
            FeedbackItem item = invocation.getArgument(0);
            item.setId(52L);
            return item;
        });

        FeedbackItem created = retrospectiveService.addFeedbackItem(41L, "Clear communication", "WENT_WELL", "Alice");

        assertThat(created.getId()).isEqualTo(52L);
        assertThat(created.getContent()).isEqualTo("Clear communication");
        assertThat(created.getType()).isEqualTo("WENT_WELL");
        assertThat(created.getSubmittedBy()).isEqualTo("Alice");
        assertThat(created.getRetrospective().getId()).isEqualTo(41L);
        assertThat(created.getRetrospective().getTeam()).usingRecursiveComparison().isEqualTo(team);
    }

    @Test
    void addFeedbackItem_acceptsUnicodeContentAt5000Characters() {
        when(retrospectiveRepository.findById(41L))
            .thenReturn(Optional.of(retrospective(Retrospective.Status.OPEN)));
        when(feedbackItemRepository.save(any(FeedbackItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        String content = "改善".repeat(2500);

        FeedbackItem created = retrospectiveService.addFeedbackItem(41L, content, "WENT_WELL", "Alice");

        assertThat(created.getContent()).isEqualTo(content).hasSize(5000);
        assertThat(created.getRetrospective().getId()).isEqualTo(41L);
    }

    @Test
    void addFeedbackItem_rejectsClosedRetrospective() {
        when(retrospectiveRepository.findById(41L))
            .thenReturn(Optional.of(retrospective(Retrospective.Status.CLOSED)));

        assertThatThrownBy(() -> retrospectiveService.addFeedbackItem(41L, "Text", "WENT_WELL", "Alice"))
            .isInstanceOf(RetroflowException.class)
            .hasMessageContaining("closed retrospective");
        verify(feedbackItemRepository, never()).save(any(FeedbackItem.class));
    }

    @Test
    void addFeedbackItem_reportsMissingParentAsNotFound() {
        when(retrospectiveRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> retrospectiveService.addFeedbackItem(404L, "Text", "WENT_WELL", "Alice"))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Retrospective 404: retrospective not found");
        verify(feedbackItemRepository, never()).save(any(FeedbackItem.class));
    }

    @Test
    void addFeedbackItem_rejectsBlankContent() {
        when(retrospectiveRepository.findById(41L))
            .thenReturn(Optional.of(retrospective(Retrospective.Status.OPEN)));

        assertThatThrownBy(() -> retrospectiveService.addFeedbackItem(41L, " ", "WENT_WELL", "Alice"))
            .isInstanceOf(RetroflowException.class);

        verify(feedbackItemRepository, never()).save(any(FeedbackItem.class));
    }

    @Test
    void addFeedbackItem_rejectsContentOver5000Characters() {
        when(retrospectiveRepository.findById(41L))
            .thenReturn(Optional.of(retrospective(Retrospective.Status.OPEN)));

        assertThatThrownBy(() -> retrospectiveService.addFeedbackItem(41L, "A".repeat(5001), "WENT_WELL", "Alice"))
            .isInstanceOf(RetroflowException.class);

        verify(feedbackItemRepository, never()).save(any(FeedbackItem.class));
    }

    @Test
    void addFeedbackItem_rejectsUnknownType() {
        when(retrospectiveRepository.findById(41L))
            .thenReturn(Optional.of(retrospective(Retrospective.Status.OPEN)));

        assertThatThrownBy(() -> retrospectiveService.addFeedbackItem(41L, "Text", "UNKNOWN", "Alice"))
            .isInstanceOf(RetroflowException.class);

        verify(feedbackItemRepository, never()).save(any(FeedbackItem.class));
    }

    @Test
    void addFeedbackItem_rejectsMalformedSubmitterEmail() {
        when(retrospectiveRepository.findById(41L))
            .thenReturn(Optional.of(retrospective(Retrospective.Status.OPEN)));

        assertThatThrownBy(() -> retrospectiveService.addFeedbackItem(41L, "Text", "WENT_WELL", "not-an-email"))
            .isInstanceOf(RetroflowException.class);

        verify(feedbackItemRepository, never()).save(any(FeedbackItem.class));
    }

    @Test
    void addFeedbackItem_acceptsDefinedTypesWithDifferentCasing() {
        when(retrospectiveRepository.findById(41L))
            .thenReturn(Optional.of(retrospective(Retrospective.Status.OPEN)));
        when(feedbackItemRepository.save(any(FeedbackItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        for (String type : new String[] {
            "WENT_WELL", "went_well", "NEEDS_IMPROVEMENT", "needs_improvement", "ACTION_ITEM", "action_item"
        }) {
            FeedbackItem created = retrospectiveService.addFeedbackItem(41L, "Useful feedback", type, "Alice");

            assertThat(created.getType()).isEqualTo(type);
            assertThat(created.getContent()).isEqualTo("Useful feedback");
            assertThat(created.getRetrospective().getId()).isEqualTo(41L);
        }

        verify(feedbackItemRepository, org.mockito.Mockito.times(6)).save(any(FeedbackItem.class));
    }

    @Test
    void addActionItem_persistsAllFieldsAndStartsIncomplete() {
        when(retrospectiveRepository.findById(41L))
            .thenReturn(Optional.of(retrospective(Retrospective.Status.OPEN)));
        when(actionItemRepository.save(any(ActionItem.class))).thenAnswer(invocation -> {
            ActionItem item = invocation.getArgument(0);
            item.setId(63L);
            return item;
        });

        ActionItem created = retrospectiveService.addActionItem(41L, "Fix the build", "HIGH", "Alice");

        assertThat(created.getId()).isEqualTo(63L);
        assertThat(created.getDescription()).isEqualTo("Fix the build");
        assertThat(created.getPriority()).isEqualTo("HIGH");
        assertThat(created.getAssignee()).isEqualTo("Alice");
        assertThat(created.isCompleted()).isFalse();
        assertThat(created.getRetrospective().getId()).isEqualTo(41L);
        assertThat(created.getRetrospective().getTeam()).usingRecursiveComparison().isEqualTo(team);
    }

    @Test
    void addActionItem_requiresPriority() {
        when(retrospectiveRepository.findById(41L))
            .thenReturn(Optional.of(retrospective(Retrospective.Status.OPEN)));

        assertThatThrownBy(() -> retrospectiveService.addActionItem(41L, "Fix the build", null, "Alice"))
            .isInstanceOf(RetroflowException.class);

        verify(actionItemRepository, never()).save(any(ActionItem.class));
    }

    @Test
    void addActionItem_rejectsUnknownPriority() {
        when(retrospectiveRepository.findById(41L))
            .thenReturn(Optional.of(retrospective(Retrospective.Status.OPEN)));

        assertThatThrownBy(() -> retrospectiveService.addActionItem(41L, "Fix the build", "URGENT", "Alice"))
            .isInstanceOf(RetroflowException.class);

        verify(actionItemRepository, never()).save(any(ActionItem.class));
    }

    @Test
    void addActionItem_acceptsDefinedPrioritiesWithDifferentCasing() {
        when(retrospectiveRepository.findById(41L))
            .thenReturn(Optional.of(retrospective(Retrospective.Status.OPEN)));
        when(actionItemRepository.save(any(ActionItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        for (String priority : new String[] {"LOW", "low", "MEDIUM", "medium", "HIGH", "high"}) {
            ActionItem created = retrospectiveService.addActionItem(41L, "Fix the build", priority, "Alice");

            assertThat(created.getPriority()).isEqualTo(priority);
            assertThat(created.getDescription()).isEqualTo("Fix the build");
            assertThat(created.getRetrospective().getId()).isEqualTo(41L);
        }

        verify(actionItemRepository, org.mockito.Mockito.times(6)).save(any(ActionItem.class));
    }

    @Test
    void completeActionItem_isAllowedAfterItsRetrospectiveIsClosed() {
        Retrospective closed = retrospective(Retrospective.Status.CLOSED);
        ActionItem item = actionItem(closed, false);
        when(actionItemRepository.findById(63L)).thenReturn(Optional.of(item));
        when(actionItemRepository.save(item)).thenReturn(item);

        ActionItem completed = retrospectiveService.completeActionItem(63L);

        assertThat(completed).isSameAs(item);
        assertThat(completed.isCompleted()).isTrue();
        assertThat(completed.getRetrospective().getStatus()).isEqualTo(Retrospective.Status.CLOSED);
        assertThat(completed.getRetrospective().getTeam()).usingRecursiveComparison().isEqualTo(team);
        verify(actionItemRepository).save(item);
    }

    @Test
    void uncompleteActionItem_rejectsCompletedItems() {
        when(actionItemRepository.findById(63L))
            .thenReturn(Optional.of(actionItem(retrospective(Retrospective.Status.OPEN), true)));

        assertThatThrownBy(() -> retrospectiveService.uncompleteActionItem(63L))
            .isInstanceOf(RetroflowException.class)
            .hasMessageContaining("cannot be uncompleted");
        verify(actionItemRepository, never()).save(any(ActionItem.class));
    }

    private Retrospective retrospective(Retrospective.Status status) {
        Retrospective retrospective = new Retrospective();
        retrospective.setId(41L);
        retrospective.setTitle("Sprint 1");
        retrospective.setTeam(team);
        retrospective.setStatus(status);
        return retrospective;
    }

    private ActionItem actionItem(Retrospective retrospective, boolean completed) {
        ActionItem item = new ActionItem();
        item.setId(63L);
        item.setDescription("Fix the build");
        item.setPriority("HIGH");
        item.setAssignee("Alice");
        item.setRetrospective(retrospective);
        item.setCompleted(completed);
        return item;
    }
}
