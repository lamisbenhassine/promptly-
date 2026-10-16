import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { extractErrorMessage } from '../../core/models/api-error.model';
import { Course } from '../../core/models/course.model';
import { ForumPost } from '../../core/models/forum.model';
import { Quiz } from '../../core/models/quiz.model';
import { AuthService } from '../../core/services/auth.service';
import { CourseService } from '../../core/services/course.service';
import { ForumService } from '../../core/services/forum.service';
import { QuizService } from '../../core/services/quiz.service';

@Component({
  selector: 'app-search',
  imports: [RouterLink],
  templateUrl: './search.html',
  styleUrl: './search.css'
})
export class SearchPage implements OnInit {

  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly courseService = inject(CourseService);
  private readonly quizService = inject(QuizService);
  private readonly forumService = inject(ForumService);

  protected readonly query = signal('');
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly courses = signal<Course[]>([]);
  protected readonly quizzes = signal<Quiz[]>([]);
  protected readonly posts = signal<ForumPost[]>([]);

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const q = (params.get('q') ?? '').trim();
      this.query.set(q);
      if (q) {
        this.runSearch(q);
      } else {
        this.courses.set([]);
        this.quizzes.set([]);
        this.posts.set([]);
      }
    });
  }

  protected total(): number {
    return this.courses().length + this.quizzes().length + this.posts().length;
  }

  private runSearch(raw: string): void {
    if (!this.authService.isAuthenticated()) {
      this.errorMessage.set('Connectez-vous pour utiliser la recherche.');
      return;
    }

    const needle = raw.toLowerCase();
    this.loading.set(true);
    this.errorMessage.set(null);

    forkJoin({
      courses: this.courseService.list({ size: 100 }).pipe(catchError(() => of({ content: [] as Course[] }))),
      quizzes: this.quizService.list().pipe(catchError(() => of([] as Quiz[]))),
      posts: this.forumService.list({}).pipe(catchError(() => of({ content: [] as ForumPost[] })))
    }).subscribe({
      next: ({ courses, quizzes, posts }) => {
        this.courses.set(courses.content.filter((c) =>
          `${c.title} ${c.description}`.toLowerCase().includes(needle)));
        this.quizzes.set(quizzes.filter((q) =>
          `${q.title} ${q.description} ${q.courseTitle}`.toLowerCase().includes(needle)));
        this.posts.set(posts.content.filter((p) =>
          `${p.title} ${p.content}`.toLowerCase().includes(needle)));
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'La recherche a echoue'));
        this.loading.set(false);
      }
    });
  }
}
