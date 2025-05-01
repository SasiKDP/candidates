package com.profile.candidate.exceptions;

public class InvalidCandidateDataException extends RuntimeException {
    public InvalidCandidateDataException(String message) {
        super(message);
    }
}
