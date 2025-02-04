package com.profile.candidate.controller;

import com.profile.candidate.dto.*;
import com.profile.candidate.exceptions.CandidateAlreadyExistsException;
import com.profile.candidate.exceptions.CandidateNotFoundException;
import com.profile.candidate.model.CandidateDetails;
import com.profile.candidate.repository.CandidateRepository;
import com.profile.candidate.service.CandidateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@CrossOrigin(origins = {"http://35.188.150.92", "http://192.168.0.140:3000", "http://192.168.0.139:3000","https://mymulya.com/"})



@RestController
@RequestMapping("/candidate")
public class CandidateController {

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private CandidateRepository candidateRepository;

    private static final Logger logger = LoggerFactory.getLogger(CandidateController.class);

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
            // Check if the resume file is valid (PDF or DOCX)
            if (!isValidFileType(resumeFile)) {
                // Log the invalid file type error
                logger.error("Invalid file type uploaded for candidate {}. Only PDF and DOCX are allowed.", fullName);

                // Return the error response in the correct format
                return new ResponseEntity<>(new CandidateResponseDto(
                        "Error",
                        "Invalid file type. Only PDF and DOCX are allowed.",
                        new CandidateResponseDto.Payload(null, null, null),
                        null
                ), HttpStatus.BAD_REQUEST); // Return HTTP 400 for invalid file type
            }

            // Construct CandidateDetails object from request parameters
            CandidateDetails candidateDetails = new CandidateDetails();
            candidateDetails.setJobId(jobId);
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
            candidateDetails.setPreferredLocation(preferredLocation);
            candidateDetails.setSkills(skills);
            candidateDetails.setCommunicationSkills(communicationSkills);
            candidateDetails.setRequiredTechnologiesRating(requiredTechnologiesRating);
            candidateDetails.setOverallFeedback(overallFeedback);
            candidateDetails.setRelevantExperience(relevantExperience);
            candidateDetails.setCurrentOrganization(currentOrganization);
            candidateDetails.setUserEmail(userEmail);

            // Call service method to submit the candidate and handle file upload
            CandidateResponseDto response = candidateService.submitCandidate(candidateDetails, resumeFile);

            // Log the success of candidate submission
            logger.info("Candidate successfully submitted: {}", fullName);

