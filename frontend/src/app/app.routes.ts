import { Routes } from '@angular/router';
import { DashboardComponent } from './dashboard/dashboard';
import { StoresComponent } from './stores/stores';
import { AppShellComponent } from './shell/app-shell';
import { authGuard } from './auth/auth.guard';
import { roleGuard } from './auth/role.guard';
import { landingGuard } from './landing/landing.guard';

const plannerRoles = roleGuard(['ADMINISTRATOR', 'BOTSCHAFTER']);

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    canActivate: [landingGuard],
    loadComponent: () => import('./landing/landing').then((m) => m.LandingComponent),
  },
  {
    path: 'login',
    loadComponent: () => import('./auth/login/login').then((m) => m.LoginComponent),
  },
  {
    path: 'quiz',
    loadComponent: () => import('./quiz/quiz').then((m) => m.QuizComponent),
  },
  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
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
        canActivate: [plannerRoles],
        loadComponent: () =>
          import('./pickups/pickups/pickups').then((m) => m.PickupsComponent),
      },
      {
        path: 'pickups/new',
        canActivate: [plannerRoles],
        loadComponent: () =>
          import('./pickups/pickup-edit/pickup-edit').then((m) => m.PickupEditComponent),
      },
      {
        path: 'pickups/edit/:id',
        canActivate: [plannerRoles],
        loadComponent: () =>
          import('./pickups/pickup-edit/pickup-edit').then((m) => m.PickupEditComponent),
      },
      {
        path: 'users',
        loadComponent: () =>
          import('./users/users-list/users-list').then((m) => m.UsersListComponent),
      },
      {
        path: 'users/new',
        loadComponent: () =>
          import('./users/user-edit/user-edit').then((m) => m.UserEditComponent),
      },
      {
        path: 'users/edit/:id',
        loadComponent: () =>
          import('./users/user-edit/user-edit').then((m) => m.UserEditComponent),
      },
      {
        path: 'profile',
        loadComponent: () => import('./profile/profile').then((m) => m.ProfileComponent),
      },
      {
        path: 'admin/store-members',
        loadComponent: () =>
          import('./admin/store-members/store-members').then((m) => m.StoreMembersComponent),
      },
      {
        path: 'admin/quiz/questions',
        loadComponent: () =>
          import('./quiz/admin/quiz-questions').then((m) => m.QuizQuestionsComponent),
      },
      {
        path: 'admin/quiz/questions/new',
        loadComponent: () =>
          import('./quiz/admin/question-edit').then((m) => m.QuestionEditComponent),
      },
      {
        path: 'admin/quiz/questions/edit/:id',
        loadComponent: () =>
          import('./quiz/admin/question-edit').then((m) => m.QuestionEditComponent),
      },
      {
        path: 'admin/quiz/categories',
        loadComponent: () =>
          import('./quiz/admin/quiz-categories').then((m) => m.QuizCategoriesComponent),
      },
      {
        path: 'admin/quiz/attempts',
        loadComponent: () =>
          import('./quiz/admin/quiz-attempts').then((m) => m.QuizAttemptsComponent),
      },
    ],
  },
];
