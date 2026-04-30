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
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '../../auth/auth.service';
import { AdminCreateUserRequest, UserService } from '../user.service';
import { UserAvailabilityComponent } from '../user-availability/user-availability';
import {
  ONLINE_STATUSES,
  ONLINE_STATUS_LABELS,
  OnlineStatus,
  Role,
  RoleOption,
  USER_STATUSES,
  USER_STATUS_LABELS,
  User,
  UserStatus,
  emptyUser,
} from '../user.model';

type UserForm = FormGroup<{
  firstName: FormControl<string>;
  lastName: FormControl<string>;
  role: FormControl<Role>;
  email: FormControl<string>;
  password: FormControl<string>;
  phone: FormControl<string>;
  city: FormControl<string>;
  photoUrl: FormControl<string>;
  onlineStatus: FormControl<OnlineStatus>;
  status: FormControl<UserStatus>;
  tags: FormArray<FormControl<string>>;
}>;

@Component({
  selector: 'app-user-edit',
  imports: [ReactiveFormsModule, RouterLink, UserAvailabilityComponent],
  templateUrl: './user-edit.html',
  styleUrl: './user-edit.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserEditComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(UserService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly roles = signal<RoleOption[]>([]);
  readonly onlineStatuses = ONLINE_STATUSES;
  readonly onlineStatusLabels = ONLINE_STATUS_LABELS;
  readonly userStatuses = USER_STATUSES;
  readonly userStatusLabels = USER_STATUS_LABELS;

  readonly userId = signal<number | null>(null);
  readonly saving = signal(false);
  readonly deleting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly isAdmin = computed(() => this.auth.currentUser()?.role === 'ADMINISTRATOR');

  readonly form: UserForm = this.buildForm();

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
      this.userId.set(id);
      this.form.controls.password.disable();
      this.service.get(id).subscribe({
        next: (user) => this.patchForm(user),
        error: () => this.errorMessage.set('Nutzer konnte nicht geladen werden.'),
      });
    } else {
      this.form.controls.password.setValidators([Validators.required, Validators.minLength(8)]);
      this.form.controls.password.updateValueAndValidity();

      const qp = this.route.snapshot.queryParamMap;
      const firstName = qp.get('firstName');
      const lastName = qp.get('lastName');
      const email = qp.get('email');
      if (firstName) this.form.controls.firstName.setValue(firstName);
      if (lastName) this.form.controls.lastName.setValue(lastName);
      if (email) this.form.controls.email.setValue(email);
    }
  }

  get tagsArray(): FormArray<FormControl<string>> {
    return this.form.controls.tags;
  }

  get isEdit(): boolean {
    return this.userId() !== null;
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
    this.saving.set(true);
    this.errorMessage.set(null);
    const req$ = this.isEdit
      ? this.service.update(this.userId()!, this.toUser())
      : this.service.create(this.toCreateRequest());
    req$.subscribe({
      next: () => {
        this.saving.set(false);
        this.router.navigate(['/users']);
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
    const id = this.userId();
    if (!id) return;
    if (!confirm('Nutzer wirklich löschen?')) return;
    this.deleting.set(true);
    this.service.delete(id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.router.navigate(['/users']);
      },
      error: () => {
        this.deleting.set(false);
        this.errorMessage.set('Löschen fehlgeschlagen.');
      },
    });
  }

  private buildForm(): UserForm {
    const defaults = emptyUser('RETTER');
    return this.fb.group({
      firstName: this.fb.nonNullable.control(defaults.firstName, Validators.required),
      lastName: this.fb.nonNullable.control(defaults.lastName, Validators.required),
      role: this.fb.nonNullable.control<Role>(defaults.role, Validators.required),
      email: this.fb.nonNullable.control(defaults.email, [Validators.required, Validators.email]),
      password: this.fb.nonNullable.control(''),
      phone: this.fb.nonNullable.control(defaults.phone ?? ''),
      city: this.fb.nonNullable.control(defaults.city ?? ''),
      photoUrl: this.fb.nonNullable.control(''),
      onlineStatus: this.fb.nonNullable.control<OnlineStatus>(defaults.onlineStatus),
      status: this.fb.nonNullable.control<UserStatus>(defaults.status),
      tags: this.fb.array<FormControl<string>>([]),
    });
  }

  private patchForm(user: User): void {
    this.form.patchValue({
      firstName: user.firstName,
      lastName: user.lastName,
      role: user.role,
      email: user.email ?? '',
      phone: user.phone ?? '',
      city: user.city ?? '',
      photoUrl: user.photoUrl ?? '',
      onlineStatus: user.onlineStatus,
      status: user.status,
    });
    this.tagsArray.clear();
    for (const tag of user.tags ?? []) {
      this.tagsArray.push(this.fb.nonNullable.control(tag));
    }
  }

  private toUser(): User {
    const raw = this.form.getRawValue();
    return {
      id: this.userId(),
      firstName: raw.firstName,
      lastName: raw.lastName,
      role: raw.role,
      email: raw.email,
      phone: raw.phone || null,
      city: raw.city || null,
      photoUrl: raw.photoUrl?.trim() ? raw.photoUrl.trim() : null,
      onlineStatus: raw.onlineStatus,
      status: raw.status,
      tags: raw.tags.map((t) => t.trim()).filter((t) => t.length > 0),
    };
  }

  private toCreateRequest(): AdminCreateUserRequest {
    const raw = this.form.getRawValue();
    return {
      email: raw.email,
      password: raw.password,
      firstName: raw.firstName,
      lastName: raw.lastName,
      role: raw.role,
      phone: raw.phone || null,
      city: raw.city || null,
    };
  }
}
