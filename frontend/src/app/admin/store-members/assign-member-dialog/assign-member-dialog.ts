import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
  computed,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Member } from '../../../members/member.model';
import { PhotoUrlPipe } from '../../../members/photo-url.pipe';

@Component({
  selector: 'app-assign-member-dialog',
  imports: [FormsModule, PhotoUrlPipe],
  templateUrl: './assign-member-dialog.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssignMemberDialogComponent {
  @Input({ required: true }) partnerName = '';
  @Input({ required: true }) set members(value: Member[]) {
    this._members.set(value);
  }
  @Output() assign = new EventEmitter<number>();
  @Output() close = new EventEmitter<void>();

  private readonly _members = signal<Member[]>([]);
  readonly search = signal('');

  readonly filtered = computed(() => {
    const term = this.search().trim().toLowerCase();
    const list = this._members();
    if (!term) return list;
    return list.filter((m) =>
      `${m.firstName} ${m.lastName}`.toLowerCase().includes(term),
    );
  });

  initials(member: Member): string {
    return ((member.firstName?.charAt(0) ?? '') + (member.lastName?.charAt(0) ?? '')).toUpperCase();
  }

  onPick(member: Member): void {
    if (member.id) this.assign.emit(member.id);
  }

  onBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.close.emit();
    }
  }
}
