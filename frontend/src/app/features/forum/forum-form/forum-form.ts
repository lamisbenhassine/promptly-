import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { of, switchMap } from 'rxjs';

import { extractErrorMessage } from '../../../core/models/api-error.model';
import {
  FORUM_CATEGORIES,
  FORUM_CATEGORY_LABELS,
  ForumCategory,
  ForumPostPayload
} from '../../../core/models/forum.model';
import { ForumService } from '../../../core/services/forum.service';

@Component({
  selector: 'app-forum-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './forum-form.html'
})
export class ForumForm implements OnInit {

  private readonly formBuilder = inject(FormBuilder);
  private readonly forumService = inject(ForumService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly categories = FORUM_CATEGORIES;
  protected readonly labels = FORUM_CATEGORY_LABELS;
  protected readonly editingId = signal<number | null>(null);
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly imagePreview = signal<string | null>(null);

  private imageFile: File | null = null;

  protected readonly form = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(180)]],
    content: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(10000)]],
    category: this.formBuilder.nonNullable.control<ForumCategory>('GENERAL', Validators.required)
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) {
      return;
    }

    const id = Number(idParam);
    this.editingId.set(id);
    this.forumService.get(id).subscribe({
      next: (post) => {
        this.form.patchValue({
          title: post.title,
          content: post.content,
          category: post.category
        });
        this.imagePreview.set(ForumService.fileUrl(post.imageUrl));
      },
      error: (error) => this.errorMessage.set(
        extractErrorMessage(error, 'Impossible de charger ce sujet'))
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

  protected submit(): void {
    this.errorMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload: ForumPostPayload = this.form.getRawValue();
    this.submitting.set(true);

    const save$ = this.editingId()
      ? this.forumService.update(this.editingId()!, payload)
      : this.forumService.create(payload);

    save$.pipe(
      switchMap((post) => {
        if (this.imageFile) {
          return this.forumService.uploadImage(post.id, this.imageFile);
        }
        return of(post);
      })
    ).subscribe({
      next: (post) => this.router.navigate(['/forum', post.id]),
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
