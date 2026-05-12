import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { FoodCategory } from './food-category.model';

@Injectable({ providedIn: 'root' })
export class FoodCategoryService {
  private readonly http = inject(HttpClient);
  private readonly publicUrl = `${environment.apiBaseUrl}/api/food-categories`;
  private readonly adminUrl = `${environment.apiBaseUrl}/api/admin/food-categories`;

  listActive(): Observable<FoodCategory[]> {
    return this.http.get<FoodCategory[]>(this.publicUrl);
  }

  listAll(): Observable<FoodCategory[]> {
    return this.http.get<FoodCategory[]>(this.adminUrl);
  }

  create(dto: FoodCategory): Observable<FoodCategory> {
    return this.http.post<FoodCategory>(this.adminUrl, dto);
  }

  update(id: number, dto: FoodCategory): Observable<FoodCategory> {
    return this.http.put<FoodCategory>(`${this.adminUrl}/${id}`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.adminUrl}/${id}`);
  }
}
