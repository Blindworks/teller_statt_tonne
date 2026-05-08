import { DatePipe } from '@angular/common';
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
import { ConfirmDialogService } from '../../shared/confirm-dialog/confirm-dialog.service';
import { AdminCreateUserRequest, UserService } from '../user.service';
import { UserAvailabilityComponent } from '../user-availability/user-availability';
import {
  ONLINE_STATUSES,
  ONLINE_STATUS_LABELS,
  OnlineStatus,
  RoleName,
  RoleOption,
  USER_STATUS_LABELS,
  User,
  UserStatus,
  emptyUser,
} from '../user.model';

type UserForm = FormGroup<{
  firstName: FormControl<string>;
  lastName: FormControl<string>;
  role: FormControl<RoleName>;
  email: FormControl<string>;
  password: FormControl<string>;
  phone: FormControl<string>;
  street: FormControl<string>;
  postalCode: FormControl<string>;
  city: FormControl<string>;
  country: FormControl<string>;
  photoUrl: FormControl<string>;
  onlineStatus: FormControl<OnlineStatus>;
  tags: FormArray<FormControl<string>>;
}>;

@Component({
  selector: 'app-user-edit',
  imports: [DatePipe, ReactiveFormsModule, RouterLink, UserAvailabilityComponent],
  templateUrl: './user-edit.html',
  styleUrl: './user-edit.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserEditComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(UserService);
  private readonly auth = inject(AuthService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly roles = signal<RoleOption[]>([]);
  readonly onlineStatuses = ONLINE_STATUSES;
  readonly onlineStatusLabels = ONLINE_STATUS_LABELS;
  readonly userStatusLabels = USER_STATUS_LABELS;
  readonly lifecycleSteps: Array<{ status: UserStatus }> = [
    { status: 'PENDING' },
    { status: 'ACTIVE' },
    { status: 'PAUSED' },
    { status: 'LEFT' },
    { status: 'REMOVED' },
  ];

  readonly userId = signal<number | null>(null);
  readonly saving = signal(false);
  readonly deleting = signal(false);
  readonly transitioning = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly currentUser = signal<User | null>(null);

  readonly isAdmin = computed(() => !!this.auth.currentUser()?.roles?.includes('ADMINISTRATOR'));
  readonly isSelf = computed(() => this.auth.currentUser()?.id === this.userId());

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
        next: (user) => {
          this.currentUser.set(user);
          this.patchForm(user);
        },
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

  statusBadgeClass(status: UserStatus): string {
    switch (status) {
      case 'ACTIVE':
        return 'bg-primary-container text-on-primary-container';
      case 'PENDING':
        return 'bg-surface-container-high text-on-surface-variant';
      case 'PAUSED':
        return 'bg-tertiary-container text-on-tertiary-container';
      case 'LEFT':
      case 'REMOVED':
        return 'bg-error-container text-on-error-container';
    }
  }

  async confirmIntroduction(): Promise<void> {
    const id = this.userId();
    if (!id) return;
    const ok = await this.confirmDialog.ask({
      title: 'Einführungsgespräch bestätigen',
      message: 'Hat der Nutzer am Einführungsgespräch teilgenommen?',
      confirmLabel: 'Bestätigen',
    });
    if (!ok) return;
    this.runTransition(this.service.markIntroductionCompleted(id), 'Einführung konnte nicht bestätigt werden.');
  }

  async pauseUser(): Promise<void> {
    const id = this.userId();
    if (!id) return;
    const ok = await this.confirmDialog.ask({
      title: 'Nutzer pausieren',
      message: 'Nutzer pausieren? Er nimmt dann keine Pickups mehr wahr, kann aber später reaktiviert werden.',
      confirmLabel: 'Pausieren',
    });
    if (!ok) return;
    this.runTransition(this.service.pause(id), 'Pausieren fehlgeschlagen.');
  }

  async reactivateUser(): Promise<void> {
    const id = this.userId();
    if (!id) return;
    this.runTransition(this.service.reactivate(id), 'Reaktivieren fehlgeschlagen.');
  }

  async leaveUser(): Promise<void> {
    const id = this.userId();
    if (!id) return;
    const ok = await this.confirmDialog.ask({
      title: 'Austritt bestätigen',
      message: 'Den Nutzer als ausgetreten markieren? Dieser Schritt ist endgültig.',
      confirmLabel: 'Austritt',
      tone: 'danger',
    });
    if (!ok) return;
    this.runTransition(this.service.leave(id), 'Austritt fehlgeschlagen.');
  }

  async removeUser(): Promise<void> {
    const id = this.userId();
    if (!id) return;
    const ok = await this.confirmDialog.ask({
      title: 'Nutzer entfernen',
      message: 'Den Nutzer aus dem Team entfernen? Dieser Schritt ist endgültig.',
      confirmLabel: 'Entfernen',
      tone: 'danger',
    });
    if (!ok) return;
    this.runTransition(this.service.remove(id), 'Entfernen fehlgeschlagen.');
  }

  private runTransition(req$: ReturnType<UserService['pause']>, errorMsg: string): void {
    this.transitioning.set(true);
    this.errorMessage.set(null);
    req$.subscribe({
      next: (user) => {
        this.transitioning.set(false);
        this.currentUser.set(user);
        this.patchForm(user);
      },
      error: (err) => {
        this.transitioning.set(false);
        this.errorMessage.set(typeof err?.error === 'string' && err.error ? err.error : errorMsg);
      },
    });
  }

  async delete(): Promise<void> {
    const id = this.userId();
    if (!id) return;
    const ok = await this.confirmDialog.ask({
      title: 'Nutzer löschen',
      message: 'Nutzer wirklich löschen? Die Aktion kann nicht rückgängig gemacht werden.',
      confirmLabel: 'Löschen',
      tone: 'danger',
    });
    if (!ok) return;
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
      role: this.fb.nonNullable.control<RoleName>(defaults.roles[0] ?? '', Validators.required),
      email: this.fb.nonNullable.control(defaults.email, [Validators.required, Validators.email]),
      password: this.fb.nonNullable.control(''),
      phone: this.fb.nonNullable.control(defaults.phone ?? ''),
      street: this.fb.nonNullable.control(defaults.street ?? ''),
      postalCode: this.fb.nonNullable.control(defaults.postalCode ?? ''),
      city: this.fb.nonNullable.control(defaults.city ?? ''),
      country: this.fb.nonNullable.control(defaults.country ?? ''),
      photoUrl: this.fb.nonNullable.control(''),
      onlineStatus: this.fb.nonNullable.control<OnlineStatus>(defaults.onlineStatus),
      tags: this.fb.array<FormControl<string>>([]),
    });
  }

  private patchForm(user: User): void {
    this.form.patchValue({
      firstName: user.firstName,
      lastName: user.lastName,
      role: user.roles[0] ?? '',
      email: user.email ?? '',
      phone: user.phone ?? '',
      street: user.street ?? '',
      postalCode: user.postalCode ?? '',
      city: user.city ?? '',
      country: user.country ?? '',
      photoUrl: user.photoUrl ?? '',
      onlineStatus: user.onlineStatus,
    });
    this.tagsArray.clear();
    for (const tag of user.tags ?? []) {
      this.tagsArray.push(this.fb.nonNullable.control(tag));
    }
  }

  private toUser(): User {
    const raw = this.form.getRawValue();
    const existing = this.currentUser();
    return {
      id: this.userId(),
      firstName: raw.firstName,
      lastName: raw.lastName,
      roles: [raw.role],
      email: raw.email,
      phone: raw.phone || null,
      street: raw.street || null,
      postalCode: raw.postalCode || null,
      city: raw.city || null,
      country: raw.country || null,
      photoUrl: raw.photoUrl?.trim() ? raw.photoUrl.trim() : null,
      onlineStatus: raw.onlineStatus,
      status: existing?.status ?? 'PENDING',
      introductionCompletedAt: existing?.introductionCompletedAt ?? null,
      hygieneApproved: existing?.hygieneApproved ?? false,
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
      roleNames: [raw.role],
      phone: raw.phone || null,
      street: raw.street || null,
      postalCode: raw.postalCode || null,
      city: raw.city || null,
      country: raw.country || null,
    };
  }
}
