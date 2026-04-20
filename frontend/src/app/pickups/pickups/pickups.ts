import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PickupService } from '../pickup.service';
import { Pickup } from '../pickup.model';
import { PickupCardComponent } from '../pickup-card/pickup-card';

interface DayColumn {
  date: Date;
  iso: string;
  shortLabel: string;
  dayOfMonth: number;
  isToday: boolean;
  isSunday: boolean;
  pickups: Pickup[];
}

type ViewMode = 'WEEK' | 'LIST' | 'MAP';

@Component({
  selector: 'app-pickups',
  imports: [RouterLink, PickupCardComponent],
  templateUrl: './pickups.html',
  styleUrl: './pickups.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PickupsComponent {
  private readonly service = inject(PickupService);

  readonly loadError = signal<string | null>(null);
  readonly pickups = signal<Pickup[]>([]);
  readonly recent = signal<Pickup[]>([]);
  readonly view = signal<ViewMode>('WEEK');
  readonly availableOnly = signal(false);

  readonly weekStart: Date;
  readonly weekEnd: Date;

  readonly days = computed<DayColumn[]>(() => {
    const filter = this.availableOnly();
    const byDate = new Map<string, Pickup[]>();
    for (const p of this.pickups()) {
      if (filter && p.capacity - p.assignments.length <= 0) continue;
      if (filter && (p.status === 'COMPLETED' || p.status === 'CANCELLED')) continue;
      const list = byDate.get(p.date) ?? [];
      list.push(p);
      byDate.set(p.date, list);
    }
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const result: DayColumn[] = [];
    for (let i = 0; i < 7; i++) {
      const d = new Date(this.weekStart);
      d.setDate(this.weekStart.getDate() + i);
      const iso = toIsoDate(d);
      result.push({
        date: d,
        iso,
        shortLabel: SHORT_WEEKDAYS[i],
        dayOfMonth: d.getDate(),
        isToday: d.getTime() === today.getTime(),
        isSunday: i === 6,
        pickups: byDate.get(iso) ?? [],
      });
    }
    return result;
  });

  readonly openSlotCount = computed(() =>
    this.pickups()
      .filter((p) => p.status === 'SCHEDULED')
      .reduce((sum, p) => sum + Math.max(0, p.capacity - p.assignments.length), 0),
  );

  readonly kgSaved = computed(
    () => this.pickups().filter((p) => p.status === 'COMPLETED').length * 6,
  );

  readonly nextPickup = computed<Pickup | null>(() => {
    const now = new Date();
    const upcoming = this.pickups()
      .filter((p) => p.status === 'SCHEDULED')
      .filter((p) => {
        const dt = new Date(`${p.date}T${p.startTime}:00`);
        return dt.getTime() >= now.getTime();
      })
      .sort((a, b) => (a.date + a.startTime).localeCompare(b.date + b.startTime));
    return upcoming[0] ?? null;
  });

  readonly nextPickupLabel = computed(() => {
    const p = this.nextPickup();
    if (!p) return '—';
    return isSameDay(new Date(p.date), new Date()) ? 'Heute' : formatDateShort(p.date);
  });

  readonly avatarStack: readonly string[] = [
    'https://lh3.googleusercontent.com/aida-public/AB6AXuBtVKsL1FOcM0VUpnEYlRsDNzbghZ7J5TarPYwWw3E_4knompPJvy3wjg1pbLmytQB0I57RVWrEugMxt4mznlC16rJiaQzXwbmNlecY7-GPFPE2pGttNI1vsyGz00VbEjs9Wr2iOkDHG6s8_duCzMeVP6xQtpWpMTRURlbGG9xjusYcKyJxEJJxHlr1qyIHg_gGqm7NiTMbWcGZe3vVJZkUtzDo3-pNhTYEyjKgNIzqJ4V6GKiBUA-2lGgVNzBd4B_FaNGmOKKx8Wor',
    'https://lh3.googleusercontent.com/aida-public/AB6AXuA4htIOEO0hu3LjesxL7RONkEAtjlSdOsdwGhnZY45QjRq_FZloTT8HJtmJhaxFf2NCzatYirb9vQYNTcEEIpISZgSkkm-Xv2Yz9dPmJ0wxp3fRM6fVenJJaIzgHd2Iekm7fixkUlUnEI5j3wqBnci47y9X3HwRCU-1iVIho08leIHBxQHp6hmXeN0dSJyvUkXDfrlT7EG7Y_9a1q_UOfo7gQKXT9eh7qIfw_EkR1iTiaJGwR4dYSKNI-_gpK692zkuKls12gyq9QIY',
    'https://lh3.googleusercontent.com/aida-public/AB6AXuBMKyo5nTrSPEcHrsWQSPzh6IlJJx6hiwx2DnxC0qMnefm0psbaq4GYVs4tneVBmZOzb1_B3PP4TnZViuXaKQ2YcenhnNlz4Iq78FSlUXKcDk-4TESPkSNt4CO3su7TBX1SvarNfW34DfV0vkYYwhkEbrlhUl28JTvoMY2BpQIjCEaEKt0sqb0v0j2zciQik2XnK3CxDKar9wJZI9ajUindkeb-dS6w8a6kcL-bbaOiFU4I4XKdHugYME65ylkldcoOB1k6xmhDJXvS',
  ];

  constructor() {
    const today = new Date();
    this.weekStart = startOfIsoWeek(today);
    this.weekEnd = new Date(this.weekStart);
    this.weekEnd.setDate(this.weekStart.getDate() + 6);

    this.service.list(toIsoDate(this.weekStart), toIsoDate(this.weekEnd)).subscribe({
      next: (list) => this.pickups.set(list),
      error: () => this.loadError.set('Abholungen konnten nicht geladen werden.'),
    });

    this.service.recent().subscribe({
      next: (list) => this.recent.set(list),
      error: () => {},
    });
  }

  toggleAvailable(): void {
    this.availableOnly.update((v) => !v);
  }

  setView(view: ViewMode): void {
    this.view.set(view);
  }

  logStatusLabel(p: Pickup): string {
    switch (p.status) {
      case 'COMPLETED':
        return 'VERIFIED';
      case 'CANCELLED':
        return 'OPEN';
      default:
        return 'SCHEDULED';
    }
  }

  logActionLabel(p: Pickup): string {
    switch (p.status) {
      case 'COMPLETED':
        return 'completed a pickup at';
      case 'CANCELLED':
        return 'cancelled their slot at';
      default:
        return 'scheduled a pickup at';
    }
  }

  logMemberName(p: Pickup): string {
    return p.assignments[0]?.memberName ?? 'Unbekannt';
  }

  logAvatar(p: Pickup): string | null {
    return p.assignments[0]?.memberAvatarUrl ?? null;
  }
}

const SHORT_WEEKDAYS = ['Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa', 'So'];

function startOfIsoWeek(date: Date): Date {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  const day = d.getDay();
  const diff = (day + 6) % 7;
  d.setDate(d.getDate() - diff);
  return d;
}

function toIsoDate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function isSameDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

function formatDateShort(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleDateString('de-DE', { day: '2-digit', month: 'short' });
}
