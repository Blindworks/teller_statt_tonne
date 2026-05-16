import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { ConfirmDialogService } from '../../shared/confirm-dialog/confirm-dialog.service';
import { hasAnyRole } from '../../users/user.model';
import { resolvePhotoUrl } from '../../users/photo-url';
import { TicketService } from '../ticket.service';
import {
  TICKET_CATEGORY_LABEL,
  TICKET_STATUS_LABEL,
  Ticket,
  TicketCategory,
  TicketStatus,
} from '../ticket.model';

type TicketForm = FormGroup<{
  title: FormControl<string>;
  description: FormControl<string>;
  category: FormControl<TicketCategory>;
}>;

@Component({
  selector: 'app-ticket-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './ticket-form.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TicketFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(TicketService);
  private readonly auth = inject(AuthService);
  private readonly confirm = inject(ConfirmDialogService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly ticketId = signal<number | null>(null);
  readonly ticket = signal<Ticket | null>(null);
  readonly loadError = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly saving = signal(false);
  readonly uploading = signal(false);
  readonly newComment = signal('');
  readonly postingComment = signal(false);
  readonly statusBusy = signal(false);
  readonly lightboxUrl = signal<string | null>(null);
  readonly lightboxAlt = signal<string>('');

  readonly isEdit = computed(() => this.ticketId() !== null);

  readonly currentUserId = computed(() => this.auth.currentUser()?.id ?? null);

  readonly isAdmin = computed(() =>
    hasAnyRole(this.auth.currentUser(), 'ADMINISTRATOR', 'TEAMLEITER'),
  );

  readonly isOwner = computed(() => {
    const t = this.ticket();
    const uid = this.currentUserId();
    return !!t && uid !== null && t.createdById === uid;
  });

  readonly canEditContent = computed(() => {
    if (!this.isEdit()) return true;
    const t = this.ticket();
    if (!t) return false;
    if (this.isAdmin()) return true;
    return this.isOwner() && t.status === 'OPEN';
  });

  readonly canDelete = this.canEditContent;

  readonly statuses: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'DONE', 'REJECTED'];
  readonly categories: TicketCategory[] = ['BUG', 'FEATURE'];

  readonly form: TicketForm = this.fb.group({
    title: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(255)]),
    description: this.fb.nonNullable.control(''),
    category: this.fb.nonNullable.control<TicketCategory>('BUG', [Validators.required]),
  });

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      const numId = Number(id);
      this.ticketId.set(numId);
      this.loadTicket(numId);
    }
  }

  private loadTicket(id: number): void {
    this.service.get(id).subscribe({
      next: (t) => {
        this.ticket.set(t);
        this.form.patchValue({
          title: t.title,
          description: t.description ?? '',
          category: t.category,
        });
        if (!this.canEditContent()) {
          this.form.disable({ emitEvent: false });
        }
      },
      error: () => this.loadError.set('Ticket konnte nicht geladen werden.'),
    });
  }

  statusLabel(status: TicketStatus): string {
    return TICKET_STATUS_LABEL[status];
  }

  categoryLabel(category: TicketCategory): string {
    return TICKET_CATEGORY_LABEL[category];
  }

  statusBadgeClass(status: TicketStatus): string {
    switch (status) {
      case 'OPEN':
        return 'bg-blue-100 text-blue-800';
      case 'IN_PROGRESS':
        return 'bg-amber-100 text-amber-800';
      case 'DONE':
        return 'bg-emerald-100 text-emerald-800';
      case 'REJECTED':
        return 'bg-zinc-200 text-zinc-700';
    }
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const payload = {
      title: v.title.trim(),
      description: v.description.trim() === '' ? null : v.description.trim(),
      category: v.category,
    };
    this.saving.set(true);
    this.errorMessage.set(null);
    const id = this.ticketId();
    const obs = id === null ? this.service.create(payload) : this.service.update(id, payload);
    obs.subscribe({
      next: (t) => {
        this.saving.set(false);
        if (id === null) {
          this.router.navigate(['/tickets', t.id]);
        } else {
          this.ticket.set(t);
        }
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMessage.set(this.extractError(err, 'Speichern fehlgeschlagen.'));
      },
    });
  }

  changeStatus(status: TicketStatus): void {
    const id = this.ticketId();
    if (id === null) return;
    if (this.ticket()?.status === status) return;
    this.statusBusy.set(true);
    this.errorMessage.set(null);
    this.service.updateStatus(id, status).subscribe({
      next: (t) => {
        this.ticket.set(t);
        this.statusBusy.set(false);
        if (!this.canEditContent()) {
          this.form.disable({ emitEvent: false });
        }
      },
      error: (err) => {
        this.statusBusy.set(false);
        this.errorMessage.set(this.extractError(err, 'Statuswechsel fehlgeschlagen.'));
      },
    });
  }

  onFiles(input: HTMLInputElement): void {
    const files = input.files;
    const id = this.ticketId();
    if (!files || files.length === 0 || id === null) return;
    this.uploading.set(true);
    this.errorMessage.set(null);

    const queue = Array.from(files);
    const next = () => {
      const file = queue.shift();
      if (!file) {
        this.uploading.set(false);
        input.value = '';
        return;
      }
      this.service.uploadAttachment(id, file).subscribe({
        next: (t) => {
          this.ticket.set(t);
          next();
        },
        error: (err) => {
          this.uploading.set(false);
          input.value = '';
          this.errorMessage.set(this.extractError(err, 'Upload fehlgeschlagen.'));
        },
      });
    };
    next();
  }

  async deleteAttachment(attachmentId: number): Promise<void> {
    const id = this.ticketId();
    if (id === null) return;
    const ok = await this.confirm.ask({
      title: 'Anhang entfernen?',
      message: 'Soll das Bild wirklich entfernt werden?',
      tone: 'danger',
      confirmLabel: 'Entfernen',
    });
    if (!ok) return;
    this.service.deleteAttachment(id, attachmentId).subscribe({
      next: () => {
        const t = this.ticket();
        if (t) {
          this.ticket.set({ ...t, attachments: t.attachments.filter((a) => a.id !== attachmentId) });
        }
      },
      error: (err) => this.errorMessage.set(this.extractError(err, 'Loeschen fehlgeschlagen.')),
    });
  }

  postComment(): void {
    const id = this.ticketId();
    const body = this.newComment().trim();
    if (id === null || body === '') return;
    this.postingComment.set(true);
    this.service.addComment(id, body).subscribe({
      next: (c) => {
        this.postingComment.set(false);
        this.newComment.set('');
        const t = this.ticket();
        if (t) {
          this.ticket.set({ ...t, comments: [...t.comments, c] });
        }
      },
      error: (err) => {
        this.postingComment.set(false);
        this.errorMessage.set(this.extractError(err, 'Kommentar konnte nicht gespeichert werden.'));
      },
    });
  }

  async deleteComment(commentId: number): Promise<void> {
    const id = this.ticketId();
    if (id === null) return;
    const ok = await this.confirm.ask({
      title: 'Kommentar löschen?',
      message: 'Der Kommentar wird unwiderruflich gelöscht.',
      tone: 'danger',
      confirmLabel: 'Löschen',
    });
    if (!ok) return;
    this.service.deleteComment(id, commentId).subscribe({
      next: () => {
        const t = this.ticket();
        if (t) {
          this.ticket.set({ ...t, comments: t.comments.filter((c) => c.id !== commentId) });
        }
      },
      error: (err) => this.errorMessage.set(this.extractError(err, 'Löschen fehlgeschlagen.')),
    });
  }

  canDeleteComment(authorId: number): boolean {
    return this.isAdmin() || authorId === this.currentUserId();
  }

  async askDeleteTicket(): Promise<void> {
    const id = this.ticketId();
    if (id === null) return;
    const ok = await this.confirm.ask({
      title: 'Ticket löschen?',
      message: 'Das Ticket sowie alle Anhänge und Kommentare werden entfernt.',
      tone: 'danger',
      confirmLabel: 'Löschen',
    });
    if (!ok) return;
    this.service.remove(id).subscribe({
      next: () => this.router.navigate(['/tickets']),
      error: (err) => this.errorMessage.set(this.extractError(err, 'Löschen fehlgeschlagen.')),
    });
  }

  onCommentInput(value: string): void {
    this.newComment.set(value);
  }

  resolveUrl(url: string): string {
    return resolvePhotoUrl(url) ?? url;
  }

  openLightbox(url: string, alt: string | null): void {
    this.lightboxUrl.set(this.resolveUrl(url));
    this.lightboxAlt.set(alt ?? 'Anhang');
  }

  closeLightbox(): void {
    this.lightboxUrl.set(null);
  }

  formatDate(iso: string): string {
    try {
      return new Date(iso).toLocaleString('de-DE', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return iso;
    }
  }

  private extractError(err: unknown, fallback: string): string {
    const anyErr = err as { error?: unknown };
    if (anyErr && typeof anyErr.error === 'string' && anyErr.error.length > 0) {
      return anyErr.error;
    }
    return fallback;
  }
}
