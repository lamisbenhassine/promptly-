import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { extractErrorMessage } from '../../../core/models/api-error.model';
import { Chapter, Course, PdfSummary } from '../../../core/models/course.model';
import { AuthService } from '../../../core/services/auth.service';
import { CourseService } from '../../../core/services/course.service';

@Component({
  selector: 'app-chapter-detail',
  imports: [RouterLink],
  templateUrl: './chapter-detail.html',
  styleUrl: './chapter-detail.css'
})
export class ChapterDetail implements OnInit {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly courseService = inject(CourseService);
  private readonly authService = inject(AuthService);

  protected readonly fileUrl = CourseService.fileUrl;
  protected readonly course = signal<Course | null>(null);
  protected readonly chapter = signal<Chapter | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly deleting = signal(false);

  protected readonly summarizing = signal(false);
  protected readonly summary = signal<PdfSummary | null>(null);
  protected readonly summaryError = signal<string | null>(null);

  ngOnInit(): void {
    const courseId = Number(this.route.snapshot.paramMap.get('id'));
    const chapterId = Number(this.route.snapshot.paramMap.get('chapterId'));

    this.courseService.get(courseId).subscribe({
      next: (course) => this.course.set(course),
      error: () => { /* le chapitre affichera l'erreur principale */ }
    });

    this.courseService.getChapter(courseId, chapterId).subscribe({
      next: (chapter) => this.chapter.set(chapter),
      error: (error) => this.errorMessage.set(
        extractErrorMessage(error, 'Chapitre introuvable'))
    });
  }

  protected canManage(): boolean {
    const course = this.course();
    const user = this.authService.currentUser();
    if (!course || !user) {
      return false;
    }
    return user.role === 'ADMIN' || user.id === course.instructor.id;
  }

  protected summarizePdf(): void {
    const chapter = this.chapter();
    if (!chapter) {
      return;
    }

    this.summaryError.set(null);
    this.summarizing.set(true);

    this.courseService.summarizeChapterPdf(chapter.courseId, chapter.id).subscribe({
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

  protected remove(): void {
    const course = this.course();
    const chapter = this.chapter();
    if (!course || !chapter) {
      return;
    }
    if (!confirm(`Supprimer le chapitre « ${chapter.title} » ?`)) {
      return;
    }

    this.deleting.set(true);
    this.courseService.deleteChapter(course.id, chapter.id).subscribe({
      next: () => this.router.navigate(['/cours', course.id]),
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'La suppression a echoue'));
        this.deleting.set(false);
      }
    });
  }
}
