package com.stackcraft.retroflow.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * STUB — left behind by the previous vendor.
 *
 * A FeedbackItem belongs to a Retrospective, has content (text), a type
 * (WENT_WELL, NEEDS_IMPROVEMENT, ACTION_ITEM), and the name of the team
 * member who submitted it. Items can be added to an OPEN retrospective;
 * once the retrospective is CLOSED, its feedback items can never be
 * modified or deleted.
 *
 * ActionItem is a special kind of FeedbackItem (type ACTION_ITEM) that
 * additionally tracks a priority and a completed flag — see the brief for
 * how that relationship should work. Deciding and implementing the
 * inheritance strategy between this class and ActionItem is part of
 * Block 1; nothing has been decided yet.
 */
@Entity
public class FeedbackItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    private String type;

    private String submittedBy;

    @ManyToOne
    private TeamMember submitter;

    @Enumerated(EnumType.STRING)
    private ActionPriority priority;

    private boolean completed;

    @ManyToOne(optional = false)
    private Retrospective retrospective;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }

    public TeamMember getSubmitter() {
        return submitter;
    }

    public void setSubmitter(TeamMember submitter) {
        this.submitter = submitter;
    }

    public ActionPriority getPriority() {
        return priority;
    }

    public void setPriority(ActionPriority priority) {
        this.priority = priority;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Retrospective getRetrospective() {
        return retrospective;
    }

    public void setRetrospective(Retrospective retrospective) {
        this.retrospective = retrospective;
    }

}
