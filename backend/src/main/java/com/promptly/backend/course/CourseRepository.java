package com.promptly.backend.course;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.promptly.backend.user.User;

/**
 * Acces aux cours.
 *
 * La requete de liste applique la visibilite : un etudiant ne voit que les
 * cours publies ; un enseignant voit en plus ses brouillons ; un administrateur
 * voit tout.
 */
public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("""
            select c from Course c
            where (:category is null or c.category = :category)
              and (:published is null or c.published = :published)
              and (:seeAll = true or c.published = true or c.instructor = :viewer)
            """)
    Page<Course> search(@Param("category") CourseCategory category,
                        @Param("published") Boolean published,
                        @Param("seeAll") boolean seeAll,
                        @Param("viewer") User viewer,
                        Pageable pageable);
}
