import { Routes } from '@angular/router';
import { DashboardComponent } from './dashboard/dashboard';
import { StoresComponent } from './stores/stores';
import { AppShellComponent } from './shell/app-shell';

export const routes: Routes = [
  {
    path: '',
    component: AppShellComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'stores', component: StoresComponent },
      {
        path: 'stores/new',
        loadComponent: () =>
          import('./partners/partner-edit/partner-edit').then((m) => m.PartnerEditComponent),
      },
      {
        path: 'stores/edit/:id',
        loadComponent: () =>
          import('./partners/partner-edit/partner-edit').then((m) => m.PartnerEditComponent),
      },
      {
        path: 'pickups',
        loadComponent: () =>
          import('./pickups/pickups/pickups').then((m) => m.PickupsComponent),
      },
      {
        path: 'pickups/new',
        loadComponent: () =>
          import('./pickups/pickup-edit/pickup-edit').then((m) => m.PickupEditComponent),
      },
      {
        path: 'pickups/edit/:id',
        loadComponent: () =>
          import('./pickups/pickup-edit/pickup-edit').then((m) => m.PickupEditComponent),
      },
      {
        path: 'members',
        loadComponent: () =>
          import('./members/members-list/members-list').then((m) => m.MembersListComponent),
      },
      {
        path: 'members/new',
        loadComponent: () =>
          import('./members/member-edit/member-edit').then((m) => m.MemberEditComponent),
      },
      {
        path: 'members/edit/:id',
        loadComponent: () =>
          import('./members/member-edit/member-edit').then((m) => m.MemberEditComponent),
      },
    ],
  },
];
