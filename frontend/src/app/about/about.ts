import { Component, inject, signal, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { APP_VERSION } from '../version';
import { environment } from '../../environments/environment';

interface BackendVersion {
  name?: string;
  version?: string;
  time?: string | null;
}

@Component({
  selector: 'app-about',
  imports: [RouterLink],
  templateUrl: './about.html',
  styleUrl: './about.css',
})
export class AboutComponent implements OnInit {
  private readonly http = inject(HttpClient);

  protected readonly frontendVersion = APP_VERSION;
  protected readonly backend = signal<BackendVersion | null>(null);
  protected readonly backendError = signal<string | null>(null);
  protected readonly changelog = signal<string>('');
  protected readonly changelogError = signal<string | null>(null);

  ngOnInit(): void {
    this.http
      .get<BackendVersion>(`${environment.apiBaseUrl}/api/version`)
      .pipe(
        catchError(() => {
          this.backendError.set('Backend-Version nicht erreichbar.');
          return of(null);
        }),
      )
      .subscribe((res) => {
        if (res) this.backend.set(res);
      });

    this.http
      .get('CHANGELOG.md', { responseType: 'text' })
      .pipe(
        catchError(() => {
          this.changelogError.set('Changelog konnte nicht geladen werden.');
          return of('');
        }),
      )
      .subscribe((text) => this.changelog.set(text));
  }
}
