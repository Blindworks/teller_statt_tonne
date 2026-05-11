import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PartnerCategoryService } from '../partner-category.service';
import {
  CURATED_ICONS,
  PartnerCategory,
  emptyPartnerCategory,
} from '../partner-category.model';
import { PartnerCategoryRegistry } from '../../../partners/partner-category-registry.service';

type CategoryForm = FormGroup<{
  code: FormControl<string>;
  label: FormControl<string>;
  icon: FormControl<string>;
  orderIndex: FormControl<number>;
  active: FormControl<boolean>;
}>;

@Component({
  selector: 'app-partner-category-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './partner-category-form.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PartnerCategoryFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(PartnerCategoryService);
  private readonly registry = inject(PartnerCategoryRegistry);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly itemId = signal<number | null>(null);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly isEdit = computed(() => this.itemId() !== null);

  readonly curatedIcons = CURATED_ICONS;

  readonly form: CategoryForm = this.buildForm(emptyPartnerCategory());

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      const numId = Number(id);
      this.itemId.set(numId);
      this.service.get(numId).subscribe({
        next: (item) => this.form.patchValue(item),
        error: () => this.errorMessage.set('Kategorie konnte nicht geladen werden.'),
      });
    }
  }

  private buildForm(defaults: PartnerCategory): CategoryForm {
    return this.fb.group({
      code: this.fb.nonNullable.control(defaults.code, [Validators.required, Validators.maxLength(64)]),
      label: this.fb.nonNullable.control(defaults.label, [Validators.required, Validators.maxLength(128)]),
      icon: this.fb.nonNullable.control(defaults.icon, [Validators.required, Validators.maxLength(64)]),
      orderIndex: this.fb.nonNullable.control(defaults.orderIndex),
      active: this.fb.nonNullable.control(defaults.active),
    });
  }

  pickIcon(icon: string): void {
    this.form.controls.icon.setValue(icon);
    this.form.controls.icon.markAsDirty();
  }

  isIconSelected(icon: string): boolean {
    return this.form.controls.icon.value === icon;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const payload: PartnerCategory = {
      id: this.itemId(),
      code: raw.code.trim().toUpperCase(),
      label: raw.label.trim(),
      icon: raw.icon.trim(),
      orderIndex: raw.orderIndex,
      active: raw.active,
    };

    this.saving.set(true);
    this.errorMessage.set(null);
    const req$ = this.isEdit()
      ? this.service.update(this.itemId()!, payload)
      : this.service.create(payload);
    req$.subscribe({
      next: () => {
        this.saving.set(false);
        this.registry.reload();
        this.router.navigate(['/admin/partner-categories']);
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
}
