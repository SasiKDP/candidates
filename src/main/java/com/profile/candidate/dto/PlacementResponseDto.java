package com.profile.candidate.dto;

public class PlacementResponseDto {
    private String id;
    private String consultantName;
    private String phone;
    private String consultantEmail;

    // Constructors
    public PlacementResponseDto() {}

    public PlacementResponseDto(String id, String consultantName, String phone, String consultantEmail) {
        this.id = id;
        this.consultantName = consultantName;
        this.phone = phone;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getConsultantEmail() {
        return consultantEmail;
    }

    public void setConsultantEmail(String consultantEmail) {
        this.consultantEmail = consultantEmail;
    }
    // (Generate using IDE or Lombok if you're using it)
}

