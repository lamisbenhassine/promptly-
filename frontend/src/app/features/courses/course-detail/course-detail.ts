import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { extractErrorMessage } from '../../../core/models/api-error.model';
import {
  CATEGORY_LABELS,
  Chapter,
  Course,
  Flashcard,
  PdfSummary
} from '../../../core/models/course.model';
import { AuthService } from '../../../core/services/auth.service';
import { CourseService } from '../../../core/services/course.service';

@Component({
  selector: 'app-course-detail',
  imports: [DatePipe, RouterLink],
  templateUrl: './course-detail.html',
  styleUrl: './course-detail.css'
})
export class CourseDetail implements OnInit {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly courseService = inject(CourseService);
  private readonly authService = inject(AuthService);

  protected readonly labels = CATEGORY_LABELS;
  protected readonly fileUrl = CourseService.fileUrl;
  protected readonly course = signal<Course | null>(null);
  protected readonly chapters = signal<Chapter[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly deleting = signal(false);

  protected readonly summarizing = signal(false);
  protected readonly summary = signal<PdfSummary | null>(null);
  protected readonly summaryError = signal<string | null>(null);

  protected readonly loadingFlashcards = signal(false);
  protected readonly flashcards = signal<Flashcard[]>([]);
  protected readonly flipped = signal<boolean[]>([false, false, false, false]);
  protected readonly flashcardsError = signal<string | null>(null);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.errorMessage.set('Identifiant de cours invalide.');
      return;
    }

    this.courseService.get(id).subscribe({
      next: (course) => {
        this.course.set(course);
        this.loadChapters(course.id);
      },
      error: (error) => this.errorMessage.set(
        extractErrorMessage(error, 'Ce cours est introuvable ou n\'est pas encore publie.'))
    });
  }

  protected canManage(course: Course): boolean {
    const user = this.authService.currentUser();
    if (!user) {
      return false;
    }
    return user.role === 'ADMIN' || user.id === course.instructor.id;
  }

  protected summarizePdf(course: Course): void {
    this.summaryError.set(null);
    this.summarizing.set(true);

    this.courseService.summarizeCoursePdf(course.id).subscribe({
      next: (result) => {
        this.summary.set(result);
        this.summarizing.set(false);
      },
      error: (error) => {
        this.summary.set(null);
        this.summaryError.set(extractErrorMessage(error, 'Le resume automatique a echoue'));
        this.summarizing.set(false);
      }
    });
  }

  protected generateFlashcards(course: Course): void {
    this.flashcardsError.set(null);
    this.loadingFlashcards.set(true);
    this.flipped.set([false, false, false, false]);

    this.courseService.flashcardsForCourse(course.id).subscribe({
      next: (result) => {
        this.flashcards.set(result.cards.slice(0, 4));
        this.loadingFlashcards.set(false);
      },
      error: (error) => {
        this.flashcards.set([]);
        this.flashcardsError.set(extractErrorMessage(error, 'Generation des flashcards echouee'));
        this.loadingFlashcards.set(false);
      }
    });
  }

  protected toggleCard(index: number): void {
    this.flipped.update((states) => {
      const next = [...states];
      next[index] = !next[index];
      return next;
    });
  }

  protected remove(course: Course): void {
    if (!confirm(`Supprimer le cours « ${course.title} » et tous ses chapitres ?`)) {
      return;
    }

    this.deleting.set(true);
    this.courseService.delete(course.id).subscribe({
      next: () => this.router.navigate(['/cours']),
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'La suppression a echoue'));
        this.deleting.set(false);
      }
    });
  }

  private loadChapters(courseId: number): void {
    this.courseService.listChapters(courseId).subscribe({
      next: (chapters) => this.chapters.set(chapters),
      error: () => this.chapters.set([])
    });
  }
}
