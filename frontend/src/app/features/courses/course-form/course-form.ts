import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { of, switchMap } from 'rxjs';

import { extractErrorMessage } from '../../../core/models/api-error.model';
import {
  CATEGORY_LABELS,
  COURSE_CATEGORIES,
  CourseCategory,
  CoursePayload
} from '../../../core/models/course.model';
import { CourseService } from '../../../core/services/course.service';

@Component({
  selector: 'app-course-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './course-form.html'
})
export class CourseForm implements OnInit {

  private readonly formBuilder = inject(FormBuilder);
  private readonly courseService = inject(CourseService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly categories = COURSE_CATEGORIES;
  protected readonly labels = CATEGORY_LABELS;
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly editingId = signal<number | null>(null);
  protected readonly imagePreview = signal<string | null>(null);
  protected readonly currentPdfUrl = signal<string | null>(null);

  private imageFile: File | null = null;
  private pdfFile: File | null = null;

  protected readonly form = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(120)]],
    description: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(2000)]],
    learningObjectives: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(2000)]],
    category: this.formBuilder.nonNullable.control<CourseCategory>('JAVA', Validators.required),
    published: false
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) {
      return;
    }

    const id = Number(idParam);
    this.editingId.set(id);

    this.courseService.get(id).subscribe({
      next: (course) => {
        this.form.patchValue({
          title: course.title,
          description: course.description,
          learningObjectives: course.learningObjectives,
          category: course.category,
          published: course.published
        });
        this.imagePreview.set(CourseService.fileUrl(course.imageUrl));
        this.currentPdfUrl.set(CourseService.fileUrl(course.pdfUrl));
      },
      error: (error) => this.errorMessage.set(
        extractErrorMessage(error, 'Impossible de charger ce cours'))
    });
  }

  protected onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.imageFile = file;
    if (file) {
      this.imagePreview.set(URL.createObjectURL(file));
    }
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

    const payload: CoursePayload = this.form.getRawValue();
    this.submitting.set(true);

    const save$ = this.editingId()
      ? this.courseService.update(this.editingId()!, payload)
      : this.courseService.create(payload);

    save$.pipe(
      switchMap((course) => {
        if (this.imageFile) {
          return this.courseService.uploadImage(course.id, this.imageFile);
        }
        return of(course);
      }),
      switchMap((course) => {
        if (this.pdfFile) {
          return this.courseService.uploadPdf(course.id, this.pdfFile);
        }
        return of(course);
      })
    ).subscribe({
      next: (course) => this.router.navigate(['/cours', course.id]),
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
}
