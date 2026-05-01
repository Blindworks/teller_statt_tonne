import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CATEGORY_ICONS, CATEGORY_LABELS, Partner } from '../../partners/partner.model';
import { PartnerService } from '../../partners/partner.service';
import { UserService } from '../../users/user.service';
import { User } from '../../users/user.model';
import { StoreMember } from './store-member.model';
import { StoreMembersService } from './store-members.service';
import { AssignMemberDialogComponent } from './assign-member-dialog/assign-member-dialog';
import { PhotoUrlPipe } from '../../users/photo-url.pipe';
import { UserProfileDialogService } from '../../users/user-profile-dialog/user-profile-dialog.service';

@Component({
  selector: 'app-store-members',
  imports: [FormsModule, AssignMemberDialogComponent, PhotoUrlPipe],
  templateUrl: './store-members.html',
  styleUrl: './store-members.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StoreMembersComponent {
  private readonly partnerService = inject(PartnerService);
  private readonly userService = inject(UserService);
  private readonly storeMembersService = inject(StoreMembersService);
  private readonly profileDialog = inject(UserProfileDialogService);

  readonly stores = signal<Partner[]>([]);
  readonly memberCounts = signal<Record<number, number>>({});
  readonly selectedStoreId = signal<number | null>(null);
  readonly members = signal<StoreMember[]>([]);
  readonly filterTerm = signal('');
  readonly loadError = signal<string | null>(null);
  readonly allMembers = signal<User[]>([]);
  readonly assignDialogOpen = signal(false);

  readonly categoryIcons = CATEGORY_ICONS;
  readonly categoryLabels = CATEGORY_LABELS;

  readonly selectedStore = computed(() => {
    const id = this.selectedStoreId();
    return id ? this.stores().find((s) => s.id === id) ?? null : null;
  });

  readonly filteredMembers = computed(() => {
    const term = this.filterTerm().trim().toLowerCase();
    if (!term) return this.members();
    return this.members().filter((m) =>
      `${m.firstName} ${m.lastName}`.toLowerCase().includes(term),
    );
  });

  readonly assignableMembers = computed(() => {
    const assigned = new Set(this.members().map((m) => m.id));
    return this.allMembers().filter((m) => m.id && !assigned.has(m.id));
  });

  constructor() {
    this.loadStores();
    this.userService.list().subscribe({
      next: (list) => this.allMembers.set(list),
    });
  }

  selectStore(id: number): void {
    if (this.selectedStoreId() === id) return;
    this.selectedStoreId.set(id);
    this.filterTerm.set('');
    this.loadMembers(id);
  }

  onFilterChange(value: string): void {
    this.filterTerm.set(value);
  }

  openAssignDialog(): void {
    this.assignDialogOpen.set(true);
  }

  closeAssignDialog(): void {
    this.assignDialogOpen.set(false);
  }

  onAssignMember(memberId: number): void {
    const partnerId = this.selectedStoreId();
    if (!partnerId) return;
    this.storeMembersService.assign(partnerId, memberId).subscribe({
      next: () => {
        this.assignDialogOpen.set(false);
        this.loadMembers(partnerId);
        this.refreshCounts();
      },
      error: () => this.loadError.set('Mitglied konnte nicht zugewiesen werden.'),
    });
  }

  removeMember(memberId: number): void {
    const partnerId = this.selectedStoreId();
    if (!partnerId) return;
    this.storeMembersService.unassign(partnerId, memberId).subscribe({
      next: () => {
        this.loadMembers(partnerId);
        this.refreshCounts();
      },
      error: () => this.loadError.set('Mitglied konnte nicht entfernt werden.'),
    });
  }

  memberCount(storeId: number | null): number {
    if (!storeId) return 0;
    return this.memberCounts()[storeId] ?? 0;
  }

  openProfile(memberId: number): void {
    this.profileDialog.open(memberId);
  }

  initials(member: { firstName: string; lastName: string }): string {
    const f = member.firstName?.charAt(0) ?? '';
    const l = member.lastName?.charAt(0) ?? '';
    return (f + l).toUpperCase();
  }

  formatLastPickup(date: string | null): string {
    if (!date) return 'Noch keine Abholung';
    const d = new Date(date);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const target = new Date(d.getFullYear(), d.getMonth(), d.getDate());
    const diffDays = Math.round(
      (today.getTime() - target.getTime()) / (1000 * 60 * 60 * 24),
    );
    if (diffDays === 0) return 'Letzte Abholung: heute';
    if (diffDays === 1) return 'Letzte Abholung: gestern';
    if (diffDays > 1 && diffDays < 30) return `Letzte Abholung: vor ${diffDays} Tagen`;
    return `Letzte Abholung: ${d.toLocaleDateString('de-DE')}`;
  }

  onlineDotClass(status: StoreMember['onlineStatus']): string {
    switch (status) {
      case 'ONLINE':
        return 'bg-primary';
      case 'AWAY':
        return 'bg-tertiary';
      case 'ON_BREAK':
        return 'bg-error';
      case 'OFFLINE':
      default:
        return 'bg-on-surface-variant/30';
    }
  }

  private loadStores(): void {
    this.partnerService.list().subscribe({
      next: (list) => {
        this.stores.set(list);
        if (list.length > 0 && !this.selectedStoreId()) {
          this.selectStore(list[0].id!);
        }
      },
      error: () => this.loadError.set('Stores konnten nicht geladen werden.'),
    });
    this.refreshCounts();
  }

  private refreshCounts(): void {
    this.partnerService.memberCounts().subscribe({
      next: (counts) => this.memberCounts.set(counts),
    });
  }

  private loadMembers(partnerId: number): void {
    this.storeMembersService.list(partnerId).subscribe({
      next: (members) => {
        this.members.set(members);
        this.loadError.set(null);
      },
      error: () => this.loadError.set('Mitglieder konnten nicht geladen werden.'),
    });
  }
}
