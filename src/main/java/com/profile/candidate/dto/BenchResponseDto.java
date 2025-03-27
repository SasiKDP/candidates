package com.profile.candidate.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BenchResponseDto {
    private String status;
    private String message;
    private List<Payload> payload;  // ✅ Changed to List<Payload>
    private String errorMessage;

    public BenchResponseDto(String status, String message, List<Payload> payload, String errorMessage) {
        this.status = status;
        this.message = message;
        this.payload = payload;
        this.errorMessage = errorMessage;
    }

    public BenchResponseDto(boolean b, String message) {
    }

    // ✅ Getters and Setters
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public List<Payload> getPayload() { return payload; }  // ✅ Return List<Payload>
    public String getErrorMessage() { return errorMessage; }

    // ✅ Response Payload (Includes id, fullName, email, referredBy)
    public static class Payload {
        private String id;
        private String fullName;

        public Payload(String id, String fullName) {
            this.id = id;
            this.fullName = fullName;
        }

        // ✅ Getters
        public String getId() { return id; }
        public String getFullName() { return fullName; }
    }
}