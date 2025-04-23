    package com.profile.candidate.model;

    import com.fasterxml.jackson.annotation.JsonFormat;
    import jakarta.persistence.*;

    import javax.validation.constraints.*;

    import java.time.LocalDate;
    import java.time.LocalDateTime;
    import java.time.OffsetDateTime;
    import java.util.Random;

    @Entity
    @Table(name = "candidates_prod")
    public class CandidateDetails {

        @Id
        @Column(unique = true, nullable = false)
        private String candidateId;

        @Column(nullable = false)
        @NotBlank(message = "Job ID is required")
        private String jobId;  // No unique constraint on jobId

        @Column(name = "user_email")
        private String userEmail;

        @Column(name = "client_email")
        private String clientEmail;


        @Column(nullable = false)  // Ensures userId is unique
        @NotBlank(message = "User ID is required")
        private String userId;


        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        private String fullName;

        @NotBlank(message = "Email ID is required")
        @Email(message = "Invalid email format")
        private String candidateEmailId;

        @NotBlank(message = "Contact number is required")
        @Pattern(regexp = "^[0-9]{10}$", message = "Contact number must be exactly 10 digits and numeric")
        private String contactNumber;

        private String currentOrganization;

        @NotBlank(message = "Qualification is required")
        private String qualification;

        @NotNull(message = "Total experience is required")
        @Min(value = 0, message = "Total experience cannot be negative")
        private float totalExperience;

        private float relevantExperience;

        private String currentCTC;

        private String expectedCTC;

        private String noticePeriod;

        private String currentLocation;

        private String preferredLocation;

        // Change the skills field from List<String> to String
        private String skills;  // Now it's just a single string

        private String communicationSkills;

        private Double requiredTechnologiesRating;

        private String overallFeedback;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        private OffsetDateTime interviewDateTime;

        private Integer duration; // in minutes

        private LocalDateTime timestamp;

        public String getClientName() {
            return clientName;
        }

        public void setClientName(String clientName) {
            this.clientName = clientName;
        }

        public String getInterviewLevel() {
            return interviewLevel;
        }

        public void setInterviewLevel(String interviewLevel) {
            this.interviewLevel = interviewLevel;
        }

        private String zoomLink;

        private String clientName;

        private String interviewLevel;

        private String externalInterviewDetails;

        public String getExternalInterviewDetails() {
            return externalInterviewDetails;
        }

        public void setExternalInterviewDetails(String externalInterviewDetails) {
            this.externalInterviewDetails = externalInterviewDetails;
        }

        // New field profileReceivedDate
        @Column(nullable = false)
        private LocalDate profileReceivedDate;

        @Lob
        @Column(name = "resume", columnDefinition = "LONGBLOB")
        private byte[] resume;  // Storing file as byte array in DB

        @Column(name = "resume_file_path")
        private String resumeFilePath;

        @Column(name = "interview_status", columnDefinition = "TEXT")
        private String interviewStatus; // Store JSON as String

        public String getInterviewStatus() {
            return interviewStatus;
        }

        public void setInterviewStatus(String interviewStatus) {
            this.interviewStatus = interviewStatus;
        }


        public byte[] getResume() {
            return resume;
        }

        public void setResume(byte[] resume) {
            this.resume = resume;
        }

        public String getResumeFilePath() {
            return resumeFilePath;
        }

        public void setResumeFilePath(String resumeFilePath) {
            this.resumeFilePath = resumeFilePath;
        }

        @PrePersist
        public void prePersist() {
            if (this.profileReceivedDate == null) {
                this.profileReceivedDate = LocalDate.now();  // Set the date before saving
            }
            if (this.candidateId == null) {
                generateCandidateId();  // Call the method to generate the candidate ID
            }
        }

        @PreUpdate
        public void preUpdate() {
            if (this.profileReceivedDate == null) {
                // Option 1: Set it to today's date
                this.profileReceivedDate = LocalDate.now();

                // Option 2 (better): fetch existing value from DB (requires service-layer change)
                // this.profileReceivedDate = existingValueFromDB;
            }
        }

        public void generateCandidateId() {
            if (this.candidateId == null || this.candidateId.isEmpty()) {
                Random random = new Random();
                int randomNumber = 1000 + random.nextInt(9000);  // Generates a random number between 100 and 999
                this.candidateId = "CAND" + randomNumber;  // Combine the prefix with the random number
            }
        }

        // Getters and Setters for new fields

        public String getUserEmail() {
            return userEmail;
        }

        public void setUserEmail(String userEmail) {
            this.userEmail = userEmail;
        }

        public String getClientEmail() {
            return clientEmail;
        }

        public void setClientEmail(String clientEmail) {
            this.clientEmail = clientEmail;
        }

        public LocalDate getProfileReceivedDate() {
            return profileReceivedDate;
        }

        public void setProfileReceivedDate(LocalDate profileReceivedDate) {
            this.profileReceivedDate = profileReceivedDate;
        }


        // Getters and Setters for existing fields
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

        public String getCandidateEmailId() {
            return candidateEmailId;
        }

        public void setCandidateEmailId(String candidateEmailId) {
            this.candidateEmailId = candidateEmailId;
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

        public String getCommunicationSkills() {
            return communicationSkills;
        }

        public void setCommunicationSkills(String communicationSkills) {
            this.communicationSkills = communicationSkills;
        }

        public Double getRequiredTechnologiesRating() {
            return requiredTechnologiesRating;
        }

        public void setRequiredTechnologiesRating(Double requiredTechnologiesRating) {
            this.requiredTechnologiesRating = requiredTechnologiesRating;
        }

        public String getOverallFeedback() {
            return overallFeedback;
        }

        public void setOverallFeedback(String overallFeedback) {
            this.overallFeedback = overallFeedback;
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

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }

        public String getZoomLink() {
            return zoomLink;
        }

        public void setZoomLink(String zoomLink) {
            this.zoomLink = zoomLink;
        }
    }