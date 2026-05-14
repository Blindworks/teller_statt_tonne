import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Feature, FeatureRequest } from '../feature.model';

@Component({
  selector: 'app-feature-form',
  imports: [FormsModule],
  templateUrl: './feature-form.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeatureFormComponent {
  readonly key = signal<string>('');
  readonly label = signal<string>('');
  readonly description = signal<string>('');
  readonly category = signal<string>('');
  readonly sortOrder = signal<number>(0);
  readonly editing = signal<boolean>(false);

  @Input() busy = false;

  @Input() set feature(value: Feature | null) {
    if (value) {
      this.editing.set(true);
      this.key.set(value.key);
      this.label.set(value.label);
      this.description.set(value.description ?? '');
      this.category.set(value.category ?? '');
      this.sortOrder.set(value.sortOrder);
    } else {
      this.editing.set(false);
      this.key.set('');
      this.label.set('');
      this.description.set('');
      this.category.set('');
      this.sortOrder.set(0);
    }
  }

  @Output() submitForm = new EventEmitter<FeatureRequest>();
  @Output() cancel = new EventEmitter<void>();

  onSubmit(event: Event): void {
    event.preventDefault();
    const payload: FeatureRequest = {
      key: this.key().trim(),
      label: this.label().trim(),
      description: this.description().trim() || null,
      category: this.category().trim() || null,
      sortOrder: this.sortOrder(),
    };
    this.submitForm.emit(payload);
  }

  onCancel(): void {
    this.cancel.emit();
  }
}
