import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

type IconId = 'leaf' | 'hands' | 'heart' | 'group' | 'basket' | 'truck' | 'bulb';

@Component({
  selector: 'app-landing',
  imports: [RouterLink],
  templateUrl: './landing.html',
  styleUrl: './landing.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LandingComponent {
  readonly instagramUrl = 'https://www.instagram.com/tellerstatttonne_badvilbel/';
  readonly facebookUrl = 'https://www.facebook.com/';
  readonly currentYear = new Date().getFullYear();

  readonly ziele: { id: IconId; text: string }[] = [
    { id: 'leaf', text: 'Lebensmittel retten und weitergeben' },
    { id: 'hands', text: 'Für einen bewussteren Umgang mit Lebensmitteln sensibilisieren' },
    { id: 'heart', text: 'Wertschätzung für Lebensmittel stärken' },
    { id: 'group', text: 'Gemeinschaft fördern und Menschen zusammenbringen' },
  ];

  readonly taten: { id: IconId; text: string }[] = [
    { id: 'basket', text: 'Wir retten Lebensmittel vor der Tonne.' },
    { id: 'truck', text: 'Wir verteilen gerettete Lebensmittel weiter.' },
    { id: 'bulb', text: 'Wir klären über Lebensmittelverschwendung auf.' },
    { id: 'heart', text: 'Wir stärken Gemeinschaft und Zusammenhalt.' },
  ];

  readonly rollen: string[] = [
    'beim Abholen und Retten von Lebensmitteln',
    'bei der Verteilung',
    'bei Organisation, Aktionen oder Workshops',
    'bei Aufklärung & Social Media',
  ];
}
