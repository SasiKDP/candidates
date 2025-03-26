package com.profile.candidate.service;

import com.profile.candidate.model.BenchDetails;
import com.profile.candidate.repository.BenchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class BenchService {
    private final BenchRepository benchRepository;

    @Autowired
    public BenchService(BenchRepository benchRepository) {
        this.benchRepository = benchRepository;
    }

    public List<BenchDetails> findAllBenchDetails() {
        return benchRepository.findAll();
    }

    public Optional<BenchDetails> findBenchDetailsById(String id) {
        return benchRepository.findById(id);
    }

    public Optional<BenchDetails> findBenchDetailsByEmail(String email) {
        return benchRepository.findByEmail(email);
    }

    public List<BenchDetails> findBenchDetailsByReferredBy(String referredBy) {
        return benchRepository.findByReferredBy(referredBy);
    }

    private String generateCustomId() {
        // Fetch all existing Bench IDs that follow the pattern "BENCH###"
        List<Integer> existingNumbers = benchRepository.findAll().stream()
                .map(BenchDetails::getId)
                .filter(id -> id != null && id.matches("BENCH\\d{3}"))  // Filter only valid "BENCH###" IDs
                .map(id -> Integer.parseInt(id.replace("BENCH", "")))  // Extract number
                .toList();

        // Find the highest existing number
        int nextNumber = existingNumbers.stream().max(Integer::compare).orElse(0) + 1;

        // ✅ Generate new ID in "BENCH001" format
        return String.format("BENCH%03d", nextNumber);
    }



    @Transactional
    public BenchDetails saveBenchDetails(BenchDetails benchDetails, MultipartFile resumeFile) throws IOException {
        // ✅ Check for duplicate email
        if (benchRepository.existsByEmail(benchDetails.getEmail())) {
            throw new IllegalArgumentException("Duplicate entry: Email already exists -> " + benchDetails.getEmail());
        }

        // ✅ Check for duplicate contact number
        if (benchRepository.existsByContactNumber(benchDetails.getContactNumber())) {
            throw new IllegalArgumentException("Duplicate entry: Contact number already exists -> " + benchDetails.getContactNumber());
        }

        // ✅ Check for duplicate full name
        if (benchRepository.existsByFullName(benchDetails.getFullName())) {
            throw new IllegalArgumentException("Duplicate entry: Full name already exists -> " + benchDetails.getFullName());
        }

        // ✅ Auto-generate ID if not provided
        if (benchDetails.getId() == null || benchDetails.getId().isEmpty()) {
            benchDetails.setId(generateCustomId());
        }

        // ✅ Store resume if provided
        if (resumeFile != null && !resumeFile.isEmpty()) {
            benchDetails.setResume(resumeFile.getBytes());
        }

        return benchRepository.save(benchDetails);
    }


    @Transactional
    public BenchDetails updateBenchDetails(String id, BenchDetails benchDetails) {
        return benchRepository.findById(id).map(existingBench -> {
            if (benchDetails.getFullName() != null) existingBench.setFullName(benchDetails.getFullName());
            if (benchDetails.getEmail() != null) existingBench.setEmail(benchDetails.getEmail());
            if (benchDetails.getRelevantExperience() != null) existingBench.setRelevantExperience(benchDetails.getRelevantExperience());
            if (benchDetails.getTotalExperience() != null) existingBench.setTotalExperience(benchDetails.getTotalExperience());
            if (benchDetails.getContactNumber() != null && !benchDetails.getContactNumber().isBlank()) {
                existingBench.setContactNumber(benchDetails.getContactNumber());
            }
            if (benchDetails.getSkills() != null) existingBench.setSkills(benchDetails.getSkills());
            if (benchDetails.getResume() != null && benchDetails.getResume().length > 0) {
                existingBench.setResume(benchDetails.getResume());  // Store the byte array of resume
            }
            if (benchDetails.getLinkedin() != null) existingBench.setLinkedin(benchDetails.getLinkedin());
            if (benchDetails.getReferredBy() != null) existingBench.setReferredBy(benchDetails.getReferredBy());

            return benchRepository.save(existingBench);
        }).orElseThrow(() -> new IllegalArgumentException("BenchDetails with ID " + id + " not found"));
    }


    @Transactional
    public void deleteBenchDetailsById(String id) {
        if (!benchRepository.existsById(id)) {
            throw new RuntimeException("Bench details with ID " + id + " not found.");
        }

        try {
            benchRepository.deleteByIdIgnoreCase(id);
            System.out.println("Successfully deleted BenchDetails with ID: " + id);
        } catch (Exception e) {
            throw new RuntimeException("Error while deleting BenchDetails with ID: " + id + " -> " + e.getMessage());
        }
    }

    public boolean existsBenchDetailsById(String id) {
        return benchRepository.existsById(id);
    }

    public boolean existsBenchDetailsByEmail(String email) {
        return benchRepository.existsByEmail(email);
    }
}
