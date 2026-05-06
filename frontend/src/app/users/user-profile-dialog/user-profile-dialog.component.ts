import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { ONLINE_STATUS_LABELS, User } from '../user.model';
import { UserService } from '../user.service';
import { PhotoUrlPipe } from '../photo-url.pipe';
import { UserProfileDialogService } from './user-profile-dialog.service';

@Component({
  selector: 'app-user-profile-dialog',
  imports: [PhotoUrlPipe],
  templateUrl: './user-profile-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserProfileDialogComponent {
  private readonly dialogService = inject(UserProfileDialogService);
  private readonly userService = inject(UserService);

  readonly userId = this.dialogService.userId;
  readonly user = signal<User | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly fullName = computed(() => {
    const u = this.user();
    if (!u) return '';
    return `${u.firstName ?? ''} ${u.lastName ?? ''}`.trim();
  });

  readonly initials = computed(() => {
    const u = this.user();
    if (!u) return '';
    return ((u.firstName?.charAt(0) ?? '') + (u.lastName?.charAt(0) ?? '')).toUpperCase();
  });

  readonly onlineLabel = computed(() => {
    const u = this.user();
    return u ? ONLINE_STATUS_LABELS[u.onlineStatus] : '';
  });

  constructor() {
    effect(() => {
      const id = this.userId();
      if (id == null) {
        this.user.set(null);
        this.error.set(null);
        this.loading.set(false);
        return;
      }
      this.loading.set(true);
      this.error.set(null);
      this.user.set(null);
      this.userService.get(id).subscribe({
        next: (u) => {
          this.user.set(u);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Profil konnte nicht geladen werden.');
          this.loading.set(false);
        },
      });
    });
  }

  close(): void {
    this.dialogService.close();
  }

  onBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.close();
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.userId() != null) this.close();
  }

  onlineDotClass(status: User['onlineStatus'] | undefined): string {
    switch (status) {
      case 'ONLINE':
        return 'bg-primary animate-pulse';
      case 'AWAY':
        return 'bg-tertiary';
      case 'ON_BREAK':
        return 'bg-error';
      default:
        return 'bg-on-surface-variant/30';
    }
  }

  roleBadgeClass(role: string | null | undefined): string {
    switch (role) {
      case 'ADMINISTRATOR':
        return 'bg-error-container text-on-error-container';
      case 'BOTSCHAFTER':
        return 'bg-tertiary-container text-on-tertiary-fixed';
      case 'RETTER':
        return 'bg-primary-container text-on-primary-container';
      case 'NEW_MEMBER':
        return 'bg-secondary-container text-on-secondary-container';
      default:
        return 'bg-surface-container text-on-surface';
    }
  }
}
