package com.profile.candidate.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.profile.candidate.dto.*;
import com.profile.candidate.exceptions.*;
import com.profile.candidate.repository.InterviewRepository;
import com.profile.candidate.service.CandidateService;
import com.profile.candidate.service.InterviewService;
import com.profile.candidate.service.SubmissionService;
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

@CrossOrigin(origins = {"http://35.188.150.92", "http://192.168.0.140:3000", "http://192.168.0.139:3000","https://mymulya.com", "http://localhost:3000","http://192.168.0.135:3000",
        "http://192.168.0.135:80",
        "http://mymulya.com:443",
        "http://182.18.177.16:443",
        "http://localhost/","http://192.168.0.135",
        "http://182.18.177.16"})
@RestController
@RequestMapping("/candidate")
public class InterviewController {

    @Autowired
    InterviewService interviewService;
    @Autowired
    InterviewRepository interviewRepository;
    @Autowired
    CandidateService candidateService;
    @Autowired
    SubmissionService submissionService;
    private static final Logger logger = LoggerFactory.getLogger(InterviewController.class);

    @PostMapping("/interview-schedule/{userId}")
    public ResponseEntity<InterviewResponseDto> scheduleInterview(
            @PathVariable String userId,
            @RequestBody InterviewDto interviewRequest) {
        try {
            // Log the incoming interview request
            logger.info("Received interview request for userId: {} with candidateId: {}", userId, interviewRequest.getCandidateId());

            boolean isInterviewScheduled = interviewService.isInterviewScheduled(interviewRequest.getCandidateId(),interviewRequest.getJobId(), interviewRequest.getInterviewDateTime());
            if (isInterviewScheduled) {
                logger.error("Interview Already Scheduled for Candidate Id :"+interviewRequest.getCandidateId());
                throw new InterviewAlreadyScheduledException("Interview Already Scheduled for Candidate Id :"+interviewRequest.getCandidateId());
            }

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
                    interviewRequest.isSkipNotification(),
                    interviewRequest.getAssignedTo(),
                    interviewRequest.getComments());
            return ResponseEntity.ok(response);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
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
                    interviewRequest.getInternalFeedback(),
                    interviewRequest.getInterviewStatus(),
                    interviewRequest.isSkipNotification(),
                    interviewRequest.getAssignedTo()); // Added status update
            return ResponseEntity.ok(response);

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
            //Check if the candidate belongs to the user// Proceed with scheduling the interview if the validation passes
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
                    interviewRequest.isSkipNotification(),
                    interviewRequest.getAssignedTo(),
                    interviewRequest.getComments());
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
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    @PutMapping("/interview-update/{candidateId}/{jobId}")
    public ResponseEntity<InterviewResponseDto> updateScheduledInterviewWithOutUserId(
            @PathVariable String candidateId,
            @PathVariable String jobId,
            @RequestBody InterviewDto interviewRequest) {

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
                    interviewRequest.getInternalFeedback(),
                    interviewRequest.getInterviewStatus(),
                    interviewRequest.isSkipNotification(),
                    interviewRequest.getComments()
            ); // Added status update
            return ResponseEntity.ok(response);
    }
    @GetMapping("/interviews/interviewsByUserId/{userId}")
    public ResponseEntity<List<GetInterviewResponseDto>> getInterviewsByUserId(
            @PathVariable String userId,
            @RequestParam(defaultValue = "ALL") String interviewLevel  // NEW: filter by level
    ) throws JsonProcessingException {
        List<GetInterviewResponseDto> interviews = interviewService.getAllScheduledInterviewsByUserId(userId, interviewLevel);
        return new ResponseEntity<>(interviews, HttpStatus.OK);
    }

    @GetMapping("/interviews/{userId}/filterByDate")
    public ResponseEntity<GetInterviewResponse> getInterviewsByUserIdAndDateRange(
            @PathVariable String userId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "ALL") String interviewLevel // NEW: filter by level
    ) {
        GetInterviewResponse interviews = interviewService.getScheduledInterviewsByUserIdAndDateRange(
                userId, startDate, endDate, interviewLevel);
        return ResponseEntity.ok(interviews);
    }

    @GetMapping("/interviews/filterByDate")
    public ResponseEntity<GetInterviewResponse> getInterviewsByDateRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

            GetInterviewResponse interviews = interviewService.getScheduledInterviewsByDateOnly(startDate, endDate);

            return ResponseEntity.ok(interviews);

    }
    @GetMapping("/interviews/teamlead/{userId}")
    public ResponseEntity<TeamleadInterviewsDTO> getInterviewsForTeamlead(@PathVariable String userId) {
        try {
            // Call the service to get the teamlead interviews
            TeamleadInterviewsDTO teamleadInterviewsDTO = interviewService.getTeamleadScheduledInterviews(userId);
            // Return the response with status 200 OK
            return ResponseEntity.ok(teamleadInterviewsDTO);

        } catch (CandidateNotFoundException ex) {
            logger.error("No interviews found for teamlead with userId: {}", userId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        } catch (Exception ex) {
            logger.error("An error occurred while fetching interviews for teamlead with userId: {}: {}", userId, ex.getMessage(), ex);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @GetMapping("/interviews/teamlead/{userId}/filterByDate")
    public ResponseEntity<?> getTeamleadScheduledInterviewsByDateRange(
            @PathVariable String userId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            // Validate date range
            if (endDate.isBefore(startDate)) {
                logger.warn("End date {} is before start date {}", endDate, startDate);
                return ResponseEntity.badRequest()
                        .body(Collections.singletonMap("message", "End date cannot be before start date"));
            }
            // Call service to get scheduled interviews by team lead and date range
            TeamleadInterviewsDTO interviews = interviewService.getTeamleadScheduledInterviewsByDateRange(userId, startDate, endDate);

            if (interviews == null || (interviews.getSelfInterviews().isEmpty() && interviews.getTeamInterviews().isEmpty())) {
                logger.warn("No scheduled interviews found for userId: {} between {} and {}", userId, startDate, endDate);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "No interviews found for team lead: " + userId + " between " + startDate + " and " + endDate));
            }
            return ResponseEntity.ok(interviews);
        } catch (Exception e) {
            logger.error("Error while fetching scheduled interviews for userId: {} between {} and {}", userId, startDate, endDate, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "An error occurred while fetching interviews"));
        }
    }
    @GetMapping("/interviewSlots/{userId}")
    public ResponseEntity<InterviewSlotsDto> getInterviewSlots(
            @PathVariable String userId
    ){

        return new ResponseEntity<>(interviewService.getInterviewSlots(userId),HttpStatus.OK);
    }


    @PutMapping("/updateInterviewByCoordinator/{coordinatorId}/{interviewId}")
    public ResponseEntity<InterviewResponseDto> updateInterviewByCoordinator(
            @PathVariable String coordinatorId,@PathVariable String interviewId,
            @RequestBody CoordinatorInterviewUpdateDto dto){

       return new ResponseEntity<>(interviewService.updateInterviewByCoordinator(coordinatorId,interviewId,dto),HttpStatus.OK);
    }

    @GetMapping("/coordinatorInterviews/{userId}")
    public ResponseEntity<List<CoordinatorInterviewDto>> getCoordinatorInterviews(String userId){

          return new ResponseEntity<>(interviewService.getCoordinatorInterviews(userId),HttpStatus.OK);
    }
}
