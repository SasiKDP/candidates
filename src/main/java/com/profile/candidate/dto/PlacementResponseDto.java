package com.profile.candidate.dto;

public class PlacementResponseDto {
    private String id;
    private String candidateFullName;
    private String candidateContactNo;
    private String clientEmail;
    private boolean isPlaced;  // Add isPlaced field

    // Constructors
    public PlacementResponseDto() {}

    public PlacementResponseDto(String id, String candidateFullName, String candidateContactNo, String clientEmail, boolean isPlaced) {
        this.id = id;
        this.candidateFullName = candidateFullName;
        this.candidateContactNo = candidateContactNo;
        this.clientEmail = clientEmail;
        this.isPlaced = isPlaced;  // Set the value of isPlaced in the constructor
    }

    public PlacementResponseDto(String id, String candidateFullName, String candidateContactNo, String clientEmail) {
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

    public String getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
    }

    public String getCandidateContactNo() {
        return candidateContactNo;
    }

    public void setCandidateContactNo(String candidateContactNo) {
        this.candidateContactNo = candidateContactNo;
    }

    public boolean isPlaced() {
        return isPlaced;  // Getter for isPlaced
    }

    public void setPlaced(boolean isPlaced) {
        this.isPlaced = isPlaced;  // Setter for isPlaced
    }
}
