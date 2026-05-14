import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { HygieneCertificateSectionComponent } from '../hygiene-certificate/hygiene-certificate-section/hygiene-certificate-section.component';
import { ConfirmDialogService } from '../shared/confirm-dialog/confirm-dialog.service';
import { OnboardingService } from './onboarding.service';
import { IntroductionSlot, OnboardingStatus } from './onboarding.models';

type ProfileForm = FormGroup<{
  phone: FormControl<string>;
  street: FormControl<string>;
  postalCode: FormControl<string>;
  city: FormControl<string>;
  country: FormControl<string>;
}>;

@Component({
  selector: 'app-onboarding-page',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule, HygieneCertificateSectionComponent],
  templateUrl: './onboarding-page.html',
  styleUrl: './onboarding-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OnboardingPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly onboarding = inject(OnboardingService);
  private readonly router = inject(Router);
  private readonly confirm = inject(ConfirmDialogService);

  readonly currentUser = this.auth.currentUser;
  readonly userId = computed(() => this.currentUser()?.id ?? null);
  readonly status = signal<OnboardingStatus | null>(null);
  readonly slots = signal<IntroductionSlot[]>([]);
  readonly loading = signal(true);
  readonly slotsLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly savingProfile = signal(false);
  readonly uploadingAgreement = signal(false);
  readonly bookingSlot = signal<number | null>(null);

  readonly profileForm: ProfileForm = this.fb.group({
    phone: this.fb.nonNullable.control('', Validators.required),
    street: this.fb.nonNullable.control('', Validators.required),
    postalCode: this.fb.nonNullable.control('', Validators.required),
    city: this.fb.nonNullable.control('', Validators.required),
    country: this.fb.nonNullable.control('Deutschland'),
  });

  readonly completedSteps = computed(() => {
    const s = this.status();
    if (!s) return 0;
    return (
      Number(s.hygieneCompleted) +
      Number(s.introductionCompleted) +
      Number(s.profileCompleted) +
      Number(s.agreementCompleted) +
      Number(s.testPickupCompleted)
    );
  });

  ngOnInit(): void {
    const user = this.currentUser();
    if (user) {
      this.profileForm.patchValue({
        phone: user.phone ?? '',
        street: user.street ?? '',
        postalCode: user.postalCode ?? '',
        city: user.city ?? '',
        country: user.country ?? 'Deutschland',
      });
    }
    this.reloadStatus();
    this.loadSlots();
  }

  reloadStatus(): void {
    this.loading.set(true);
    this.onboarding.getMyStatus().subscribe({
      next: (s) => {
        this.status.set(s);
        this.loading.set(false);
        if (s.activated) {
          this.auth.reloadCurrentUser().subscribe(() => this.router.navigateByUrl('/dashboard'));
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err?.error || 'Onboarding-Status konnte nicht geladen werden.');
      },
    });
  }

  loadSlots(): void {
    this.slotsLoading.set(true);
    this.onboarding.availableSlots().subscribe({
      next: (list) => {
        this.slots.set(list);
        this.slotsLoading.set(false);
      },
      error: () => {
        this.slotsLoading.set(false);
      },
    });
  }

  saveProfile(): void {
    const id = this.userId();
    if (id == null || this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }
    this.savingProfile.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    const v = this.profileForm.getRawValue();
    this.onboarding.updateSelfProfile(id, v).subscribe({
      next: () => {
        this.savingProfile.set(false);
        this.successMessage.set('Profildaten gespeichert.');
        this.auth.reloadCurrentUser().subscribe(() => this.reloadStatus());
      },
      error: (err) => {
        this.savingProfile.set(false);
        this.errorMessage.set(err?.error || 'Speichern fehlgeschlagen.');
      },
    });
  }

  bookSlot(slot: IntroductionSlot): void {
    if (this.bookingSlot() !== null) return;
    this.bookingSlot.set(slot.id);
    this.errorMessage.set(null);
    this.onboarding.bookSlot(slot.id).subscribe({
      next: () => {
        this.bookingSlot.set(null);
        this.successMessage.set('Termin gebucht.');
        this.loadSlots();
        this.reloadStatus();
      },
      error: (err) => {
        this.bookingSlot.set(null);
        this.errorMessage.set(err?.error || 'Buchung fehlgeschlagen.');
      },
    });
  }

  async cancelBooking(): Promise<void> {
    const s = this.status();
    if (!s?.introductionBookingId) return;
    const ok = await this.confirm.ask({
      title: 'Termin stornieren',
      message: 'Möchtest du den gebuchten Termin wirklich stornieren?',
      confirmLabel: 'Stornieren',
      tone: 'danger',
    });
    if (!ok) return;
    this.onboarding.cancelBooking(s.introductionBookingId).subscribe({
      next: () => {
        this.successMessage.set('Termin storniert.');
        this.loadSlots();
        this.reloadStatus();
      },
      error: (err) => this.errorMessage.set(err?.error || 'Stornierung fehlgeschlagen.'),
    });
  }

  onAgreementSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    const id = this.userId();
    if (id == null) return;
    if (file.size > 10 * 1024 * 1024) {
      this.errorMessage.set('Datei zu groß (max 10 MB).');
      return;
    }
    this.uploadingAgreement.set(true);
    this.errorMessage.set(null);
    this.onboarding.uploadAgreement(id, file).subscribe({
      next: (s) => {
        this.status.set(s);
        this.uploadingAgreement.set(false);
        this.successMessage.set('Rettervereinbarung hochgeladen.');
      },
      error: (err) => {
        this.uploadingAgreement.set(false);
        this.errorMessage.set(err?.error || 'Upload fehlgeschlagen.');
      },
    });
  }

  logout(): void {
    this.auth.logout().subscribe({
      complete: () => this.router.navigateByUrl('/login'),
    });
  }

  bookedSlot(): IntroductionSlot | null {
    const s = this.status();
    if (!s?.introductionSlotId) return null;
    return this.slots().find((x) => x.id === s.introductionSlotId) ?? null;
  }
}
