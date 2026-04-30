import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  QuizAttempt,
  QuizQuestion,
  QuizResult,
  QuizResultCategory,
  QuizSubmission,
} from './quiz.model';

@Injectable({ providedIn: 'root' })
export class QuizService {
  private readonly http = inject(HttpClient);
  private readonly publicUrl = `${environment.apiBaseUrl}/api/public/quiz`;
  private readonly adminUrl = `${environment.apiBaseUrl}/api/quiz`;

  // Public
  getPublicQuestions(): Observable<QuizQuestion[]> {
    return this.http.get<QuizQuestion[]>(`${this.publicUrl}/questions`);
  }

  submit(submission: QuizSubmission): Observable<QuizResult> {
    return this.http.post<QuizResult>(`${this.publicUrl}/submit`, submission);
  }

  // Admin: Questions
  listQuestions(): Observable<QuizQuestion[]> {
    return this.http.get<QuizQuestion[]>(`${this.adminUrl}/questions`);
  }

  getQuestion(id: number): Observable<QuizQuestion> {
    return this.http.get<QuizQuestion>(`${this.adminUrl}/questions/${id}`);
  }

  createQuestion(q: QuizQuestion): Observable<QuizQuestion> {
    return this.http.post<QuizQuestion>(`${this.adminUrl}/questions`, q);
  }

  updateQuestion(id: number, q: QuizQuestion): Observable<QuizQuestion> {
    return this.http.put<QuizQuestion>(`${this.adminUrl}/questions/${id}`, q);
  }

  deleteQuestion(id: number): Observable<void> {
    return this.http.delete<void>(`${this.adminUrl}/questions/${id}`);
  }

  // Admin: Categories
  listCategories(): Observable<QuizResultCategory[]> {
    return this.http.get<QuizResultCategory[]>(`${this.adminUrl}/categories`);
  }

  createCategory(c: QuizResultCategory): Observable<QuizResultCategory> {
    return this.http.post<QuizResultCategory>(`${this.adminUrl}/categories`, c);
  }

  updateCategory(id: number, c: QuizResultCategory): Observable<QuizResultCategory> {
    return this.http.put<QuizResultCategory>(`${this.adminUrl}/categories/${id}`, c);
  }

  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.adminUrl}/categories/${id}`);
  }

  // Admin: Attempts
  listAttempts(): Observable<QuizAttempt[]> {
    return this.http.get<QuizAttempt[]>(`${this.adminUrl}/attempts`);
  }

  getAttempt(id: number): Observable<QuizAttempt> {
    return this.http.get<QuizAttempt>(`${this.adminUrl}/attempts/${id}`);
  }
}
