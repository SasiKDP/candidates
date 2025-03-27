package com.profile.candidate.controller;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.profile.candidate.dto.BenchDetailsDto;
import com.profile.candidate.dto.BenchResponseDto;
import com.profile.candidate.dto.ErrorResponseDto;
import com.profile.candidate.model.BenchDetails;
import com.profile.candidate.repository.BenchRepository;
import com.profile.candidate.service.BenchService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = {"http://35.188.150.92", "http://192.168.0.140:3000", "http://192.168.0.139:3000","https://mymulya.com", "http://localhost:3000", "http://192.168.0.135:8080"})
@RestController
@RequestMapping("/candidates/bench")
public class BenchController {

    private static final String UPLOAD_DIR = "/your/upload/directory"; // Ensu
    private final BenchService benchService;
    @Autowired
    private BenchRepository benchRepository;

    @Autowired
    public BenchController(BenchService benchService) {
        this.benchService = benchService;
    }
    @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BenchResponseDto> createBenchDetails(
            @RequestParam(value = "resumeFiles", required = false) MultipartFile resumeFile,
            @RequestParam(value = "fullName") String fullName,
            @RequestParam(value = "email") String email,
            @RequestParam(value = "relevantExperience") BigDecimal relevantExperience,
            @RequestParam(value = "totalExperience") BigDecimal totalExperience,
            @RequestParam(value = "contactNumber") String contactNumber,
            @RequestParam(value = "skills") String skillsJson, // Expecting JSON string
            @RequestParam(value = "linkedin", required = false) String linkedin,
            @RequestParam(value = "referredBy", required = false) String referredBy) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<String> skillsList = objectMapper.readValue(skillsJson, new TypeReference<List<String>>() {});

            BenchDetails benchDetails = new BenchDetails();
            benchDetails.setFullName(fullName);
            benchDetails.setEmail(email);
            benchDetails.setRelevantExperience(relevantExperience);
            benchDetails.setTotalExperience(totalExperience);
            benchDetails.setContactNumber(contactNumber);
            benchDetails.setSkills(skillsList);
            benchDetails.setLinkedin(linkedin);
            benchDetails.setReferredBy(referredBy);

            // Process resume file
            if (resumeFile != null && !resumeFile.isEmpty()) {
                benchDetails.setResume(resumeFile.getBytes());
            }

