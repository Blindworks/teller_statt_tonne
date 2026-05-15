import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';
import { AppointmentService } from '../appointment.service';
import { AppointmentInput, emptyAppointmentInput } from '../appointment.model';
import { fromLocalInputValue, toLocalInputValue } from '../format';
import { RoleService } from '../../admin/roles/role.service';
import { Role } from '../../admin/roles/role.model';

@Component({
  selector: 'app-appointment-form',
  imports: [FormsModule, RouterLink],
  templateUrl: './appointment-form.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppointmentFormComponent {
  private readonly service = inject(AppointmentService);
  private readonly roleService = inject(RoleService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly id = signal<number | null>(null);
  readonly model = signal<AppointmentInput>(emptyAppointmentInput());
  readonly startLocal = signal('');
  readonly endLocal = signal('');
  readonly roles = signal<Role[]>([]);
  readonly selectedRoleIds = signal<Set<number>>(new Set<number>());
  readonly busy = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly saveError = signal<string | null>(null);

  constructor() {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idParam ? Number(idParam) : null;
    this.id.set(id && Number.isFinite(id) ? id : null);

    if (this.id() !== null) {
      forkJoin({
        roles: this.roleService.list(false),
        appointment: this.service.get(this.id() as number),
      })
        .pipe(takeUntilDestroyed())
        .subscribe({
          next: ({ roles, appointment }) => {
            this.roles.set(roles.filter((r) => r.enabled));
            this.model.set({
              title: appointment.title,
              description: appointment.description ?? '',
              startTime: appointment.startTime,
              endTime: appointment.endTime,
              location: appointment.location ?? '',
              attachmentUrl: appointment.attachmentUrl ?? '',
              isPublic: appointment.isPublic,
              targetRoleIds: appointment.targetRoles.map((r) => r.id),
            });
            this.startLocal.set(toLocalInputValue(appointment.startTime));
            this.endLocal.set(toLocalInputValue(appointment.endTime));
            this.selectedRoleIds.set(new Set(appointment.targetRoles.map((r) => r.id)));
          },
          error: () => this.loadError.set('Termin konnte nicht geladen werden.'),
        });
    } else {
      this.roleService
        .list(false)
        .pipe(takeUntilDestroyed())
        .subscribe({
          next: (roles) => this.roles.set(roles.filter((r) => r.enabled)),
          error: () => this.loadError.set('Rollen konnten nicht geladen werden.'),
        });
    }
  }

  toggleRole(roleId: number, checked: boolean): void {
    const next = new Set(this.selectedRoleIds());
    if (checked) next.add(roleId);
    else next.delete(roleId);
    this.selectedRoleIds.set(next);
  }

  isRoleSelected(roleId: number): boolean {
    return this.selectedRoleIds().has(roleId);
  }

  updateField<K extends keyof AppointmentInput>(key: K, value: AppointmentInput[K]): void {
    this.model.update((m) => ({ ...m, [key]: value }));
  }

  save(): void {
    this.saveError.set(null);
    const m = this.model();
    const start = fromLocalInputValue(this.startLocal());
    const end = fromLocalInputValue(this.endLocal());
    if (!m.title.trim()) {
      this.saveError.set('Titel ist erforderlich.');
      return;
    }
    if (!start || !end) {
      this.saveError.set('Start- und Endzeit sind erforderlich.');
      return;
    }
    if (new Date(end).getTime() < new Date(start).getTime()) {
      this.saveError.set('Ende darf nicht vor Beginn liegen.');
      return;
    }
    const selected = Array.from(this.selectedRoleIds());
    if (selected.length === 0 && !m.isPublic) {
      this.saveError.set('Bitte mindestens eine Ziel-Rolle wählen oder "Öffentlich" aktivieren.');
      return;
    }
    const payload: AppointmentInput = {
      title: m.title.trim(),
      description: (m.description ?? '').trim() || null,
      startTime: start,
      endTime: end,
      location: (m.location ?? '').trim() || null,
      attachmentUrl: (m.attachmentUrl ?? '').trim() || null,
      isPublic: m.isPublic,
      targetRoleIds: selected,
    };

    this.busy.set(true);
    const obs = this.id() === null
      ? this.service.create(payload)
      : this.service.update(this.id() as number, payload);
    obs.subscribe({
      next: (saved) => {
        this.busy.set(false);
        this.router.navigate(['/termine', saved.id]);
      },
      error: (err) => {
        this.busy.set(false);
        this.saveError.set(
          typeof err?.error === 'string' && err.error.length > 0
            ? err.error
            : 'Speichern fehlgeschlagen.',
        );
      },
    });
  }
}
