
package com.profile.candidate.dto;

import java.math.BigDecimal;
import java.util.List;

public class BenchDetailsDto {
    private String id;
    private String fullName;
    private String email;
    private BigDecimal relevantExperience;
    private BigDecimal totalExperience;
    private String contactNumber;
    private List<String> skills;
    private String linkedin;
    private String referredBy;

    public BenchDetailsDto(String id, String fullName, String email, BigDecimal relevantExperience, BigDecimal totalExperience, String contactNumber, List<String> skills, String linkedin, String referredBy) {
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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
}
