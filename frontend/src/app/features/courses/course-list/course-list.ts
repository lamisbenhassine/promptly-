import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { extractErrorMessage } from '../../../core/models/api-error.model';
import {
  CATEGORY_LABELS,
  COURSE_CATEGORIES,
  Course,
  CourseCategory
} from '../../../core/models/course.model';
import { AuthService } from '../../../core/services/auth.service';
import { CourseService } from '../../../core/services/course.service';

@Component({
  selector: 'app-course-list',
  imports: [DatePipe, RouterLink],
  templateUrl: './course-list.html',
  styleUrl: './course-list.css'
})
export class CourseList implements OnInit {

  private readonly courseService = inject(CourseService);
  private readonly authService = inject(AuthService);

  protected readonly categories = COURSE_CATEGORIES;
  protected readonly labels = CATEGORY_LABELS;
  protected readonly fileUrl = CourseService.fileUrl;
  protected readonly canManage = this.authService.canManageCourses;

  protected readonly courses = signal<Course[]>([]);
  protected readonly selectedCategory = signal<CourseCategory | null>(null);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.reload();
  }

  protected filterBy(category: CourseCategory | null): void {
    this.selectedCategory.set(category);
    this.reload();
  }

  protected reload(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.courseService.list({ category: this.selectedCategory() }).subscribe({
      next: (page) => {
        this.courses.set(page.content);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'Impossible de charger les cours'));
        this.loading.set(false);
      }
    });
  }
}
