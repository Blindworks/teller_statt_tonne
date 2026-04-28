export type QuizColor = 'GREEN' | 'YELLOW' | 'RED';

export interface QuizAnswer {
  id: string | null;
  text: string;
  isCorrect: boolean | null;
  isKnockout: boolean | null;
}

export interface QuizQuestion {
  id: string | null;
  text: string;
  weight: number;
  answers: QuizAnswer[];
}

export interface QuizResultCategory {
  id: string | null;
  label: string;
  color: QuizColor;
  minScore: number;
  maxScore: number | null;
}

export interface SubmittedAnswer {
  questionId: string;
  selectedAnswerIds: string[];
}

export interface QuizSubmission {
  applicantName: string;
  applicantEmail: string;
  answers: SubmittedAnswer[];
}

export interface QuizResult {
  attemptId: string;
  score: number;
  resultLabel: string | null;
  color: QuizColor;
  knockoutTriggered: boolean;
}

export interface QuizAttemptSelectedAnswer {
  answerId: string | null;
  answerText: string | null;
  wasKnockout: boolean;
}

export interface QuizAttemptAnswer {
  questionId: string | null;
  questionText: string | null;
  questionWeight: number;
  wasCorrect: boolean;
  selectedAnswers: QuizAttemptSelectedAnswer[];
}

export interface QuizAttempt {
  id: string;
  applicantName: string;
  applicantEmail: string;
  score: number;
  resultLabel: string | null;
  color: QuizColor;
  knockoutTriggered: boolean;
  completedAt: string;
  answers: QuizAttemptAnswer[];
}

export const ALLOWED_WEIGHTS: ReadonlyArray<number> = [0.5, 1.0, 1.5];

export const COLOR_LABELS: Record<QuizColor, string> = {
  GREEN: 'Grün',
  YELLOW: 'Gelb',
  RED: 'Rot',
};

export const COLOR_EMOJI: Record<QuizColor, string> = {
  GREEN: '🟢',
  YELLOW: '🟡',
  RED: '🔴',
};

export function emptyQuestion(): QuizQuestion {
  return {
    id: null,
    text: '',
    weight: 1.0,
    answers: [
      { id: null, text: '', isCorrect: false, isKnockout: false },
      { id: null, text: '', isCorrect: false, isKnockout: false },
      { id: null, text: '', isCorrect: false, isKnockout: false },
      { id: null, text: '', isCorrect: false, isKnockout: false },
    ],
  };
}

export function emptyCategory(): QuizResultCategory {
  return { id: null, label: '', color: 'GREEN', minScore: 0, maxScore: 0 };
}
