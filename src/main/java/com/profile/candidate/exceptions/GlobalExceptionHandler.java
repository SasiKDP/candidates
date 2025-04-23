package com.profile.candidate.exceptions;

import com.profile.candidate.dto.CandidateResponseDto;
import com.profile.candidate.dto.DeleteSubmissionResponseDto;
import com.profile.candidate.dto.GetInterviewResponse;
import com.profile.candidate.dto.InterviewResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.ArrayList;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle CandidateNotFoundException
    @ExceptionHandler(CandidateNotFoundException.class)
    public ResponseEntity<CandidateResponseDto> handleCandidateNotFoundException(CandidateNotFoundException ex) {
        // Assuming candidateId, employeeId, and jobId are null by default, as per the previous DTO definition
        CandidateResponseDto response = new CandidateResponseDto(
                ex.getMessage(),  // Custom exception message
                null,  // candidateId = null
                null,  // employeeId = null
                null   // jobId = null
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND); // HTTP 404
    }
    // Handle InterviewAlreadyScheduledException
    @ExceptionHandler(InterviewAlreadyScheduledException.class)
    public ResponseEntity<InterviewResponseDto> handleInterviewAlreadyScheduledException(InterviewAlreadyScheduledException ex) {

        InterviewResponseDto response=new InterviewResponseDto(false,"An error occurred while scheduling the interview",null,ex.getMessage());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST); // HTTP 400 for invalid request
    }
    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<CandidateResponseDto> handleInvalidFileTypeException(InvalidFileTypeException ex) {
        // Prepare the error response
        CandidateResponseDto.Payload payload = new CandidateResponseDto.Payload(null,  null,null);
        CandidateResponseDto response = new CandidateResponseDto(
                "Error", // Status
                ex.getMessage(), // Error message
                payload, // Empty payload for now
                ex.getMessage() // Same error message for the response
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Handle FileSizeExceededException (added for file size exceeded)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<CandidateResponseDto> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        // Log the error if needed
        CandidateResponseDto errorResponse = new CandidateResponseDto(
                "Error",
                "File size exceeds the maximum allowed size of 10 MB.", // Custom error message
                new CandidateResponseDto.Payload(null, null, null),
                null
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.PAYLOAD_TOO_LARGE);  // Return 413 Payload Too Large
    }
    // Handle CandidateAlreadyExistsException
    @ExceptionHandler(CandidateAlreadyExistsException.class)
    public ResponseEntity<CandidateResponseDto> handleCandidateAlreadyExistsException(CandidateAlreadyExistsException ex) {
        // Assuming candidateId, employeeId, and jobId are null by default
        CandidateResponseDto response = new CandidateResponseDto(
                ex.getMessage(),  // Custom exception message
                null,  // candidateId = null
                null,  // employeeId = null
                null   // jobId = null
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT); // HTTP 409
    }
    @ExceptionHandler(SubmissionNotFoundException.class)
    public ResponseEntity<DeleteSubmissionResponseDto> handleSubmissionNotFoundException(SubmissionNotFoundException ex){
        DeleteSubmissionResponseDto response=new DeleteSubmissionResponseDto(
                false,
                "An Error occurred while deleting submission",
                null,
                ex.getMessage());
        return new ResponseEntity<>(response,HttpStatus.NOT_FOUND);
    }
    // Handle all other unchecked exceptions (generic fallback)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<CandidateResponseDto> handleRuntimeException(RuntimeException ex) {
        // Assuming candidateId, employeeId, and jobId are null by default
        CandidateResponseDto response = new CandidateResponseDto(
                "Internal server error occurred",  // Generic error message
                null,  // candidateId = null
                null,  // employeeId = null
                null   // jobId = null
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR); // HTTP 500
    }
    @ExceptionHandler(InterviewNotScheduledException.class)
    public ResponseEntity<GetInterviewResponse> handleInterviewNotScheduledException(InterviewNotScheduledException ex){

        GetInterviewResponse response=new GetInterviewResponse(false,"No Interviews Found",new ArrayList<>(),ex.getMessage());

        return new ResponseEntity<>(response,HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(InvalidClientException.class)
    public ResponseEntity<InterviewResponseDto> handleInvalidClientException(InvalidClientException ex){
        InterviewResponseDto response=new InterviewResponseDto(false,"Interview Not scheduled",null,ex.getMessage());

        return new ResponseEntity<>(response,HttpStatus.FORBIDDEN);
    }
    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<InterviewResponseDto> handleInterviewResponseDto(JobNotFoundException ex){

        InterviewResponseDto response=new InterviewResponseDto(false,"Interview Not Scheduled",null,ex.getMessage());
        return new ResponseEntity<>(response,HttpStatus.FORBIDDEN);
    }
    @ExceptionHandler(NoInterviewsFoundException.class)
    public ResponseEntity<GetInterviewResponse> handleNoInterviewsFoundException(NoInterviewsFoundException ex){

        GetInterviewResponse response=new GetInterviewResponse(false,"No Interviews Found",new ArrayList<>(),ex.getMessage());
        return new ResponseEntity<>(response,HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(DateRangeValidationException.class)
    public ResponseEntity<GetInterviewResponse> handleDateRangeValidationException(DateRangeValidationException ex){

        GetInterviewResponse response=new GetInterviewResponse(false,"No Interviews Found",new ArrayList<>(),ex.getMessage());
        return new ResponseEntity<>(response,HttpStatus.CONFLICT);

    }
}
