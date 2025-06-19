package com.profile.candidate.controller;

import com.profile.candidate.dto.*;
import com.profile.candidate.exceptions.*;
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

//@CrossOrigin(origins = {"http://35.188.150.92", "http://192.168.0.140:3000", "http://192.168.0.139:3000","https://mymulya.com", "http://localhost:3000","http://192.168.0.135:3000",
//        "http://192.168.0.135:80",
//        "http://mymulya.com:443",
//        "http://182.18.177.16:443",
//        "http://localhost/","http://192.168.0.135",
//        "http://182.18.177.16"})

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

    @PostMapping("/candidatesubmissions")
    public ResponseEntity<CandidateResponseDto> submitCandidate(
            @RequestParam Map<String, String> formData,
            @RequestParam("resumeFile") MultipartFile resumeFile) {
        try {
            // Validate file size (10 MB max)
            validateFileSize(resumeFile);
            // Check if the resume file is valid (PDF or DOCX)
            if (!isValidFileType(resumeFile)) {
                // Log the invalid file type error
                logger.error("Invalid file type uploaded for candidate {}. Only PDF, DOC and DOCX are allowed.", formData.get("fullName"));
                throw new InvalidFileTypeException("Invalid file type. Only PDF, DOC and DOCX are allowed.");

            }
            CandidateDetails candidateDetails = new CandidateDetails();
            candidateDetails.setUserId(formData.get("userId"));
            candidateDetails.setFullName(formData.get("fullName"));
            candidateDetails.setCandidateEmailId(formData.get("candidateEmailId"));
            candidateDetails.setContactNumber(formData.get("contactNumber"));
            candidateDetails.setQualification(formData.get("qualification"));
            candidateDetails.setTotalExperience(Float.parseFloat(formData.get("totalExperience")));
            candidateDetails.setCurrentCTC(formData.get("currentCTC"));
            candidateDetails.setExpectedCTC(formData.get("expectedCTC"));
            candidateDetails.setNoticePeriod(formData.get("noticePeriod"));
            candidateDetails.setCurrentLocation(formData.get("currentLocation"));
            candidateDetails.setRelevantExperience(Float.parseFloat(formData.getOrDefault("relevantExperience", "0")));
            candidateDetails.setCurrentOrganization(formData.get("currentOrganization"));
            candidateDetails.setUserEmail(formData.get("userEmail"));
            // Build Submissions
            Submissions submission = new Submissions();
            submission.setJobId(formData.get("jobId"));
            submission.setCandidate(candidateDetails);
            submission.setPreferredLocation(formData.get("preferredLocation"));
            submission.setSkills(formData.get("skills"));
            submission.setCommunicationSkills(formData.get("communicationSkills"));
            submission.setClientName(formData.get("clientName"));
            submission.setUserId(formData.get("userId"));
            submission.setUserEmail(formData.get("userEmail"));
            candidateDetails.setUserEmail(formData.get("userEmail"));
            if (formData.get("requiredTechnologiesRating") != null) {
                submission.setRequiredTechnologiesRating(Double.parseDouble(formData.get("requiredTechnologiesRating")));
            }
            submission.setOverallFeedback(formData.get("overallFeedback"));
            submission.setClientName(formData.get("clientName"));

            // Call service method to submit the candidate and handle file upload
            CandidateResponseDto response = candidateService.submitCandidate(candidateDetails,submission, resumeFile);

            logger.info("Candidate successfully submitted: {}", formData.get("fullName"));

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (MaxUploadSizeExceededException ex) {
            CandidateResponseDto errorResponse = new CandidateResponseDto(
                    "Error",
                    "File size exceeds the maximum allowed size of 20 MB.", // Custom error message
                    new CandidateResponseDto.CandidateData(null, null, null),
                    null
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.PAYLOAD_TOO_LARGE);  // Return 413 Payload Too Large
        }
        catch (IOException ex) {
            // Handle file I/O exceptions (e.g., file save errors)
            logger.error("Error processing resume file for candidate {}. Error: {}", formData.get("fullName"), ex.getMessage());
            CandidateResponseDto errorResponse = new CandidateResponseDto(
                    "Error",
                    "Error processing resume file.",
                    new CandidateResponseDto.CandidateData(null, null, null),
                    null
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
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
                    "Error occurred while deleting the candidate."+ex.getMessage(),
                    null,
                    ex.getMessage()
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}