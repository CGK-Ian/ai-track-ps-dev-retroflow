package com.stackcraft.retroflow.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;

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
public class ActionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;

    private String priority;

    private String assignee;

    private boolean completed;

    @Column(unique = true)
    private Long feedbackItemId;

    @ManyToOne(optional = false)
    private Retrospective retrospective;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Long getFeedbackItemId() {
        return feedbackItemId;
    }

    public void setFeedbackItemId(Long feedbackItemId) {
        this.feedbackItemId = feedbackItemId;
    }

    public Retrospective getRetrospective() {
        return retrospective;
    }

    public void setRetrospective(Retrospective retrospective) {
        this.retrospective = retrospective;
    }

}
