package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.dto.CreateFeedbackItemRequest;
import com.stackcraft.retroflow.dto.CreateRetrospectiveRequest;
import com.stackcraft.retroflow.dto.CreateTeamMemberRequest;
import com.stackcraft.retroflow.dto.CreateTeamRequest;
import com.stackcraft.retroflow.entity.ActionPriority;
import com.stackcraft.retroflow.entity.FeedbackType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FeedbackItemServiceIntegrationTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private RetrospectiveService retrospectiveService;

    @Autowired
    private FeedbackItemService feedbackItemService;

    @Test
    void requestWorkflow_persistsTeamRetrospectiveAndActionItem() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = suffix + "@example.com";
        var team = teamService.createTeam(new CreateTeamRequest("Team-" + suffix,
                List.of(new CreateTeamMemberRequest("Alice", email))));
        var retrospective = retrospectiveService.createRetrospective(team.id(),
                new CreateRetrospectiveRequest("API workflow", null, email));

        var created = feedbackItemService.createFeedbackItem(retrospective.id(),
                new CreateFeedbackItemRequest("Automate the release", FeedbackType.ACTION_ITEM,
                        email, ActionPriority.HIGH));

        assertThat(team.members()).hasSize(1);
        assertThat(team.members().get(0).id()).isNotNull();
        assertThat(team.members().get(0).email()).isEqualTo(email);
        assertThat(retrospective.date()).isNotNull();
        assertThat(created.submittedBy().id()).isEqualTo(team.members().get(0).id());
        assertThat(created.priority()).isEqualTo(ActionPriority.HIGH);
        assertThat(created.completed()).isFalse();

        var page = feedbackItemService.listFeedbackItems(retrospective.id(), ActionPriority.HIGH,
                false, 0, 20);
        assertThat(page.items()).containsExactly(created);

        var completed = feedbackItemService.completeActionItem(retrospective.id(), created.id(), email);
        assertThat(completed.completed()).isTrue();
    }
}
