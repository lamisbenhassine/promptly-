import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { extractErrorMessage } from '../../../core/models/api-error.model';
import {
  QuestionTimingPayload,
  Quiz,
  QuizAttempt,
  QuizQuestion
} from '../../../core/models/quiz.model';
import { QuizService } from '../../../core/services/quiz.service';

interface QuestionStats {
  timeSpentMs: number;
  selectionChanges: number;
  choiceIndex: number;
}

@Component({
  selector: 'app-quiz-detail',
  imports: [DatePipe, DecimalPipe, FormsModule, RouterLink],
  templateUrl: './quiz-detail.html',
  styleUrl: './quiz-detail.css'
})
export class QuizDetail implements OnInit, OnDestroy {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly quizService = inject(QuizService);

  protected readonly quiz = signal<Quiz | null>(null);
  protected readonly myAttempts = signal<QuizAttempt[]>([]);
  protected readonly allAttempts = signal<QuizAttempt[]>([]);
  protected readonly reviewedAttempt = signal<QuizAttempt | null>(null);
  protected readonly selected = signal<Record<number, number>>({});
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly submitting = signal(false);
  protected readonly deleting = signal(false);
  protected readonly canManage = signal(false);
  protected readonly quizSubmitted = signal(false);
  protected readonly quizStarted = signal(false);

  protected readonly currentIndex = signal(0);
  protected readonly globalRemainingSec = signal<number | null>(null);
  protected readonly questionRemainingSec = signal<number | null>(null);

  protected readonly editAttemptId = signal<number | null>(null);
  protected readonly editScore = signal(0);
  protected readonly editCorrect = signal(0);
  protected readonly savingEdit = signal(false);

