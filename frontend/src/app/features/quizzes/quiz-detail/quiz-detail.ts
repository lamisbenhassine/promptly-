import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { extractErrorMessage } from '../../../core/models/api-error.model';
import { Quiz, QuizAttempt } from '../../../core/models/quiz.model';
import { QuizService } from '../../../core/services/quiz.service';

@Component({
  selector: 'app-quiz-detail',
  imports: [DatePipe, RouterLink],
  templateUrl: './quiz-detail.html',
  styleUrl: './quiz-detail.css'
})
export class QuizDetail implements OnInit {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly quizService = inject(QuizService);

  protected readonly quiz = signal<Quiz | null>(null);
  protected readonly myAttempts = signal<QuizAttempt[]>([]);
  protected readonly allAttempts = signal<QuizAttempt[]>([]);
  protected readonly lastResult = signal<QuizAttempt | null>(null);
  protected readonly selected = signal<Record<number, number>>({});
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly submitting = signal(false);
  protected readonly deleting = signal(false);
  protected readonly canManage = signal(false);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.load(id);
  }

  protected selectChoice(questionId: number, choiceId: number): void {
    this.selected.update((current) => ({ ...current, [questionId]: choiceId }));
  }

  protected isSelected(questionId: number, choiceId: number): boolean {
    return this.selected()[questionId] === choiceId;
  }

  protected submit(): void {
    const quiz = this.quiz();
    if (!quiz?.questions?.length) {
      return;
    }

    const selectedIds = Object.values(this.selected());
    if (selectedIds.length < quiz.questions.length) {
      this.errorMessage.set('Repondez a toutes les questions avant de soumettre.');
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);
    this.quizService.submit(quiz.id, { selectedChoiceIds: selectedIds }).subscribe({
      next: (attempt) => {
        this.lastResult.set(attempt);
        this.submitting.set(false);
        this.loadMyAttempts(quiz.id);
        if (this.canManage()) {
          this.loadAllAttempts(quiz.id);
        }
      },
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'La soumission a echoue'));
        this.submitting.set(false);
      }
    });
  }

  protected remove(): void {
    const quiz = this.quiz();
    if (!quiz) {
      return;
    }
    if (!confirm(`Supprimer le quiz « ${quiz.title} » ?`)) {
      return;
    }
    this.deleting.set(true);
    this.quizService.delete(quiz.id).subscribe({
      next: () => this.router.navigate(['/quiz']),
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'La suppression a echoue'));
        this.deleting.set(false);
      }
    });
  }

  private load(id: number): void {
    this.quizService.get(id).subscribe({
      next: (quiz) => {
        this.quiz.set(quiz);
        const manage = this.revealsAnswers(quiz);
        this.canManage.set(manage);
        this.loadMyAttempts(quiz.id);
        if (manage) {
          this.loadAllAttempts(quiz.id);
        }
      },
      error: (error) => this.errorMessage.set(
        extractErrorMessage(error, 'Quiz introuvable'))
    });
  }

  private revealsAnswers(quiz: Quiz): boolean {
    return (quiz.questions ?? []).some((question) =>
      question.choices.some((choice) => typeof choice.correct === 'boolean'));
  }

  private loadMyAttempts(quizId: number): void {
    this.quizService.myAttempts(quizId).subscribe({
      next: (attempts) => this.myAttempts.set(attempts),
      error: () => this.myAttempts.set([])
    });
  }

  private loadAllAttempts(quizId: number): void {
    this.quizService.allAttempts(quizId).subscribe({
      next: (attempts) => this.allAttempts.set(attempts),
      error: () => this.allAttempts.set([])
    });
  }
}
