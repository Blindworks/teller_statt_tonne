import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PermissionsService } from '../../auth/permissions.service';

interface DashboardCard {
  title: string;
  description: string;
  icon: string;
  link: string;
}

@Component({
  selector: 'app-teamleiter-dashboard',
  imports: [RouterLink],
  templateUrl: './teamleiter-dashboard.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TeamleiterDashboardComponent {
  private readonly perms = inject(PermissionsService);

  readonly cards = computed<DashboardCard[]>(() => {
    const list: DashboardCard[] = [
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
    if (this.perms.features().has('nav.quiz-admin')) {
      list.push({
        title: 'Quiz',
        description: 'Quiz-Fragen, Kategorien und Versuche verwalten.',
        icon: 'quiz',
        link: '/admin/quiz/questions',
      });
    }
    return list;
  });
}
