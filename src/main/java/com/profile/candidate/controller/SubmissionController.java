package com.profile.candidate.controller;

import com.profile.candidate.dto.DeleteSubmissionResponseDto;
import com.profile.candidate.dto.ErrorResponseDto;
import com.profile.candidate.dto.SubmissionsGetResponse;
import com.profile.candidate.exceptions.CandidateNotFoundException;
import com.profile.candidate.model.Submissions;
import com.profile.candidate.repository.SubmissionRepository;
import com.profile.candidate.service.CandidateService;
import com.profile.candidate.service.SubmissionService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/submission")
public class SubmissionController {

    @Autowired
    SubmissionService submissionService;
    @Autowired
    SubmissionRepository submissionRepository;
    @Autowired
    CandidateService candidateService;
    private static final Logger logger = LoggerFactory.getLogger(SubmissionController.class);

    @GetMapping("/submissions")
    public ResponseEntity<List<SubmissionsGetResponse>> getAllSubmissions(){

        return new  ResponseEntity<>(submissionService.getAllSubmissions(),HttpStatus.OK);
    }
    @GetMapping("/submissions/{candidateId}")
    public ResponseEntity<List<SubmissionsGetResponse>> getSubmissions(@PathVariable String candidateId){

        return new ResponseEntity<>(submissionService.getSubmissions(candidateId),HttpStatus.OK);
    }
    @GetMapping("/submissionsById/{submissionId}")
    public ResponseEntity<SubmissionsGetResponse> getSubmissionById(@PathVariable String submissionId){

        return new ResponseEntity<>(submissionService.getSubmissionById(submissionId),HttpStatus.OK);
    }
    @GetMapping("/submissionsByUserId/{userId}")
    public ResponseEntity<List<SubmissionsGetResponse>> getSubmissionsByUserId(@PathVariable String userId){

        return new ResponseEntity<>(submissionService.getSubmissionsByUserId(userId),HttpStatus.OK);
    }
    @GetMapping("/download-resume/{candidateId}/{jobId}")
    public ResponseEntity<Object> downloadResume(@PathVariable String candidateId,@PathVariable String jobId) {
        try {
            logger.info("Downloading resume for candidate ID: {}", candidateId);
            // Fetch candidate details from the database
            Submissions submissions = submissionRepository.findByCandidate_CandidateIdAndJobId(candidateId,jobId);
            if (submissions==null) throw new CandidateNotFoundException("Candidate not found with ID: "+candidateId+" and JobId: "+jobId);

            // Fetch the resume BLOB field from the candidate entity
            byte[] resumeBytes = submissions.getResume(); // Assuming `getResume()` returns the BLOB data

            if (resumeBytes == null || resumeBytes.length == 0) {
                logger.error("Resume is missing for candidate ID and Job Id: {}", candidateId,jobId);
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponseDto(false, "Resume is missing for candidate ID: " + candidateId));
            }
            // Assuming you want to set the filename based on candidate's name or other criteria
            String filename = submissions.getCandidate().getFullName()+ "-Resume.pdf"; // Adjust filename logic as needed

            // Convert the byte array to a ByteArrayResource
            ByteArrayResource resource = new ByteArrayResource(resumeBytes);

            // Set content type (you can change this to match the actual file type)
            String contentType = "application/pdf"; // You can dynamically determine the content type if needed

            // Return the file as a response for download
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
            // Return the response entity with status 200 OK
            return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
