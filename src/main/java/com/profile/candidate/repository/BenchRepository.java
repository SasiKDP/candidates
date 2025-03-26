package com.profile.candidate.repository;
import com.profile.candidate.model.BenchDetails;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BenchRepository extends JpaRepository<BenchDetails, String> {

    Optional<BenchDetails> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByFullName(String fullName);
    boolean existsByContactNumber(String contactNumber);


    List<BenchDetails> findByReferredBy(String referredBy);  // ✅ Add this method

    @Query("DELETE FROM BenchDetails b WHERE LOWER(b.id) = LOWER(:id)")
    @Modifying
    @Transactional
    void deleteByIdIgnoreCase(@Param("id") String id);

}

