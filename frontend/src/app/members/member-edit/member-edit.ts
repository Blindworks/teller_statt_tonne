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
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MemberService } from '../member.service';
import { MemberAvailabilityComponent } from '../member-availability/member-availability';
import {
  MEMBER_STATUSES,
  MEMBER_STATUS_LABELS,
  Member,
  MemberRole,
  MemberRoleOption,
  MemberStatus,
  ONLINE_STATUSES,
  ONLINE_STATUS_LABELS,
  OnlineStatus,
  emptyMember,
} from '../member.model';

type MemberForm = FormGroup<{
  firstName: FormControl<string>;
  lastName: FormControl<string>;
  role: FormControl<MemberRole>;
  email: FormControl<string>;
  phone: FormControl<string>;
  city: FormControl<string>;
  photoUrl: FormControl<string>;
  onlineStatus: FormControl<OnlineStatus>;
  status: FormControl<MemberStatus>;
  tags: FormArray<FormControl<string>>;
}>;

@Component({
  selector: 'app-member-edit',
  imports: [ReactiveFormsModule, RouterLink, MemberAvailabilityComponent],
  templateUrl: './member-edit.html',
  styleUrl: './member-edit.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MemberEditComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(MemberService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly roles = signal<MemberRoleOption[]>([]);
  readonly onlineStatuses = ONLINE_STATUSES;
  readonly onlineStatusLabels = ONLINE_STATUS_LABELS;
  readonly memberStatuses = MEMBER_STATUSES;
  readonly memberStatusLabels = MEMBER_STATUS_LABELS;

  readonly memberId = signal<number | null>(null);
  readonly saving = signal(false);
  readonly deleting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form: MemberForm = this.buildForm();

  constructor() {
    this.service
      .roles()
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (roles) => {
          this.roles.set(roles);
          if (!this.isEdit && roles.length > 0 && !this.form.controls.role.value) {
            this.form.controls.role.setValue(roles[0].value);
          }
        },
      });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      this.memberId.set(id);
      this.service.get(id).subscribe({
        next: (member) => this.patchForm(member),
        error: () => this.errorMessage.set('Mitglied konnte nicht geladen werden.'),
      });
    }
  }

  get tagsArray(): FormArray<FormControl<string>> {
    return this.form.controls.tags;
  }

  get isEdit(): boolean {
    return this.memberId() !== null;
  }

  addTag(): void {
    this.tagsArray.push(this.fb.nonNullable.control(''));
  }

  removeTag(index: number): void {
    this.tagsArray.removeAt(index);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const payload = this.toMember();
    this.saving.set(true);
    this.errorMessage.set(null);
    const req$ = this.isEdit
      ? this.service.update(this.memberId()!, payload)
      : this.service.create(payload);
    req$.subscribe({
      next: () => {
        this.saving.set(false);
        this.router.navigate(['/members']);
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
    const id = this.memberId();
    if (!id) return;
    if (!confirm('Mitglied wirklich löschen?')) return;
    this.deleting.set(true);
    this.service.delete(id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.router.navigate(['/members']);
      },
      error: () => {
        this.deleting.set(false);
        this.errorMessage.set('Löschen fehlgeschlagen.');
      },
    });
  }

  private buildForm(): MemberForm {
    const defaults = emptyMember('');
    return this.fb.group({
      firstName: this.fb.nonNullable.control(defaults.firstName, Validators.required),
      lastName: this.fb.nonNullable.control(defaults.lastName, Validators.required),
      role: this.fb.nonNullable.control<MemberRole>(defaults.role, Validators.required),
      email: this.fb.nonNullable.control(defaults.email, Validators.email),
      phone: this.fb.nonNullable.control(defaults.phone),
      city: this.fb.nonNullable.control(defaults.city),
      photoUrl: this.fb.nonNullable.control(''),
      onlineStatus: this.fb.nonNullable.control<OnlineStatus>(defaults.onlineStatus),
      status: this.fb.nonNullable.control<MemberStatus>(defaults.status),
      tags: this.fb.array<FormControl<string>>([]),
    });
  }

  private patchForm(member: Member): void {
    this.form.patchValue({
      firstName: member.firstName,
      lastName: member.lastName,
      role: member.role,
      email: member.email ?? '',
      phone: member.phone ?? '',
      city: member.city ?? '',
      photoUrl: member.photoUrl ?? '',
      onlineStatus: member.onlineStatus,
      status: member.status,
    });
    this.tagsArray.clear();
    for (const tag of member.tags ?? []) {
      this.tagsArray.push(this.fb.nonNullable.control(tag));
    }
  }

  private toMember(): Member {
    const raw = this.form.getRawValue();
    return {
      id: this.memberId(),
      firstName: raw.firstName,
      lastName: raw.lastName,
      role: raw.role,
      email: raw.email,
      phone: raw.phone,
      city: raw.city,
      photoUrl: raw.photoUrl?.trim() ? raw.photoUrl.trim() : null,
      onlineStatus: raw.onlineStatus,
      status: raw.status,
      tags: raw.tags.map((t) => t.trim()).filter((t) => t.length > 0),
    };
  }
}
