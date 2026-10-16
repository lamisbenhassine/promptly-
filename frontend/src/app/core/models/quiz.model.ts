export interface QuizChoice {
  id: number;
  text: string;
  correct?: boolean | null;
}

export interface QuizQuestion {
  id: number;
  text: string;
  position: number;
  choices: QuizChoice[];
}

export interface Quiz {
  id: number;
  title: string;
  description: string;
  published: boolean;
  courseId: number;
  courseTitle: string;
  chapterId: number | null;
  chapterTitle: string | null;
  questionCount: number;
  questions: QuizQuestion[] | null;
  createdAt: string;
  updatedAt: string;
}

export interface QuizChoicePayload {
  text: string;
  correct: boolean;
}

export interface QuizQuestionPayload {
  text: string;
  position?: number | null;
  choices: QuizChoicePayload[];
}

export interface QuizPayload {
  title: string;
  description: string;
  courseId: number;
  chapterId?: number | null;
  published: boolean;
  questions: QuizQuestionPayload[];
}

export interface QuizAttempt {
  id: number;
  quizId: number;
  quizTitle: string;
  studentId: number;
  studentEmail: string;
  studentFirstName: string;
  studentLastName: string;
  scorePercent: number;
  correctCount: number;
  totalQuestions: number;
  selectedChoiceIds: number[];
  submittedAt: string;
}

export interface AttemptPayload {
  selectedChoiceIds: number[];
}
