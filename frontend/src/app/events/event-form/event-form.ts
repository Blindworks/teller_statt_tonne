import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EventService } from '../event.service';
import { CharityEvent } from '../event.model';

type EventForm = FormGroup<{
  name: FormControl<string>;
  description: FormControl<string>;
  startDate: FormControl<string>;
  endDate: FormControl<string>;
  street: FormControl<string>;
  postalCode: FormControl<string>;
  city: FormControl<string>;
  contactName: FormControl<string>;
  contactEmail: FormControl<string>;
  contactPhone: FormControl<string>;
}>;

@Component({
  selector: 'app-event-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './event-form.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EventFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(EventService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly itemId = signal<number | null>(null);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly logoUrl = signal<string | null>(null);
  readonly uploadingLogo = signal(false);
  readonly isEdit = computed(() => this.itemId() !== null);

  readonly form: EventForm = this.fb.group({
    name: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(255)]),
    description: this.fb.nonNullable.control(''),
    startDate: this.fb.nonNullable.control('', [Validators.required]),
    endDate: this.fb.nonNullable.control('', [Validators.required]),
    street: this.fb.nonNullable.control(''),
    postalCode: this.fb.nonNullable.control('', [Validators.maxLength(16)]),
    city: this.fb.nonNullable.control(''),
    contactName: this.fb.nonNullable.control(''),
    contactEmail: this.fb.nonNullable.control(''),
    contactPhone: this.fb.nonNullable.control(''),
  });

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'new') {
      const numId = Number(id);
      this.itemId.set(numId);
      this.service.get(numId).subscribe({
        next: (item) => this.patchForm(item),
        error: () => this.errorMessage.set('Veranstaltung konnte nicht geladen werden.'),
      });
    }
  }

  private patchForm(item: CharityEvent): void {
    this.form.patchValue({
      name: item.name,
      description: item.description ?? '',
      startDate: item.startDate,
      endDate: item.endDate,
      street: item.street ?? '',
      postalCode: item.postalCode ?? '',
      city: item.city ?? '',
      contactName: item.contact?.name ?? '',
      contactEmail: item.contact?.email ?? '',
      contactPhone: item.contact?.phone ?? '',
    });
    this.logoUrl.set(item.logoUrl);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    if (raw.endDate < raw.startDate) {
      this.errorMessage.set('Das Enddatum darf nicht vor dem Startdatum liegen.');
      return;
    }
    const payload: CharityEvent = {
      id: this.itemId(),
      name: raw.name.trim(),
      description: raw.description.trim() || null,
      startDate: raw.startDate,
      endDate: raw.endDate,
      street: raw.street.trim() || null,
      postalCode: raw.postalCode.trim() || null,
      city: raw.city.trim() || null,
      latitude: null,
      longitude: null,
      logoUrl: this.logoUrl(),
      contact: {
        name: raw.contactName.trim() || null,
        email: raw.contactEmail.trim() || null,
        phone: raw.contactPhone.trim() || null,
      },
    };

    this.saving.set(true);
    this.errorMessage.set(null);
    const obs = this.isEdit()
      ? this.service.update(this.itemId()!, payload)
      : this.service.create(payload);
    obs.subscribe({
      next: () => {
        this.saving.set(false);
        this.router.navigate(['/events']);
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

  onLogoFile(input: HTMLInputElement): void {
    const file = input.files?.[0];
    if (!file) return;
    const id = this.itemId();
    if (id == null) {
      this.errorMessage.set('Bitte zuerst die Veranstaltung speichern, dann ein Logo hochladen.');
      input.value = '';
      return;
    }
    this.uploadingLogo.set(true);
    this.service.uploadLogo(id, file).subscribe({
      next: (event) => {
        this.uploadingLogo.set(false);
        this.logoUrl.set(event.logoUrl);
        input.value = '';
      },
      error: () => {
        this.uploadingLogo.set(false);
        this.errorMessage.set('Logo-Upload fehlgeschlagen.');
        input.value = '';
      },
    });
  }
}
