package com.profile.candidate.dto;

import java.math.BigDecimal;
import java.util.List;

public class BenchJsonRequest {
    private String fullName;
    private String email;
    private BigDecimal relevantExperience;
    private BigDecimal totalExperience;
    private String contactNumber;
    private List<String> skills;
    private String linkedin;
    private String referredBy;
    private String technology;
    private String remarks;
    private String resume; // Base64-encoded string

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public BigDecimal getRelevantExperience() {
        return relevantExperience;
    }

    public void setRelevantExperience(BigDecimal relevantExperience) {
        this.relevantExperience = relevantExperience;
    }

    public BigDecimal getTotalExperience() {
        return totalExperience;
    }

    public void setTotalExperience(BigDecimal totalExperience) {
        this.totalExperience = totalExperience;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public String getLinkedin() {
        return linkedin;
    }

    public void setLinkedin(String linkedin) {
        this.linkedin = linkedin;
    }

    public String getReferredBy() {
        return referredBy;
    }

    public void setReferredBy(String referredBy) {
        this.referredBy = referredBy;
    }

    public String getTechnology() {
        return technology;
    }

    public void setTechnology(String technology) {
        this.technology = technology;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getResume() {
        return resume;
    }

    public void setResume(String resume) {
        this.resume = resume;
    }

    // Getters and Setters
}
