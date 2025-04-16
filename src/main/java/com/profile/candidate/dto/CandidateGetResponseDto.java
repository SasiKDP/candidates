package com.profile.candidate.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.profile.candidate.model.CandidateDetails;

import java.util.*;

public class CandidateGetResponseDto {

    private String candidateId;
    private String jobId;
    private String userId;
    private String fullName;
    private String emailId;
    private String contactNumber;
    private String currentOrganization;
    private String qualification;
    private float totalExperience;
    private float relevantExperience;
    private String currentCTC;
    private String expectedCTC;
    private String noticePeriod;
    private String currentLocation;
    private String userEmail;
//    private String preferredLocation;
//    private String skills;
//    private String communicationSkills;
//    private Double requiredTechnologiesRating;
//    private String overallFeedback;
//    private String interviewStatus;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Constructor that takes a CandidateDetails object


    public CandidateGetResponseDto(String candidateId, String jobId, String userId, String fullName, String emailId, String contactNumber, String currentOrganization, String qualification, float totalExperience, float relevantExperience, String currentCTC, String expectedCTC, String noticePeriod, String currentLocation, String userEmail) {
        this.candidateId = candidateId;
        this.jobId = jobId;
        this.userId = userId;
        this.fullName = fullName;
        this.emailId = emailId;
        this.contactNumber = contactNumber;
        this.currentOrganization = currentOrganization;
        this.qualification = qualification;
        this.totalExperience = totalExperience;
        this.relevantExperience = relevantExperience;
        this.currentCTC = currentCTC;
        this.expectedCTC = expectedCTC;
        this.noticePeriod = noticePeriod;
        this.currentLocation = currentLocation;
        this.userEmail = userEmail;
    }

    // Method to extract latest interview status
    private String extractLatestInterviewStatus(String interviewStatusJson) {
        if (interviewStatusJson == null || interviewStatusJson.trim().isEmpty()) {
            return "Not Scheduled";
        }

        try {
            if (interviewStatusJson.trim().startsWith("[") || interviewStatusJson.trim().startsWith("{")) {
                // JSON format detected
                List<Map<String, Object>> statusHistory = objectMapper.readValue(interviewStatusJson, List.class);

                return statusHistory.stream()
                        .max(Comparator.comparing(entry -> (String) entry.get("timestamp"))) // Sort by latest timestamp
                        .map(entry -> (String) entry.get("status"))
                        .orElse("Not Scheduled");
            }
        } catch (JsonProcessingException e) {
            System.err.println("Error parsing interview status JSON: " + e.getMessage());
        }

        // If it's not JSON, assume it's a simple status string
        return interviewStatusJson;
    }

    // Getters and Setters
    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getCurrentOrganization() {
        return currentOrganization;
    }

    public void setCurrentOrganization(String currentOrganization) {
        this.currentOrganization = currentOrganization;
    }

    public String getQualification() {
        return qualification;
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
    }

    public float getTotalExperience() {
        return totalExperience;
    }

    public void setTotalExperience(float totalExperience) {
        this.totalExperience = totalExperience;
    }

    public float getRelevantExperience() {
        return relevantExperience;
    }

    public void setRelevantExperience(float relevantExperience) {
        this.relevantExperience = relevantExperience;
    }

    public String getCurrentCTC() {
        return currentCTC;
    }

    public void setCurrentCTC(String currentCTC) {
        this.currentCTC = currentCTC;
    }

    public String getExpectedCTC() {
        return expectedCTC;
    }

    public void setExpectedCTC(String expectedCTC) {
        this.expectedCTC = expectedCTC;
    }

    public String getNoticePeriod() {
        return noticePeriod;
    }

    public void setNoticePeriod(String noticePeriod) {
        this.noticePeriod = noticePeriod;
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
}
