package com.stackcraft.retroflow.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String status = "OPEN";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Team team;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

}
