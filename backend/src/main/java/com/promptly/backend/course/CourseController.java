package com.promptly.backend.course;

import java.net.URI;
import java.security.Principal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.promptly.backend.course.dto.CourseRequest;
import com.promptly.backend.course.dto.CourseResponse;

import jakarta.validation.Valid;

/**
 * Catalogue des cours.
 *
 * La lecture est ouverte a tout utilisateur authentifie. La creation, la
 * modification, la suppression et l'upload de fichiers sont reserves aux
 * enseignants et administrateurs.
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public Page<CourseResponse> list(Principal principal,
                                     @RequestParam(required = false) CourseCategory category,
                                     @RequestParam(required = false) Boolean published,
                                     @PageableDefault(size = 12, sort = "createdAt",
                                             direction = Sort.Direction.DESC)
                                     Pageable pageable) {
        return courseService.search(principal.getName(), category, published, pageable);
    }

    @GetMapping("/{id}")
    public CourseResponse get(Principal principal, @PathVariable Long id) {
        return courseService.getVisible(id, principal.getName());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<CourseResponse> create(Principal principal,
                                                 @Valid @RequestBody CourseRequest request) {
        CourseResponse created = courseService.create(principal.getName(), request);
        return ResponseEntity.created(URI.create("/api/courses/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public CourseResponse update(Principal principal,
                                 @PathVariable Long id,
                                 @Valid @RequestBody CourseRequest request) {
        return courseService.update(id, principal.getName(), request);
    }

    @PostMapping(path = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public CourseResponse uploadImage(Principal principal,
                                      @PathVariable Long id,
                                      @RequestParam("file") MultipartFile file) {
        return courseService.uploadImage(id, principal.getName(), file);
    }

    @PostMapping(path = "/{id}/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public CourseResponse uploadPdf(Principal principal,
                                    @PathVariable Long id,
                                    @RequestParam("file") MultipartFile file) {
        return courseService.uploadPdf(id, principal.getName(), file);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<Void> delete(Principal principal, @PathVariable Long id) {
        courseService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
