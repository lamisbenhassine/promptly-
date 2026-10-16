import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { extractErrorMessage } from '../../../core/models/api-error.model';
import {
  FORUM_CATEGORY_LABELS,
  ForumPost,
  REACTION_EMOJI,
  REACTION_LABELS,
  REACTION_TYPES,
  ReactionType
} from '../../../core/models/forum.model';
import { AuthService } from '../../../core/services/auth.service';
import { ForumService } from '../../../core/services/forum.service';

@Component({
  selector: 'app-forum-detail',
  imports: [DatePipe, ReactiveFormsModule, RouterLink],
  templateUrl: './forum-detail.html',
  styleUrl: './forum-detail.css'
})
export class ForumDetail implements OnInit {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly forumService = inject(ForumService);
  private readonly authService = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly labels = FORUM_CATEGORY_LABELS;
  protected readonly reactionTypes = REACTION_TYPES;
  protected readonly reactionEmoji = REACTION_EMOJI;
  protected readonly reactionLabels = REACTION_LABELS;
  protected readonly fileUrl = ForumService.fileUrl;

  protected readonly post = signal<ForumPost | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly submitting = signal(false);
  protected readonly locking = signal(false);
  protected readonly deleting = signal(false);
  protected readonly reacting = signal(false);
  protected readonly showReactions = signal(false);

  protected readonly commentForm = this.formBuilder.nonNullable.group({
    content: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(5000)]]
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.load(id);
  }

  protected isAuthor(): boolean {
    const post = this.post();
    const user = this.authService.currentUser();
    return !!post && !!user && post.author.id === user.id;
  }

  protected canModerate(): boolean {
    const role = this.authService.currentUser()?.role;
    return role === 'ADMIN' || role === 'ENSEIGNANT';
  }

  protected canManagePost(): boolean {
    return this.isAuthor() || this.canModerate();
  }

  protected canManageComment(authorId: number): boolean {
    const user = this.authService.currentUser();
    return !!user && (user.id === authorId || this.canModerate());
  }

  protected reactionCount(type: ReactionType): number {
    return this.post()?.reactions?.counts?.[type] ?? 0;
  }

  protected react(type: ReactionType): void {
    const post = this.post();
    if (!post || this.reacting()) {
      return;
    }
    this.reacting.set(true);
    this.showReactions.set(false);
    this.forumService.react(post.id, type).subscribe({
      next: (updated) => {
        this.post.set(updated);
        this.reacting.set(false);
      },
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'Reaction impossible'));
        this.reacting.set(false);
      }
    });
  }

  protected submitComment(): void {
    const post = this.post();
    if (!post || this.commentForm.invalid) {
      this.commentForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);
    this.forumService.addComment(post.id, this.commentForm.getRawValue()).subscribe({
      next: () => {
        this.commentForm.reset({ content: '' });
        this.submitting.set(false);
        this.load(post.id);
      },
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'Le commentaire a echoue'));
        this.submitting.set(false);
      }
    });
  }

  protected toggleLock(): void {
    const post = this.post();
    if (!post) {
      return;
    }
    this.locking.set(true);
    this.forumService.setLocked(post.id, !post.locked).subscribe({
      next: (updated) => {
        this.post.set(updated);
        this.locking.set(false);
      },
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'Impossible de modifier le verrouillage'));
        this.locking.set(false);
      }
    });
  }

  protected removePost(): void {
    const post = this.post();
    if (!post) {
      return;
    }
    if (!confirm(`Supprimer le sujet « ${post.title} » ?`)) {
      return;
    }
    this.deleting.set(true);
    this.forumService.delete(post.id).subscribe({
      next: () => this.router.navigate(['/forum']),
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'La suppression a echoue'));
        this.deleting.set(false);
      }
    });
  }

  protected removeComment(commentId: number): void {
    const post = this.post();
    if (!post) {
      return;
    }
    if (!confirm('Supprimer ce commentaire ?')) {
      return;
    }
    this.forumService.deleteComment(post.id, commentId).subscribe({
      next: () => this.load(post.id),
      error: (error) => this.errorMessage.set(
        extractErrorMessage(error, 'La suppression du commentaire a echoue'))
    });
  }

  private load(id: number): void {
    this.forumService.get(id).subscribe({
      next: (post) => this.post.set(post),
      error: (error) => this.errorMessage.set(
        extractErrorMessage(error, 'Sujet introuvable'))
    });
  }
}
