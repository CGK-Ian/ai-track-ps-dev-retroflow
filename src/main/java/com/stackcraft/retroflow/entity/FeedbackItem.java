package com.stackcraft.retroflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "feedback_items")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "item_kind", discriminatorType = DiscriminatorType.STRING, length = 30)
@DiscriminatorValue("FEEDBACK_ITEM")
public class FeedbackItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 5000)
    @Column(nullable = false, length = 5000)
    private String content;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FeedbackType type;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitter_member_id", nullable = false)
    private TeamMember submitter;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retrospective_id", nullable = false)
    private Retrospective retrospective;

    public FeedbackItem() {
    }

    public FeedbackItem(Long id, String content, FeedbackType type,
                        TeamMember submitter, Retrospective retrospective) {
        this.id = id;
        this.content = content;
        this.type = type;
        this.submitter = submitter;
        this.retrospective = retrospective;
    }

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

    public FeedbackType getType() {
        return type;
    }

    public void setType(FeedbackType type) {
        this.type = type;
    }

    public TeamMember getSubmitter() {
        return submitter;
    }

    public void setSubmitter(TeamMember submitter) {
        this.submitter = submitter;
    }

    public Retrospective getRetrospective() {
        return retrospective;
    }

    public void setRetrospective(Retrospective retrospective) {
        this.retrospective = retrospective;
    }

    @AssertTrue(message = "Action items must have type ACTION_ITEM; other feedback must not")
    public boolean isTypeConsistentWithSubtype() {
        return this instanceof ActionItem
                ? type == FeedbackType.ACTION_ITEM
                : type != FeedbackType.ACTION_ITEM;
    }
}
