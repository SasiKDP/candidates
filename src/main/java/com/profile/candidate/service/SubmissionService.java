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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    public List<SubmissionsGetResponse> getAllSubmissions() {
        List<Submissions> submissions = submissionRepository.findAll();
        return submissions.stream()
                .map(this::convertToSubmissionsGetResponse)
                .collect(Collectors.toList());
    }

    public List<SubmissionsGetResponse> getSubmissions(String candidateId) {
        Optional<CandidateDetails> candidateDetails = candidateRepository.findById(candidateId);
        if (candidateDetails.isEmpty()) {
            throw new CandidateNotFoundException("Invalid CandidateId " + candidateId);
        }

        List<Submissions> submissions = submissionRepository.findByCandidate_CandidateId(candidateId);
        return submissions.stream()
                .map(this::convertToSubmissionsGetResponse)
                .collect(Collectors.toList());
    }
    public SubmissionsGetResponse getSubmissionById(String submissionId) {
        Optional<Submissions> submissions = submissionRepository.findById(submissionId);

        if (submissions.isEmpty()) {
            throw new SubmissionNotFoundException("Invalid SubmissionId " + submissionId);
        }
        return convertToSubmissionsGetResponse(submissions.get());
    }

    private SubmissionsGetResponse convertToSubmissionsGetResponse(Submissions sub) {
        SubmissionsGetResponse dto = new SubmissionsGetResponse();

        dto.setSubmissionId(sub.getSubmissionId());
        dto.setCandidateId(sub.getCandidate().getCandidateId());
        dto.setJobId(sub.getJobId());
        dto.setSubmittedAt(sub.getSubmittedAt());
        dto.setCommunicationSkills(sub.getCommunicationSkills());
        dto.setSkills(sub.getSkills());
        dto.setOverallFeedback(sub.getOverallFeedback());
        dto.setPreferredLocation(sub.getPreferredLocation());
        dto.setProfileReceivedDate(sub.getProfileReceivedDate());
        dto.setRequiredTechnologiesRating(sub.getRequiredTechnologiesRating());
        dto.setClientName(sub.getClientName());
        CandidateDetails candidate = sub.getCandidate();
        //CandidateDto candidateDto = new CandidateDto();
        dto.setUserId(candidate.getUserId());
        dto.setFullName(candidate.getFullName());
        dto.setCandidateEmailId(candidate.getCandidateEmailId());
        dto.setContactNumber(candidate.getContactNumber());
        dto.setCurrentOrganization(candidate.getCurrentOrganization());
        dto.setQualification(candidate.getQualification());
        dto.setTotalExperience(candidate.getTotalExperience());
        dto.setRelevantExperience(candidate.getRelevantExperience());
        dto.setCurrentCTC(candidate.getCurrentCTC());
        dto.setExpectedCTC(candidate.getExpectedCTC());
        dto.setNoticePeriod(candidate.getNoticePeriod());
        dto.setCurrentLocation(candidate.getCurrentLocation());

        return dto;
    }
    public List<SubmissionsGetResponse> getSubmissionsByUserId(String userId) {

        List<CandidateDetails> candidates = candidateRepository.findByUserId(userId);
        if (candidates.isEmpty()) throw new CandidateNotFoundException("No Candidate Found for UserId: "+userId);
        List<SubmissionsGetResponse> submissionsResponses = new ArrayList<>();

        for (CandidateDetails candidate : candidates) {
            List<Submissions> submissions =  submissionRepository.findByCandidate(candidate);

            for (Submissions submission : submissions) {
                submissionsResponses.add(convertToSubmissionsGetResponse(submission));
            }
        }
        return submissionsResponses;
    }
    @Transactional
    public DeleteSubmissionResponseDto deleteSubmissionById(String submissionId) {
        logger.info("Received request to delete candidate with candidateId: {}", submissionId);
        // Fetch candidate details before deletion
        Submissions submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> {
                    logger.error("Submission with ID {} not found", submissionId);
                    return new SubmissionNotFoundException("Submission not found with id: " + submissionId);
                });
        logger.info("Candidate found: {}, Proceeding with deletion", submission.getSubmissionId());
        // Store the candidate details before deletion
        String submissionIdBeforeDelete = submission.getSubmissionId();
        String jobIdBeforeDelete = submission.getJobId();

        // Delete the candidate from the repository
        submissionRepository.delete(submission);
        logger.info("Candidate with ID {} deleted successfully", submissionId);

        // Prepare the response with candidate details
        DeleteSubmissionResponseDto.Payload payload = new DeleteSubmissionResponseDto.Payload(submissionIdBeforeDelete, jobIdBeforeDelete);

        return new DeleteSubmissionResponseDto(true,
                "Submission deleted successfully",
                payload,
                null);
    }

    public CandidateResponseDto editSubmission(String candidateId, CandidateDetails updatedCandidateDetails, Submissions updatedSubmissionsDetails, MultipartFile resumeFile) {
        try {
            // Fetch the existing candidate from the database
            Optional<CandidateDetails> existingCandidateOpt = candidateRepository.findById(candidateId);
            if (!existingCandidateOpt.isPresent())
                throw new CandidateNotFoundException("Candidate Not Exists with candidateId "+candidateId);

            Optional<CandidateDetails> optionalCandidate=candidateRepository.findByCandidateIdAndUserId(candidateId,updatedCandidateDetails.getUserId());

            if(optionalCandidate.isEmpty())
                throw new CandidateNotFoundException("Candidate Id: "+candidateId+" Not related to UserId: "+updatedCandidateDetails.getUserId());

            Submissions existedSubmission=submissionRepository.findByCandidate_CandidateIdAndJobId(candidateId, updatedSubmissionsDetails.getJobId());
            if(existedSubmission==null)
                throw new CandidateAlreadyExistsException("Candidate Not Submitted for JobId "+updatedSubmissionsDetails.getJobId());

            CandidateDetails existingCandidate = existingCandidateOpt.get();
            updateCandidateFields(existingCandidate, updatedCandidateDetails);

            if (resumeFile!=null && !resumeFile.isEmpty())   {
                existedSubmission.setResumeFilePath(updatedSubmissionsDetails.getResumeFilePath());
            }
            String submissionId=candidateId+"_"+updatedSubmissionsDetails.getJobId();
            existedSubmission.setSubmissionId(submissionId);
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

            if (recruiterEmail != null && teamLeadEmail != null) {
                try {
                    logger.info("Sending candidate resubmission email notification...");
                    emailService.sendCandidateNotification(existedSubmission, recruiterName, recruiterEmail, teamLeadEmail, "submission");
                } catch (Exception e) {
                    logger.error("Error sending resubmission email: {}", e.getMessage(), e);
                }
            } else {
                logger.warn("Email not sent. recruiterEmail or teamLeadEmail is null. RecruiterEmail: {}, TeamLeadEmail: {}",
                        recruiterEmail, teamLeadEmail);
            }
// --------------------------------------------------------------------------

            // Return a success response with the updated candidate details
            CandidateResponseDto.Payload payload = new CandidateResponseDto.Payload(
                    existingCandidate.getCandidateId(),
                    existingCandidate.getUserId(),
                    existedSubmission.getSubmissionId()
            );
            return new CandidateResponseDto(
                    "Success",
                    "Candidate successfully updated",
                    payload,
                    null // No error message since no error occurred
            );
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
            return false; // Candidate does not belong to the provided userId
        }
        return true; // Candidate is valid for the user
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
        String resumeDirectory = "C:\\Users\\jaiva\\Downloads"; // Ensure the directory path is correct and does not have extra quotes

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


