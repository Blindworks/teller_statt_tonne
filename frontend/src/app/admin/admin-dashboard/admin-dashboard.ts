import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-admin-dashboard',
  imports: [RouterLink],
  templateUrl: './admin-dashboard.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminDashboardComponent {
  readonly cards = [
    {
      title: 'Rollen',
      description: 'Rollen anlegen, bearbeiten oder deaktivieren.',
      icon: 'badge',
      link: '/admin/roles',
    },
    {
      title: 'Betrieb-Kategorien',
      description: 'Kategorien für Betriebe pflegen (Bäckerei, Supermarkt, …) — Icons, Reihenfolge, aktiv/inaktiv.',
      icon: 'category',
      link: '/admin/partner-categories',
    },
    {
      title: 'Lebensmittel-Kategorien',
      description:
        'Master-Liste der Lebensmittel für die Schnellerfassung im Abhol-Wizard (Brot, Obst, …) — Emoji, Farbe, Reihenfolge.',
      icon: 'restaurant',
      link: '/admin/food-categories',
    },
    {
      title: 'Verteilerplätze',
      description:
        'Orte verwalten, an denen gerettete Lebensmittel an Endkund:innen weitergegeben werden (Teller-Treff).',
      icon: 'storefront',
      link: '/admin/distribution-points',
    },
    {
      title: 'Systemlog',
      description:
        'Audit-Log aller systemrelevanten Aktionen: Logins, Passwort-Resets, Admin-Aktionen, Fehler.',
      icon: 'receipt_long',
      link: '/admin/system-log',
    },
  ];
}
