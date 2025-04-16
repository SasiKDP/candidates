package com.profile.candidate.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name="interview_details")
public class InterviewDetails {

    @Id
    private String interviewId;

    private String clientId;

    private String userId;

    private String jobId;

    private String candidateId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime interviewDateTime;

    private Integer duration;
    private String zoomLink;

//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
//    private LocalDateTime scheduledTimeStamp;

    private String clientName;
    private String fullName;

    private String externalInterviewDetails;
    private String contactNumber;
    private String userEmail;
    private String interviewLevel;

    @Lob
    @Column(name = "interview_status", columnDefinition = "TEXT")
    private String interviewStatus; // Store JSON as String

    private String clientEmail;

    private String candidateEmailId;

    private LocalDateTime timestamp;

    public void updateInterviewStatus(String round, String status) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> interviewHistory = new LinkedHashMap<>();

        // Load existing interview status JSON (if available)
        if (this.interviewStatus != null && !this.interviewStatus.isEmpty()) {
            try {
                interviewHistory = objectMapper.readValue(this.interviewStatus, LinkedHashMap.class);
            } catch (JsonProcessingException e) {
                // Log and reset interviewHistory in case of invalid JSON
                System.err.println("Invalid JSON format in interviewStatus, resetting it.");
                interviewHistory = new LinkedHashMap<>();
            }
        }
        // Add or update the round status
        interviewHistory.put(round, status);
        // Convert back to JSON
        try {
            this.interviewStatus = objectMapper.writeValueAsString(interviewHistory);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting interview status to JSON", e);
        }
    }
    public String getCandidateEmailId() {
        return candidateEmailId;
    }

    public void setCandidateEmailId(String candidateEmailId) {
        this.candidateEmailId = candidateEmailId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(String interviewId) {
        this.interviewId = interviewId;
    }

    public String getClientId() {
        return clientId;
    }
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getJobId() {
        return jobId;
    }
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public OffsetDateTime getInterviewDateTime() {
        return interviewDateTime;
    }

    public void setInterviewDateTime(OffsetDateTime interviewDateTime) {
        this.interviewDateTime = interviewDateTime;
    }
    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getZoomLink() {
        return zoomLink;
    }

    public void setZoomLink(String zoomLink) {
        this.zoomLink = zoomLink;
    }


    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    public String getExternalInterviewDetails() {
        return externalInterviewDetails;
    }

    public void setExternalInterviewDetails(String externalInterviewDetails) {
        this.externalInterviewDetails = externalInterviewDetails;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getInterviewLevel() {
        return interviewLevel;
    }

    public void setInterviewLevel(String interviewLevel) {
        this.interviewLevel = interviewLevel;
    }

    public String getInterviewStatus() {
        return interviewStatus;
    }

    public void setInterviewStatus(String interviewStatus) {
        this.interviewStatus = interviewStatus;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
    }
}


