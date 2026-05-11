import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { AuthService } from '../../auth/auth.service';
import {
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PartnerService } from '../partner.service';
import { PartnerCategoryRegistry } from '../partner-category-registry.service';
import {
  LocationPickResult,
  LocationPickerDialogComponent,
} from '../location-picker/location-picker-dialog';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog';
import { PartnerNotesSectionComponent } from '../../stores/notes/partner-notes-section/partner-notes-section.component';
import { PhotoUrlPipe } from '../../users/photo-url.pipe';
import {
  Partner,
  PickupSlot,
  STATUS_LABELS,
  STATUS_ORDER,
  Status,
  WEEKDAYS,
  Weekday,
  emptyPartner,
} from '../partner.model';

type SlotForm = FormGroup<{
  weekday: FormControl<Weekday>;
  startTime: FormControl<string>;
  endTime: FormControl<string>;
  active: FormControl<boolean>;
  capacity: FormControl<number>;
  expectedKg: FormControl<number | null>;
  availableMemberCount: FormControl<number | null>;
}>;

type PartnerForm = FormGroup<{
  name: FormControl<string>;
  categoryId: FormControl<number | null>;
  street: FormControl<string>;
  postalCode: FormControl<string>;
  city: FormControl<string>;
  logoUrl: FormControl<string>;
  contact: FormGroup<{
    name: FormControl<string>;
    email: FormControl<string>;
    phone: FormControl<string>;
  }>;
  pickupSlots: FormArray<SlotForm>;
  status: FormControl<Status>;
}>;

