package com.profile.candidate.controller;

import com.profile.candidate.dto.*;
import com.profile.candidate.exceptions.CandidateNotFoundException;
import com.profile.candidate.exceptions.DateRangeValidationException;
import com.profile.candidate.model.CandidateDetails;
import com.profile.candidate.model.Submissions;
import com.profile.candidate.repository.SubmissionRepository;
import com.profile.candidate.service.CandidateService;
import com.profile.candidate.service.SubmissionService;
import jakarta.transaction.Transactional;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.*;
//@CrossOrigin(origins = {"http://35.188.150.92", "http://192.168.0.140:3000", "http://192.168.0.139:3000","https://mymulya.com", "http://localhost:3000","http://192.168.0.135:3000",
//        "http://192.168.0.135:80",
//        "http://mymulya.com:443",
//        "http://182.18.177.16:443",
//        "http://localhost/","http://192.168.0.135",
//        "http://182.18.177.16"})

@RestController
@RequestMapping("/candidate")
public class SubmissionController {

    @Autowired
    SubmissionService submissionService;
    @Autowired
    SubmissionRepository submissionRepository;
    @Autowired
    CandidateService candidateService;
    private static final Logger logger = LoggerFactory.getLogger(SubmissionController.class);

    @GetMapping("/submissions")
    public ResponseEntity<SubmissionsGetResponse> getAllSubmissions(){

        return new  ResponseEntity<>(submissionService.getAllSubmissions(),HttpStatus.OK);
    }
    @GetMapping("/submissions/filterByDate")
    public ResponseEntity<List<SubmissionGetResponseDto>> getAllSubmissionsByDateRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<SubmissionGetResponseDto> submissions =
                submissionService.getAllSubmissionsByDateRange(startDate, endDate);
        if (submissions.isEmpty()) {
            logger.warn("No submissions found between {} and {}", startDate, endDate);
            throw new CandidateNotFoundException("No submissions found in the given date range.");
        }
        logger.info("Fetched {} submissions between {} and {}", submissions.size(), startDate, endDate);
        return ResponseEntity.ok(submissions);
    }
    @GetMapping("/submissions/{userId}/filterByDate")
    public ResponseEntity<?> getSubmissionsByUserIdAndDateRange(
            @PathVariable String userId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            // Fetch submissions by userId within the given date range
            List<SubmissionGetResponseDto> submissions = submissionService.getSubmissionsByUserIdAndDateRange(userId, startDate, endDate);
            // Check if submissions are found
            if (submissions.isEmpty()) {
                logger.warn("No submissions found for userId: {} between {} and {}", userId, startDate, endDate);
                throw new CandidateNotFoundException("No submissions found for userId: " + userId + " between " + startDate + " and " + endDate);
            }
            // Log success
            logger.info("Fetched {} submissions successfully for userId: {} between {} and {}", submissions.size(), userId, startDate, endDate);
            // Return all candidate details with status 200 OK
            return ResponseEntity.ok(submissions);
        } catch (CandidateNotFoundException ex) {
            // Return message in JSON body for 404
            logger.error("No submissions found for userId: {} between {} and {}", userId, startDate, endDate,ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("message", ex.getMessage()));

        } catch (Exception ex) {
            // Log the error and return HTTP 500 with message
            logger.error("An error occurred while fetching submissions: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "An internal error occurred while fetching submissions."+ex.getMessage()));
        }
    }
    @GetMapping("/submissions/{candidateId}")
    public ResponseEntity<SubmissionsGetResponse> getSubmissions(@PathVariable String candidateId){

        return new ResponseEntity<>(submissionService.getSubmissions(candidateId),HttpStatus.OK);
    }
    @GetMapping("/submissionsById/{submissionId}")
    public ResponseEntity<SubmissionsGetResponse> getSubmissionById(@PathVariable String submissionId){
        logger.info("Getting Submissions for submission Id {}",submissionId);
        return new ResponseEntity<>(submissionService.getSubmissionById(submissionId),HttpStatus.OK);
    }
    @GetMapping("/submissionsByUserId/{userId}")
    public ResponseEntity<List<SubmissionGetResponseDto>> getSubmissionsByUserId(@PathVariable String userId){
        logger.info("Getting Submissions for user Id {}",userId);
        return new ResponseEntity<>(submissionService.getSubmissionsByUserId(userId),HttpStatus.OK);
    }

    @GetMapping("/download-resume/{candidateId}/{jobId}")
    public ResponseEntity<Object> downloadResume(@PathVariable String candidateId, @PathVariable String jobId) {
        try {
            logger.info("Downloading resume for candidate ID: {}", candidateId);
            Submissions submissions = submissionRepository.findByCandidate_CandidateIdAndJobId(candidateId, jobId);
            if (submissions == null) {
                logger.error("Submission Not Found with Candidate ID : {} for Job Id :{}", candidateId, jobId,);
                throw new CandidateNotFoundException("Submissions not found with Candidate ID: " + candidateId + " and JobId: " + jobId);
            }

            byte[] resumeBytes = submissions.getResume();
            if (resumeBytes == null || resumeBytes.length == 0) {
                logger.error("Resume is missing for candidate ID and Job Id: {}", candidateId, jobId);
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponseDto(false, "Resume is missing for candidate ID: " + candidateId));
            }

            // Detect MIME type using Tika
            Tika tika = new Tika();
            String contentType = tika.detect(resumeBytes);

            // Get appropriate file extension
            String extension;
            try {
                extension = MimeTypes.getDefaultMimeTypes().forName(contentType).getExtension();
            } catch (Exception e) {
                extension = ".bin"; // fallback if type is unknown
            }

            // Build dynamic filename
            String filename = submissions.getCandidate().getFullName().replaceAll("\\s+", "_") + "-Resume" + extension;

            ByteArrayResource resource = new ByteArrayResource(resumeBytes);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (CandidateNotFoundException e) {
            logger.error("Candidate not found: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDto(false, e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error while downloading resume for candidate ID {}: {}", candidateId, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto(false, "Unexpected error: " + e.getMessage()));
        }
    }

    @Transactional
    @DeleteMapping("deletesubmission/{submissionId}")
    public ResponseEntity<DeleteSubmissionResponseDto> deleteSubmission(@PathVariable("submissionId") String submissionId) {
            // Call the service method to delete the candidate by ID and get the response DTO
            DeleteSubmissionResponseDto response = submissionService.deleteSubmissionById(submissionId);
            return new ResponseEntity<>(response, HttpStatus.OK);
    }
    @PutMapping("/editSubmission/{submissionId}")
    public ResponseEntity<CandidateResponseDto> editSubmission(
            @PathVariable("submissionId") String submissionId,
            @RequestParam Map<String, String> allParams,
            @RequestParam(value = "resumeFile", required = false) MultipartFile resumeFile) {

        Submissions updateSubmission = new Submissions();
        CandidateDetails updatedCandidateDetails = new CandidateDetails();

        updateSubmission.setJobId(allParams.get("jobId"));
        updatedCandidateDetails.setUserId(allParams.get("userId"));
        updatedCandidateDetails.setFullName(allParams.get("fullName"));
        updatedCandidateDetails.setCandidateEmailId(allParams.get("candidateEmailId"));
        updatedCandidateDetails.setContactNumber(allParams.get("contactNumber"));
        updatedCandidateDetails.setQualification(allParams.get("qualification"));
        if (allParams.get("totalExperience") != null) {
            updatedCandidateDetails.setTotalExperience(Float.parseFloat(allParams.get("totalExperience")));
        }
        updatedCandidateDetails.setCurrentCTC(allParams.get("currentCTC"));
        updatedCandidateDetails.setExpectedCTC(allParams.get("expectedCTC"));
        updatedCandidateDetails.setNoticePeriod(allParams.get("noticePeriod"));
        updatedCandidateDetails.setCurrentLocation(allParams.get("currentLocation"));
        if (allParams.get("relevantExperience") != null) {
            updatedCandidateDetails.setRelevantExperience(Float.parseFloat(allParams.get("relevantExperience")));
        }
        updatedCandidateDetails.setCurrentOrganization(allParams.get("currentOrganization"));
        updatedCandidateDetails.setUserEmail(allParams.get("userEmail"));

        updateSubmission.setCandidate(updatedCandidateDetails);
        updateSubmission.setPreferredLocation(allParams.get("preferredLocation"));
        updateSubmission.setSkills(allParams.get("skills"));
        updateSubmission.setCommunicationSkills(allParams.get("communicationSkills"));
        if (allParams.get("requiredTechnologiesRating") != null) {
            updateSubmission.setRequiredTechnologiesRating(Double.parseDouble(allParams.get("requiredTechnologiesRating")));
        }
        updateSubmission.setOverallFeedback(allParams.get("overallFeedback"));

        CandidateResponseDto response = submissionService.editSubmission(submissionId, updatedCandidateDetails, updateSubmission, resumeFile);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    @GetMapping("/submissions/teamlead/{userId}")
    public ResponseEntity<TeamleadSubmissionsDTO> getSubmissionsForTeamlead(@PathVariable String userId) {
        try {
            // Call the service to get the submissions
            TeamleadSubmissionsDTO submissionsDTO = submissionService.getSubmissionsForTeamlead(userId);
            // Return the response with status 200 OK
            return ResponseEntity.ok(submissionsDTO);

        } catch (CandidateNotFoundException ex) {
            logger.error("No submissions found for userId: {}", userId,ex.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            logger.error("An error occurred while fetching submissions: {}", ex.getMessage(), ex);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PutMapping("/editSubmissionSuperAdmin/{submissionId}")
    public ResponseEntity<CandidateResponseDto> editSubmissionWithOutUserId(
            @PathVariable("submissionId") String submissionId,
            @RequestParam Map<String, String> allParams,
            @RequestParam(value = "resumeFile", required = false) MultipartFile resumeFile) {

        Submissions updateSubmission = new Submissions();
        CandidateDetails updatedCandidateDetails = new CandidateDetails();

        updateSubmission.setJobId(allParams.get("jobId"));
        //updatedCandidateDetails.setUserId(allParams.get("userId"));
        updatedCandidateDetails.setFullName(allParams.get("fullName"));
        updatedCandidateDetails.setCandidateEmailId(allParams.get("candidateEmailId"));
        updatedCandidateDetails.setContactNumber(allParams.get("contactNumber"));
        updatedCandidateDetails.setQualification(allParams.get("qualification"));
        if (allParams.get("totalExperience") != null) {
            updatedCandidateDetails.setTotalExperience(Float.parseFloat(allParams.get("totalExperience")));
        }
        updatedCandidateDetails.setCurrentCTC(allParams.get("currentCTC"));
        updatedCandidateDetails.setExpectedCTC(allParams.get("expectedCTC"));
        updatedCandidateDetails.setNoticePeriod(allParams.get("noticePeriod"));
        updatedCandidateDetails.setCurrentLocation(allParams.get("currentLocation"));
        if (allParams.get("relevantExperience") != null) {
            updatedCandidateDetails.setRelevantExperience(Float.parseFloat(allParams.get("relevantExperience")));
        }
        updatedCandidateDetails.setCurrentOrganization(allParams.get("currentOrganization"));
        updatedCandidateDetails.setUserEmail(allParams.get("userEmail"));

        updateSubmission.setCandidate(updatedCandidateDetails);
        updateSubmission.setPreferredLocation(allParams.get("preferredLocation"));
        updateSubmission.setSkills(allParams.get("skills"));
        updateSubmission.setCommunicationSkills(allParams.get("communicationSkills"));
        if (allParams.get("requiredTechnologiesRating") != null) {
            updateSubmission.setRequiredTechnologiesRating(Double.parseDouble(allParams.get("requiredTechnologiesRating")));
        }
        updateSubmission.setOverallFeedback(allParams.get("overallFeedback"));

        CandidateResponseDto response = submissionService.editSubmissionWithOutUserId(submissionId, updatedCandidateDetails, updateSubmission, resumeFile);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    @GetMapping("/submissions/teamlead/{userId}/filterByDate")
    public ResponseEntity<?> getSubmissionsByDateRange(@PathVariable String userId,
      @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            TeamleadSubmissionsDTO submissions = submissionService.getSubmissionsForTeamlead(userId, startDate, endDate);
            return ResponseEntity.ok(submissions);
        } catch (DateRangeValidationException e) {
            logger.warn("Invalid date range: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching submissions for userId: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "An error occurred while fetching submissions"));
        }
    }
}
