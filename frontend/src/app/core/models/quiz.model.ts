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
  durationMinutes: number | null;
  secondsPerQuestion: number | null;
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
  durationMinutes?: number | null;
  secondsPerQuestion?: number | null;
  questions: QuizQuestionPayload[];
}

export interface ChoiceReview {
  id: number;
  text: string;
  correct: boolean;
  selected: boolean;
}

export interface QuestionReview {
  questionId: number;
  text: string;
  position: number;
  answeredCorrectly: boolean;
  choices: ChoiceReview[];
  /** GUESS | KNOW — enseignant seulement */
  integrityLabel?: string | null;
  integrityConfidence?: number | null;
  timeSpentMs?: number | null;
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
  review: QuestionReview[];
  submittedAt: string;
}

export interface QuestionTimingPayload {
  questionId: number;
  timeSpentMs: number;
  selectionChanges: number;
  choiceIndex: number;
}

export interface AttemptPayload {
  selectedChoiceIds: number[];
  questionTimings: QuestionTimingPayload[];
  timedOut: boolean;
}

export interface UpdateAttemptPayload {
  scorePercent: number;
  correctCount: number;
}

export interface GenerateQuizPayload {
  title: string;
  description: string;
  courseId: number;
  chapterId?: number | null;
}

export interface GeneratedQuiz {
  questions: QuizQuestionPayload[];
  model: string;
  courseTitle: string;
}