@Component({
  selector: 'app-partner-edit',
  imports: [ReactiveFormsModule, RouterLink, DecimalPipe, LocationPickerDialogComponent, ConfirmDialogComponent, PartnerNotesSectionComponent, PhotoUrlPipe],
  templateUrl: './partner-edit.html',
  styleUrl: './partner-edit.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PartnerEditComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(PartnerService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly categoryRegistry = inject(PartnerCategoryRegistry);

  readonly isAdmin = computed(() => !!this.auth.currentUser()?.roles?.includes('ADMINISTRATOR'));
  readonly partnerStatus = signal<Status | null>(null);
  readonly partnerName = signal<string>('');
  readonly isDeleted = computed(() => this.partnerStatus() === 'EXISTIERT_NICHT_MEHR');
  readonly deleteDialogOpen = signal(false);
  readonly deleting = signal(false);

  readonly weekdays = WEEKDAYS;
  readonly categories = this.categoryRegistry.categories;
  readonly statusLabels = STATUS_LABELS;
  readonly statuses = STATUS_ORDER;

  readonly partnerId = signal<number | null>(null);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly latitude = signal<number | null>(null);
  readonly longitude = signal<number | null>(null);
  readonly geocoding = signal(false);
  readonly geocodeMessage = signal<string | null>(null);
  readonly pickerOpen = signal(false);

  readonly uploadingLogo = signal(false);
  readonly logoUploadError = signal<string | null>(null);

  readonly form: PartnerForm = this.buildForm();

  constructor() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      this.partnerId.set(id);
      this.service.get(id).subscribe({
        next: (partner) => this.patchForm(partner),
        error: () => this.errorMessage.set('Betrieb konnte nicht geladen werden.'),
      });
    }
  }

  get slots(): FormArray<SlotForm> {
    return this.form.controls.pickupSlots;
  }

  get isEdit(): boolean {
    return this.partnerId() !== null;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const payload = this.toPartner();
    this.saving.set(true);
    this.errorMessage.set(null);
    const req$ = this.isEdit
      ? this.service.update(this.partnerId()!, payload)
      : this.service.create(payload);
    req$.subscribe({
      next: () => {
        this.saving.set(false);
        this.router.navigate(['/stores']);
      },
      error: (err) => {
        this.saving.set(false);
        if (err?.status === 409) {
          this.errorMessage.set(
            typeof err?.error === 'string' ? err.error : 'Abholzeit wird noch genutzt.',
          );
          return;
        }
        this.errorMessage.set(
          typeof err?.error === 'string' ? err.error : 'Speichern fehlgeschlagen.',
        );
      },
    });
  }

  private buildForm(): PartnerForm {
    const defaults = emptyPartner();
    return this.fb.group({
      name: this.fb.nonNullable.control(defaults.name, Validators.required),
      categoryId: this.fb.control<number | null>(defaults.categoryId, Validators.required),
      street: this.fb.nonNullable.control(defaults.street),
      postalCode: this.fb.nonNullable.control(defaults.postalCode),
      city: this.fb.nonNullable.control(defaults.city),
      logoUrl: this.fb.nonNullable.control(''),
      contact: this.fb.group({
        name: this.fb.nonNullable.control(''),
        email: this.fb.nonNullable.control('', Validators.email),
        phone: this.fb.nonNullable.control(''),
      }),
      pickupSlots: this.fb.array(defaults.pickupSlots.map((s) => this.slotGroup(s))),
      status: this.fb.nonNullable.control<Status>(defaults.status),
    });
  }

  private slotGroup(slot: PickupSlot): SlotForm {
    return this.fb.group({
      weekday: this.fb.nonNullable.control<Weekday>(slot.weekday),
      startTime: this.fb.nonNullable.control(slot.startTime),
      endTime: this.fb.nonNullable.control(slot.endTime),
      active: this.fb.nonNullable.control(slot.active),
      capacity: this.fb.nonNullable.control(slot.capacity ?? 1, [
        Validators.required,
        Validators.min(0),
      ]),
      expectedKg: this.fb.control<number | null>(slot.expectedKg ?? null, [Validators.min(0)]),
      availableMemberCount: this.fb.control<number | null>(slot.availableMemberCount ?? null),
    });
  }

  openPicker(): void {
    this.pickerOpen.set(true);
  }

  closePicker(): void {
    this.pickerOpen.set(false);
  }

  applyPick(pick: LocationPickResult): void {
    this.latitude.set(pick.latitude);
    this.longitude.set(pick.longitude);
    const patch: Partial<{ street: string; postalCode: string; city: string }> = {};
    if (pick.street != null) patch.street = pick.street;
    if (pick.postalCode != null) patch.postalCode = pick.postalCode;
    if (pick.city != null) patch.city = pick.city;
    if (Object.keys(patch).length > 0) {
      this.form.patchValue(patch);
    }
    this.geocodeMessage.set(null);
    this.pickerOpen.set(false);
  }

  regeocode(): void {
    const id = this.partnerId();
    if (!id) return;
    this.geocoding.set(true);
    this.geocodeMessage.set(null);
    this.service.regeocode(id).subscribe({
      next: (partner) => {
        this.geocoding.set(false);
        this.latitude.set(partner.latitude);
        this.longitude.set(partner.longitude);
        if (partner.latitude == null || partner.longitude == null) {
          this.geocodeMessage.set('Adresse konnte nicht gefunden werden.');
        }
      },
      error: () => {
        this.geocoding.set(false);
        this.geocodeMessage.set('Geocoding fehlgeschlagen.');
      },
    });
  }

  onLogoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.logoUploadError.set(null);

    if (!file.type.startsWith('image/')) {
      this.logoUploadError.set('Bitte eine Bilddatei auswählen.');
      input.value = '';
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.logoUploadError.set('Datei ist zu groß (max. 5 MB).');
      input.value = '';
      return;
    }

    const id = this.partnerId();
    if (!id) {
      this.logoUploadError.set('Bitte erst Betrieb speichern, dann Logo hochladen.');
      input.value = '';
      return;
    }

    this.uploadingLogo.set(true);
    this.service.uploadLogo(id, file).subscribe({
      next: (partner) => {
        this.uploadingLogo.set(false);
        this.form.controls.logoUrl.setValue(partner.logoUrl ?? '');
        this.form.controls.logoUrl.markAsDirty();
        input.value = '';
      },
      error: (err) => {
        this.uploadingLogo.set(false);
        input.value = '';
        this.logoUploadError.set(
          typeof err?.error === 'string' ? err.error : 'Upload fehlgeschlagen.',
        );
      },
    });
  }

  openDeleteDialog(): void {
    this.deleteDialogOpen.set(true);
  }

  closeDeleteDialog(): void {
    if (this.deleting()) return;
    this.deleteDialogOpen.set(false);
  }

  confirmDelete(): void {
    const id = this.partnerId();
    if (!id) return;
    this.errorMessage.set(null);
    this.deleting.set(true);
    this.service.delete(id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.deleteDialogOpen.set(false);
        this.router.navigate(['/stores']);
      },
      error: () => {
        this.deleting.set(false);
        this.deleteDialogOpen.set(false);
        this.errorMessage.set('Löschen fehlgeschlagen.');
      },
    });
  }

  restore(): void {
    const id = this.partnerId();
    if (!id) return;
    this.errorMessage.set(null);
    this.service.restore(id).subscribe({
      next: () => this.router.navigate(['/stores']),
      error: () => this.errorMessage.set('Wiederherstellen fehlgeschlagen.'),
    });
  }

  private patchForm(partner: Partner): void {
    this.latitude.set(partner.latitude);
    this.longitude.set(partner.longitude);
    this.partnerStatus.set(partner.status);
    this.partnerName.set(partner.name);
    this.form.patchValue({
      name: partner.name,
      categoryId: partner.categoryId,
      street: partner.street,
      postalCode: partner.postalCode,
      city: partner.city,
      logoUrl: partner.logoUrl ?? '',
      contact: partner.contact,
      status: partner.status,
    });
    this.slots.clear();
    for (const slot of partner.pickupSlots) {
      this.slots.push(this.slotGroup(slot));
    }
  }

  addSlot(): void {
    this.slots.push(
      this.slotGroup({
        weekday: 'MONDAY',
        startTime: '09:00',
        endTime: '10:00',
        active: true,
        capacity: 1,
        expectedKg: null,
      }),
    );
  }

  removeSlot(index: number): void {
    this.slots.removeAt(index);
  }

  compareNumbers(a: number | null, b: number | null): boolean {
    return a === b;
  }

  private toPartner(): Partner {
    const raw = this.form.getRawValue();
    return {
      id: this.partnerId(),
      name: raw.name,
      categoryId: raw.categoryId,
      street: raw.street,
      postalCode: raw.postalCode,
      city: raw.city,
      logoUrl: raw.logoUrl?.trim() ? raw.logoUrl.trim() : null,
      contact: raw.contact,
      pickupSlots: raw.pickupSlots.map((s) => ({
        weekday: s.weekday,
        startTime: s.startTime,
        endTime: s.endTime,
        active: s.active,
        capacity: s.capacity ?? 1,
        expectedKg: s.expectedKg != null && !Number.isNaN(s.expectedKg) ? s.expectedKg : null,
      })),
      status: raw.status,
      latitude: this.latitude(),
      longitude: this.longitude(),
    };
  }
}
