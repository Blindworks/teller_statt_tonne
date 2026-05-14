import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PermissionsService } from '../../auth/permissions.service';

interface AdminCard {
  title: string;
  description: string;
  icon: string;
  link: string;
  featureKey: string;
}

@Component({
  selector: 'app-admin-dashboard',
  imports: [RouterLink],
  templateUrl: './admin-dashboard.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminDashboardComponent {
  private readonly perms = inject(PermissionsService);

  private readonly allCards: AdminCard[] = [
    {
      title: 'Rollen',
      description: 'Rollen anlegen, bearbeiten oder deaktivieren.',
      icon: 'badge',
      link: '/admin/roles',
      featureKey: 'route.admin',
    },
    {
      title: 'Berechtigungen',
      description:
        'Pro Rolle festlegen, welche Menüpunkte, Routen und Aktionen in der GUI sichtbar sind.',
      icon: 'lock_person',
      link: '/admin/permissions',
      featureKey: 'route.admin.permissions',
    },
    {
      title: 'Betrieb-Kategorien',
      description:
        'Kategorien für Betriebe pflegen (Bäckerei, Supermarkt, …) — Icons, Reihenfolge, aktiv/inaktiv.',
      icon: 'category',
      link: '/admin/partner-categories',
      featureKey: 'route.admin',
    },
    {
      title: 'Lebensmittel-Kategorien',
      description:
        'Master-Liste der Lebensmittel für die Schnellerfassung im Abhol-Wizard (Brot, Obst, …) — Emoji, Farbe, Reihenfolge.',
      icon: 'restaurant',
      link: '/admin/food-categories',
      featureKey: 'route.admin',
    },
    {
      title: 'Verteilerplätze',
      description:
        'Orte verwalten, an denen gerettete Lebensmittel an Endkund:innen weitergegeben werden (Teller-Treff).',
      icon: 'storefront',
      link: '/admin/distribution-points',
      featureKey: 'route.admin',
    },
    {
      title: 'Systemlog',
      description:
        'Audit-Log aller systemrelevanten Aktionen: Logins, Passwort-Resets, Admin-Aktionen, Fehler.',
      icon: 'receipt_long',
      link: '/admin/system-log',
      featureKey: 'route.admin',
    },
    {
      title: 'Papierkorb · Betriebe',
      description:
        'Geschlossene Betriebe einsehen und bei Bedarf wiederherstellen (Start im Status »Kein Kontakt«).',
      icon: 'delete',
      link: '/admin/stores/deleted',
      featureKey: 'route.admin',
    },
  ];

  readonly cards = computed(() => this.allCards.filter((c) => this.perms.has(c.featureKey)));
}
