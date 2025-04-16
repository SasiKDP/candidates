package com.profile.candidate.controller;

import com.profile.candidate.dto.*;
import com.profile.candidate.exceptions.CandidateAlreadyExistsException;
import com.profile.candidate.exceptions.CandidateNotFoundException;
import com.profile.candidate.exceptions.DateRangeValidationException;
import com.profile.candidate.exceptions.InterviewNotScheduledException;
import com.profile.candidate.model.CandidateDetails;
import com.profile.candidate.model.Submissions;
import com.profile.candidate.repository.CandidateRepository;
import com.profile.candidate.service.CandidateService;
import com.profile.candidate.service.InterviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = {"http://35.188.150.92", "http://192.168.0.140:3000", "http://192.168.0.139:3000","https://mymulya.com", "http://localhost:3000","http://192.168.0.135:3000",
        "http://192.168.0.135:80",
        "http://mymulya.com:443",
        "http://182.18.177.16:443",
        "http://localhost/","http://192.168.0.135",
        "http://182.18.177.16"})



@RestController
@RequestMapping("/candidate")
public class CandidateController {

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private InterviewService interviewService;

    private static final Logger logger = LoggerFactory.getLogger(CandidateController.class);

    // Endpoint to submit candidate profile (Create new candidate)
    // Endpoint to submit candidate profile (Create new candidate)
    @PostMapping("/candidatesubmissions")
    public ResponseEntity<CandidateResponseDto> submitCandidate(
            @RequestParam("jobId") String jobId,
            @RequestParam("userId") String userId,
            @RequestParam("fullName") String fullName,
            @RequestParam("candidateEmailId") String candidateEmailId,
            @RequestParam("contactNumber") String contactNumber,
            @RequestParam("qualification") String qualification,
            @RequestParam("totalExperience") float totalExperience,
            @RequestParam("currentCTC") String currentCTC,
            @RequestParam("expectedCTC") String expectedCTC,
            @RequestParam("noticePeriod") String noticePeriod,
            @RequestParam("currentLocation") String currentLocation,
            @RequestParam("preferredLocation") String preferredLocation,
            @RequestParam("skills") String skills,
            @RequestParam(value = "communicationSkills", required = false) String communicationSkills,
            @RequestParam(value = "requiredTechnologiesRating", required = false) Double requiredTechnologiesRating,
            @RequestParam(value = "overallFeedback", required = false) String overallFeedback,
            @RequestParam(value = "relevantExperience", required = false) float relevantExperience,
            @RequestParam(value = "currentOrganization", required = false) String currentOrganization,
            @RequestParam(value = "userEmail", required = false) String userEmail,
            @RequestParam("resumeFile") MultipartFile resumeFile) {

        try {
            // Validate file size (10 MB max)
            validateFileSize(resumeFile);
            // Check if the resume file is valid (PDF or DOCX)
            if (!isValidFileType(resumeFile)) {
                // Log the invalid file type error
                logger.error("Invalid file type uploaded for candidate {}. Only PDF, DOC and DOCX are allowed.", fullName);
                // Return the error response in the correct format
                return new ResponseEntity<>(new CandidateResponseDto(
                        "Error",
                        "Invalid file type. Only PDF, DOC and DOCX are allowed.",
                        new CandidateResponseDto.Payload(null, null,null),
                        null
                ), HttpStatus.BAD_REQUEST); // Return HTTP 400 for invalid file type
            }
            // Construct CandidateDetails object from request parameters
            Submissions submission=new Submissions();
            CandidateDetails candidateDetails = new CandidateDetails();
            submission.setJobId(jobId);
            candidateDetails.setUserId(userId);
            candidateDetails.setFullName(fullName);
            candidateDetails.setCandidateEmailId(candidateEmailId);
            candidateDetails.setContactNumber(contactNumber);
            candidateDetails.setQualification(qualification);
            candidateDetails.setTotalExperience(totalExperience);
            candidateDetails.setCurrentCTC(currentCTC);
            candidateDetails.setExpectedCTC(expectedCTC);
            candidateDetails.setNoticePeriod(noticePeriod);
            candidateDetails.setCurrentLocation(currentLocation);
            candidateDetails.setRelevantExperience(relevantExperience);
            candidateDetails.setCurrentOrganization(currentOrganization);
            candidateDetails.setUserEmail(userEmail);
            submission.setCandidate(candidateDetails);
            submission.setPreferredLocation(preferredLocation);
            submission.setSkills(skills);
            submission.setCommunicationSkills(communicationSkills);
            submission.setRequiredTechnologiesRating(requiredTechnologiesRating);
            submission.setOverallFeedback(overallFeedback);

            // Call service method to submit the candidate and handle file upload
            CandidateResponseDto response = candidateService.submitCandidate(candidateDetails,submission, resumeFile);

            // Log the success of candidate submission
            logger.info("Candidate successfully submitted: {}", fullName);

            // Return success response
            return new ResponseEntity<>(response, HttpStatus.OK);

        }  catch (MaxUploadSizeExceededException ex) {
            CandidateResponseDto errorResponse = new CandidateResponseDto(
                    "Error",
                    "File size exceeds the maximum allowed size of 20 MB.", // Custom error message
                    new CandidateResponseDto.Payload(null, null, null),
                    null
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.PAYLOAD_TOO_LARGE);  // Return 413 Payload Too Large
        } catch (CandidateAlreadyExistsException ex) {
            // Handle specific CandidateAlreadyExistsException
            logger.error("Candidate already exists: {}", ex.getMessage());
            CandidateResponseDto errorResponse = new CandidateResponseDto(
                    "Error",
                    ex.getMessage(),
                    new CandidateResponseDto.Payload(null, null, null),
                    null
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT); // 409 Conflict

        } catch (CandidateNotFoundException ex) {
            // Handle specific CandidateNotFoundException
            logger.error("Candidate not found: {}", ex.getMessage());
            CandidateResponseDto errorResponse = new CandidateResponseDto(
                    "Error",
                    "Candidate not found",
                    new CandidateResponseDto.Payload(null, null, null),
                    null
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND); // 404 Not Found

        } catch (IOException ex) {
            // Handle file I/O exceptions (e.g., file save errors)
            logger.error("Error processing resume file for candidate {}. Error: {}", fullName, ex.getMessage());
            CandidateResponseDto errorResponse = new CandidateResponseDto(
                    "Error",
                    "Error processing resume file.",
                    new CandidateResponseDto.Payload(null, null, null),
                    null
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR); // 500 Internal Server Error

        } catch (Exception ex) {
            // General error handler for any issues during candidate submission
            logger.error("An error occurred while submitting the candidate {}. Error: {}", fullName, ex.getMessage());
            CandidateResponseDto errorResponse = new CandidateResponseDto(
                    "Error",
                    "An error occurred while submitting the candidate",
                    new CandidateResponseDto.Payload(null, null, null),
                    null
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR); // 500 Internal Server Error
        }
    }
    private void validateFileSize(MultipartFile file) {
        long maxSize = 10 * 1024 * 1024; // 10 MB
        if (file.getSize() > maxSize) {
            // Throw MaxUploadSizeExceededException instead of FileSizeExceededException
            throw new MaxUploadSizeExceededException(maxSize);
        }
    }
    private boolean isValidFileType(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName != null) {
            String fileExtension = getFileExtension(fileName).toLowerCase();
            return fileExtension.equals("pdf") || fileExtension.equals("docx") || fileExtension.equals("doc");
        }
        return false;
    }
    private String getFileExtension(String fileName) {
        int index = fileName.lastIndexOf(".");
        if (index > 0) {
            return fileName.substring(index + 1);
        }
        return "";
    }
    @PutMapping("/candidatesubmissions/{candidateId}")
    public ResponseEntity<CandidateResponseDto> resubmitCandidate(
            @PathVariable("candidateId") String candidateId,
            @RequestParam(value = "jobId", required = false) String jobId,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "candidateEmailId", required = false) String candidateEmailId,
            @RequestParam(value = "contactNumber", required = false) String contactNumber,
            @RequestParam(value = "qualification", required = false) String qualification,
            @RequestParam(value = "totalExperience", required = false) Float totalExperience,
            @RequestParam(value = "currentCTC", required = false) String currentCTC,
            @RequestParam(value = "expectedCTC", required = false) String expectedCTC,
            @RequestParam(value = "noticePeriod", required = false) String noticePeriod,
            @RequestParam(value = "currentLocation", required = false) String currentLocation,
            @RequestParam(value = "preferredLocation", required = false) String preferredLocation,
            @RequestParam(value = "skills", required = false) String skills,
            @RequestParam(value = "communicationSkills", required = false) String communicationSkills,
            @RequestParam(value = "requiredTechnologiesRating", required = false) Double requiredTechnologiesRating,
            @RequestParam(value = "overallFeedback", required = false) String overallFeedback,
            @RequestParam(value = "relevantExperience", required = false) Float relevantExperience,
            @RequestParam(value = "currentOrganization", required = false) String currentOrganization,
            @RequestParam(value = "userEmail", required = false) String userEmail,
            @RequestParam(value = "resumeFile", required = false) MultipartFile resumeFile) {
//        try {
//            // Create a CandidateDetails object from the request parameters
        Submissions updateSubmission=new Submissions();
        CandidateDetails updatedCandidateDetails = new CandidateDetails();
        updateSubmission.setJobId(jobId);
        updatedCandidateDetails .setUserId(userId);
        updatedCandidateDetails .setFullName(fullName);
        updatedCandidateDetails .setCandidateEmailId(candidateEmailId);
        updatedCandidateDetails .setContactNumber(contactNumber);
        updatedCandidateDetails .setQualification(qualification);
        updatedCandidateDetails .setTotalExperience(totalExperience);
        updatedCandidateDetails .setCurrentCTC(currentCTC);
        updatedCandidateDetails .setExpectedCTC(expectedCTC);
        updatedCandidateDetails .setNoticePeriod(noticePeriod);
        updatedCandidateDetails .setCurrentLocation(currentLocation);
        updatedCandidateDetails .setRelevantExperience(relevantExperience);
        updatedCandidateDetails .setCurrentOrganization(currentOrganization);
        updatedCandidateDetails .setUserEmail(userEmail);
        updateSubmission.setCandidate(updatedCandidateDetails);
        updateSubmission.setPreferredLocation(preferredLocation);
        updateSubmission.setSkills(skills);
        updateSubmission.setCommunicationSkills(communicationSkills);
        updateSubmission.setRequiredTechnologiesRating(requiredTechnologiesRating);
        updateSubmission.setOverallFeedback(overallFeedback);


        // Call the service method to resubmit the candidate
        CandidateResponseDto response = candidateService.resubmitCandidate(candidateId, updatedCandidateDetails,updateSubmission, resumeFile);

        // Return the response entity with status 200 OK
        return new ResponseEntity<>(response, HttpStatus.OK);

//        } catch (Exception ex) {
//            // Handle any exceptions and return an error response
//            logger.error("An error occurred while resubmitting the candidate: {}", ex.getMessage());
//            CandidateResponseDto errorResponse = new CandidateResponseDto(
//                    "An error occurred while resubmitting the candidate", null, null, null
//            );
//            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
    }
    @DeleteMapping("/deletecandidate/{candidateId}")
    public ResponseEntity<DeleteCandidateResponseDto> deleteCandidate(@PathVariable("candidateId") String candidateId) {
        try {
            // Call the service method to delete the candidate by ID and get the response DTO
            DeleteCandidateResponseDto response = candidateService.deleteCandidateById(candidateId);
            // Return the response entity with status 200 OK
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception ex) {
            // Handle any exceptions and return an error response
            logger.error("An error occurred while deleting the candidate: {}", ex.getMessage());
            // Create an error response DTO with error details
            DeleteCandidateResponseDto errorResponse = new DeleteCandidateResponseDto(
                    "error",
                    "Error occurred while deleting the candidate.",
                    null,
                    ex.getMessage()
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}