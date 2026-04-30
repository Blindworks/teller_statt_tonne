import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { UserAvailabilityService } from '../availability.service';
import { UserAvailability } from '../availability.model';
import { WEEKDAYS, Weekday } from '../../partners/partner.model';

interface SlotEntry {
  startTime: string;
  endTime: string;
}

@Component({
  selector: 'app-user-availability',
  imports: [FormsModule],
  templateUrl: './user-availability.html',
  styleUrl: './user-availability.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserAvailabilityComponent {
  private readonly service = inject(UserAvailabilityService);

  readonly userId = input.required<number>();

  readonly weekdays = WEEKDAYS;
  readonly slotsByDay = signal<Record<Weekday, SlotEntry[]>>(this.emptyState());
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  constructor() {
    queueMicrotask(() => this.load());
  }

  private load(): void {
    const id = this.userId();
    if (!id) return;
    this.service.list(id).subscribe({
      next: (items) => this.applyLoaded(items),
      error: () => this.errorMessage.set('Verfügbarkeiten konnten nicht geladen werden.'),
    });
  }

  private applyLoaded(items: UserAvailability[]): void {
    const next = this.emptyState();
    for (const a of items) {
      next[a.weekday].push({ startTime: a.startTime, endTime: a.endTime });
    }
    for (const day of Object.keys(next) as Weekday[]) {
      next[day].sort((a, b) => a.startTime.localeCompare(b.startTime));
    }
    this.slotsByDay.set(next);
  }

  addSlot(day: Weekday): void {
    const current = this.slotsByDay();
    this.slotsByDay.set({
      ...current,
      [day]: [...current[day], { startTime: '09:00', endTime: '12:00' }],
    });
  }

  removeSlot(day: Weekday, index: number): void {
    const current = this.slotsByDay();
    this.slotsByDay.set({
      ...current,
      [day]: current[day].filter((_, i) => i !== index),
    });
  }

  updateSlot(day: Weekday, index: number, field: 'startTime' | 'endTime', value: string): void {
    const current = this.slotsByDay();
    const updated = current[day].map((s, i) => (i === index ? { ...s, [field]: value } : s));
    this.slotsByDay.set({ ...current, [day]: updated });
  }

  save(): void {
    const id = this.userId();
    if (!id) return;
    const items = this.toRequestPayload(id);
    if (items === null) return;
    this.saving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.service.replaceAll(id, items).subscribe({
      next: (result) => {
        this.saving.set(false);
        this.applyLoaded(result);
        this.successMessage.set('Verfügbarkeiten gespeichert.');
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMessage.set(
          typeof err?.error === 'string' ? err.error : 'Speichern fehlgeschlagen.',
        );
      },
    });
  }

  private toRequestPayload(userId: number): UserAvailability[] | null {
    const result: UserAvailability[] = [];
    const slots = this.slotsByDay();
    for (const day of Object.keys(slots) as Weekday[]) {
      for (const s of slots[day]) {
        if (!s.startTime || !s.endTime || s.startTime >= s.endTime) {
          this.errorMessage.set(
            `Ungültiges Zeitfenster bei ${this.weekdays.find((w) => w.value === day)?.label}: Startzeit muss vor Endzeit liegen.`,
          );
          return null;
        }
        result.push({ id: null, userId, weekday: day, startTime: s.startTime, endTime: s.endTime });
      }
    }
    return result;
  }

  private emptyState(): Record<Weekday, SlotEntry[]> {
    return WEEKDAYS.reduce(
      (acc, w) => {
        acc[w.value] = [];
        return acc;
      },
      {} as Record<Weekday, SlotEntry[]>,
    );
  }
}
