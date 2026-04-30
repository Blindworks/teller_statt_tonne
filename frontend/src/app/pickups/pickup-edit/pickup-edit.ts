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
import { MemberService } from '../../members/member.service';
import { Member } from '../../members/member.model';
import { PartnerService } from '../../partners/partner.service';
import { Partner } from '../../partners/partner.model';
import { PickupService } from '../pickup.service';
import { Pickup, PickupStatus, emptyPickup } from '../pickup.model';

type AssignmentForm = FormGroup<{
  memberId: FormControl<number>;
}>;

type PickupForm = FormGroup<{
  partnerId: FormControl<number | null>;
  date: FormControl<string>;
  startTime: FormControl<string>;
  endTime: FormControl<string>;
  status: FormControl<PickupStatus>;
  capacity: FormControl<number>;
  notes: FormControl<string>;
  assignments: FormArray<AssignmentForm>;
}>;

@Component({
  selector: 'app-pickup-edit',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './pickup-edit.html',
  styleUrl: './pickup-edit.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PickupEditComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(PickupService);
  private readonly partnerService = inject(PartnerService);
  private readonly memberService = inject(MemberService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly pickupId = signal<number | null>(null);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly partners = signal<Partner[]>([]);
  readonly members = signal<Member[]>([]);

  readonly statuses: PickupStatus[] = ['SCHEDULED', 'COMPLETED', 'CANCELLED'];

  readonly form: PickupForm = this.buildForm();

  constructor() {
    this.partnerService.list().subscribe({
      next: (list) => this.partners.set(list),
      error: () => {},
    });
    this.memberService.list({}).subscribe({
      next: (list) => this.members.set(list),
      error: () => {},
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      this.pickupId.set(id);
      this.service.get(id).subscribe({
        next: (p) => this.patchForm(p),
        error: () => this.errorMessage.set('Abholung konnte nicht geladen werden.'),
      });
    }
  }

  get assignments(): FormArray<AssignmentForm> {
    return this.form.controls.assignments;
  }

  get isEdit(): boolean {
    return this.pickupId() !== null;
  }

  addAssignment(): void {
    this.assignments.push(
      this.fb.group({
        memberId: this.fb.nonNullable.control<number>(0, Validators.required),
      }),
    );
  }

  removeAssignment(index: number): void {
    this.assignments.removeAt(index);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.errorMessage.set(null);
    const payload = this.toPickup();
    const req$ = this.isEdit
      ? this.service.update(this.pickupId()!, payload)
      : this.service.create(payload);
    req$.subscribe({
      next: () => {
        this.saving.set(false);
        this.router.navigate(['/pickups']);
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMessage.set(
          typeof err?.error === 'string' ? err.error : 'Speichern fehlgeschlagen.',
        );
      },
    });
  }

  delete(): void {
    const id = this.pickupId();
    if (!id) return;
    this.service.delete(id).subscribe({
      next: () => this.router.navigate(['/pickups']),
      error: () => this.errorMessage.set('Löschen fehlgeschlagen.'),
    });
  }

  private buildForm(): PickupForm {
    const defaults = emptyPickup();
    return this.fb.group({
      partnerId: this.fb.control<number | null>(defaults.partnerId, Validators.required),
      date: this.fb.nonNullable.control(defaults.date, Validators.required),
      startTime: this.fb.nonNullable.control(defaults.startTime, Validators.required),
      endTime: this.fb.nonNullable.control(defaults.endTime, Validators.required),
      status: this.fb.nonNullable.control<PickupStatus>(defaults.status),
      capacity: this.fb.nonNullable.control(defaults.capacity, [
        Validators.required,
        Validators.min(1),
      ]),
      notes: this.fb.nonNullable.control(defaults.notes ?? ''),
      assignments: this.fb.array<AssignmentForm>([]),
    }) as PickupForm;
  }

  private patchForm(p: Pickup): void {
    this.form.patchValue({
      partnerId: p.partnerId,
      date: p.date,
      startTime: p.startTime,
      endTime: p.endTime,
      status: p.status,
      capacity: p.capacity,
      notes: p.notes ?? '',
    });
    this.assignments.clear();
    for (const a of p.assignments) {
      this.assignments.push(
        this.fb.group({
          memberId: this.fb.nonNullable.control<number>(a.memberId, Validators.required),
        }),
      );
    }
  }

  private toPickup(): Pickup {
    const raw = this.form.getRawValue();
    return {
      id: this.pickupId(),
      partnerId: raw.partnerId,
      partnerName: null,
      partnerCategory: null,
      partnerStreet: null,
      partnerCity: null,
      partnerLogoUrl: null,
      date: raw.date,
      startTime: raw.startTime,
      endTime: raw.endTime,
      status: raw.status,
      capacity: raw.capacity,
      notes: raw.notes?.trim() ? raw.notes.trim() : null,
      assignments: raw.assignments.map((a) => ({
        memberId: a.memberId,
        memberName: null,
        memberAvatarUrl: null,
      })),
    };
  }
}
