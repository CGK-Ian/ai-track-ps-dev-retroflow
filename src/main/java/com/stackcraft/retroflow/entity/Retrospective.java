package com.stackcraft.retroflow.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;

/**
 * STUB — left behind by the previous vendor.
 *
 * A Retrospective belongs to a Team, has a title, a date, and a status
 * (OPEN or CLOSED). A team can have multiple retrospectives but only one
 * OPEN at a time; once closed, a retrospective can never be reopened. See
 * the RetroFlow brief for the full requirements — fields, the status
 * enum, and the relationship to Team still need to be designed and
 * implemented.
 */
@Entity
public class Retrospective {

    public enum Status {
        OPEN,
        CLOSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ManyToOne(optional = false)
    private Team team;

    @Enumerated(EnumType.STRING)
    private Status status = Status.OPEN;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

}
