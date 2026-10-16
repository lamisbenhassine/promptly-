import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable, of, switchMap } from 'rxjs';

import { extractErrorMessage } from '../../../core/models/api-error.model';
import { ChapterPayload, Course } from '../../../core/models/course.model';
import { CourseService } from '../../../core/services/course.service';

@Component({
  selector: 'app-chapter-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './chapter-form.html'
})
export class ChapterForm implements OnInit {

  private readonly formBuilder = inject(FormBuilder);
  private readonly courseService = inject(CourseService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly chapterId = signal<number | null>(null);
  protected readonly courseLocked = signal(false);
  protected readonly courses = signal<Course[]>([]);
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly currentPdfUrl = signal<string | null>(null);

  private pdfFile: File | null = null;

  protected readonly form = this.formBuilder.nonNullable.group({
    courseId: [0 as number, [Validators.required, Validators.min(1)]],
    title: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(150)]],
    content: ['', [Validators.required, Validators.minLength(10)]],
    codeExample: [''],
    position: [1 as number, [Validators.required, Validators.min(1)]]
  });

  ngOnInit(): void {
    const routeCourseId = Number(this.route.snapshot.paramMap.get('id') ?? 0);
    const chapterIdParam = this.route.snapshot.paramMap.get('chapterId');

    this.loadCourses(routeCourseId);

    if (!chapterIdParam) {
      if (routeCourseId > 0) {
        this.form.patchValue({ courseId: routeCourseId });
      }
      return;
    }

    const chapterId = Number(chapterIdParam);
    this.chapterId.set(chapterId);
    this.courseLocked.set(true);

    if (routeCourseId <= 0) {
      this.errorMessage.set('Identifiant de cours invalide.');
      return;
    }

    this.form.patchValue({ courseId: routeCourseId });
    this.form.controls.courseId.disable();

    this.courseService.getChapter(routeCourseId, chapterId).subscribe({
      next: (chapter) => {
        this.form.patchValue({
          courseId: chapter.courseId,
          title: chapter.title,
          content: chapter.content,
          codeExample: chapter.codeExample ?? '',
          position: chapter.position
        });
        this.currentPdfUrl.set(CourseService.fileUrl(chapter.pdfUrl));
      },
      error: (error) => this.errorMessage.set(
        extractErrorMessage(error, 'Impossible de charger ce chapitre'))
    });
  }

  protected onPdfSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.pdfFile = input.files?.[0] ?? null;
  }

  protected submit(): void {
    this.errorMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const courseId = raw.courseId;
    const payload: ChapterPayload = {
      title: raw.title,
      content: raw.content,
      codeExample: raw.codeExample || null,
      position: raw.position
    };

    this.submitting.set(true);

    const save$: Observable<{ id: number }> = this.chapterId()
      ? this.courseService.updateChapter(courseId, this.chapterId()!, payload)
      : this.courseService.createChapter(courseId, payload);

    save$.pipe(
      switchMap((chapter) => {
        if (this.pdfFile) {
          return this.courseService.uploadChapterPdf(courseId, chapter.id, this.pdfFile);
        }
        return of(chapter);
      })
    ).subscribe({
      next: (chapter) => this.router.navigate(['/cours', courseId, 'chapitres', chapter.id]),
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'L\'enregistrement a echoue'));
        this.submitting.set(false);
      }
    });
  }

  protected showError(field: string): boolean {
    const control = this.form.get(field);
    return !!control && control.invalid && control.touched;
  }

  protected backLink(): (string | number)[] {
    const courseId = this.form.getRawValue().courseId;
    return courseId > 0 ? ['/cours', courseId] : ['/cours'];
  }

  private loadCourses(preferredCourseId: number): void {
    this.courseService.list({ size: 100 }).subscribe({
      next: (page) => {
        this.courses.set(page.content);
        if (!this.chapterId() && preferredCourseId <= 0 && page.content.length === 1) {
          this.form.patchValue({ courseId: page.content[0].id });
        }
      },
      error: (error) => this.errorMessage.set(
        extractErrorMessage(error, 'Impossible de charger la liste des cours'))
    });
  }
}
