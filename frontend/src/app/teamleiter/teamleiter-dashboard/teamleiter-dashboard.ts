import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-teamleiter-dashboard',
  imports: [RouterLink],
  templateUrl: './teamleiter-dashboard.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TeamleiterDashboardComponent {
  readonly cards = [
    {
      title: 'Bewerbungen',
      description: 'Bewerbungen von Retter:innen auf Betriebe annehmen oder ablehnen.',
      icon: 'how_to_reg',
      link: '/teamleitung/applications',
    },
    {
      title: 'Onboarding',
      description:
        'Kennenlern-Termine verwalten, Teilnahme bestätigen und Testabholungen abhaken.',
      icon: 'task_alt',
      link: '/teamleitung/onboarding',
    },
    {
      title: 'Hygienezertifikate',
      description: 'Eingereichte Hygienezertifikate prüfen und Retter freischalten.',
      icon: 'verified_user',
      link: '/teamleitung/zertifikate',
    },
  ];
}
