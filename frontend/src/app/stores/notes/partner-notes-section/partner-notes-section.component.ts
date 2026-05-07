import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { AuthService } from '../../../auth/auth.service';
import { ConfirmDialogService } from '../../../shared/confirm-dialog/confirm-dialog.service';
import { CreatePartnerNoteRequest, NoteVisibility, PartnerNote } from '../partner-note.models';
import { PartnerNotesService } from '../partner-notes.service';

@Component({
  selector: 'app-partner-notes-section',
  imports: [FormsModule, DatePipe],
  templateUrl: './partner-notes-section.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PartnerNotesSectionComponent {
  private readonly notesService = inject(PartnerNotesService);
  private readonly auth = inject(AuthService);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly partnerId = input.required<number>();

  readonly notes = signal<PartnerNote[]>([]);
  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly draftBody = signal('');
  readonly draftVisibility = signal<NoteVisibility>('SHARED');

  readonly isAdminOrTeamleiter = computed(() => {
    const roles = this.auth.currentUser()?.roles ?? [];
    return roles.includes('ADMINISTRATOR') || roles.includes('TEAMLEITER');
  });

  readonly canChooseVisibility = this.isAdminOrTeamleiter;
  readonly canDelete = this.isAdminOrTeamleiter;

  constructor() {
    effect(() => {
      const id = this.partnerId();
      if (id != null) {
        this.reload(id);
      }
    });
  }

  private reload(partnerId: number): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.notesService.list(partnerId).subscribe({
      next: (notes) => {
        this.notes.set(notes);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(this.toMessage(err, 'Notizen konnten nicht geladen werden.'));
        this.loading.set(false);
      },
    });
  }

  submit(): void {
    const body = this.draftBody().trim();
    if (!body) return;
    const partnerId = this.partnerId();
    const visibility = this.canChooseVisibility() ? this.draftVisibility() : 'SHARED';
    const request: CreatePartnerNoteRequest = { body, visibility };
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.notesService.create(partnerId, request).subscribe({
      next: (created) => {
        this.notes.update((current) => [created, ...current]);
        this.draftBody.set('');
        this.draftVisibility.set(this.canChooseVisibility() ? 'SHARED' : 'SHARED');
        this.submitting.set(false);
      },
      error: (err) => {
        this.errorMessage.set(this.toMessage(err, 'Notiz konnte nicht gespeichert werden.'));
        this.submitting.set(false);
      },
    });
  }

  async delete(note: PartnerNote): Promise<void> {
    if (!this.canDelete()) return;
    const ok = await this.confirmDialog.ask({
      title: 'Notiz entfernen',
      message: 'Diese Notiz wirklich entfernen? Sie bleibt in der Historie als gelöscht.',
      confirmLabel: 'Entfernen',
      tone: 'danger',
    });
    if (!ok) return;
    const partnerId = this.partnerId();
    this.notesService.delete(partnerId, note.id).subscribe({
      next: () => {
        this.notes.update((current) => current.filter((n) => n.id !== note.id));
      },
      error: (err) => {
        this.errorMessage.set(this.toMessage(err, 'Notiz konnte nicht gelöscht werden.'));
      },
    });
  }

  setVisibility(value: NoteVisibility): void {
    this.draftVisibility.set(value);
  }

  private toMessage(err: unknown, fallback: string): string {
    const e = err as { error?: unknown; message?: string } | null;
    if (e && typeof e.error === 'string' && e.error.trim()) return e.error;
    if (e && e.message) return e.message;
    return fallback;
  }
}
