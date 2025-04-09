package com.profile.candidate.exceptions;

import com.profile.candidate.dto.CandidateResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

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
    public ResponseEntity<CandidateResponseDto> handleInterviewAlreadyScheduledException(InterviewAlreadyScheduledException ex) {
        CandidateResponseDto response = new CandidateResponseDto(
                ex.getMessage(),  // Custom exception message
                null,  // candidateId = null
                null,  // employeeId = null
                null   // jobId = null
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST); // HTTP 400 for invalid request
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<CandidateResponseDto> handleInvalidFileTypeException(InvalidFileTypeException ex) {
        // Prepare the error response
        CandidateResponseDto.Payload payload = new CandidateResponseDto.Payload(null, null, null);
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

    @ExceptionHandler(DateRangeValidationException.class)
    public ResponseEntity<Map<String, String>> handleDateRangeValidationException(DateRangeValidationException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
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

    // ✅ Handle IllegalArgumentException (e.g., invalid date range)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

}