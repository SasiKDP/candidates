package com.profile.candidate.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.profile.candidate.dto.*;
import com.profile.candidate.exceptions.CandidateNotFoundException;
import com.profile.candidate.exceptions.DateRangeValidationException;
import com.profile.candidate.exceptions.InterviewNotScheduledException;
import com.profile.candidate.repository.InterviewRepository;
import com.profile.candidate.service.CandidateService;
import com.profile.candidate.service.InterviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/candidate")
public class InterviewController {

    @Autowired
    InterviewService interviewService;
    @Autowired
    InterviewRepository interviewRepository;
    @Autowired
    CandidateService candidateService;

    private static final Logger logger = LoggerFactory.getLogger(InterviewController.class);

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
            boolean isInterviewScheduled = interviewService.isInterviewScheduled(interviewRequest.getCandidateId(),interviewRequest.getJobId(), interviewRequest.getInterviewDateTime());
            if (isInterviewScheduled) {
                // Return a 400 Bad Request response if an interview is already scheduled
                return ResponseEntity.badRequest().body(new InterviewResponseDto(
                        false,
                        "An interview is already scheduled for this candidate at the specified time.",
                        null,
                        null
                ));
            }
             //Check if the candidate belongs to the user
            boolean isValidCandidate = interviewService.isCandidateValidForUser(userId, interviewRequest.getCandidateId());
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
            InterviewResponseDto response = interviewService.scheduleInterview(
                    userId,
                    interviewRequest.getCandidateId(),
                    interviewRequest.getInterviewDateTime(),
                    interviewRequest.getDuration(),
                    interviewRequest.getZoomLink(),
                    interviewRequest.getUserEmail(), // Pass userEmail
                    interviewRequest.getClientEmail(),
                    interviewRequest.getClientName(),
                    interviewRequest.getInterviewLevel(),
                    interviewRequest.getExternalInterviewDetails(),
                    interviewRequest.getJobId(),
                    interviewRequest.getFullName(),
                    interviewRequest.getContactNumber(),
                    interviewRequest.getCandidateEmailId(),
                    interviewRequest.isSkipNotification());
            return ResponseEntity.ok(response);
        } catch (CandidateNotFoundException e) {
            // If the candidate is not found
            logger.error("Candidate not found for userId: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new InterviewResponseDto(
                    false,
                    "Candidate not found for the User Id :"+userId,
                    null,
                    null
            ));
