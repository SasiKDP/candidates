package com.profile.candidate.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.ArrayList;
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
    private String clientName;
    private String fullName;
    private String externalInterviewDetails;
    private String contactNumber;
    private String userEmail;
    private String interviewLevel;
    @Lob
    @Column(name = "interview_status", columnDefinition = "TEXT")
    private String interviewStatus; // Store JSON as String
    @Lob
    @Column(name = "client_email", columnDefinition = "TEXT")
    private String clientEmail;
    private String candidateEmailId;
    private LocalDateTime timestamp;
    private boolean isPlaced;


    public Boolean getIsPlaced() {
        return isPlaced;
    }

    public void setIsPlaced(Boolean isPlaced) {
        this.isPlaced = isPlaced;
    }
    @Transient
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void setClientEmailList(List<String> emails) {
        try {
            // Serialize the list into a JSON string and store it in clientEmail
            this.clientEmail = objectMapper.writeValueAsString(emails);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize client emails", e);
        }
    }
    // Get the client emails from the JSON string
    public List<String> getClientEmailList() {
        try {
            // If clientEmail is null or empty, return an empty list
            if (this.clientEmail == null || this.clientEmail.isEmpty()) {
                return new ArrayList<>();
            }
            // Deserialize the JSON string into a list of client emails
            return objectMapper.readValue(this.clientEmail, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize client emails", e);
        }
    }
//    public void updateInterviewStatus(int stage, String status) {
//        ObjectMapper objectMapper = new ObjectMapper();
//        ArrayNode statusArray;
//        try {
//            if (this.interviewStatus != null && !this.interviewStatus.isEmpty()) {
//                statusArray = (ArrayNode) objectMapper.readTree(this.interviewStatus);
//            } else {
//                statusArray = objectMapper.createArrayNode();
//            }
//            ObjectNode statusEntry = objectMapper.createObjectNode();
//            statusEntry.put("stage", stage);
//            statusEntry.put("status", status);
//            statusEntry.put("timestamp", OffsetDateTime.now().toString());
//
//            // Add interview level from entity field
//            statusEntry.put("interviewLevel", this.interviewLevel != null ? this.interviewLevel : "");
//
//            statusArray.add(statusEntry);
//            this.interviewStatus = objectMapper.writeValueAsString(statusArray);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException("Error updating interview status", e);
//        }
//    }

    public boolean isPlaced() {
        return isPlaced;
    }
    public void setPlaced(boolean placed) {
        isPlaced = placed;
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

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}


