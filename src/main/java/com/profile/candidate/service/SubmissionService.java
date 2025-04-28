package com.profile.candidate.service;

import com.profile.candidate.dto.CandidateResponseDto;
import com.profile.candidate.dto.DeleteSubmissionResponseDto;
import com.profile.candidate.dto.SubmissionsGetResponse;
import com.profile.candidate.exceptions.CandidateAlreadyExistsException;
import com.profile.candidate.exceptions.CandidateNotFoundException;
import com.profile.candidate.exceptions.InvalidFileTypeException;
import com.profile.candidate.exceptions.SubmissionNotFoundException;
import com.profile.candidate.model.CandidateDetails;
import com.profile.candidate.model.Submissions;
import com.profile.candidate.repository.CandidateRepository;
import com.profile.candidate.repository.SubmissionRepository;
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

    private static final Logger logger = LoggerFactory.getLogger(SubmissionService.class);

    public SubmissionsGetResponse getAllSubmissions() {
        List<Submissions> submissions = submissionRepository.findAll();
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

        return data;
    }
    public SubmissionsGetResponse getSubmissionsByUserId(String userId) {


        List<CandidateDetails> candidates = candidateRepository.findByUserId(userId);
        List<SubmissionsGetResponse.GetSubmissionData> submissionsResponses = new ArrayList<>();

        for (CandidateDetails candidate : candidates) {
            List<Submissions> submissions =  submissionRepository.findByCandidate(candidate);

            for (Submissions submission : submissions) {
                submissionsResponses.add(convertToSubmissionsGetResponse(submission));
            }
        }
        SubmissionsGetResponse response=new SubmissionsGetResponse(true,"Submissions Found",submissionsResponses,null);
    return response;
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
         logger.info("recruiterName {} and  recruiterEmail {} and teamLeadEmail {} and teamLeadName {} ",recruiterName,recruiterEmail,teamLeadEmail,teamLeadName);
        emailService.sendCandidateNotification(submission, recruiterName, recruiterEmail, teamLeadName,teamLeadEmail, "deletion");
        // Prepare the response with candidate details
        DeleteSubmissionResponseDto.SubmissionData data = new DeleteSubmissionResponseDto.SubmissionData(submissionIdBeforeDelete, jobIdBeforeDelete);

        return new DeleteSubmissionResponseDto(true,
                "Submission deleted successfully",
                data,
                null);
    }
    public CandidateResponseDto editSubmission(String submissionId, CandidateDetails updatedCandidateDetails, Submissions updatedSubmissionsDetails, MultipartFile resumeFile) {

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
            Optional<CandidateDetails> optionalCandidate=candidateRepository.findByCandidateIdAndUserId(candidateId,updatedCandidateDetails.getUserId());
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
// --------------------------------------------------------------------------
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
    public boolean isCandidateValidForUser(String userId, String candidateId) {
        // Fetch the candidate by candidateId
        CandidateDetails candidateDetails = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new CandidateNotFoundException("Candidate not found"));
        // Check if the userId associated with the candidate matches the provided userId
        if (!candidateDetails.getUserId().equals(userId)) {
            return false;
        }
        return true;
    }
    // Method to update the candidate fields with new values
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

}


