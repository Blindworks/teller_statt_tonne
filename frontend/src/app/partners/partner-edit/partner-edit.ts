import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
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
import {
  LocationPickResult,
  LocationPickerDialogComponent,
} from '../location-picker/location-picker-dialog';
import {
  CATEGORY_LABELS,
  Category,
  Partner,
  PickupSlot,
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
  availableMemberCount: FormControl<number | null>;
}>;

type PartnerForm = FormGroup<{
  name: FormControl<string>;
  category: FormControl<Category>;
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
  imports: [ReactiveFormsModule, RouterLink, DecimalPipe, LocationPickerDialogComponent],
  templateUrl: './partner-edit.html',
  styleUrl: './partner-edit.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PartnerEditComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(PartnerService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly weekdays = WEEKDAYS;
  readonly categoryLabels = CATEGORY_LABELS;
  readonly categories: Category[] = ['BAKERY', 'SUPERMARKET', 'CAFE', 'RESTAURANT'];

  readonly partnerId = signal<string | null>(null);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly latitude = signal<number | null>(null);
  readonly longitude = signal<number | null>(null);
  readonly geocoding = signal(false);
  readonly geocodeMessage = signal<string | null>(null);
  readonly pickerOpen = signal(false);

  readonly form: PartnerForm = this.buildForm();

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.partnerId.set(id);
      this.service.get(id).subscribe({
        next: (partner) => this.patchForm(partner),
        error: () => this.errorMessage.set('Partner konnte nicht geladen werden.'),
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
      category: this.fb.nonNullable.control<Category>(defaults.category, Validators.required),
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

  private patchForm(partner: Partner): void {
    this.latitude.set(partner.latitude);
    this.longitude.set(partner.longitude);
    this.form.patchValue({
      name: partner.name,
      category: partner.category,
      street: partner.street,
      postalCode: partner.postalCode,
      city: partner.city,
      logoUrl: partner.logoUrl ?? '',
      contact: partner.contact,
      status: partner.status,
    });
    const byDay = new Map<Weekday, PickupSlot>(
      partner.pickupSlots.map((s) => [s.weekday, s]),
    );
    this.slots.controls.forEach((group) => {
      const weekday = group.controls.weekday.value;
      const loaded = byDay.get(weekday);
      if (loaded) {
        group.patchValue(loaded);
      }
    });
  }

  private toPartner(): Partner {
    const raw = this.form.getRawValue();
    return {
      id: this.partnerId(),
      name: raw.name,
      category: raw.category,
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
      })),
      status: raw.status,
      latitude: this.latitude(),
      longitude: this.longitude(),
    };
  }
}
