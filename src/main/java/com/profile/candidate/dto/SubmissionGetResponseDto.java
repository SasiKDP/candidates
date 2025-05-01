package com.profile.candidate.dto;

import com.profile.candidate.model.Submissions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class SubmissionGetResponseDto {

        private String submissionId;

        private String candidateId;

        private String userId;

         private String userName;

        private String userEmail;

        private String fullName;

        private String contactNumber;

        private String candidateEmail;

        private float totalExperience;

        private float relevantExperience;

        private String jobId;

        private String clientName;

        private LocalDate profileReceivedDate;

        private String preferredLocation;

        private String skills;

    public SubmissionGetResponseDto(Submissions submissions) {
    }

    public SubmissionGetResponseDto() {
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
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


    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public LocalDate getProfileReceivedDate() {
        return profileReceivedDate;
    }

    public void setProfileReceivedDate(LocalDate profileReceivedDate) {
        this.profileReceivedDate = profileReceivedDate;
    }

    public String getPreferredLocation() {
        return preferredLocation;
    }

    public void setPreferredLocation(String preferredLocation) {
        this.preferredLocation = preferredLocation;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getCandidateEmail() {
        return candidateEmail;
    }

    public void setCandidateEmail(String candidateEmail) {
        this.candidateEmail = candidateEmail;
    }
}
