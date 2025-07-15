package com.profile.candidate.repository;
import com.profile.candidate.model.BenchDetails;
import com.profile.candidate.model.Submissions;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BenchRepository extends JpaRepository<BenchDetails, String> {

    Optional<BenchDetails> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByFullName(String fullName);
    boolean existsByContactNumber(String contactNumber);

    // ✅ Check for duplicates **excluding** a specific ID (for updates)
    boolean existsByFullNameAndIdNot(String fullName, String id);
    boolean existsByEmailAndIdNot(String email, String id);
    boolean existsByContactNumberAndIdNot(String contactNumber, String id);

    List<BenchDetails> findByReferredBy(String referredBy);  // ✅ Add this method

    @Query("DELETE FROM BenchDetails b WHERE LOWER(b.id) = LOWER(:id)")
    @Modifying
    @Transactional
    void deleteByIdIgnoreCase(@Param("id") String id);

    @Query("SELECT b FROM BenchDetails b WHERE b.createdDate BETWEEN :startDate AND :endDate")
    List<BenchDetails> findByCreatedDateBetween(@Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

}