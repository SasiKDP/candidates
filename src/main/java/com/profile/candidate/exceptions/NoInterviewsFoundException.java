package com.profile.candidate.exceptions;

public class NoInterviewsFoundException extends RuntimeException{

    public NoInterviewsFoundException(String message){
        super(message);
    }
}
