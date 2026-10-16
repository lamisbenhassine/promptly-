import { Role } from './user.model';

export type ForumCategory =
  | 'GENERAL'
  | 'JAVA'
  | 'SPRING_BOOT'
  | 'ANGULAR'
  | 'ALGORITHMES'
  | 'AIDE';

export type ReactionType = 'LIKE' | 'LOVE' | 'HAHA' | 'WOW' | 'SAD' | 'ANGRY';

export const FORUM_CATEGORIES: ForumCategory[] = [
  'GENERAL',
  'JAVA',
  'SPRING_BOOT',
  'ANGULAR',
  'ALGORITHMES',
  'AIDE'
];

export const FORUM_CATEGORY_LABELS: Record<ForumCategory, string> = {
  GENERAL: 'General',
  JAVA: 'Java',
  SPRING_BOOT: 'Spring Boot',
  ANGULAR: 'Angular',
  ALGORITHMES: 'Algorithmes',
  AIDE: 'Aide'
};

export const REACTION_TYPES: ReactionType[] = [
  'LIKE', 'LOVE', 'HAHA', 'WOW', 'SAD', 'ANGRY'
];

export const REACTION_EMOJI: Record<ReactionType, string> = {
  LIKE: '👍',
  LOVE: '❤️',
  HAHA: '😂',
  WOW: '😮',
  SAD: '😢',
  ANGRY: '😡'
};

export const REACTION_LABELS: Record<ReactionType, string> = {
  LIKE: 'J\'aime',
  LOVE: 'J\'adore',
  HAHA: 'Haha',
  WOW: 'Wouah',
  SAD: 'Triste',
  ANGRY: 'Grrr'
};

export interface ForumAuthor {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
}

export interface ForumComment {
  id: number;
  postId: number;
  content: string;
  author: ForumAuthor;
  createdAt: string;
  updatedAt: string;
}

export interface ReactionSummary {
  total: number;
  counts: Partial<Record<ReactionType, number>>;
  myReaction: ReactionType | null;
}

export interface ForumPost {
  id: number;
  title: string;
  content: string;
  category: ForumCategory;
  locked: boolean;
  imageUrl: string | null;
  author: ForumAuthor;
  commentCount: number;
  comments: ForumComment[] | null;
  reactions: ReactionSummary;
  createdAt: string;
  updatedAt: string;
}

export interface ForumPostPayload {
  title: string;
  content: string;
  category: ForumCategory;
}

export interface ForumCommentPayload {
  content: string;
}