            // Check for duplicate email
            if (benchRepository.existsByEmail(email)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        new BenchResponseDto("Error", "Duplicate entry: Email already exists.", null, null)
                );
            }

            // Save bench details
            BenchDetails savedBenchDetails = benchService.saveBenchDetails(benchDetails, resumeFile);

            BenchResponseDto responseDto = new BenchResponseDto(
                    "Success",
                    "Bench details saved successfully",
                    List.of(new BenchResponseDto.Payload(savedBenchDetails.getId(), savedBenchDetails.getFullName())),
                    null
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new BenchResponseDto("Error", "Failed to process resume file: " + e.getMessage(), null, null)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new BenchResponseDto("Error", "Error while saving bench details: " + e.getMessage(), null, null)
            );
        }
    }



    @GetMapping("/getBenchList")
    public ResponseEntity<List<BenchDetailsDto>> getAllBenchDetails() {
        try {
            List<BenchDetails> benchDetailsList = benchService.findAllBenchDetails();

            // ✅ Convert BenchDetails to BenchDetailsDto (excluding resume)
            List<BenchDetailsDto> dtoList = benchDetailsList.stream()
                    .map(bench -> new BenchDetailsDto(
                            bench.getId(),
                            bench.getFullName(),
                            bench.getEmail(),
                            bench.getRelevantExperience(),
                            bench.getTotalExperience(),
                            bench.getContactNumber(),
                            bench.getSkills() != null ? bench.getSkills() : Collections.<String>emptyList(),  // ✅ Ensure skills is a List<String>
                            bench.getLinkedin(),
                            bench.getReferredBy()
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtoList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }





    @GetMapping("/{id}")
    public ResponseEntity<Object> getBenchDetailsById(@PathVariable String id) {
        try {
            Optional<BenchDetails> benchDetails = benchService.findBenchDetailsById(id);

            if (benchDetails.isPresent()) {
                BenchResponseDto.Payload payload = new BenchResponseDto.Payload(
                        benchDetails.get().getId(),
                        benchDetails.get().getFullName()
                );

                BenchResponseDto responseDto = new BenchResponseDto(
                        "Success",
                        "Fetched bench details successfully",
                        (List<BenchResponseDto.Payload>) payload,
                        null
                );

                return ResponseEntity.ok(responseDto);
            } else {
                ErrorResponseDto errorResponse = new ErrorResponseDto(false, "Bench details not found for ID " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
        } catch (Exception e) {
            ErrorResponseDto errorResponse = new ErrorResponseDto(false, "Error while fetching bench details: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/updatebench/{id}")
    public ResponseEntity<Object> updateBenchDetails(
            @PathVariable String id,
            @RequestParam(value = "resumeFiles", required = false) MultipartFile resumeFile,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "relevantExperience", required = false) BigDecimal relevantExperience,
            @RequestParam(value = "totalExperience", required = false) BigDecimal totalExperience,
            @RequestParam(value = "contactNumber", required = false) String contactNumber,
            @RequestPart(value = "skills", required = false) String skillsJson, // Expecting JSON array
            @RequestParam(value = "linkedin", required = false) String linkedin,
            @RequestParam(value = "referredBy", required = false) String referredBy
    ) {
        try {
            // 🔹 Log incoming request data
            System.out.println("Updating BenchDetails ID: " + id);
            System.out.println("Received Skills JSON: " + skillsJson);

            // ✅ Convert JSON string to List<String>
            ObjectMapper objectMapper = new ObjectMapper();
            List<String> skillsList = (skillsJson != null && !skillsJson.isBlank())
                    ? objectMapper.readValue(skillsJson, new TypeReference<List<String>>() {})
                    : Collections.emptyList();

            // ✅ Convert MultipartFile to byte array (if provided)
            byte[] resumeData = null;
            if (resumeFile != null && !resumeFile.isEmpty()) {
                resumeData = resumeFile.getBytes();
            }

            // ✅ Create BenchDetails object with provided data
            BenchDetails benchDetails = new BenchDetails();
            benchDetails.setFullName(fullName);
            benchDetails.setEmail(email);
            benchDetails.setRelevantExperience(relevantExperience);
            benchDetails.setTotalExperience(totalExperience);
            benchDetails.setContactNumber(contactNumber);
            benchDetails.setSkills(skillsList); // ✅ Ensure List<String>
            benchDetails.setLinkedin(linkedin);
            benchDetails.setReferredBy(referredBy);
            benchDetails.setResume(resumeData);

            // ✅ Call service to update details
            BenchDetails updatedBenchDetails = benchService.updateBenchDetails(id, benchDetails);

            // ✅ Prepare response
            BenchResponseDto.Payload payload = new BenchResponseDto.Payload(
                    updatedBenchDetails.getId().toString(),
                    updatedBenchDetails.getFullName()
            );

            BenchResponseDto responseDto = new BenchResponseDto(
                    "Success",
                    "Bench details updated successfully",
                    List.of(payload),
                    null
            );

            return ResponseEntity.ok(responseDto);
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDto(false, "Invalid JSON format for skills: " + e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDto(false, "Failed to process resume file: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDto(false, "Failed to update bench details: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto(false, "Error while updating bench details: " + e.getMessage()));
        }
    }

    @DeleteMapping("/deletebench/{id}")
    public ResponseEntity<Object> deleteBenchDetails(@PathVariable String id) {
        try {
            // ✅ Check if the bench ID exists before deleting
            if (!benchRepository.existsById(id)) {
                throw new EntityNotFoundException("Bench Details with ID " + id + " does not exist.");
            }

            // ✅ Proceed with deletion
            benchService.deleteBenchDetailsById(id);

            // ✅ Prepare success response
            BenchResponseDto responseDto = new BenchResponseDto(
                    "Success",
                    "Bench Details with ID " + id + " successfully deleted",
                    null,
                    null
            );

            return ResponseEntity.ok(responseDto);
        } catch (EntityNotFoundException e) {
            // ❌ Bench ID not found
            ErrorResponseDto errorResponse = new ErrorResponseDto(false, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            // ❌ Other errors
            ErrorResponseDto errorResponse = new ErrorResponseDto(false, "Error while deleting bench details: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadResume(@PathVariable String id) {
        try {
            // Fetch BenchDetails by ID
            Optional<BenchDetails> benchDetailsOptional = benchRepository.findById(id);
            if (benchDetailsOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            BenchDetails benchDetails = benchDetailsOptional.get();
            byte[] resumeFile = benchDetails.getResume();

            if (resumeFile == null || resumeFile.length == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            // **Use actual filename stored in the database (or default name)**
            String fileName = benchDetails.getFullName(); // Assuming you have this field in your entity

            if (fileName == null || fileName.isBlank()) {
                fileName = "Resume_" + id + ".pdf"; // Fallback name
            }

            // **Return the file with correct Content-Disposition**
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resumeFile);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

}