import { Routes } from '@angular/router';
import { DashboardComponent } from './dashboard/dashboard';
import { StoresComponent } from './stores/stores';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'stores', component: StoresComponent },
];
