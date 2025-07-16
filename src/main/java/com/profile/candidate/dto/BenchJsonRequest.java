package com.profile.candidate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public class BenchJsonRequest {

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("candidate_email_id") // or "email" if that is correct
    private String email;

    @JsonProperty("relevant_experience")
    private BigDecimal relevantExperience;

    @JsonProperty("total_experience")
    private BigDecimal totalExperience;

    @JsonProperty("contact_number")
    private String contactNumber;

    private List<String> skills;

    private String linkedin;

    @JsonProperty("referred_by")
    private String referredBy;

    private String technology;

    private String remarks;

    private String resume;

    // getters and setters.

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
    //
    // ..
}