//        } catch (Exception e) {
//            // Log unexpected errors and return 500
//            logger.error("Error while scheduling interview: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new InterviewResponseDto(
//                    false,
//                    "An error occurred while scheduling the interview.",
//                    null,
//                    null
//            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    //@GetMapping("/allInterviews")
    @GetMapping(value = "/allInterviews")
    public ResponseEntity<GetInterviewResponse> getAllInterviews(){

        return new ResponseEntity<>(interviewService.getAllInterviews(),HttpStatus.OK);
    }
     @GetMapping("/interviewByCandidateId/{candidateId}")
     public ResponseEntity<GetInterviewResponse> getInterviewByCandidateId(@PathVariable String candidateId){

      return new ResponseEntity<>(interviewService.getInterviews(candidateId),HttpStatus.OK);
     }
     @GetMapping("/interviewsById/{interviewId}")
     public ResponseEntity<GetInterviewResponse> getInterviewById(@PathVariable String interviewId){

        return new ResponseEntity<>(interviewService.getInterviewsById(interviewId),HttpStatus.OK);
     }
    @DeleteMapping("/deleteinterview/{candidateId}/{jobId}")
    public ResponseEntity<DeleteInterviewResponseDto> deleteInterview(@PathVariable String candidateId,@PathVariable String jobId) {
        try {
            logger.info("Received request to Remove Scheduled Interview Details for candidateId: {}", candidateId);
            interviewService.deleteInterview(candidateId,jobId);
            DeleteInterviewResponseDto response = new DeleteInterviewResponseDto(
                    "success",
                    "Scheduled Interview is Removed successfully for candidateId: " + candidateId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (InterviewNotScheduledException e) {
            logger.error("Scheduled Interview not found for candidateId: {}", candidateId);
            DeleteInterviewResponseDto errorResponse = new DeleteInterviewResponseDto(
                    "error",
                    e.getMessage()
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error Removing Scheduled Interview details for candidateId {}: {}", candidateId, e.getMessage());

            DeleteInterviewResponseDto errorResponse = new DeleteInterviewResponseDto(
                    "error",
                    "An error occurred while Removing the Scheduled Interview details."
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PutMapping("/interview-update/{userId}/{candidateId}/{jobId}")
    public ResponseEntity<InterviewResponseDto> updateScheduledInterview(
            @PathVariable String userId,
            @PathVariable String candidateId,
            @PathVariable String jobId,
            @RequestBody InterviewDto interviewRequest) {
        //try {
            logger.info("Received interview update request for userId: {} and candidateId: {}", userId, candidateId);

            if (candidateId == null || userId == null) {
                return ResponseEntity.badRequest().body(new InterviewResponseDto(
                        false, "Candidate ID or User ID cannot be null.", null, null
                ));
            }
            InterviewResponseDto response = interviewService.updateScheduledInterview(
                    userId,
                    candidateId,
                    interviewRequest.getCandidateEmailId(),
                    jobId,
                    interviewRequest.getInterviewDateTime(),
                    interviewRequest.getDuration(),
                    interviewRequest.getZoomLink(),
                    interviewRequest.getUserEmail(),
                    interviewRequest.getClientEmail(),
                    interviewRequest.getClientName(),
                    interviewRequest.getInterviewLevel(),
                    interviewRequest.getExternalInterviewDetails(),
                    interviewRequest.getInterviewStatus(),
                    interviewRequest.isSkipNotification()
                    ); // Added status update
            return ResponseEntity.ok(response);
       // }
//        catch (CandidateNotFoundException e) {
//            logger.error("Candidate not found for userId: {}", userId);
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new InterviewResponseDto(
//                    false, "Candidate not found for the User Id.", null, null
//            ));
//        }
//        catch (InterviewNotScheduledException e) {
//            logger.error("No interview scheduled for candidateId: {}", candidateId);
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new InterviewResponseDto(
//                    false, "No scheduled interview found for this candidate.", null, null
//            ));
//        }
//        catch (Exception e) {
//            logger.error("Error while updating interview: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new InterviewResponseDto(
//                    false, "An error occurred while updating the interview.", null, null
//            ));
//        }

    }
    @PostMapping("/interview-schedule")
    public ResponseEntity<InterviewResponseDto> scheduleInterviewWithoutUserId(
            @RequestBody InterviewDto interviewRequest) {
        try {
            // Log the incoming interview request
            logger.info("Received interview request for userId: {} with candidateId: {}",  interviewRequest.getCandidateId());

            // Ensure the candidateId is not null
            if (interviewRequest.getCandidateId() == null) {
                return ResponseEntity.badRequest().body(new InterviewResponseDto(
                        false,
                        "Candidate ID cannot be null  " ,
                        null,
                        null
                ));
            }
            // Check if an interview is already scheduled for the candidate at the specified time
            boolean isInterviewScheduled = interviewService.isInterviewScheduled(interviewRequest.getCandidateId(),interviewRequest.getJobId(), interviewRequest.getInterviewDateTime());
            if (isInterviewScheduled) {
                // Return a 400 Bad Request response if an interview is already scheduled
                return ResponseEntity.badRequest().body(new InterviewResponseDto(
                        false,
                        "An interview is already scheduled for this candidate at the specified time.",
                        null,
                        null
                ));
            }
            //Check if the candidate belongs to the user

            // Proceed with scheduling the interview if the validation passes
            InterviewResponseDto response = interviewService.scheduleInterviewWithOutUserId(
                    interviewRequest.getCandidateId(),
                    interviewRequest.getInterviewDateTime(),
                    interviewRequest.getDuration(),
                    interviewRequest.getZoomLink(),
                    interviewRequest.getClientEmail(),
                    interviewRequest.getClientName(),
                    interviewRequest.getInterviewLevel(),
                    interviewRequest.getExternalInterviewDetails(),
                    interviewRequest.getJobId(),
                    interviewRequest.getFullName(),
                    interviewRequest.getContactNumber(),
                    interviewRequest.getCandidateEmailId(),
                    interviewRequest.isSkipNotification());
            return ResponseEntity.ok(response);
        } catch (CandidateNotFoundException e) {
            // If the candidate is not found
            logger.error("Candidate not found for userId: {}");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new InterviewResponseDto(
                    false,
                    "Candidate not found ",
                    null,
                    null
            ));
//        } catch (Exception e) {
//            // Log unexpected errors and return 500
//            logger.error("Error while scheduling interview: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new InterviewResponseDto(
//                    false,
//                    "An error occurred while scheduling the interview.",
//                    null,
//                    null
//            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    @PutMapping("/interview-update/{candidateId}/{jobId}")
    public ResponseEntity<InterviewResponseDto> updateScheduledInterviewWithOutUserId(
            @PathVariable String candidateId,
            @PathVariable String jobId,
            @RequestBody InterviewDto interviewRequest) {
        //try {
            logger.info("Received interview update request for and candidateId: {}", candidateId);

            if (candidateId == null || jobId == null) {
                return ResponseEntity.badRequest().body(new InterviewResponseDto(
                        false, "Candidate ID or JobID cannot be null.", null, null
                ));
            }
            InterviewResponseDto response = interviewService.updateScheduledInterviewWithoutUserId(
                    candidateId,
                    interviewRequest.getCandidateEmailId(),
                    jobId,
                    interviewRequest.getInterviewDateTime(),
                    interviewRequest.getDuration(),
                    interviewRequest.getZoomLink(),
                    interviewRequest.getClientEmail(),
                    interviewRequest.getClientName(),
                    interviewRequest.getInterviewLevel(),
                    interviewRequest.getExternalInterviewDetails(),
                    interviewRequest.getInterviewStatus(),
                    interviewRequest.isSkipNotification()
            ); // Added status update

            return ResponseEntity.ok(response);
    }
    @GetMapping("/interviews/interviewsByUserId/{userId}")
    public ResponseEntity<GetInterviewResponse> getInterviewsByUserId(@PathVariable String userId){

       return new ResponseEntity<>(interviewService.getInterviewsByUserId(userId),HttpStatus.OK);
    }
    @GetMapping("/interviews/{userId}/filterByDate")
    public ResponseEntity<GetInterviewResponse> getInterviewsByUserIdAndDateRange(
            @PathVariable String userId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        GetInterviewResponse interviews = interviewService.getScheduledInterviewsByUserIdAndDateRange(userId, startDate, endDate);

        return ResponseEntity.ok(interviews);

    }

    @GetMapping("/interviews/filterByDate")
    public ResponseEntity<GetInterviewResponse> getInterviewsByDateRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

            GetInterviewResponse interviews = interviewService.getScheduledInterviewsByDateOnly(startDate, endDate);

            return ResponseEntity.ok(interviews);

    }


}
