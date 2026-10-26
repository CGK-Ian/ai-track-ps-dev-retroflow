package com.stackcraft.retroflow.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * STUB — left behind by the previous vendor.
 *
 * A Team has a name and a list of members (member names only, no
 * authentication) and must have at least one member. See the RetroFlow
 * brief for the full requirements — fields, validation, and relationships
 * to Retrospective still need to be designed and implemented.
 */
@Entity
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ElementCollection
    @CollectionTable(name = "team_member_names", joinColumns = @JoinColumn(name = "team_id"))
    private List<String> members = new ArrayList<>();

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeamMember> teamMembers = new ArrayList<>();

    public List<TeamMember> getTeamMembers() {
        return teamMembers;
    }

    public void addTeamMember(TeamMember member) {
        member.setTeam(this);
        teamMembers.add(member);
    }

    public Optional<TeamMember> findMemberByEmail(String email) {
        return teamMembers.stream()
            .filter(member -> member.getEmail().equalsIgnoreCase(email))
            .findFirst();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

}
