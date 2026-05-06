import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { RoleService } from '../role.service';
import { Role } from '../role.model';

type RoleForm = FormGroup<{
  name: FormControl<string>;
  label: FormControl<string>;
  description: FormControl<string>;
  sortOrder: FormControl<number>;
  enabled: FormControl<boolean>;
}>;

@Component({
  selector: 'app-role-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './role-form.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RoleFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(RoleService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly roleId = signal<number | null>(null);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly isEdit = computed(() => this.roleId() !== null);

  readonly form: RoleForm = this.fb.group({
    name: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.maxLength(64),
      Validators.pattern(/^[A-Z][A-Z0-9_]*$/),
    ]),
    label: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(128)]),
    description: this.fb.nonNullable.control(''),
    sortOrder: this.fb.nonNullable.control(100),
    enabled: this.fb.nonNullable.control(true),
  });

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      const numId = Number(id);
      this.roleId.set(numId);
      this.service.get(numId).subscribe({
        next: (role) => this.patchForm(role),
        error: () => this.errorMessage.set('Rolle konnte nicht geladen werden.'),
      });
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const payload = {
      name: raw.name.trim(),
      label: raw.label.trim(),
      description: raw.description.trim() ? raw.description.trim() : null,
      sortOrder: raw.sortOrder,
      enabled: raw.enabled,
    };
    this.saving.set(true);
    this.errorMessage.set(null);
    const req$ = this.isEdit()
      ? this.service.update(this.roleId()!, payload)
      : this.service.create(payload);
    req$.subscribe({
      next: () => {
        this.saving.set(false);
        this.router.navigate(['/admin/roles']);
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

  private patchForm(role: Role): void {
    this.form.patchValue({
      name: role.name,
      label: role.label,
      description: role.description ?? '',
      sortOrder: role.sortOrder,
      enabled: role.enabled,
    });
  }
}
