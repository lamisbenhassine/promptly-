import { Role } from './user.model';

export type CourseCategory =
  | 'JAVA'
  | 'SPRING_BOOT'
  | 'ANGULAR'
  | 'TYPESCRIPT'
  | 'ALGORITHMES'
  | 'GENIE_LOGICIEL';

export const COURSE_CATEGORIES: CourseCategory[] = [
  'JAVA',
  'SPRING_BOOT',
  'ANGULAR',
  'TYPESCRIPT',
  'ALGORITHMES',
  'GENIE_LOGICIEL'
];

export const CATEGORY_LABELS: Record<CourseCategory, string> = {
  JAVA: 'Java',
  SPRING_BOOT: 'Spring Boot',
  ANGULAR: 'Angular',
  TYPESCRIPT: 'TypeScript',
  ALGORITHMES: 'Algorithmes',
  GENIE_LOGICIEL: 'Genie logiciel'
};

export interface InstructorSummary {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
}

export interface Course {
  id: number;
  title: string;
  description: string;
  learningObjectives: string;
  category: CourseCategory;
  published: boolean;
  imageUrl: string | null;
  pdfUrl: string | null;
  instructor: InstructorSummary;
  createdAt: string;
  updatedAt: string;
}

export interface CoursePayload {
  title: string;
  description: string;
  learningObjectives: string;
  category: CourseCategory;
  published: boolean;
  instructorId?: number | null;
}

export interface Chapter {
  id: number;
  courseId: number;
  title: string;
  content: string;
  codeExample: string | null;
  position: number;
  pdfUrl: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ChapterPayload {
  title: string;
  content: string;
  codeExample?: string | null;
  position?: number | null;
}

/** Reponse de POST /api/ai/.../summarize-pdf (Ollama). */
export interface PdfSummary {
  summary: string;
  model: string;
  sourceTitle: string;
  truncatedHint: boolean;
}

export interface Flashcard {
  front: string;
  back: string;
}

/** Reponse de POST /api/ai/.../flashcards. */
export interface FlashcardsResult {
  cards: Flashcard[];
  model: string;
  sourceTitle: string;
}
