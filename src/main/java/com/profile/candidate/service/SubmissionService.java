package com.profile.candidate.service;

import com.profile.candidate.dto.*;
import com.profile.candidate.exceptions.*;
import com.profile.candidate.model.CandidateDetails;
import com.profile.candidate.model.InterviewDetails;
import com.profile.candidate.model.Submissions;
import com.profile.candidate.repository.CandidateRepository;
import com.profile.candidate.repository.InterviewRepository;
import com.profile.candidate.repository.SubmissionRepository;
import jakarta.persistence.Tuple;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SubmissionService {

    @Autowired
    SubmissionRepository submissionRepository;
    @Autowired
    CandidateRepository candidateRepository;
    @Autowired
    InterviewEmailService emailService;
    @Autowired
    InterviewRepository interviewRepository;

    private static final Logger logger = LoggerFactory.getLogger(SubmissionService.class);

    public SubmissionsGetResponse getAllSubmissions() {

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
        List<Submissions> submissions = submissionRepository.findByProfileReceivedDateBetween(startOfMonth,endOfMonth);
        List<SubmissionsGetResponse.GetSubmissionData> data =submissions.stream()
                .map(this::convertToSubmissionsGetResponse)
                .collect(Collectors.toList());
        SubmissionsGetResponse response=new SubmissionsGetResponse(true,"Submissions found",data,null);
        return response;
    }

    public SubmissionsGetResponse getSubmissions(String candidateId) {
        Optional<CandidateDetails> candidateDetails = candidateRepository.findById(candidateId);
        if (candidateDetails.isEmpty()) {
            throw new CandidateNotFoundException("Invalid CandidateId " + candidateId);
        }
        List<Submissions> submissions = submissionRepository.findByCandidate_CandidateId(candidateId);
       List<SubmissionsGetResponse.GetSubmissionData> data=submissions.stream()
                .map(this::convertToSubmissionsGetResponse)
                .collect(Collectors.toList());
       SubmissionsGetResponse response=new SubmissionsGetResponse(true,"Submissions Found",data,null);
      return response;
    }
    public SubmissionsGetResponse getSubmissionById(String submissionId) {
        Optional<Submissions> submissions = submissionRepository.findById(submissionId);
        if (submissions.isEmpty()) {
            throw new SubmissionNotFoundException("Invalid SubmissionId " + submissionId);
        }
        List<SubmissionsGetResponse.GetSubmissionData> data= Collections.singletonList(convertToSubmissionsGetResponse(submissions.get()));
        SubmissionsGetResponse response=new SubmissionsGetResponse(true,"Submissions Found",data,null);
      return  response;
    }
    private SubmissionsGetResponse.GetSubmissionData convertToSubmissionsGetResponse(Submissions sub) {

        SubmissionsGetResponse.GetSubmissionData data = new SubmissionsGetResponse.GetSubmissionData();

        data.setSubmissionId(sub.getSubmissionId());
        data.setCandidateId(sub.getCandidate().getCandidateId());
        data.setJobId(sub.getJobId());
        data.setSubmittedAt(sub.getSubmittedAt());
        data.setCommunicationSkills(sub.getCommunicationSkills());
        data.setSkills(sub.getSkills());
        data.setOverallFeedback(sub.getOverallFeedback());
        data.setPreferredLocation(sub.getPreferredLocation());
        data.setProfileReceivedDate(sub.getProfileReceivedDate());
        data.setRequiredTechnologiesRating(sub.getRequiredTechnologiesRating());
        data.setClientName(sub.getClientName());
        data.setRecruiterName(sub.getRecruiterName());
        data.setStatus(sub.getStatus());
        data.setTechnology(submissionRepository.findJobTitleByJobId(sub.getJobId()));

        CandidateDetails candidate = sub.getCandidate();
        //CandidateDto candidateDto = new CandidateDto();
        data.setUserId(candidate.getUserId());
        data.setFullName(candidate.getFullName());
        data.setCandidateEmailId(candidate.getCandidateEmailId());
        data.setContactNumber(candidate.getContactNumber());
        data.setCurrentOrganization(candidate.getCurrentOrganization());
        data.setQualification(candidate.getQualification());
        data.setTotalExperience(candidate.getTotalExperience());
        data.setRelevantExperience(candidate.getRelevantExperience());
        data.setCurrentCTC(candidate.getCurrentCTC());
        data.setExpectedCTC(candidate.getExpectedCTC());
        data.setNoticePeriod(candidate.getNoticePeriod());
        data.setCurrentLocation(candidate.getCurrentLocation());
        data.setUserEmail(candidate.getUserEmail());


        return data;
    }
    @Transactional
    public DeleteSubmissionResponseDto deleteSubmissionById(String submissionId) {
        logger.info("Received request to delete candidate with candidateId: {}", submissionId);
        // Fetch candidate details before deletion
        Submissions submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> {
                    logger.error("Submission with ID {} not found", submissionId);
                    return new SubmissionNotFoundException("Submission not found with id: " + submissionId+" to Delete Submission");
                });
        logger.info("Submission found: {}, Proceeding with deletion", submission.getSubmissionId());
        // Store the candidate details before deletion
        String recruiterEmail =submission.getCandidate().getUserEmail();
        String recruiterName = candidateRepository.findUserNameByEmail(recruiterEmail);
        String teamLeadEmail = candidateRepository.findTeamLeadEmailByJobId(submission.getJobId());
        String teamLeadName=candidateRepository.findUserNameByEmail(teamLeadEmail);
        String submissionIdBeforeDelete = submission.getSubmissionId();
        String jobIdBeforeDelete = submission.getJobId();
        // Delete the candidate from the repository
        submissionRepository.delete(submission);
        logger.info("Candidate with ID {} deleted successfully", submissionId);

        InterviewDetails interview=interviewRepository.findInterviewsByCandidateIdAndJobId(submission.getCandidate().getCandidateId(),submission.getJobId());
        if (interview!=null){
            interviewRepository.delete(interview);
            logger.info("Interview with ID {} deleted successfully", interview.getInterviewId());
        }

        // Prepare the response with candidate details
        DeleteSubmissionResponseDto.SubmissionData data = new DeleteSubmissionResponseDto.SubmissionData(submissionIdBeforeDelete, jobIdBeforeDelete);

        return new DeleteSubmissionResponseDto(true,
                "Submission deleted successfully",
                data,
                null);
    }
    public CandidateResponseDto editSubmission(String submissionId, CandidateDetails updatedCandidateDetails, Submissions updatedSubmissionsDetails, MultipartFile resumeFile) {

        logger.info("Updating status: {}", updatedSubmissionsDetails.getStatus());

        Optional<Submissions> submissions=submissionRepository.findById(submissionId);
        if(submissions.isEmpty()) throw new SubmissionNotFoundException("No Submissions Found with Submission Id :"+submissionId);
        try {
            logger.info("Edit Submission started for SubmissionId: {}", submissionId);
            String candidateId=submissionRepository.findCandidateIdBySubmissionId(submissionId);
            logger.info("Candidate Id fetched with submissionId :"+candidateId);
            if (candidateId==null)  throw new CandidateNotFoundException("No Candidate Found with SubmissionId :"+submissionId);
            Optional<CandidateDetails> existingCandidateOpt = candidateRepository.findById(candidateId);
            if (!existingCandidateOpt.isPresent()) {
                logger.error("No Candidate found with Id {}" + candidateId);
                throw new CandidateNotFoundException("Candidate Not Exists with candidateId " + candidateId);
            }
            Optional<Submissions> optionalCandidate=submissionRepository.findBySubmissionIdAndUserId(submissionId,updatedCandidateDetails.getUserId());
            if(optionalCandidate.isEmpty()) {
                logger.error("Candidate Id {} Not Related to User Id {}", candidateId, updatedCandidateDetails.getUserId());
                throw new CandidateNotFoundException("Candidate Id: " + candidateId + " Not related to UserId: " + updatedCandidateDetails.getUserId());
            }
            Submissions existedSubmission=submissionRepository.findByCandidate_CandidateIdAndJobId(candidateId, updatedSubmissionsDetails.getJobId());
            if(existedSubmission==null) {
                logger.error("Candidate Not Submitted For JobId {} ",updatedSubmissionsDetails.getJobId());
                throw new SubmissionNotFoundException("Candidate Not Submitted for JobId " + updatedSubmissionsDetails.getJobId());
            }
            CandidateDetails existingCandidate = existingCandidateOpt.get();
            updateCandidateFields(existingCandidate, updatedCandidateDetails);
            if (resumeFile!=null && !resumeFile.isEmpty())   {
                existedSubmission.setResumeFilePath(updatedSubmissionsDetails.getResumeFilePath());
            }
            existedSubmission.setCandidate(existingCandidate);
            existedSubmission.setJobId(updatedSubmissionsDetails.getJobId());
            existedSubmission.setSkills(updatedSubmissionsDetails.getSkills());
            existedSubmission.setPreferredLocation(updatedSubmissionsDetails.getPreferredLocation());
            existedSubmission.setProfileReceivedDate(LocalDate.now());
            existedSubmission.setCommunicationSkills(updatedSubmissionsDetails.getCommunicationSkills());
            existedSubmission.setRequiredTechnologiesRating(updatedSubmissionsDetails.getRequiredTechnologiesRating());
            existedSubmission.setOverallFeedback(updatedSubmissionsDetails.getOverallFeedback());
            existedSubmission.setSubmittedAt(LocalDateTime.now());
            existedSubmission.setStatus(updatedSubmissionsDetails.getStatus());
            if (resumeFile != null && !resumeFile.isEmpty()) {
                // Convert the resume file to byte[] and set it in the candidateDetails object
                byte[] resumeData = resumeFile.getBytes();
                //candidateDetails.setResume(resumeData);// Store the resume as binary data in DB
                existedSubmission.setResume(resumeData);
                // Save the resume to the file system and store the file path in DB
                String resumeFilePath = saveResumeToFileSystem(resumeFile);
                // candidateDetails.setResumeFilePath(resumeFilePath);// Store the file path in DB
                existedSubmission.setResumeFilePath(resumeFilePath);
            }
            // Update candidate fields with the new data (e.g., name, contact, etc.)
            updateCandidateFields(existingCandidate, updatedCandidateDetails);
            if (resumeFile != null && !resumeFile.isEmpty())
            saveFile(existedSubmission, resumeFile);  // This saves the file and updates the submission's resumeFilePath

            candidateRepository.save(existingCandidate);
            submissionRepository.save(existedSubmission);
            // ------------------ 📧 Send Resubmission Notification Email ------------------
            String recruiterEmail = existingCandidate.getUserEmail();
            String recruiterName = candidateRepository.findUserNameByEmail(recruiterEmail);
            String teamLeadEmail = candidateRepository.findTeamLeadEmailByJobId(existedSubmission.getJobId());
            String teamLeadName=candidateRepository.findUserNameByEmail(teamLeadEmail);
            if (recruiterEmail != null && teamLeadEmail != null) {
                try {
                    logger.info("Sending candidate resubmission email notification...");
                    emailService.sendCandidateNotification(existedSubmission, recruiterName, recruiterEmail,teamLeadName, teamLeadEmail, "submission");
                } catch (Exception e) {
                    logger.error("Error sending resubmission email: {}", e.getMessage(), e);
                }
            } else {
                logger.warn("Email not sent. recruiterEmail or teamLeadEmail is null. RecruiterEmail: {}, TeamLeadEmail: {}",
                        recruiterEmail, teamLeadEmail);
            }
            // Return a success response with the updated candidate details
            CandidateResponseDto.CandidateData data = new CandidateResponseDto.CandidateData(
                    existingCandidate.getCandidateId(),
                    existingCandidate.getUserId(),
                    existedSubmission.getSubmissionId()
            );
            return new CandidateResponseDto(
                    "Success",
                    "Candidate successfully updated",
                    data,
                    null);
        }
        catch (InvalidFileTypeException ex) {
            // Custom handling for InvalidFileTypeException
            logger.error("Invalid file type for resume: {}", ex.getMessage());
            throw ex; // Rethrow to be caught by GlobalExceptionHandler
        } catch (IOException ex) {
            // Specific handling for I/O issues, such as file saving errors
            logger.error("Failed to save resume file: {}", ex.getMessage());
            throw new RuntimeException("An error occurred while saving the resume file", ex);
        }
    }
    private void updateCandidateFields(CandidateDetails existingCandidate, CandidateDetails updatedCandidateDetails) {
        //if (updatedCandidateDetails.getJobId() != null) existingCandidate.setJobId(updatedCandidateDetails.getJobId());
        if (updatedCandidateDetails.getUserId() != null) existingCandidate.setUserId(updatedCandidateDetails.getUserId());
        if (updatedCandidateDetails.getFullName() != null) existingCandidate.setFullName(updatedCandidateDetails.getFullName());
        if (updatedCandidateDetails.getCandidateEmailId() != null)
            existingCandidate.setCandidateEmailId(updatedCandidateDetails.getCandidateEmailId());
        if (updatedCandidateDetails.getContactNumber() != null) existingCandidate.setContactNumber(updatedCandidateDetails.getContactNumber());
        if (updatedCandidateDetails.getQualification() != null) existingCandidate.setQualification(updatedCandidateDetails.getQualification());
        if (updatedCandidateDetails.getTotalExperience() != 0)
            existingCandidate.setTotalExperience(updatedCandidateDetails.getTotalExperience());
        if (updatedCandidateDetails.getCurrentCTC() != null) existingCandidate.setCurrentCTC(updatedCandidateDetails.getCurrentCTC());
        if (updatedCandidateDetails.getExpectedCTC() != null)
            existingCandidate.setExpectedCTC(updatedCandidateDetails.getExpectedCTC());
        if (updatedCandidateDetails.getNoticePeriod() != null)
            existingCandidate.setNoticePeriod(updatedCandidateDetails.getNoticePeriod());
        if (updatedCandidateDetails.getCurrentLocation() != null)
            existingCandidate.setCurrentLocation(updatedCandidateDetails.getCurrentLocation());
        if (updatedCandidateDetails.getRelevantExperience() != 0)
            existingCandidate.setRelevantExperience(updatedCandidateDetails.getRelevantExperience());
        if (updatedCandidateDetails.getCurrentOrganization() != null)
            existingCandidate.setCurrentOrganization(updatedCandidateDetails.getCurrentOrganization());

        existingCandidate.setTimestamp(LocalDateTime.now());
    }
    private void saveFile(Submissions submissions, MultipartFile file) throws IOException {
        // Define the path where files will be stored
        Path uploadsDirectory = Paths.get("uploads");
        // Check if the directory exists, if not, create it
        if (Files.notExists(uploadsDirectory)) {
            Files.createDirectories(uploadsDirectory);
            logger.info("Created directory: {}", uploadsDirectory.toString());
        }
        // Generate a filename that combines the candidateId and timestamp
        String filename = submissions.getSubmissionId() + "-" + System.currentTimeMillis() + "-" + file.getOriginalFilename();
        Path targetPath = uploadsDirectory.resolve(filename);  // Save the file inside the "uploads" directory
        try {
            // Log the file saving action
            logger.info("Saving file to path: {}", targetPath);
            // Save the file to the directory
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            // Optionally save the file path in the database (for example, updating the candidate)
            submissions.setResumeFilePath(targetPath.toString());
            submissionRepository.save(submissions);
        } catch (IOException e) {
            logger.error("Failed to save file: {}", e.getMessage());
            throw new IOException("Failed to save file to path: " + targetPath, e);  // Throw exception to indicate failure
        }
    }
    // Set default values for userEmail and clientEmail if not provided
    private String saveResumeToFileSystem(MultipartFile resumeFile) throws IOException {
        // Set the directory where resumes will be stored
        String resumeDirectory = "C:\\Users\\User\\Downloads"; // Ensure the directory path is correct and does not have extra quotes

        // Generate a unique file name using UUID to avoid conflicts
        String fileName = UUID.randomUUID().toString() + "-" + resumeFile.getOriginalFilename();
        Path filePath = Paths.get(resumeDirectory, fileName);

        // Create the directories if they don't exist
        Files.createDirectories(filePath.getParent());

        // Save the file to the disk
        Files.write(filePath, resumeFile.getBytes());

        // Return the path where the file is saved
        return filePath.toString();
    }

    public TeamleadSubmissionsDTO getSubmissionsForTeamlead(String userId) {
        // Get the current date
        LocalDate currentDate = LocalDate.now();

        // Calculate the start and end date for the current month
        LocalDate startOfMonth = currentDate.withDayOfMonth(1);  // First day of the current month
        LocalDate endOfMonth = currentDate.withDayOfMonth(currentDate.lengthOfMonth());  // Last day of the current month

        // Convert LocalDate to LocalDateTime for query compatibility (starting at the beginning and end of the day)
        LocalDateTime startDateTime = startOfMonth.atStartOfDay();
        LocalDateTime endDateTime = endOfMonth.atTime(LocalTime.MAX);

        // Log the date range being fetched
        logger.info("Fetching current month submissions for teamlead with userId: {} between {} and {}", userId, startDateTime, endDateTime);

        // Fetch self submissions for the current month
        List<Tuple> selfSubs = submissionRepository.findSelfSubmissionsByTeamleadAndDateRange(userId, startDateTime, endDateTime);

        // Fetch team submissions for the current month
        List<Tuple> teamSubs = submissionRepository.findTeamSubmissionsByTeamleadAndDateRange(userId, startDateTime, endDateTime);
        logger.info("Fetched {} self submissions for teamlead with userId: {} between {} and {}", selfSubs.size(), userId, startDateTime, endDateTime);
        logger.info("Fetched {} team submissions for teamlead with userId: {} between {} and {}", teamSubs.size(), userId, startDateTime, endDateTime);

        // Convert Tuple data to DTO for both self and team submissions
        List<SubmissionGetResponseDto> selfSubDtos = mapTuplesToResponseDto(selfSubs);
        List<SubmissionGetResponseDto> teamSubDtos = mapTuplesToResponseDto(teamSubs);

        // Return the DTO containing both self and team submissions
        return new TeamleadSubmissionsDTO(selfSubDtos, teamSubDtos);
    }
    public List<SubmissionGetResponseDto> mapTuplesToResponseDto(List<Tuple> tuples) {
        return tuples.stream().map(tuple -> {
            SubmissionGetResponseDto dto = new SubmissionGetResponseDto();

            // Common fields from both queries
            dto.setSubmissionId(tuple.get("submission_id", String.class));
            dto.setCandidateId(tuple.get("candidate_id", String.class));
            dto.setFullName(tuple.get("full_name", String.class));
            dto.setSkills(tuple.get("skills", String.class));
            dto.setJobId(tuple.get("job_id", String.class));
            dto.setUserId(tuple.get("user_id", String.class));
            dto.setUserEmail(tuple.get("user_email", String.class));
            dto.setPreferredLocation(tuple.get("preferred_location", String.class));
            dto.setClientName(tuple.get("client_name", String.class));
            dto.setRecruiterName(tuple.get("recruiter_name", String.class));
            dto.setUserName(tuple.get("recruiter_name", String.class));

            // Candidate information fields
            dto.setContactNumber(tuple.get("contact_number", String.class));
            dto.setCandidateEmailId(tuple.get("candidate_email_id", String.class));
            dto.setTotalExperience(tuple.get("total_experience", Float.class));
            dto.setRelevantExperience(tuple.get("relevant_experience", Float.class));
            dto.setCurrentOrganization(tuple.get("current_organization", String.class));
            dto.setQualification(tuple.get("qualification", String.class));
            dto.setCurrentCTC(tuple.get("current_ctc", String.class));
            dto.setExpectedCTC(tuple.get("expected_ctc", String.class));
            dto.setNoticePeriod(tuple.get("notice_period", String.class));
            dto.setCurrentLocation(tuple.get("current_location", String.class));
            dto.setTechnology(tuple.get("job_title",String.class));

            // Submission information fields
            dto.setCommunicationSkills(tuple.get("communication_skills", String.class));
            dto.setRequiredTechnologiesRating(tuple.get("required_technologies_rating", Double.class));
            dto.setOverallFeedback(tuple.get("overall_feedback", String.class));

            String timestamp = tuple.get("profile_received_date", String.class);  // Assuming timestamp is a string
            if (timestamp != null) {
                try {
                    LocalDate profileReceivedDate = LocalDate.parse(timestamp, DateTimeFormatter.ISO_DATE);
                    dto.setProfileReceivedDate(profileReceivedDate);
                } catch (Exception e) {
                    // Fallback if date parsing fails
                    System.err.println("Error parsing profileReceivedDate: " + e.getMessage());
                }
            }
            return dto;
        }).collect(Collectors.toList());
    }
    // Method to get candidate submissions by userId
    public List<SubmissionGetResponseDto> getSubmissionsByUserId(String userId) {
        // ✅ Validate user existence and fetch role
        String role = submissionRepository.findRoleByUserId(userId); // Native query to join user_roles_prod and roles_prod
        if (role == null) {
            throw new ResourceNotFoundException("User ID '" + userId + "' not found or role not assigned.");
        }
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        List<Submissions> submissions;

        if ("EMPLOYEE".equalsIgnoreCase(role)) {
            submissions = submissionRepository.findByUserIdAndProfileReceivedDateBetween(userId, startOfMonth, endOfMonth);
        } else if ("BDM".equalsIgnoreCase(role)) {
            submissions = submissionRepository.findSubmissionsByBdmUserIdAndDateRange(userId, startOfMonth, endOfMonth);
        } else {
            throw new UnsupportedOperationException("Only EMPLOYEE and BDM roles are supported.");
        }

        if (submissions.isEmpty()) {
            throw new CandidateNotFoundException("No submissions found for userId: " + userId + " in the current month.");
        }
        return submissions.stream().map(submission -> {

            Optional<String> clientNameOpt = candidateRepository.findClientNameByJobId(submission.getJobId());
            String technology = submissionRepository.findJobTitleByJobId(submission.getJobId());
            String clientName = clientNameOpt.orElse(null);
            SubmissionGetResponseDto dto= convertToSubmissionGetResponseDto(submission);
            dto.setTechnology(technology);
            dto.setClientName(clientName);
            return dto;
        }).collect(Collectors.toList());
    }


    public List<SubmissionGetResponseDto> getSubmissionsByUserIdAndDateRange(String userId, LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new DateRangeValidationException("End date cannot be before start date.");
        }

        // Fetch role
        String role = submissionRepository.findRoleByUserId(userId); // write this query to join user_roles_prod and roles_prod

        List<Submissions> submissions;

        if ("EMPLOYEE".equalsIgnoreCase(role)) {
            submissions = submissionRepository.findByUserIdAndProfileReceivedDateBetween(userId, startDate, endDate);
        } else if ("BDM".equalsIgnoreCase(role)) {
            submissions = submissionRepository.findSubmissionsByBdmUserIdAndDateRange(userId, startDate, endDate);
        } else {
            throw new UnsupportedOperationException("Only EMPLOYEE and BDM roles are supported.");
        }

        if (submissions.isEmpty()) {
            throw new CandidateNotFoundException("No submissions found for userId: " + userId + " between " + startDate + " and " + endDate);
        }

        return submissions.stream().map(submission -> {
            Optional<String> clientNameOpt = candidateRepository.findClientNameByJobId(submission.getJobId());
            String technology = submissionRepository.findJobTitleByJobId(submission.getJobId());
            String clientName = clientNameOpt.orElse(null);
            SubmissionGetResponseDto dto=convertToSubmissionGetResponseDto(submission);
            dto.setTechnology(technology);
            dto.setClientName(clientName);
            return dto;
        }).collect(Collectors.toList());
    }
    private SubmissionGetResponseDto convertToSubmissionGetResponseDto(Submissions sub) {

        SubmissionGetResponseDto dto = new SubmissionGetResponseDto();

        dto.setSubmissionId(sub.getSubmissionId());
        dto.setCandidateId(sub.getCandidate().getCandidateId());
        dto.setUserId(sub.getCandidate().getUserId());
        dto.setFullName(sub.getCandidate().getFullName());
        dto.setCandidateEmailId(sub.getCandidate().getCandidateEmailId());
        dto.setContactNumber(sub.getCandidate().getContactNumber());
        dto.setCurrentOrganization(sub.getCandidate().getCurrentOrganization());
        dto.setQualification(sub.getCandidate().getQualification());
        dto.setTotalExperience(sub.getCandidate().getTotalExperience());
        dto.setRelevantExperience(sub.getCandidate().getRelevantExperience());
        dto.setCurrentCTC(sub.getCandidate().getCurrentCTC());
        dto.setExpectedCTC(sub.getCandidate().getExpectedCTC());
        dto.setNoticePeriod(sub.getCandidate().getNoticePeriod());
        dto.setCurrentLocation(sub.getCandidate().getCurrentLocation());
        dto.setJobId(sub.getJobId());
        dto.setClientName(sub.getClientName());
        dto.setProfileReceivedDate(sub.getProfileReceivedDate());
        dto.setPreferredLocation(sub.getPreferredLocation());
        dto.setSkills(sub.getSkills());
        dto.setCommunicationSkills(sub.getCommunicationSkills());
        dto.setRequiredTechnologiesRating(sub.getRequiredTechnologiesRating());
        dto.setOverallFeedback(sub.getOverallFeedback());
        dto.setRecruiterName(sub.getRecruiterName());
        dto.setUserName(sub.getRecruiterName());
        dto.setUserEmail(sub.getUserEmail());
        dto.setStatus(sub.getStatus());
        dto.setTechnology(submissionRepository.findJobTitleByJobId(sub.getJobId()));


        return dto;
    }
    public List<SubmissionGetResponseDto> getAllSubmissionsByDateRange(LocalDate startDate, LocalDate endDate) {

        if (endDate.isBefore(startDate)) {
            throw new DateRangeValidationException("End date cannot be before start date.");
        }
        // ✅ Only hit DB after validations pass
        List<Submissions> submissions = submissionRepository.findByProfileReceivedDateBetween(startDate, endDate);

        if (submissions.isEmpty()) {
            throw new CandidateNotFoundException("No submissions found for Candidates between " + startDate + " and " + endDate);
        }
        return submissions.stream().map(submission-> {
            SubmissionGetResponseDto dto = convertToSubmissionGetResponseDto(submission);

            return dto;
        }).collect(Collectors.toList());
    }

    public SubmissionsGetResponse getAllSubmissionsFilterByDate(LocalDate startDate, LocalDate endDate) {

        List<Submissions> submissions = submissionRepository.findByProfileReceivedDateBetween(startDate,endDate);
        List<SubmissionsGetResponse.GetSubmissionData> data =submissions.stream()
                .map(this::convertToSubmissionsGetResponse)
                .collect(Collectors.toList());
        SubmissionsGetResponse response=new SubmissionsGetResponse(true,"Submissions found",data,null);
        return response;
    }

    public CandidateResponseDto editSubmissionWithOutUserId(String submissionId, CandidateDetails updatedCandidateDetails, Submissions updatedSubmissionsDetails, MultipartFile resumeFile) {

        Optional<Submissions> submissions = submissionRepository.findById(submissionId);
        if (submissions.isEmpty())
            throw new SubmissionNotFoundException("No Submissions Found with Submission Id :" + submissionId);
        try {
            logger.info("Edit Submission started for SubmissionId: {}", submissionId);
            String candidateId = submissionRepository.findCandidateIdBySubmissionId(submissionId);
            logger.info("Candidate Id fetched with submissionId :" + candidateId);
            if (candidateId == null)
                throw new CandidateNotFoundException("No Candidate Found with SubmissionId :" + submissionId);
            Optional<CandidateDetails> existingCandidateOpt = candidateRepository.findById(candidateId);
            if (!existingCandidateOpt.isPresent()) {
                logger.error("No Candidate found with Id {}" + candidateId);
                throw new CandidateNotFoundException("Candidate Not Exists with candidateId " + candidateId);
            }
            Submissions existedSubmission = submissionRepository.findByCandidate_CandidateIdAndJobId(candidateId, updatedSubmissionsDetails.getJobId());
            if (existedSubmission == null) {
                logger.error("Candidate Not Submitted For JobId {} ", updatedSubmissionsDetails.getJobId());
                throw new SubmissionNotFoundException("Candidate Not Submitted for JobId " + updatedSubmissionsDetails.getJobId());
            }
            CandidateDetails existingCandidate = existingCandidateOpt.get();
            updateCandidateFields(existingCandidate, updatedCandidateDetails);
            if (resumeFile != null && !resumeFile.isEmpty()) {
                existedSubmission.setResumeFilePath(updatedSubmissionsDetails.getResumeFilePath());
            }
            existedSubmission.setCandidate(existingCandidate);
            existedSubmission.setJobId(updatedSubmissionsDetails.getJobId());
            existedSubmission.setSkills(updatedSubmissionsDetails.getSkills());
            existedSubmission.setPreferredLocation(updatedSubmissionsDetails.getPreferredLocation());
            existedSubmission.setProfileReceivedDate(LocalDate.now());
            existedSubmission.setCommunicationSkills(updatedSubmissionsDetails.getCommunicationSkills());
            existedSubmission.setRequiredTechnologiesRating(updatedSubmissionsDetails.getRequiredTechnologiesRating());
            existedSubmission.setOverallFeedback(updatedSubmissionsDetails.getOverallFeedback());
            existedSubmission.setSubmittedAt(LocalDateTime.now());
            existedSubmission.setStatus(updatedSubmissionsDetails.getStatus());

            if (resumeFile != null && !resumeFile.isEmpty()) {
                // Convert the resume file to byte[] and set it in the candidateDetails object
                byte[] resumeData = resumeFile.getBytes();
                //candidateDetails.setResume(resumeData);// Store the resume as binary data in DB
                existedSubmission.setResume(resumeData);
                // Save the resume to the file system and store the file path in DB
                String resumeFilePath = saveResumeToFileSystem(resumeFile);
                // candidateDetails.setResumeFilePath(resumeFilePath);// Store the file path in DB
                existedSubmission.setResumeFilePath(resumeFilePath);
            }
            // Update candidate fields with the new data (e.g., name, contact, etc.)
            updateCandidateFields(existingCandidate, updatedCandidateDetails);
            if (resumeFile != null && !resumeFile.isEmpty())
                saveFile(existedSubmission, resumeFile);  // This saves the file and updates the submission's resumeFilePath

            candidateRepository.save(existingCandidate);
            submissionRepository.save(existedSubmission);
            // ------------------ 📧 Send Resubmission Notification Email ------------------
            String recruiterEmail = existingCandidate.getUserEmail();
            String recruiterName = candidateRepository.findUserNameByEmail(recruiterEmail);
            String teamLeadEmail = candidateRepository.findTeamLeadEmailByJobId(existedSubmission.getJobId());
            String teamLeadName = candidateRepository.findUserNameByEmail(teamLeadEmail);
            if (recruiterEmail != null && teamLeadEmail != null) {
                try {
                    logger.info("Sending candidate resubmission email notification...");
                    emailService.sendCandidateNotification(existedSubmission, recruiterName, recruiterEmail, teamLeadName, teamLeadEmail, "submission");
                } catch (Exception e) {
                    logger.error("Error sending resubmission email: {}", e.getMessage(), e);
                }
            } else {
                logger.warn("Email not sent. recruiterEmail or teamLeadEmail is null. RecruiterEmail: {}, TeamLeadEmail: {}",
                        recruiterEmail, teamLeadEmail);
            }
            // Return a success response with the updated candidate details
            CandidateResponseDto.CandidateData data = new CandidateResponseDto.CandidateData(
                    existingCandidate.getCandidateId(),
                    existingCandidate.getUserId(),
                    existedSubmission.getSubmissionId()
            );
            return new CandidateResponseDto(
                    "Success",
                    "Candidate successfully updated",
                    data,
                    null);
        } catch (InvalidFileTypeException ex) {
            // Custom handling for InvalidFileTypeException
            logger.error("Invalid file type for resume: {}", ex.getMessage());
            throw ex; // Rethrow to be caught by GlobalExceptionHandler
        } catch (IOException ex) {
            // Specific handling for I/O issues, such as file saving errors
            logger.error("Failed to save resume file: {}", ex.getMessage());
            throw new RuntimeException("An error occurred while saving the resume file", ex);
        }

    }
    public TeamleadSubmissionsDTO getSubmissionsForTeamlead(String userId, LocalDate startDate, LocalDate endDate) {
        // 1. Validate input dates
        if (startDate == null || endDate == null) {
            throw new DateRangeValidationException("Start date and end date must not be null.");
        }
        if (endDate.isBefore(startDate)) {
            throw new DateRangeValidationException("End date cannot be before start date.");
        }
        // 2. Convert LocalDate to LocalDateTime
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        // 3. Log
        logger.info("Fetching submissions for teamlead with userId: {} between {} and {}", userId, startDateTime, endDateTime);
        // 4. Fetch submissions
        List<Tuple> selfSubs = submissionRepository.findSelfSubmissionsByTeamleadAndDateRange(userId, startDateTime, endDateTime);
        List<Tuple> teamSubs = submissionRepository.findTeamSubmissionsByTeamleadAndDateRange(userId, startDateTime, endDateTime);
        logger.info("Fetched {} self submissions for teamlead with userId: {}", selfSubs.size(), userId);
        logger.info("Fetched {} team submissions for teamlead with userId: {}", teamSubs.size(), userId);
        // 5. Map results
        List<SubmissionGetResponseDto> selfSubDtos = mapTuplesToResponseDto(selfSubs);
        List<SubmissionGetResponseDto> teamSubDtos = mapTuplesToResponseDto(teamSubs);
        return new TeamleadSubmissionsDTO(selfSubDtos, teamSubDtos);
    }
}


