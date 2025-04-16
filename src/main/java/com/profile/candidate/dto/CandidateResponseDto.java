package com.profile.candidate.dto;

public class CandidateResponseDto {

    private String status;  // Success or Error
    private String message;  // Message to describe the status
    private Payload payload;  // Contains candidateId, employeeId, and jobId
    private String errorMessage;  // Error message in case of failure

    // Constructor
    public CandidateResponseDto(String status, String message, Payload payload, String errorMessage) {
        this.status = status;
        this.message = message;
        this.payload = payload;
        this.errorMessage = errorMessage;
    }
    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    // Inner Payload class to represent candidateId, employeeId, and jobId
    public static class Payload {
        private String candidateId;
        private String employeeId;
        private String submissionId;
        // Constructor
        public Payload(String candidateId, String employeeId,String submissionId) {
            this.candidateId = candidateId;
            this.employeeId = employeeId;
            this.submissionId=submissionId;
        }

        // Getters and Setters
        public String getCandidateId() {
            return candidateId;
        }

        public void setCandidateId(String candidateId) {
            this.candidateId = candidateId;
        }

        public String getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(String employeeId) {
            this.employeeId = employeeId;
        }

        public String getSubmissionId() {
            return submissionId;
        }
        public void setSubmissionId(String submissionId) {
            this.submissionId = submissionId;
        }
    }
}
