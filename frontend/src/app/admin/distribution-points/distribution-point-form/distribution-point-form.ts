import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import {
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DistributionPointService } from '../distribution-point.service';
import {
  DistributionPoint,
  OpeningSlot,
  OperatorRef,
  WEEKDAYS,
  WEEKDAY_LABELS,
  Weekday,
} from '../distribution-point.model';
import { UserService } from '../../../users/user.service';
import { User } from '../../../users/user.model';

type OpeningSlotForm = FormGroup<{
  weekday: FormControl<Weekday>;
  startTime: FormControl<string>;
  endTime: FormControl<string>;
}>;

type DistributionPointForm = FormGroup<{
  name: FormControl<string>;
  description: FormControl<string>;
  street: FormControl<string>;
  postalCode: FormControl<string>;
  city: FormControl<string>;
  latitude: FormControl<number | null>;
  longitude: FormControl<number | null>;
  operatorIds: FormControl<number[]>;
  openingSlots: FormArray<OpeningSlotForm>;
}>;

@Component({
  selector: 'app-distribution-point-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './distribution-point-form.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DistributionPointFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(DistributionPointService);
  private readonly userService = inject(UserService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly itemId = signal<number | null>(null);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly users = signal<User[]>([]);
  readonly geocoding = signal(false);
  readonly geocodeMessage = signal<string | null>(null);
  readonly isEdit = computed(() => this.itemId() !== null);

  readonly weekdays = WEEKDAYS;
  readonly weekdayLabels = WEEKDAY_LABELS;

  readonly form: DistributionPointForm = this.fb.group({
    name: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(255)]),
    description: this.fb.nonNullable.control(''),
    street: this.fb.nonNullable.control(''),
    postalCode: this.fb.nonNullable.control('', [Validators.maxLength(16)]),
    city: this.fb.nonNullable.control(''),
    latitude: this.fb.control<number | null>(null),
    longitude: this.fb.control<number | null>(null),
    operatorIds: this.fb.nonNullable.control<number[]>([]),
    openingSlots: this.fb.array<OpeningSlotForm>([]),
  });

  constructor() {
    this.userService.list({ activeOnly: false }).subscribe({
      next: (users) => this.users.set(users.filter((u) => u.id != null)),
      error: () => {
        // bewusst still — Liste bleibt leer; Speichern ohne Betreiber bleibt möglich
      },
    });

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      const numId = Number(id);
      this.itemId.set(numId);
      this.service.get(numId).subscribe({
        next: (item) => this.patchForm(item),
        error: () => this.errorMessage.set('Verteilerplatz konnte nicht geladen werden.'),
      });
    }
  }

  geolocate(): void {
    const raw = this.form.getRawValue();
    if (!raw.street?.trim() && !raw.postalCode?.trim() && !raw.city?.trim()) {
      this.geocodeMessage.set('Bitte zuerst eine Adresse eintragen.');
      return;
    }
    this.geocoding.set(true);
    this.geocodeMessage.set(null);
    this.service.forwardGeocode(raw.street, raw.postalCode, raw.city).subscribe({
      next: (coords) => {
        this.geocoding.set(false);
        if (coords && coords.lat != null && coords.lon != null) {
          this.form.patchValue({ latitude: coords.lat, longitude: coords.lon });
          this.geocodeMessage.set('Koordinaten übernommen.');
        } else {
          this.geocodeMessage.set('Adresse konnte nicht gefunden werden.');
        }
      },
      error: () => {
        this.geocoding.set(false);
        this.geocodeMessage.set('Geocoding fehlgeschlagen.');
      },
    });
  }

  get slots(): FormArray<OpeningSlotForm> {
    return this.form.controls.openingSlots;
  }

  addSlot(): void {
    this.slots.push(this.buildSlot({ weekday: 'MONDAY', startTime: '09:00', endTime: '12:00' }));
  }

  removeSlot(index: number): void {
    this.slots.removeAt(index);
  }

  toggleOperator(userId: number, checked: boolean): void {
    const current = this.form.controls.operatorIds.value;
    if (checked) {
      if (!current.includes(userId)) {
        this.form.controls.operatorIds.setValue([...current, userId]);
      }
    } else {
      this.form.controls.operatorIds.setValue(current.filter((id) => id !== userId));
    }
  }

  isOperatorSelected(userId: number): boolean {
    return this.form.controls.operatorIds.value.includes(userId);
  }

  userLabel(user: User): string {
    const name = `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim();
    return name.length ? `${name} (${user.email})` : user.email;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const operators: OperatorRef[] = raw.operatorIds.map((id) => ({ id, displayName: '' }));
    const openingSlots: OpeningSlot[] = raw.openingSlots.map((s) => ({
      weekday: s.weekday,
      startTime: s.startTime,
      endTime: s.endTime,
    }));
    const payload: DistributionPoint = {
      id: this.itemId(),
      name: raw.name.trim(),
      description: raw.description.trim() ? raw.description.trim() : null,
      street: raw.street.trim() ? raw.street.trim() : null,
      postalCode: raw.postalCode.trim() ? raw.postalCode.trim() : null,
      city: raw.city.trim() ? raw.city.trim() : null,
      latitude: raw.latitude,
      longitude: raw.longitude,
      operators,
      openingSlots,
    };

    this.saving.set(true);
    this.errorMessage.set(null);
    const req$ = this.isEdit()
      ? this.service.update(this.itemId()!, payload)
      : this.service.create(payload);
    req$.subscribe({
      next: () => {
        this.saving.set(false);
        this.router.navigate(['/admin/distribution-points']);
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMessage.set(
          typeof err?.error === 'string' && err.error.length > 0
            ? err.error
            : 'Speichern fehlgeschlagen.',
        );
      },
    });
  }

  private patchForm(item: DistributionPoint): void {
    this.form.patchValue({
      name: item.name,
      description: item.description ?? '',
      street: item.street ?? '',
      postalCode: item.postalCode ?? '',
      city: item.city ?? '',
      latitude: item.latitude,
      longitude: item.longitude,
      operatorIds: item.operators.map((o) => o.id),
    });
    this.slots.clear();
    for (const slot of item.openingSlots) {
      this.slots.push(this.buildSlot(slot));
    }
  }

  private buildSlot(slot: OpeningSlot): OpeningSlotForm {
    return this.fb.group({
      weekday: this.fb.nonNullable.control<Weekday>(slot.weekday, Validators.required),
      startTime: this.fb.nonNullable.control(slot.startTime ?? '', Validators.required),
      endTime: this.fb.nonNullable.control(slot.endTime ?? '', Validators.required),
    });
  }
}
