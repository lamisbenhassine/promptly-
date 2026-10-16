import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { extractErrorMessage } from '../../../core/models/api-error.model';
import { Course } from '../../../core/models/course.model';
import { Quiz } from '../../../core/models/quiz.model';
import { AuthService } from '../../../core/services/auth.service';
import { CourseService } from '../../../core/services/course.service';
import { QuizService } from '../../../core/services/quiz.service';

@Component({
  selector: 'app-quiz-list',
  imports: [DatePipe, RouterLink],
  templateUrl: './quiz-list.html',
  styleUrl: './quiz-list.css'
})
export class QuizList implements OnInit {

  private readonly quizService = inject(QuizService);
  private readonly courseService = inject(CourseService);
  private readonly authService = inject(AuthService);

  protected readonly canManage = this.authService.canManageCourses;
  protected readonly quizzes = signal<Quiz[]>([]);
  protected readonly courses = signal<Course[]>([]);
  protected readonly selectedCourseId = signal<number | null>(null);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.courseService.list({ size: 100 }).subscribe({
      next: (page) => this.courses.set(page.content),
      error: () => this.courses.set([])
    });
    this.reload();
  }

  protected filterBy(courseId: number | null): void {
    this.selectedCourseId.set(courseId);
    this.reload();
  }

  protected reload(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.quizService.list(this.selectedCourseId()).subscribe({
      next: (quizzes) => {
        this.quizzes.set(quizzes);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'Impossible de charger les quiz'));
        this.loading.set(false);
      }
    });
  }
}
