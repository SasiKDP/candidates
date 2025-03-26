package com.profile.candidate.dto;

import java.math.BigDecimal;

public class BenchDetailsDto {
    private String id;
    private String fullName;
    private String email;
    private BigDecimal relevantExperience;
    private BigDecimal totalExperience;
    private String contactNumber;
    private String skills;
    private String linkedin;
    private String referredBy;

    public BenchDetailsDto(String id, String fullName, String email, BigDecimal relevantExperience, BigDecimal totalExperience, String contactNumber, String skills, String linkedin, String referredBy) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.relevantExperience = relevantExperience;
        this.totalExperience = totalExperience;
        this.contactNumber = contactNumber;
        this.skills = skills;
        this.linkedin = linkedin;
        this.referredBy = referredBy;
    }

    // ✅ Getters & Setters
    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public BigDecimal getRelevantExperience() { return relevantExperience; }
    public BigDecimal getTotalExperience() { return totalExperience; }
    public String getContactNumber() { return contactNumber; }
    public String getSkills() { return skills; }
    public String getLinkedin() { return linkedin; }
    public String getReferredBy() { return referredBy; }
}
