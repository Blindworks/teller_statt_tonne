import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { UserService } from '../users/user.service';
import { User } from '../users/user.model';
import { resolvePhotoUrl } from '../users/photo-url';

type ProfileForm = FormGroup<{
  firstName: FormControl<string>;
  lastName: FormControl<string>;
  phone: FormControl<string>;
  city: FormControl<string>;
  tags: FormArray<FormControl<string>>;
}>;

type PasswordForm = FormGroup<{
  oldPassword: FormControl<string>;
  newPassword: FormControl<string>;
  newPasswordRepeat: FormControl<string>;
}>;

@Component({
  selector: 'app-profile',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly userService = inject(UserService);

  readonly currentUser = this.auth.currentUser;
  readonly hasProfile = computed(() => !!this.currentUser());

  readonly photoPreview = signal<string | null>(null);
  readonly displayPhoto = computed(() => {
    const preview = this.photoPreview();
    if (preview) return preview;
    return resolvePhotoUrl(this.currentUser()?.photoUrl ?? null);
  });
  readonly initials = computed(() => {
    const u = this.currentUser();
    const f = u?.firstName?.[0] ?? '';
    const l = u?.lastName?.[0] ?? '';
    const i = (f + l).trim().toUpperCase();
    if (i) return i;
    const email = u?.email ?? '';
    return email.slice(0, 1).toUpperCase() || '?';
  });

  readonly savingProfile = signal(false);
  readonly uploadingPhoto = signal(false);
  readonly changingPassword = signal(false);
  readonly profileMessage = signal<string | null>(null);
  readonly profileError = signal<string | null>(null);
  readonly photoError = signal<string | null>(null);
  readonly passwordMessage = signal<string | null>(null);
  readonly passwordError = signal<string | null>(null);

  readonly form: ProfileForm = this.fb.group({
    firstName: this.fb.nonNullable.control('', Validators.required),
    lastName: this.fb.nonNullable.control('', Validators.required),
    phone: this.fb.nonNullable.control(''),
    city: this.fb.nonNullable.control(''),
    tags: this.fb.array<FormControl<string>>([]),
  });

  readonly passwordForm: PasswordForm = this.fb.group(
    {
      oldPassword: this.fb.nonNullable.control('', Validators.required),
      newPassword: this.fb.nonNullable.control('', [Validators.required, Validators.minLength(8)]),
      newPasswordRepeat: this.fb.nonNullable.control('', Validators.required),
    },
    { validators: passwordsMatchValidator },
  );

  constructor() {
    effect(() => {
      const u = this.currentUser();
      if (u) this.patchForm(u);
    });
  }

  get tagsArray(): FormArray<FormControl<string>> {
    return this.form.controls.tags;
  }

  addTag(): void {
    this.tagsArray.push(this.fb.nonNullable.control(''));
  }

  removeTag(index: number): void {
    this.tagsArray.removeAt(index);
  }

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.photoError.set(null);

    if (!file.type.startsWith('image/')) {
      this.photoError.set('Bitte eine Bilddatei auswählen.');
      input.value = '';
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.photoError.set('Datei ist zu groß (max. 5 MB).');
      input.value = '';
      return;
    }

    const user = this.currentUser();
    if (!user?.id) {
      this.photoError.set('Kein verknüpftes Profil gefunden.');
      input.value = '';
      return;
    }

    const previewUrl = URL.createObjectURL(file);
    this.photoPreview.set(previewUrl);
    this.uploadingPhoto.set(true);
    this.userService.uploadPhoto(user.id, file).subscribe({
      next: (updated) => {
        this.uploadingPhoto.set(false);
        this.auth.setCurrentUser(updated);
        URL.revokeObjectURL(previewUrl);
        this.photoPreview.set(null);
        input.value = '';
      },
      error: (err) => {
        this.uploadingPhoto.set(false);
        URL.revokeObjectURL(previewUrl);
        this.photoPreview.set(null);
        input.value = '';
        this.photoError.set(
          typeof err?.error === 'string' ? err.error : 'Upload fehlgeschlagen.',
        );
      },
    });
  }

  submitProfile(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const user = this.currentUser();
    if (!user?.id) return;
    const raw = this.form.getRawValue();
    const payload: User = {
      ...user,
      firstName: raw.firstName,
      lastName: raw.lastName,
      phone: raw.phone || null,
      city: raw.city || null,
      tags: raw.tags.map((t) => t.trim()).filter((t) => t.length > 0),
    };
    this.savingProfile.set(true);
    this.profileError.set(null);
    this.profileMessage.set(null);
    this.userService.update(user.id, payload).subscribe({
      next: (updated) => {
        this.savingProfile.set(false);
        this.auth.setCurrentUser(updated);
        this.profileMessage.set('Profil gespeichert.');
      },
      error: (err) => {
        this.savingProfile.set(false);
        this.profileError.set(
          typeof err?.error === 'string' ? err.error : 'Speichern fehlgeschlagen.',
        );
      },
    });
  }

  submitPassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }
    const { oldPassword, newPassword } = this.passwordForm.getRawValue();
    this.changingPassword.set(true);
    this.passwordError.set(null);
    this.passwordMessage.set(null);
    this.auth.changePassword(oldPassword, newPassword).subscribe({
      next: () => {
        this.changingPassword.set(false);
        this.passwordMessage.set('Passwort geändert.');
        this.passwordForm.reset({ oldPassword: '', newPassword: '', newPasswordRepeat: '' });
      },
      error: (err) => {
        this.changingPassword.set(false);
        const msg =
          err?.status === 401
            ? 'Aktuelles Passwort ist falsch.'
            : typeof err?.error === 'string'
              ? err.error
              : 'Passwort konnte nicht geändert werden.';
        this.passwordError.set(msg);
      },
    });
  }

  private patchForm(user: User): void {
    this.form.patchValue({
      firstName: user.firstName,
      lastName: user.lastName,
      phone: user.phone ?? '',
      city: user.city ?? '',
    });
    this.tagsArray.clear();
    for (const tag of user.tags ?? []) {
      this.tagsArray.push(this.fb.nonNullable.control(tag));
    }
  }
}

function passwordsMatchValidator(group: AbstractControl): ValidationErrors | null {
  const newPw = group.get('newPassword')?.value;
  const repeat = group.get('newPasswordRepeat')?.value;
  if (newPw && repeat && newPw !== repeat) {
    return { passwordsMismatch: true };
  }
  return null;
}
