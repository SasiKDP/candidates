package com.profile.candidate.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class PlacementResponseDto {
    private String id;
    private String candidateFullName;
    private String candidateContactNo;
    @JsonIgnore
    private boolean isPlaced;  // Add isPlaced field

    // Constructors

    public PlacementResponseDto(String id, String candidateFullName, String candidateContactNo, boolean isPlaced) {
        this.id = id;
        this.candidateFullName = candidateFullName;
        this.candidateContactNo = candidateContactNo;
        this.isPlaced = isPlaced;  // Set the value of isPlaced in the constructor
    }

    public PlacementResponseDto(String id, String candidateFullName, String candidateContactNo) {
        this.id = id;
        this.candidateFullName = candidateFullName;
        this.candidateContactNo = candidateContactNo;
    }
// Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCandidateFullName() {
        return candidateFullName;
    }

    public void setCandidateFullName(String candidateFullName) {
        this.candidateFullName = candidateFullName;
    }

    public String getCandidateContactNo() {
        return candidateContactNo;
    }

    public void setCandidateContactNo(String candidateContactNo) {
        this.candidateContactNo = candidateContactNo;
    }

    @JsonIgnore
    public boolean isPlaced() {
        return isPlaced;  // Getter for isPlaced
    }

    public void setPlaced(boolean isPlaced) {
        this.isPlaced = isPlaced;  // Setter for isPlaced
    }
}
