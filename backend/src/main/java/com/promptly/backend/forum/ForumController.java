package com.promptly.backend.forum;

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

import com.promptly.backend.forum.dto.ForumCommentRequest;
import com.promptly.backend.forum.dto.ForumCommentResponse;
import com.promptly.backend.forum.dto.ForumPostRequest;
import com.promptly.backend.forum.dto.ForumPostResponse;
import com.promptly.backend.forum.dto.LockRequest;
import com.promptly.backend.forum.dto.ReactionRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/forum/posts")
public class ForumController {

    private final ForumService forumService;

    public ForumController(ForumService forumService) {
        this.forumService = forumService;
    }

    @GetMapping
    public Page<ForumPostResponse> list(
            Principal principal,
            @RequestParam(required = false) ForumCategory category,
            @PageableDefault(size = 12, sort = "updatedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return forumService.list(principal.getName(), category, pageable);
    }

    @GetMapping("/{id}")
    public ForumPostResponse get(Principal principal, @PathVariable Long id) {
        return forumService.get(id, principal.getName());
    }

    @PostMapping
    public ResponseEntity<ForumPostResponse> create(Principal principal,
                                                    @Valid @RequestBody ForumPostRequest request) {
        ForumPostResponse created = forumService.createPost(principal.getName(), request);
        return ResponseEntity
                .created(URI.create("/api/forum/posts/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ForumPostResponse update(Principal principal,
                                    @PathVariable Long id,
                                    @Valid @RequestBody ForumPostRequest request) {
        return forumService.updatePost(id, principal.getName(), request);
    }

    @PostMapping(path = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ForumPostResponse uploadImage(Principal principal,
                                         @PathVariable Long id,
                                         @RequestParam("file") MultipartFile file) {
        return forumService.uploadImage(id, principal.getName(), file);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Principal principal, @PathVariable Long id) {
        forumService.deletePost(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/lock")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ForumPostResponse lock(Principal principal,
                                  @PathVariable Long id,
                                  @Valid @RequestBody LockRequest request) {
        return forumService.setLocked(id, principal.getName(), request.locked());
    }

    @PutMapping("/{id}/reaction")
    public ForumPostResponse react(Principal principal,
                                   @PathVariable Long id,
                                   @Valid @RequestBody ReactionRequest request) {
        return forumService.react(id, principal.getName(), request.type());
    }

    @DeleteMapping("/{id}/reaction")
    public ForumPostResponse removeReaction(Principal principal, @PathVariable Long id) {
        return forumService.removeReaction(id, principal.getName());
    }

    @PostMapping("/{id}/comments")
    public ForumCommentResponse addComment(Principal principal,
                                           @PathVariable Long id,
                                           @Valid @RequestBody ForumCommentRequest request) {
        return forumService.addComment(id, principal.getName(), request);
    }

    @PutMapping("/{id}/comments/{commentId}")
    public ForumCommentResponse updateComment(Principal principal,
                                              @PathVariable Long id,
                                              @PathVariable Long commentId,
                                              @Valid @RequestBody ForumCommentRequest request) {
        return forumService.updateComment(id, commentId, principal.getName(), request);
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(Principal principal,
                                              @PathVariable Long id,
                                              @PathVariable Long commentId) {
        forumService.deleteComment(id, commentId, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
