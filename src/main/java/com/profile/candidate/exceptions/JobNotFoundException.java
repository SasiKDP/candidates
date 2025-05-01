package com.profile.candidate.exceptions;

public class JobNotFoundException extends RuntimeException{

    public JobNotFoundException(String message){
        super(message);
    }
}
