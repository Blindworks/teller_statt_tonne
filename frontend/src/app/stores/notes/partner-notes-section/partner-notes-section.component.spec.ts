import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { AuthService } from '../../../auth/auth.service';
import { User } from '../../../users/user.model';
import { PartnerNote } from '../partner-note.models';
import { PartnerNotesSectionComponent } from './partner-notes-section.component';

class AuthServiceStub {
  readonly currentUser = signal<User | null>(null);
  setRoles(roles: string[]): void {
    this.currentUser.set({
      id: 1,
      firstName: 'Test',
      lastName: 'User',
      roles,
      email: 't@example.de',
      phone: null,
      street: null,
      postalCode: null,
      city: null,
      country: null,
      photoUrl: null,
      onlineStatus: 'OFFLINE',
      status: 'ACTIVE',
      tags: [],
    });
  }
}

function makeNote(overrides: Partial<PartnerNote> = {}): PartnerNote {
  return {
    id: 1,
    partnerId: 42,
    body: 'Notiz',
    visibility: 'SHARED',
    createdAt: '2026-05-07T12:00:00Z',
    authorUserId: 1,
    authorDisplayName: 'Test User',
    deleted: false,
    ...overrides,
  };
}

describe('PartnerNotesSectionComponent', () => {
  let httpMock: HttpTestingController;
  let auth: AuthServiceStub;

  beforeEach(async () => {
    auth = new AuthServiceStub();
    await TestBed.configureTestingModule({
      imports: [PartnerNotesSectionComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('versteckt Visibility-Auswahl und Lösch-Button für Retter', async () => {
    auth.setRoles(['RETTER']);
    const fixture = TestBed.createComponent(PartnerNotesSectionComponent);
    fixture.componentRef.setInput('partnerId', 42);
    fixture.detectChanges();

    const req = httpMock.expectOne((r) => r.url.endsWith('/api/partners/42/notes'));
    req.flush([makeNote()]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.canChooseVisibility()).toBe(false);
    expect(fixture.componentInstance.canDelete()).toBe(false);

    const html = (fixture.nativeElement as HTMLElement).innerHTML;
    expect(html).not.toContain('Intern (Botschafter/Admin)');
    expect(html).not.toContain('Notiz löschen');
  });

  it('zeigt Visibility-Auswahl und Lösch-Button für Botschafter', async () => {
    auth.setRoles(['BOTSCHAFTER']);
    const fixture = TestBed.createComponent(PartnerNotesSectionComponent);
    fixture.componentRef.setInput('partnerId', 42);
    fixture.detectChanges();

    const req = httpMock.expectOne((r) => r.url.endsWith('/api/partners/42/notes'));
    req.flush([makeNote()]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.canChooseVisibility()).toBe(true);
    expect(fixture.componentInstance.canDelete()).toBe(true);

    const html = (fixture.nativeElement as HTMLElement).innerHTML;
    expect(html).toContain('Intern (Botschafter/Admin)');
  });

  it('postet Notiz mit gewählter Sichtbarkeit', async () => {
    auth.setRoles(['BOTSCHAFTER']);
    const fixture = TestBed.createComponent(PartnerNotesSectionComponent);
    fixture.componentRef.setInput('partnerId', 42);
    fixture.detectChanges();

    httpMock.expectOne((r) => r.url.endsWith('/api/partners/42/notes')).flush([]);
    await fixture.whenStable();

    const cmp = fixture.componentInstance;
    cmp.draftBody.set('Neue Notiz');
    cmp.setVisibility('INTERNAL');
    cmp.submit();

    const post = httpMock.expectOne(
      (r) => r.method === 'POST' && r.url.endsWith('/api/partners/42/notes'),
    );
    expect(post.request.body).toEqual({ body: 'Neue Notiz', visibility: 'INTERNAL' });
    post.flush(makeNote({ id: 99, body: 'Neue Notiz', visibility: 'INTERNAL' }));

    expect(cmp.notes()[0].id).toBe(99);
    expect(cmp.draftBody()).toBe('');
  });
});
