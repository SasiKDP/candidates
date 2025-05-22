package com.profile.candidate.exceptions;

public class InvalidOTPException extends RuntimeException{

    public InvalidOTPException(String message) {
        super(message);
    }
}