            // Return success response
            return new ResponseEntity<>(response, HttpStatus.OK);

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
            logger.error("Error processing resume file: {}", ex.getMessage());
            CandidateResponseDto errorResponse = new CandidateResponseDto(
                    "Error",
                    "Error processing resume file.",
                    new CandidateResponseDto.Payload(null, null, null),
                    null
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR); // 500 Internal Server Error

        } catch (Exception ex) {
            // General error handler for any issues during candidate submission
            logger.error("An error occurred while submitting the candidate: {}", ex.getMessage());
            CandidateResponseDto errorResponse = new CandidateResponseDto(
                    "Error",
                    "An error occurred while submitting the candidate",
                    new CandidateResponseDto.Payload(null, null, null),
                    null
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR); // 500 Internal Server Error
        }
    }

    private boolean isValidFileType(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName != null) {
            String fileExtension = getFileExtension(fileName).toLowerCase();
            return fileExtension.equals("pdf") || fileExtension.equals("docx");
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
            @RequestParam(value = "resumeFile", required = false) MultipartFile resumeFile) {

        try {
            // Create a CandidateDetails object from the request parameters
            CandidateDetails updatedCandidateDetails = new CandidateDetails();

            updatedCandidateDetails.setJobId(jobId);
            updatedCandidateDetails.setUserId(userId);
            updatedCandidateDetails.setFullName(fullName);
            updatedCandidateDetails.setCandidateEmailId(candidateEmailId);
            updatedCandidateDetails.setContactNumber(contactNumber);
            updatedCandidateDetails.setQualification(qualification);
            updatedCandidateDetails.setTotalExperience(totalExperience != null ? totalExperience : 0);
            updatedCandidateDetails.setCurrentCTC(currentCTC);
            updatedCandidateDetails.setExpectedCTC(expectedCTC);
            updatedCandidateDetails.setNoticePeriod(noticePeriod);
            updatedCandidateDetails.setCurrentLocation(currentLocation);
            updatedCandidateDetails.setPreferredLocation(preferredLocation);
            updatedCandidateDetails.setSkills(skills);
            updatedCandidateDetails.setCommunicationSkills(communicationSkills);
            updatedCandidateDetails.setRequiredTechnologiesRating(requiredTechnologiesRating);
            updatedCandidateDetails.setOverallFeedback(overallFeedback);
            updatedCandidateDetails.setRelevantExperience(relevantExperience != null ? relevantExperience : 0);
            updatedCandidateDetails.setCurrentOrganization(currentOrganization);

            // Call the service method to resubmit the candidate
            CandidateResponseDto response = candidateService.resubmitCandidate(candidateId, updatedCandidateDetails, resumeFile);

            // Return the response entity with status 200 OK
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception ex) {
            // Handle any exceptions and return an error response
            logger.error("An error occurred while resubmitting the candidate: {}", ex.getMessage());
            CandidateResponseDto errorResponse = new CandidateResponseDto(
                    "An error occurred while resubmitting the candidate", null, null, null
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Endpoint to fetch all submitted candidates without filtering by userId
    @GetMapping("/submissions/allsubmittedcandidates")
    public ResponseEntity<List<CandidateGetResponseDto>> getAllSubmissions() {
        try {
            // Fetch all submissions from the service
            List<CandidateGetResponseDto> submissions = candidateService.getAllSubmissions();

            // Log success
            logger.info("Fetched {} submissions successfully.", submissions.size());

            // Return response with HTTP 200 OK
            return ResponseEntity.ok(submissions);
        } catch (CandidateNotFoundException ex) {
            // Log not found error
            logger.error("No candidate submissions found: {}", ex.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            // Log unexpected error
            logger.error("An error occurred while fetching submissions: {}", ex.getMessage(), ex);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    // Endpoint to fetch all submitted candidates
    @GetMapping("/submissions/{userId}")
    public ResponseEntity<List<CandidateGetResponseDto>> getAllSubmissions(
            @PathVariable String userId) {  // Use PathVariable to get the userId from the URL
        try {
            // Fetch all submissions based on the userId
            List<CandidateGetResponseDto> submissions = candidateService.getSubmissionsByUserId(userId);

            // Log success
            logger.info("Fetched {} submissions successfully for userId: {}", submissions.size(), userId);

            // Return all candidate details with status 200 OK
            return ResponseEntity.ok(submissions);

        } catch (CandidateNotFoundException ex) {
            // Handle specific CandidateNotFoundException
            logger.error("No submissions found for userId: {}", userId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        } catch (Exception ex) {
            // Log the error and return HTTP 500
            logger.error("An error occurred while fetching submissions: {}", ex.getMessage(), ex);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

//    @PostMapping("/upload-resume/{candidateId}")
//    public ResponseEntity<String> uploadResume(@PathVariable String candidateId,
//                                               @RequestParam("file") MultipartFile file) {
//        try {
//            // Check if file is present
//            if (file.isEmpty()) {
//                logger.error("No file uploaded.");
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No file uploaded. Please upload a file.");
//            }
//
//            // Call the service to upload the resume
//            String resultMessage = candidateService.uploadResume(candidateId, file);
//
//            // If the upload is successful, respond with a 200 status code
//            logger.info("Resume uploaded successfully for candidateId: {}", candidateId);
//            return ResponseEntity.ok(resultMessage);
//
//        } catch (CandidateNotFoundException ex) {
//            logger.error("Candidate not found for candidateId: {}", candidateId);
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Candidate not found.");
//        } catch (InvalidFileTypeException ex) {
//            logger.error("Invalid file type for candidateId: {}", candidateId);
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Only .pdf and .docx files are allowed.");
//        } catch (MaxUploadSizeExceededException ex) {
//            logger.error("File size exceeds limit for candidateId: {}", candidateId);
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File size exceeds the maximum limit.");
//        } catch (IOException ex) {
//            logger.error("Error uploading the resume for candidateId: {}", candidateId, ex);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading the resume.");
//        } catch (Exception ex) {
//            logger.error("An unexpected error occurred while uploading resume for candidateId: {}", candidateId, ex);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while uploading the resume.");
//        }
//    }


    // Fetch resume for a candidate
    @GetMapping("/download-resume/{candidateId}")
    public ResponseEntity<UrlResource> downloadResume(@PathVariable String candidateId) {
        try {
            // Find the candidate by ID to get the resume file path
            CandidateDetails candidate = candidateRepository.findById(candidateId)
                    .orElseThrow(() -> new CandidateNotFoundException("Candidate not found"));

            // Get the file path from the candidate entity
            String resumeFilePath = candidate.getResumeFilePath();
            Path path = Paths.get(resumeFilePath);

            // Check if the file exists
            if (Files.exists(path)) {
                // Create a resource for the file using UrlResource
                UrlResource resource = new UrlResource(path.toUri());

                // Get the filename and determine the file type
                String filename = path.getFileName().toString();
                String contentType = Files.probeContentType(path);

                // If the content type is null, set a default content type
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                // Return the file as a resource in the response
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                        .body(resource);  // Return the UrlResource as a Resource
            } else {
                // If the file doesn't exist, return a 404 error
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null);  // No file found
            }

        }  catch (CandidateNotFoundException ex) {
            // Log the error and return a 404 response
            logger.error("Candidate not found for candidateId: {}", candidateId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);  // No file found for the given candidate

        } catch (FileNotFoundException ex) {
            // Log the error and return a 404 response if file not found
            logger.error("File not found for candidateId: {}", candidateId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);  // Resume file doesn't exist

        } catch (IOException ex) {
            // Log any I/O errors (e.g. access issues with the file)
            logger.error("Error accessing the resume for candidateId: {}", candidateId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);  // Some error occurred while accessing the file

        } catch (Exception ex) {
            // Log any unexpected errors
            logger.error("An unexpected error occurred while downloading the resume for candidateId: {}", candidateId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);  // Unexpected error
        }
    }

    @PostMapping("/interview-schedule/{userId}")
    public ResponseEntity<InterviewResponseDto> scheduleInterview(
            @PathVariable String userId,
            @RequestBody InterviewDto interviewRequest) {
        try {
            // Log the incoming interview request
            logger.info("Received interview request for userId: {} with candidateId: {}", userId, interviewRequest.getCandidateId());

            // Ensure the candidateId is not null
            if (interviewRequest.getCandidateId() == null) {
                return ResponseEntity.badRequest().body(new InterviewResponseDto(
                        false,
                        "Candidate ID cannot be null for userId: " + userId,
                        null,
                        null
                ));
            }
            // Check if an interview is already scheduled for the candidate at the specified time
            boolean isInterviewScheduled = candidateService.isInterviewScheduled(interviewRequest.getCandidateId(), interviewRequest.getInterviewDateTime());
            if (isInterviewScheduled) {
                // Return a 400 Bad Request response if an interview is already scheduled
                return ResponseEntity.badRequest().body(new InterviewResponseDto(
                        false,
                        "An interview is already scheduled for this candidate at the specified time.",
                        null,
                        null
                ));
            }

            // Check if the candidate belongs to the user
            boolean isValidCandidate = candidateService.isCandidateValidForUser(userId, interviewRequest.getCandidateId());
            if (!isValidCandidate) {
                // If the candidateId does not belong to the userId, return a 403 Forbidden response
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new InterviewResponseDto(
                        false,
                        "Candidate ID does not belong to the provided userId.",
                        null,
                        null
                ));
            }

            // Proceed with scheduling the interview if the validation passes
            InterviewResponseDto response = candidateService.scheduleInterview(
                    userId,
                    interviewRequest.getCandidateId(),
                    interviewRequest.getInterviewDateTime(),
                    interviewRequest.getDuration(),
                    interviewRequest.getZoomLink(),
                    interviewRequest.getUserEmail(), // Pass userEmail
                    interviewRequest.getClientEmail(),
                    interviewRequest.getClientName(),
                    interviewRequest.getInterviewLevel(),// Pass clientEmail
                    interviewRequest.getExternalInterviewDetails()
            );

            return ResponseEntity.ok(response);
        } catch (CandidateNotFoundException e) {
            // If the candidate is not found
            logger.error("Candidate not found for userId: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new InterviewResponseDto(
                    false,
                    "Candidate not found for the User Id.",
                    null,
                    null
            ));
        } catch (Exception e) {
            // Log unexpected errors and return 500
            logger.error("Error while scheduling interview: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new InterviewResponseDto(
                    false,
                    "An error occurred while scheduling the interview.",
                    null,
                    null
            ));
        }
    }


    @GetMapping("/interviews/{userId}")
    public ResponseEntity<List<GetInterviewResponseDto>> getAllScheduledInterviews(
            @PathVariable String userId) {
        try {
            // Fetch all scheduled interviews for the given userId
            List<GetInterviewResponseDto> interviewDetails = candidateService.getAllScheduledInterviewsByUserId(userId);

            // Return response with status 200 OK and the interview details
            return ResponseEntity.ok(interviewDetails);

        } catch (CandidateNotFoundException ex) {
            // If no interviews are found for the given userId
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        } catch (Exception ex) {
            // If any other error occurs
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Endpoint to fetch all scheduled interviews (no userId filter)
    @GetMapping("/allscheduledinterviews")
    public ResponseEntity<List<GetInterviewResponseDto>> getAllScheduledInterviews() {
        try {
            List<GetInterviewResponseDto> interviews = candidateService.getAllScheduledInterviews();
            return ResponseEntity.ok(interviews);
        } catch (Exception ex) {
            // Handle exceptions
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
