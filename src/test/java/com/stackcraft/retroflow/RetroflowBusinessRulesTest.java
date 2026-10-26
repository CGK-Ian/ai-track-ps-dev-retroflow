package com.stackcraft.retroflow;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.service.RetrospectiveService;
import com.stackcraft.retroflow.service.TeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * These tests were written by the previous vendor.
 * They describe the core business rules RetroFlow must enforce.
 *
 * All three tests currently FAIL because the service methods they call
 * do not exist yet. Your job is to make them pass — without changing
 * the test logic itself.
 *
 * Do not modify the test assertions. Do not skip or disable tests.
 * If a test feels wrong, raise it — but don't quietly remove it.
 */
@SpringBootTest
class RetroflowBusinessRulesTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private RetrospectiveService retrospectiveService;

    @Test
    void closingARetrospective_preventsAddingFeedback() {
        // Arrange
        Team team = teamService.createTeam("Team Alpha", java.util.List.of("Alice", "Bob"));
        Retrospective retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 1 Retro");

        // Act
        retrospectiveService.closeRetrospective(retro.getId());

        // Assert
        assertThatThrownBy(() ->
            retrospectiveService.addFeedbackItem(retro.getId(), "It went well", "WENT_WELL", "Alice")
        ).isInstanceOf(com.stackcraft.retroflow.exception.RetroflowException.class);
    }

    @Test
    void aTeam_cannotHaveTwoOpenRetrospectives() {
        // Arrange
        Team team = teamService.createTeam("Team Beta", java.util.List.of("Carol", "Dave"));
        retrospectiveService.createRetrospective(team.getId(), "Sprint 1 Retro");

        // Act & Assert
        assertThatThrownBy(() ->
            retrospectiveService.createRetrospective(team.getId(), "Sprint 2 Retro")
        ).isInstanceOf(com.stackcraft.retroflow.exception.RetroflowException.class);
    }

    @Test
    void completedActionItem_cannotBeUncompleted() {
        // Arrange
        Team team = teamService.createTeam("Team Gamma", java.util.List.of("Eve", "Frank"));
        Retrospective retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 1 Retro");
        ActionItem actionItem = retrospectiveService.addActionItem(retro.getId(), "Fix the build", "HIGH", "Eve");

        // Act
        retrospectiveService.completeActionItem(actionItem.getId());

        // Assert
        assertThatThrownBy(() ->
            retrospectiveService.uncompleteActionItem(actionItem.getId())
        ).isInstanceOf(com.stackcraft.retroflow.exception.RetroflowException.class);
    }

    @Test
    void createdItems_retainTheirDetails() {
        Team team = teamService.createTeam("Team Delta", java.util.List.of("Grace", "Heidi"));
        Retrospective retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 3 Retro");

        assertThat(team.getName()).isEqualTo("Team Delta");
        assertThat(team.getMembers()).containsExactly("Grace", "Heidi");
        assertThat(retro.getTitle()).isEqualTo("Sprint 3 Retro");
        assertThat(retro.getTeam().getId()).isEqualTo(team.getId());

        FeedbackItem feedback = retrospectiveService.addFeedbackItem(
            retro.getId(), "Clear communication", "WENT_WELL", "Grace");
        assertThat(feedback.getId()).isNotNull();
        assertThat(feedback.getContent()).isEqualTo("Clear communication");
        assertThat(feedback.getType()).isEqualTo("WENT_WELL");
        assertThat(feedback.getSubmittedBy()).isEqualTo("Grace");
        assertThat(feedback.getRetrospective().getId()).isEqualTo(retro.getId());

        ActionItem actionItem = retrospectiveService.addActionItem(
            retro.getId(), "Document the release", "MEDIUM", "Heidi");
        assertThat(actionItem.getId()).isNotNull();
        assertThat(actionItem.getDescription()).isEqualTo("Document the release");
        assertThat(actionItem.getPriority()).isEqualTo("MEDIUM");
        assertThat(actionItem.getAssignee()).isEqualTo("Heidi");
        assertThat(actionItem.isCompleted()).isFalse();
        assertThat(actionItem.getRetrospective().getId()).isEqualTo(retro.getId());
    }

}
