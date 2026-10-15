import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { extractErrorMessage } from '../../../core/models/api-error.model';
import { CATEGORY_LABELS, Course } from '../../../core/models/course.model';
import { AuthService } from '../../../core/services/auth.service';
import { CourseService } from '../../../core/services/course.service';

@Component({
  selector: 'app-course-detail',
  imports: [DatePipe, RouterLink],
  templateUrl: './course-detail.html',
  styleUrl: './course-detail.css'
})
export class CourseDetail implements OnInit {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly courseService = inject(CourseService);
  private readonly authService = inject(AuthService);

  protected readonly labels = CATEGORY_LABELS;
  protected readonly course = signal<Course | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly deleting = signal(false);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.errorMessage.set('Identifiant de cours invalide.');
      return;
    }

    this.courseService.get(id).subscribe({
      next: (course) => this.course.set(course),
      error: (error) => this.errorMessage.set(
        extractErrorMessage(error, 'Ce cours est introuvable ou n\'est pas encore publie.'))
    });
  }

  protected canManage(course: Course): boolean {
    const user = this.authService.currentUser();
    if (!user) {
      return false;
    }
    return user.role === 'ADMIN' || user.id === course.instructor.id;
  }

  protected remove(course: Course): void {
    if (!confirm(`Supprimer le cours « ${course.title} » ?`)) {
      return;
    }

    this.deleting.set(true);
    this.courseService.delete(course.id).subscribe({
      next: () => this.router.navigate(['/cours']),
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'La suppression a echoue'));
        this.deleting.set(false);
      }
    });
  }
}
