import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { extractErrorMessage } from '../../../core/models/api-error.model';
import {
  FORUM_CATEGORIES,
  FORUM_CATEGORY_LABELS,
  ForumCategory,
  ForumPost,
  REACTION_EMOJI,
  ReactionType
} from '../../../core/models/forum.model';
import { AuthService } from '../../../core/services/auth.service';
import { ForumService } from '../../../core/services/forum.service';

@Component({
  selector: 'app-forum-list',
  imports: [DatePipe, RouterLink],
  templateUrl: './forum-list.html',
  styleUrl: './forum-list.css'
})
export class ForumList implements OnInit {

  private readonly forumService = inject(ForumService);
  private readonly authService = inject(AuthService);

  protected readonly categories = FORUM_CATEGORIES;
  protected readonly labels = FORUM_CATEGORY_LABELS;
  protected readonly fileUrl = ForumService.fileUrl;
  protected readonly reactionEmoji = REACTION_EMOJI;
  protected readonly isAuthenticated = this.authService.isAuthenticated;

  protected readonly posts = signal<ForumPost[]>([]);
  protected readonly selectedCategory = signal<ForumCategory | null>(null);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.reload();
  }

  protected filterBy(category: ForumCategory | null): void {
    this.selectedCategory.set(category);
    this.reload();
  }

  protected reload(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.forumService.list({ category: this.selectedCategory() }).subscribe({
      next: (page) => {
        this.posts.set(page.content);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'Impossible de charger le forum'));
        this.loading.set(false);
      }
    });
  }

  protected topReactions(post: ForumPost): ReactionType[] {
    const counts = post.reactions?.counts ?? {};
    return (Object.keys(counts) as ReactionType[])
      .filter((type) => (counts[type] ?? 0) > 0)
      .sort((a, b) => (counts[b] ?? 0) - (counts[a] ?? 0))
      .slice(0, 3);
  }
}
