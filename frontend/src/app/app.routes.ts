import { Routes } from '@angular/router';

import { authGuard, guestGuard, landingRedirectGuard, roleGuard } from './core/guards/auth.guard';

/**
 * Table de routage de l'application.
 *
 * Les pages protegees passent par {@code authGuard} (connexion requise) et,
 * pour l'administration, par {@code roleGuard(['ADMIN'])}. Ces gardes sont
 * un confort d'interface : la protection reelle est appliquee cote serveur.
 *
 * La racine redirige vers la connexion (visiteur) ou les cours (connecte).
 * Connexion et inscription n'affichent ni barre laterale ni barre superieure.
 */
export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    canActivate: [landingRedirectGuard],
    // Composant factice : la garde redirige toujours avant l'activation.
    loadComponent: () => import('./features/home/home').then((m) => m.Home),
    title: 'Promptly'
  },
  {
    path: 'inscription',
    loadComponent: () => import('./features/auth/register/register').then((m) => m.Register),
    canActivate: [guestGuard],
    title: 'Promptly - Inscription'
  },
  {
    path: 'connexion',
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
    canActivate: [guestGuard],
    title: 'Promptly - Connexion'
  },
  {
    path: 'profil',
    loadComponent: () => import('./features/auth/profile/profile').then((m) => m.Profile),
    canActivate: [authGuard],
    title: 'Promptly - Mon profil'
  },
  {
    path: 'admin/utilisateurs',
    loadComponent: () => import('./features/admin/users/users').then((m) => m.Users),
    canActivate: [authGuard, roleGuard(['ADMIN'])],
    title: 'Promptly - Utilisateurs'
  },
  {
    path: 'cours',
    loadComponent: () => import('./features/courses/course-list/course-list').then((m) => m.CourseList),
    canActivate: [authGuard],
    title: 'Promptly - Cours'
  },
  {
    path: 'quiz',
    loadComponent: () => import('./features/quizzes/quiz-list/quiz-list').then((m) => m.QuizList),
    canActivate: [authGuard],
    title: 'Promptly - Quiz'
  },
  {
    path: 'forum',
    loadComponent: () => import('./features/forum/forum-list/forum-list').then((m) => m.ForumList),
    canActivate: [authGuard],
    title: 'Promptly - Forum'
  },
  {
    path: 'forum/nouveau',
    loadComponent: () => import('./features/forum/forum-form/forum-form').then((m) => m.ForumForm),
    canActivate: [authGuard],
    title: 'Promptly - Nouveau sujet'
  },
  {
    path: 'forum/:id/modifier',
    loadComponent: () => import('./features/forum/forum-form/forum-form').then((m) => m.ForumForm),
    canActivate: [authGuard],
    title: 'Promptly - Modifier le sujet'
  },
  {
    path: 'forum/:id',
    loadComponent: () => import('./features/forum/forum-detail/forum-detail').then((m) => m.ForumDetail),
    canActivate: [authGuard],
    title: 'Promptly - Sujet'
  },
  {
    path: 'recherche',
    loadComponent: () => import('./features/search/search').then((m) => m.SearchPage),
    canActivate: [authGuard],
    title: 'Promptly - Recherche'
  },
  {
    path: 'reclamations',
    loadComponent: () => import('./features/complaints/complaint-list/complaint-list').then((m) => m.ComplaintList),
    canActivate: [authGuard],
    title: 'Promptly - Reclamations'
  },
  {
    path: 'reclamations/nouveau',
    loadComponent: () => import('./features/complaints/complaint-form/complaint-form').then((m) => m.ComplaintForm),
    canActivate: [authGuard],
    title: 'Promptly - Nouvelle reclamation'
  },
  {
    path: 'reclamations/:id',
    loadComponent: () => import('./features/complaints/complaint-detail/complaint-detail').then((m) => m.ComplaintDetail),
    canActivate: [authGuard],
    title: 'Promptly - Reclamation'
  },
  {
    path: 'quiz/nouveau',
    loadComponent: () => import('./features/quizzes/quiz-form/quiz-form').then((m) => m.QuizForm),
    canActivate: [authGuard, roleGuard(['ENSEIGNANT', 'ADMIN'])],
    title: 'Promptly - Nouveau quiz'
  },
  {
    path: 'quiz/:id/modifier',
    loadComponent: () => import('./features/quizzes/quiz-form/quiz-form').then((m) => m.QuizForm),
    canActivate: [authGuard, roleGuard(['ENSEIGNANT', 'ADMIN'])],
    title: 'Promptly - Modifier le quiz'
  },
  {
    path: 'quiz/:id',
    loadComponent: () => import('./features/quizzes/quiz-detail/quiz-detail').then((m) => m.QuizDetail),
    canActivate: [authGuard],
    title: 'Promptly - Quiz'
  },
  {
    path: 'cours/nouveau',
    loadComponent: () => import('./features/courses/course-form/course-form').then((m) => m.CourseForm),
    canActivate: [authGuard, roleGuard(['ENSEIGNANT', 'ADMIN'])],
    title: 'Promptly - Nouveau cours'
  },
  {
    path: 'chapitres/nouveau',
    loadComponent: () => import('./features/courses/chapter-form/chapter-form').then((m) => m.ChapterForm),
    canActivate: [authGuard, roleGuard(['ENSEIGNANT', 'ADMIN'])],
    title: 'Promptly - Nouveau chapitre'
  },
  {
    path: 'cours/:id/modifier',
    loadComponent: () => import('./features/courses/course-form/course-form').then((m) => m.CourseForm),
    canActivate: [authGuard, roleGuard(['ENSEIGNANT', 'ADMIN'])],
    title: 'Promptly - Modifier le cours'
  },
  {
    path: 'cours/:id/chapitres/nouveau',
    loadComponent: () => import('./features/courses/chapter-form/chapter-form').then((m) => m.ChapterForm),
    canActivate: [authGuard, roleGuard(['ENSEIGNANT', 'ADMIN'])],
    title: 'Promptly - Nouveau chapitre'
  },
  {
    path: 'cours/:id/chapitres/:chapterId/modifier',
    loadComponent: () => import('./features/courses/chapter-form/chapter-form').then((m) => m.ChapterForm),
    canActivate: [authGuard, roleGuard(['ENSEIGNANT', 'ADMIN'])],
    title: 'Promptly - Modifier le chapitre'
  },
  {
    path: 'cours/:id/chapitres/:chapterId',
    loadComponent: () => import('./features/courses/chapter-detail/chapter-detail').then((m) => m.ChapterDetail),
    canActivate: [authGuard],
    title: 'Promptly - Chapitre'
  },
  {
    path: 'cours/:id',
    loadComponent: () => import('./features/courses/course-detail/course-detail').then((m) => m.CourseDetail),
    canActivate: [authGuard],
    title: 'Promptly - Cours'
  },
  {
    path: '**',
    redirectTo: ''
  }
];
