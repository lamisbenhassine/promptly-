import { Role } from './user.model';

export type ComplaintCategory =
  | 'TECHNIQUE'
  | 'DISPONIBILITE'
  | 'PEDAGOGIQUE'
  | 'COMPTE'
  | 'AUTRE';

export type ComplaintStatus = 'OUVERTE' | 'EN_COURS' | 'RESOLUE' | 'REJETEE';
export type ComplaintUrgency = 'URGENT' | 'NON_URGENT';
export type ComplaintPriority = 'HAUTE' | 'MOYENNE' | 'BASSE';
export type ComplaintEmotion =
  | 'ANGRY'
  | 'HAPPY'
  | 'SAD'
  | 'FEARFUL'
  | 'SURPRISED'
  | 'NEUTRAL';

export const COMPLAINT_CATEGORIES: ComplaintCategory[] = [
  'TECHNIQUE', 'DISPONIBILITE', 'PEDAGOGIQUE', 'COMPTE', 'AUTRE'
];

export const COMPLAINT_STATUSES: ComplaintStatus[] = [
  'OUVERTE', 'EN_COURS', 'RESOLUE', 'REJETEE'
];

export const COMPLAINT_CATEGORY_LABELS: Record<ComplaintCategory, string> = {
  TECHNIQUE: 'Technique',
  DISPONIBILITE: 'Disponibilite',
  PEDAGOGIQUE: 'Pedagogique',
  COMPTE: 'Compte',
  AUTRE: 'Autre'
};

export const COMPLAINT_STATUS_LABELS: Record<ComplaintStatus, string> = {
  OUVERTE: 'Ouverte',
  EN_COURS: 'En cours',
  RESOLUE: 'Resolue',
  REJETEE: 'Rejetee'
};

export const COMPLAINT_URGENCY_LABELS: Record<ComplaintUrgency, string> = {
  URGENT: 'Urgent',
  NON_URGENT: 'Non urgent'
};

export const COMPLAINT_PRIORITY_LABELS: Record<ComplaintPriority, string> = {
  HAUTE: 'Priorite haute',
  MOYENNE: 'Priorite moyenne',
  BASSE: 'Priorite basse'
};

export const COMPLAINT_EMOTION_LABELS: Record<ComplaintEmotion, string> = {
  ANGRY: 'En colere',
  HAPPY: 'Content',
  SAD: 'Triste',
  FEARFUL: 'Inquiet / peur',
  SURPRISED: 'Surpris',
  NEUTRAL: 'Neutre'
};

export interface ComplaintAuthor {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
}

export interface Complaint {
  id: number;
  subject: string;
  description: string;
  category: ComplaintCategory;
  status: ComplaintStatus;
  author: ComplaintAuthor;
  adminResponse: string | null;
  handledBy: ComplaintAuthor | null;
  createdAt: string;
  updatedAt: string;
  resolvedAt: string | null;
  /** Admin seulement */
  mlUrgency?: ComplaintUrgency | null;
  mlPriority?: ComplaintPriority | null;
  mlConfidence?: number | null;
  mlEmotion?: ComplaintEmotion | null;
  mlEmotionConfidence?: number | null;
}

export interface ComplaintPayload {
  subject: string;
  description: string;
}

export interface ComplaintStatusPayload {
  status: ComplaintStatus;
  adminResponse?: string | null;
}
