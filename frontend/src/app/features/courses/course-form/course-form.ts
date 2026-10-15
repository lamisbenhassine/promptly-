import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

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
      next: (course) => this.form.patchValue({
        title: course.title,
        description: course.description,
        learningObjectives: course.learningObjectives,
        category: course.category,
        published: course.published
      }),
      error: (error) => this.errorMessage.set(
        extractErrorMessage(error, 'Impossible de charger ce cours'))
    });
  }

  protected submit(): void {
    this.errorMessage.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload: CoursePayload = this.form.getRawValue();
    this.submitting.set(true);

    const request = this.editingId()
      ? this.courseService.update(this.editingId()!, payload)
      : this.courseService.create(payload);

    request.subscribe({
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
