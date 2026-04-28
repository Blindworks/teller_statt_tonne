import { Routes } from '@angular/router';
import { DashboardComponent } from './dashboard/dashboard';
import { StoresComponent } from './stores/stores';
import { AppShellComponent } from './shell/app-shell';
import { authGuard } from './auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./auth/login/login').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () => import('./auth/register/register').then((m) => m.RegisterComponent),
  },
  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard],
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
        path: 'map',
        loadComponent: () => import('./map/map').then((m) => m.MapComponent),
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
