import { Component, OnInit, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { extractErrorMessage } from '../../../core/models/api-error.model';
import { Chapter, Course } from '../../../core/models/course.model';
import { QuizPayload } from '../../../core/models/quiz.model';
import { CourseService } from '../../../core/services/course.service';
import { QuizService } from '../../../core/services/quiz.service';

@Component({
  selector: 'app-quiz-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './quiz-form.html',
  styleUrl: './quiz-form.css'
})
export class QuizForm implements OnInit {

  private readonly formBuilder = inject(FormBuilder);
  private readonly quizService = inject(QuizService);
  private readonly courseService = inject(CourseService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly courses = signal<Course[]>([]);
  protected readonly chapters = signal<Chapter[]>([]);
  protected readonly editingId = signal<number | null>(null);
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(150)]],
    description: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(2000)]],
    courseId: [0 as number, [Validators.required, Validators.min(1)]],
    chapterId: [null as number | null],
    published: false,
    questions: this.formBuilder.array([this.createQuestionGroup()])
  });

  ngOnInit(): void {
    this.courseService.list({ size: 100 }).subscribe({
      next: (page) => this.courses.set(page.content),
      error: (error) => this.errorMessage.set(
        extractErrorMessage(error, 'Impossible de charger les cours'))
    });

    this.form.controls.courseId.valueChanges.subscribe((courseId) => {
      this.loadChapters(courseId);
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) {
      return;
    }

    const id = Number(idParam);
    this.editingId.set(id);
    this.quizService.get(id).subscribe({
      next: (quiz) => {
        this.form.patchValue({
          title: quiz.title,
          description: quiz.description,
          courseId: quiz.courseId,
          chapterId: quiz.chapterId,
          published: quiz.published
        });
        this.loadChapters(quiz.courseId);
        this.questions.clear();
        for (const question of quiz.questions ?? []) {
          const group = this.createQuestionGroup();
          group.patchValue({
            text: question.text,
            position: question.position
          });
          const choices = group.controls.choices;
          choices.clear();
          for (const choice of question.choices) {
            choices.push(this.createChoiceGroup(choice.text, !!choice.correct));
          }
          this.questions.push(group);
        }
      },
      error: (error) => this.errorMessage.set(
        extractErrorMessage(error, 'Impossible de charger ce quiz'))
    });
  }

  protected get questions(): FormArray {
    return this.form.controls.questions;
  }

  protected choicesAt(questionIndex: number): FormArray {
    return this.questions.at(questionIndex).get('choices') as FormArray;
  }

  protected addQuestion(): void {
    this.questions.push(this.createQuestionGroup());
  }

  protected removeQuestion(index: number): void {
    if (this.questions.length > 1) {
      this.questions.removeAt(index);
    }
  }

  protected addChoice(questionIndex: number): void {
    this.choicesAt(questionIndex).push(this.createChoiceGroup('', false));
  }

  protected removeChoice(questionIndex: number, choiceIndex: number): void {
    const choices = this.choicesAt(questionIndex);
    if (choices.length > 2) {
      choices.removeAt(choiceIndex);
    }
  }

  protected submit(): void {
    this.errorMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const payload: QuizPayload = {
      title: raw.title,
      description: raw.description,
      courseId: raw.courseId,
      chapterId: raw.chapterId || null,
      published: raw.published,
      questions: raw.questions.map((question, index) => ({
        text: question.text,
        position: question.position || index + 1,
        choices: question.choices.map((choice) => ({
          text: choice.text,
          correct: choice.correct
        }))
      }))
    };

    this.submitting.set(true);
    const save$ = this.editingId()
      ? this.quizService.update(this.editingId()!, payload)
      : this.quizService.create(payload);

    save$.subscribe({
      next: (quiz) => this.router.navigate(['/quiz', quiz.id]),
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'L\'enregistrement a echoue'));
        this.submitting.set(false);
      }
    });
  }

  protected showError(path: string): boolean {
    const control = this.form.get(path);
    return !!control && control.invalid && control.touched;
  }

  private createQuestionGroup() {
    return this.formBuilder.nonNullable.group({
      text: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(1000)]],
      position: [1 as number],
      choices: this.formBuilder.array([
        this.createChoiceGroup('', true),
        this.createChoiceGroup('', false)
      ])
    });
  }

  private createChoiceGroup(text: string, correct: boolean) {
    return this.formBuilder.nonNullable.group({
      text: [text, [Validators.required, Validators.maxLength(500)]],
      correct: [correct]
    });
  }

  private loadChapters(courseId: number): void {
    if (!courseId || courseId <= 0) {
      this.chapters.set([]);
      return;
    }
    this.courseService.listChapters(courseId).subscribe({
      next: (chapters) => this.chapters.set(chapters),
      error: () => this.chapters.set([])
    });
  }
}
