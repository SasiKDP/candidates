package com.profile.candidate.dto;

public class DeleteSubmissionResponseDto {

    private boolean status;
    private String message;
    private Payload payload;
    private String error;


    // Constructor
    public DeleteSubmissionResponseDto(boolean status, String message, Payload payload, String error) {
        this.status = status;
        this.message = message;
        this.payload = payload;
        this.error = error;

    }

    // Getters and Setters
    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    // Nested class for Payload
    public static class Payload {
        private String candidateId;
        private String jobId;

        // Constructor
        public Payload(String candidateId, String jobId) {
            this.candidateId = candidateId;
            this.jobId = jobId;
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
    }
}
