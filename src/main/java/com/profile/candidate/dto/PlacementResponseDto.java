package com.profile.candidate.dto;


public class PlacementResponseDto {
    private String id;
    private String consultantName;
    private String candidateContactNo;
    private String consultantEmail;

    // Constructors
    public PlacementResponseDto() {}

    public PlacementResponseDto(String id, String consultantName, String candidateContactNo, String consultantEmail) {
        this.id = id;
        this.consultantName = consultantName;
        this.candidateContactNo = candidateContactNo;
        this.consultantEmail = consultantEmail;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConsultantName() {
        return consultantName;
    }

    public void setConsultantName(String consultantName) {
        this.consultantName = consultantName;
    }

    public String getCandidateContactNo() {
        return candidateContactNo;
    }

    public void setCandidateContactNo(String candidateContactNo) {
        this.candidateContactNo = candidateContactNo;
    }

    public String getConsultantEmail() {
        return consultantEmail;
    }

    public void setConsultantEmail(String consultantEmail) {
        this.consultantEmail = consultantEmail;
    }
    // (Generate using IDE or Lombok if you're using it)
}

