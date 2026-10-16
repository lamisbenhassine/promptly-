package com.promptly.backend.chapter;

import java.net.URI;
import java.security.Principal;
import java.util.List;

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

import com.promptly.backend.chapter.dto.ChapterRequest;
import com.promptly.backend.chapter.dto.ChapterResponse;

import jakarta.validation.Valid;

/**
 * Chapitres rattaches a un cours.
 *
 * Les URL sont imbriquees sous {@code /api/courses/{courseId}/chapters} pour
 * exprimer clairement la relation "un cours contient des chapitres".
 */
@RestController
@RequestMapping("/api/courses/{courseId}/chapters")
public class ChapterController {

    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @GetMapping
    public List<ChapterResponse> list(Principal principal, @PathVariable Long courseId) {
        return chapterService.listByCourse(courseId, principal.getName());
    }

    @GetMapping("/{chapterId}")
    public ChapterResponse get(Principal principal,
                               @PathVariable Long courseId,
                               @PathVariable Long chapterId) {
        return chapterService.get(courseId, chapterId, principal.getName());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<ChapterResponse> create(Principal principal,
                                                  @PathVariable Long courseId,
                                                  @Valid @RequestBody ChapterRequest request) {
        ChapterResponse created = chapterService.create(courseId, principal.getName(), request);
        return ResponseEntity
                .created(URI.create("/api/courses/" + courseId + "/chapters/" + created.id()))
                .body(created);
    }

    @PutMapping("/{chapterId}")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ChapterResponse update(Principal principal,
                                  @PathVariable Long courseId,
                                  @PathVariable Long chapterId,
                                  @Valid @RequestBody ChapterRequest request) {
        return chapterService.update(courseId, chapterId, principal.getName(), request);
    }

    @PostMapping(path = "/{chapterId}/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ChapterResponse uploadPdf(Principal principal,
                                     @PathVariable Long courseId,
                                     @PathVariable Long chapterId,
                                     @RequestParam("file") MultipartFile file) {
        return chapterService.uploadPdf(courseId, chapterId, principal.getName(), file);
    }

    @DeleteMapping("/{chapterId}")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<Void> delete(Principal principal,
                                       @PathVariable Long courseId,
                                       @PathVariable Long chapterId) {
        chapterService.delete(courseId, chapterId, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
