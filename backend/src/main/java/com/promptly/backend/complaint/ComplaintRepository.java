package com.promptly.backend.complaint;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    @EntityGraph(attributePaths = {"author", "handledBy"})
    @Query("""
            SELECT c FROM Complaint c
            WHERE (:status IS NULL OR c.status = :status)
              AND (:authorId IS NULL OR c.author.id = :authorId)
            ORDER BY c.updatedAt DESC
            """)
    Page<Complaint> search(@Param("status") ComplaintStatus status,
                           @Param("authorId") Long authorId,
                           Pageable pageable);

    @EntityGraph(attributePaths = {"author", "handledBy"})
    @Query("SELECT c FROM Complaint c WHERE c.id = :id")
    java.util.Optional<Complaint> findWithDetailsById(@Param("id") Long id);
}
