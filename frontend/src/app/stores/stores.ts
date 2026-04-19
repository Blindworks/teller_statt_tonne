import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

interface Store {
  name: string;
  icon: 'bakery_dining' | 'shopping_basket' | 'restaurant' | 'local_mall';
  address: string;
  status: 'active' | 'inactive';
  managerName: string;
  memberInitials: string[];
}

interface FeaturedStore {
  name: string;
  badge: string;
  description: string;
  managerRole: string;
  managerName: string;
  managerImage: string;
}

@Component({
  selector: 'app-stores',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './stores.html',
  styleUrl: './stores.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StoresComponent {
  readonly totalLocations = 124;
  readonly city = 'Berlin';

  readonly featured: FeaturedStore = {
    name: 'Bio-Markt Mitte',
    badge: 'Top Partner',
    description:
      'Saving over 450kg of organic produce monthly through consistent daily pickups.',
    managerRole: 'Manager',
    managerName: 'Sarah Jürgens',
    managerImage:
      'https://lh3.googleusercontent.com/aida-public/AB6AXuADKkDISV_QpEt0CugdweVFJePPQ7-E0lbWW6dKnCrgyIfpY5mO6jCp5fmwWSWdDlUceQ0w1cG3xSswmYGgzMRlfXLj_CqXKfe3q4-DySAVnY5AvaWmp_fOuf5E3mVHVU244M8e_MrzEd7Skt5W8I8_0eDMwjAAO9bx8nEsAoF_zq4CQbxiGS_JhuPrGmLOVKZalNWHcBcwBgpMwpFR95ojqfPWSyOOX9gZwE-Hmnqv90KyJUGA7k3KIB0fLY4LlB_q0RJWLQDMMyai',
  };

  readonly stores: Store[] = [
    {
      name: 'Bäckerei Sonne',
      icon: 'bakery_dining',
      address: 'Prenzlauer Allee 42',
      status: 'active',
      managerName: 'Lukas Meier',
      memberInitials: ['M', 'K'],
    },
    {
      name: 'Edeka am Kiez',
      icon: 'shopping_basket',
      address: 'Torstraße 120',
      status: 'inactive',
      managerName: 'Hanna Weber',
      memberInitials: ['H'],
    },
    {
      name: 'Café Luise',
      icon: 'restaurant',
      address: 'Kastanienallee 15',
      status: 'active',
      managerName: 'Markus Lang',
      memberInitials: ['M'],
    },
    {
      name: 'Früchte Paradies',
      icon: 'local_mall',
      address: 'Karl-Marx-Str. 88',
      status: 'active',
      managerName: 'Anja Bauer',
      memberInitials: ['A'],
    },
  ];

  readonly mapImage =
    'https://lh3.googleusercontent.com/aida-public/AB6AXuAMqXeSyG_8qK9d_1xvLbIwbKbGx8Kt_G4BsEqUYmOLyqDSpDWCLGu_6ZSf3Q3TIL3T2aaJmaiHgC4CsYRjsQKc4iDg4_RK-1T4sK8xlbqP7mCNr3DbPNyl1-5JBg_qWehzQQCIG9W39xhnXjy3IiCVrmqUzwooBQzG1f_jPzqrOOLJ4BfOMn565V3PsrDMdU36MN4OWaxMsCL5UBhl72rrQT78UIqzIY6KgAPyZkHRzEOUNHWlmIQHTs1_O6QOgsvLm0LtQjy3cQYR';
}
