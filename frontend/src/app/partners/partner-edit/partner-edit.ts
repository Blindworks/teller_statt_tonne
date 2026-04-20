import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { PartnerService } from '../partner.service';
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
  imports: [ReactiveFormsModule, RouterLink, RouterLinkActive],
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
    });
  }

  private patchForm(partner: Partner): void {
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
      pickupSlots: raw.pickupSlots,
      status: raw.status,
    };
  }
}
