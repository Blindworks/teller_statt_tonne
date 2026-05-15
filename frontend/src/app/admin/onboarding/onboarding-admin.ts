import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ConfirmDialogService } from '../../shared/confirm-dialog/confirm-dialog.service';
import { TestUserBadgeComponent } from '../../shared/test-user-badge/test-user-badge';
import { AuthService } from '../../auth/auth.service';
import { UserService } from '../../users/user.service';
import { User } from '../../users/user.model';
import { OnboardingService } from '../../onboarding/onboarding.service';
import {
  IntroductionBookingInfo,
  IntroductionSlot,
  OnboardingStatus,
} from '../../onboarding/onboarding.models';
import { TestUser, TestUserService } from './test-user.service';

type SlotForm = FormGroup<{
  date: FormControl<string>;
  startTime: FormControl<string>;
  endTime: FormControl<string>;
  location: FormControl<string>;
  capacity: FormControl<number>;
  notes: FormControl<string>;
}>;

@Component({
  selector: 'app-onboarding-admin',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule, RouterLink, TestUserBadgeComponent],
  templateUrl: './onboarding-admin.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OnboardingAdminComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly onboarding = inject(OnboardingService);
  private readonly users = inject(UserService);
  private readonly confirm = inject(ConfirmDialogService);
  private readonly testUsers = inject(TestUserService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly slots = signal<IntroductionSlot[]>([]);
  readonly pendingUsers = signal<Array<{ user: User; status: OnboardingStatus | null }>>([]);
  readonly testRetter = signal<TestUser[]>([]);
  readonly loading = signal(true);
  readonly creating = signal(false);
  readonly creatingTestUser = signal(false);
  readonly impersonatingId = signal<number | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly expandedSlotIds = signal<ReadonlySet<number>>(new Set());

  isExpanded(slotId: number): boolean {
    return this.expandedSlotIds().has(slotId);
  }

  toggleExpanded(slotId: number): void {
    this.expandedSlotIds.update((current) => {
      const next = new Set(current);
      if (next.has(slotId)) {
        next.delete(slotId);
      } else {
        next.add(slotId);
      }
      return next;
    });
  }

  initials(booking: IntroductionBookingInfo): string {
    const first = (booking.firstName ?? '').trim();
    const last = (booking.lastName ?? '').trim();
    const a = first.charAt(0);
    const b = last.charAt(0);
    const result = `${a}${b}`.toUpperCase();
    return result || (booking.email?.charAt(0).toUpperCase() ?? '?');
  }

  fullName(booking: IntroductionBookingInfo): string {
    const name = `${booking.firstName ?? ''} ${booking.lastName ?? ''}`.trim();
    return name || (booking.email ?? 'Unbekannt');
  }

  async cancelBooking(slot: IntroductionSlot, booking: IntroductionBookingInfo): Promise<void> {
    const ok = await this.confirm.ask({
      title: 'Buchung stornieren',
      message: `Buchung von ${this.fullName(booking)} für den Termin am ${slot.date} wirklich stornieren?`,
      confirmLabel: 'Stornieren',
      tone: 'danger',
    });
    if (!ok) return;
    this.onboarding.cancelBooking(booking.bookingId).subscribe({
      next: () => {
        this.successMessage.set('Buchung storniert.');
        this.refresh();
      },
      error: (err) => this.errorMessage.set(err?.error || 'Stornieren fehlgeschlagen.'),
    });
  }

  readonly slotForm: SlotForm = this.fb.group({
    date: this.fb.nonNullable.control('', Validators.required),
    startTime: this.fb.nonNullable.control('', Validators.required),
    endTime: this.fb.nonNullable.control('', Validators.required),
    location: this.fb.nonNullable.control(''),
    capacity: this.fb.nonNullable.control(1, [Validators.required, Validators.min(1)]),
    notes: this.fb.nonNullable.control(''),
  });

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.onboarding.listAllSlots().subscribe({
      next: (list) => {
        this.slots.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err?.error || 'Termine konnten nicht geladen werden.');
        this.loading.set(false);
      },
    });
    this.loadPendingUsers();
    this.loadTestUsers();
  }

  private loadTestUsers(): void {
    this.testUsers.list().subscribe({
      next: (list) => this.testRetter.set(list),
      error: (err) => this.errorMessage.set(err?.error || 'Test-Retter konnten nicht geladen werden.'),
    });
  }

  createTestUser(): void {
    this.creatingTestUser.set(true);
    this.errorMessage.set(null);
    this.testUsers.create().subscribe({
      next: () => {
        this.creatingTestUser.set(false);
        this.successMessage.set('Test-Retter angelegt.');
        this.loadTestUsers();
        this.loadPendingUsers();
      },
      error: (err) => {
        this.creatingTestUser.set(false);
        this.errorMessage.set(err?.error || 'Anlegen fehlgeschlagen.');
      },
    });
  }

  impersonate(testUser: TestUser): void {
    this.impersonatingId.set(testUser.id);
    this.errorMessage.set(null);
    this.auth.impersonateTestUser(testUser.id).subscribe({
      next: () => {
        this.impersonatingId.set(null);
        this.router.navigateByUrl('/onboarding');
      },
      error: (err) => {
        this.impersonatingId.set(null);
        this.errorMessage.set(err?.error || 'Anmeldung als Test-Retter fehlgeschlagen.');
      },
    });
  }

  async deleteTestUser(testUser: TestUser): Promise<void> {
    const ok = await this.confirm.ask({
      title: 'Test-Retter löschen',
      message: `Test-Retter ${testUser.firstName} ${testUser.lastName} (${testUser.email}) wirklich löschen? Alle Buchungen und Daten werden entfernt.`,
      confirmLabel: 'Löschen',
      tone: 'danger',
    });
    if (!ok) return;
    this.testUsers.delete(testUser.id).subscribe({
      next: () => {
        this.successMessage.set('Test-Retter gelöscht.');
        this.loadTestUsers();
        this.loadPendingUsers();
        this.refresh();
      },
      error: (err) => this.errorMessage.set(err?.error || 'Löschen fehlgeschlagen.'),
    });
  }

  private loadPendingUsers(): void {
    this.users.list({ role: 'RETTER' }).subscribe({
      next: (all) => {
        const pending = all.filter((u) => u.status === 'PENDING');
        this.pendingUsers.set(pending.map((u) => ({ user: u, status: null })));
        pending.forEach((u) => {
          if (u.id == null) return;
          this.onboarding.getUserStatus(u.id).subscribe((s) => {
            this.pendingUsers.update((list) =>
              list.map((entry) => (entry.user.id === u.id ? { ...entry, status: s } : entry)),
            );
          });
        });
      },
    });
  }

  createSlot(): void {
    if (this.slotForm.invalid) {
      this.slotForm.markAllAsTouched();
      return;
    }
    const v = this.slotForm.getRawValue();
    this.creating.set(true);
    this.errorMessage.set(null);
    this.onboarding
      .createSlot({
        date: v.date,
        startTime: v.startTime,
        endTime: v.endTime,
        location: v.location || null,
        capacity: v.capacity,
        notes: v.notes || null,
      })
      .subscribe({
        next: () => {
          this.creating.set(false);
          this.successMessage.set('Termin angelegt.');
          this.slotForm.reset({
            date: '',
            startTime: '',
            endTime: '',
            location: '',
            capacity: 1,
            notes: '',
          });
          this.refresh();
        },
        error: (err) => {
          this.creating.set(false);
          this.errorMessage.set(err?.error || 'Anlegen fehlgeschlagen.');
        },
      });
  }

  async deleteSlot(slot: IntroductionSlot): Promise<void> {
    const ok = await this.confirm.ask({
      title: 'Termin löschen',
      message: `Termin am ${slot.date} (${slot.startTime}–${slot.endTime}) wirklich löschen?`,
      confirmLabel: 'Löschen',
      tone: 'danger',
    });
    if (!ok) return;
    this.onboarding.deleteSlot(slot.id).subscribe({
      next: () => this.refresh(),
      error: (err) => this.errorMessage.set(err?.error || 'Löschen fehlgeschlagen.'),
    });
  }

  confirmAttendance(userId: number, slotId: number): void {
    this.onboarding.confirmAttendance(slotId, userId).subscribe({
      next: () => {
        this.successMessage.set('Kennenlerngespräch bestätigt.');
        this.loadPendingUsers();
      },
      error: (err) => this.errorMessage.set(err?.error || 'Bestätigung fehlgeschlagen.'),
    });
  }

  toggleTestPickup(userId: number, completed: boolean): void {
    this.onboarding.setTestPickup(userId, completed).subscribe({
      next: () => {
        this.successMessage.set(completed ? 'Testabholung markiert.' : 'Testabholung zurückgesetzt.');
        this.loadPendingUsers();
      },
      error: (err) => this.errorMessage.set(err?.error || 'Aktualisierung fehlgeschlagen.'),
    });
  }
}