  private globalTimerId: ReturnType<typeof setInterval> | null = null;
  private questionTimerId: ReturnType<typeof setInterval> | null = null;
  private questionStartedAt = 0;
  private readonly stats = new Map<number, QuestionStats>();

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.load(id);
  }

  ngOnDestroy(): void {
    this.clearTimers();
  }

  protected currentQuestion(): QuizQuestion | null {
    const questions = this.quiz()?.questions;
    if (!questions?.length) {
      return null;
    }
    return questions[this.currentIndex()] ?? null;
  }

  protected totalQuestions(): number {
    return this.quiz()?.questions?.length ?? 0;
  }

  protected startQuiz(): void {
    const quiz = this.quiz();
    if (!quiz?.questions?.length || this.quizSubmitted()) {
      return;
    }
    this.quizStarted.set(true);
    this.currentIndex.set(0);
    this.beginQuestionTimer();
    this.beginGlobalTimer(quiz.durationMinutes);
  }

  protected selectChoice(questionId: number, choiceId: number, choiceIndex: number): void {
    if (this.quizSubmitted() || !this.quizStarted()) {
      return;
    }

    const previous = this.selected()[questionId];
    this.selected.update((current) => ({ ...current, [questionId]: choiceId }));

    const stats = this.ensureStats(questionId);
    if (previous !== undefined && previous !== choiceId) {
      stats.selectionChanges += 1;
    }
    stats.choiceIndex = choiceIndex;
  }

  protected isSelected(questionId: number, choiceId: number): boolean {
    return this.selected()[questionId] === choiceId;
  }

  protected nextQuestion(): void {
    this.flushCurrentQuestionTime();
    const last = this.totalQuestions() - 1;
    if (this.currentIndex() >= last) {
      this.submit(false);
      return;
    }
    this.currentIndex.update((i) => i + 1);
    this.beginQuestionTimer();
  }

  protected prevQuestion(): void {
    if (this.currentIndex() <= 0) {
      return;
    }
    this.flushCurrentQuestionTime();
    this.currentIndex.update((i) => i - 1);
    this.beginQuestionTimer();
  }

  protected submit(timedOut: boolean): void {
    const quiz = this.quiz();
    if (!quiz?.questions?.length || this.submitting() || this.quizSubmitted()) {
      return;
    }

    this.flushCurrentQuestionTime();
    this.clearTimers();

    const selectedIds = quiz.questions
      .map((question) => this.selected()[question.id])
      .filter((id): id is number => typeof id === 'number');

    if (!timedOut && selectedIds.length < quiz.questions.length) {
      this.errorMessage.set('Repondez a toutes les questions avant de soumettre.');
      return;
    }

    // Si timeout : on envoie les reponses deja choisies (peut etre partiel).
    const timings: QuestionTimingPayload[] = quiz.questions.map((question) => {
      const stats = this.stats.get(question.id);
      const choiceId = this.selected()[question.id];
      const choiceIndex = choiceId == null
        ? 0
        : Math.max(0, question.choices.findIndex((choice) => choice.id === choiceId));
      return {
        questionId: question.id,
        timeSpentMs: stats?.timeSpentMs ?? 0,
        selectionChanges: stats?.selectionChanges ?? 0,
        choiceIndex: stats?.choiceIndex ?? choiceIndex
      };
    });

    this.submitting.set(true);
    this.errorMessage.set(null);
    this.quizService.submit(quiz.id, {
      selectedChoiceIds: selectedIds,
      questionTimings: timings,
      timedOut
    }).subscribe({
      next: (attempt) => {
        this.reviewedAttempt.set(attempt);
        this.quizSubmitted.set(true);
        this.quizStarted.set(false);
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

  protected formatTime(totalSeconds: number | null): string {
    if (totalSeconds == null) {
      return '--:--';
    }
    const sec = Math.max(0, totalSeconds);
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }

  protected openReview(attempt: QuizAttempt): void {
    this.reviewedAttempt.set(attempt);
    // Le panneau correction est plus haut sur la page : on y amene le focus.
    setTimeout(() => {
      document.getElementById('quiz-review-panel')?.scrollIntoView({
        behavior: 'smooth',
        block: 'start'
      });
    }, 0);
  }

  protected closeReview(): void {
    this.reviewedAttempt.set(null);
  }

  protected hasIntegrityLabels(attempt: QuizAttempt): boolean {
    return (attempt.review ?? []).some((q) => !!q.integrityLabel);
  }

  protected startEdit(attempt: QuizAttempt): void {
    this.editAttemptId.set(attempt.id);
    this.editScore.set(attempt.scorePercent);
    this.editCorrect.set(attempt.correctCount);
  }

  protected cancelEdit(): void {
    this.editAttemptId.set(null);
  }

  protected saveEdit(attempt: QuizAttempt): void {
    const quiz = this.quiz();
    if (!quiz) {
      return;
    }

    const score = Number(this.editScore());
    const correct = Number(this.editCorrect());
    if (Number.isNaN(score) || score < 0 || score > 100) {
      this.errorMessage.set('Le score doit etre entre 0 et 100.');
      return;
    }
    if (Number.isNaN(correct) || correct < 0 || correct > attempt.totalQuestions) {
      this.errorMessage.set(
        `Le nombre de bonnes reponses doit etre entre 0 et ${attempt.totalQuestions}.`);
      return;
    }

    this.savingEdit.set(true);
    this.errorMessage.set(null);
    this.quizService.updateAttempt(quiz.id, attempt.id, {
      scorePercent: score,
      correctCount: correct
    }).subscribe({
      next: (updated) => {
        this.allAttempts.update((list) =>
          list.map((item) => item.id === updated.id ? updated : item));
        if (this.reviewedAttempt()?.id === updated.id) {
          this.reviewedAttempt.set(updated);
        }
        this.editAttemptId.set(null);
        this.savingEdit.set(false);
      },
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'La modification a echoue'));
        this.savingEdit.set(false);
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

  private beginGlobalTimer(durationMinutes: number | null | undefined): void {
    if (this.globalTimerId) {
      clearInterval(this.globalTimerId);
      this.globalTimerId = null;
    }
    if (!durationMinutes || durationMinutes <= 0) {
      this.globalRemainingSec.set(null);
      return;
    }
    this.globalRemainingSec.set(durationMinutes * 60);
    this.globalTimerId = setInterval(() => {
      const left = (this.globalRemainingSec() ?? 1) - 1;
      this.globalRemainingSec.set(left);
      if (left <= 0) {
        this.submit(true);
      }
    }, 1000);
  }

  private beginQuestionTimer(): void {
    if (this.questionTimerId) {
      clearInterval(this.questionTimerId);
      this.questionTimerId = null;
    }
    this.questionStartedAt = Date.now();
    const perQuestion = this.quiz()?.secondsPerQuestion;
    if (!perQuestion || perQuestion <= 0) {
      this.questionRemainingSec.set(null);
      return;
    }
    this.questionRemainingSec.set(perQuestion);
    this.questionTimerId = setInterval(() => {
      const left = (this.questionRemainingSec() ?? 1) - 1;
      this.questionRemainingSec.set(left);
      if (left <= 0) {
        this.nextQuestion();
      }
    }, 1000);
  }

  private flushCurrentQuestionTime(): void {
    const question = this.currentQuestion();
    if (!question) {
      return;
    }
    const elapsed = Date.now() - this.questionStartedAt;
    const stats = this.ensureStats(question.id);
    stats.timeSpentMs += Math.max(0, elapsed);
    this.questionStartedAt = Date.now();
  }

  private ensureStats(questionId: number): QuestionStats {
    let stats = this.stats.get(questionId);
    if (!stats) {
      stats = { timeSpentMs: 0, selectionChanges: 0, choiceIndex: 0 };
      this.stats.set(questionId, stats);
    }
    return stats;
  }

  private clearTimers(): void {
    if (this.globalTimerId) {
      clearInterval(this.globalTimerId);
      this.globalTimerId = null;
    }
    if (this.questionTimerId) {
      clearInterval(this.questionTimerId);
      this.questionTimerId = null;
    }
  }

  private load(id: number): void {
    this.quizService.get(id).subscribe({
      next: (quiz) => {
        this.quiz.set(quiz);
        this.canManage.set(this.revealsAnswers(quiz));
        this.loadMyAttempts(quiz.id);
        if (this.canManage()) {
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
      next: (attempts) => {
        this.myAttempts.set(attempts);
        if (!this.canManage() && attempts.length > 0 && !this.reviewedAttempt()) {
          this.reviewedAttempt.set(attempts[0]);
          this.quizSubmitted.set(true);
        }
      },
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
