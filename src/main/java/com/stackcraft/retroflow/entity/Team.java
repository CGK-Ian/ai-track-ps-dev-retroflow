package com.stackcraft.retroflow.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 5, max = 255)
    @Pattern(regexp = "^[A-Za-z0-9-]+$")
    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Valid
    @NotEmpty
    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private List<TeamMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Retrospective> retrospectives = new ArrayList<>();

    public Team() {
    }

    public Team(Long id, String name, List<TeamMember> members, List<Retrospective> retrospectives) {
        this.id = id;
        this.name = name;
        this.members = members;
        this.retrospectives = retrospectives;
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

    public List<TeamMember> getMembers() {
        return members;
    }

    public void setMembers(List<TeamMember> members) {
        this.members = members;
    }

    public List<Retrospective> getRetrospectives() {
        return retrospectives;
    }

    public void setRetrospectives(List<Retrospective> retrospectives) {
        this.retrospectives = retrospectives;
    }
}
