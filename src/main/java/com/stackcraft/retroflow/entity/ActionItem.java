package com.stackcraft.retroflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;

/**
 * STUB — left behind by the previous vendor.
 *
 * An ActionItem is a special kind of FeedbackItem (type ACTION_ITEM) that
 * additionally tracks a priority (LOW/MEDIUM/HIGH) and a boolean
 * completed flag. Action items can be marked completed, but a completed
 * action item can never be uncompleted. They must also be retrievable
 * filtered by priority and by completed status.
 *
 * This class is not yet related to FeedbackItem — deciding and
 * implementing the inheritance strategy between the two (single table,
 * joined, or a composition approach) is part of Block 1. Nothing has
 * been decided yet; this is deliberately just a bare stub.
 */
@Entity
@DiscriminatorValue("ACTION_ITEM")
public class ActionItem extends FeedbackItem {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ActionPriority priority;

    @Column(nullable = false)
    private boolean completed = false;

    public ActionItem() {
    }

    public ActionItem(Long id, String content, TeamMember submitter, Retrospective retrospective,
                      ActionPriority priority, boolean completed) {
        super(id, content, FeedbackType.ACTION_ITEM, submitter, retrospective);
        this.priority = priority;
        this.completed = completed;
    }

    public ActionItem(Long id, String content, FeedbackType type, TeamMember submitter,
                      Retrospective retrospective, ActionPriority priority, boolean completed) {
        super(id, content, type, submitter, retrospective);
        this.priority = priority;
        this.completed = completed;
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
}
