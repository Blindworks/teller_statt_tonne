import { Routes } from '@angular/router';
import { DashboardComponent } from './dashboard/dashboard';
import { StoresComponent } from './stores/stores';
import { AppShellComponent } from './shell/app-shell';
import { authGuard } from './auth/auth.guard';
import { roleGuard } from './auth/role.guard';
import { landingGuard } from './landing/landing.guard';

const plannerRoles = roleGuard(['ADMINISTRATOR', 'TEAMLEITER']);
const plannerViewRoles = roleGuard(['ADMINISTRATOR', 'TEAMLEITER', 'RETTER']);
const userEditRoles = roleGuard(['ADMINISTRATOR', 'TEAMLEITER']);
const quizAdminRoles = roleGuard(['ADMINISTRATOR', 'TEAMLEITER']);

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
    path: 'forgot-password',
    loadComponent: () =>
      import('./auth/forgot-password/forgot-password').then((m) => m.ForgotPasswordComponent),
  },
  {
    path: 'reset-password/:token',
    loadComponent: () =>
      import('./auth/reset-password/reset-password').then((m) => m.ResetPasswordComponent),
  },
  {
    path: 'quiz',
    loadComponent: () => import('./quiz/quiz').then((m) => m.QuizComponent),
  },
  {
    path: 'about',
    loadComponent: () => import('./about/about').then((m) => m.AboutComponent),
  },
  {
    path: 'impressum',
    loadComponent: () =>
      import('./legal/imprint/imprint').then((m) => m.ImprintComponent),
  },
  {
    path: 'datenschutz',
    loadComponent: () =>
      import('./legal/privacy/privacy').then((m) => m.PrivacyComponent),
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
        canActivate: [plannerViewRoles],
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
        canActivate: [userEditRoles],
        loadComponent: () =>
          import('./users/user-edit/user-edit').then((m) => m.UserEditComponent),
      },
      {
        path: 'users/edit/:id',
        canActivate: [userEditRoles],
        loadComponent: () =>
          import('./users/user-edit/user-edit').then((m) => m.UserEditComponent),
      },
      {
        path: 'profile',
        loadComponent: () => import('./profile/profile').then((m) => m.ProfileComponent),
      },
      {
        path: 'statistik',
        canActivate: [roleGuard(['ADMINISTRATOR', 'TEAMLEITER'])],
        loadComponent: () =>
          import('./stats/stats-overview').then((m) => m.StatsOverviewComponent),
      },
      {
        path: 'admin',
        canActivate: [roleGuard(['ADMINISTRATOR'])],
        loadComponent: () =>
          import('./admin/admin-dashboard/admin-dashboard').then((m) => m.AdminDashboardComponent),
      },
      {
        path: 'admin/roles',
        canActivate: [roleGuard(['ADMINISTRATOR'])],
        loadComponent: () =>
          import('./admin/roles/roles-list/roles-list').then((m) => m.RolesListComponent),
      },
      {
        path: 'admin/roles/new',
        canActivate: [roleGuard(['ADMINISTRATOR'])],
        loadComponent: () =>
          import('./admin/roles/role-form/role-form').then((m) => m.RoleFormComponent),
      },
      {
        path: 'admin/roles/:id',
        canActivate: [roleGuard(['ADMINISTRATOR'])],
        loadComponent: () =>
          import('./admin/roles/role-form/role-form').then((m) => m.RoleFormComponent),
      },
      {
        path: 'admin/distribution-points',
        canActivate: [roleGuard(['ADMINISTRATOR', 'TEAMLEITER'])],
        loadComponent: () =>
          import(
            './admin/distribution-points/distribution-points-list/distribution-points-list'
          ).then((m) => m.DistributionPointsListComponent),
      },
      {
        path: 'admin/distribution-points/new',
        canActivate: [roleGuard(['ADMINISTRATOR', 'TEAMLEITER'])],
        loadComponent: () =>
          import(
            './admin/distribution-points/distribution-point-form/distribution-point-form'
          ).then((m) => m.DistributionPointFormComponent),
      },
      {
        path: 'admin/distribution-points/:id',
        canActivate: [roleGuard(['ADMINISTRATOR', 'TEAMLEITER'])],
        loadComponent: () =>
          import(
            './admin/distribution-points/distribution-point-form/distribution-point-form'
          ).then((m) => m.DistributionPointFormComponent),
      },
      {
        path: 'admin/partner-categories',
        canActivate: [roleGuard(['ADMINISTRATOR', 'TEAMLEITER'])],
        loadComponent: () =>
          import(
            './admin/partner-categories/partner-categories-list/partner-categories-list'
          ).then((m) => m.PartnerCategoriesListComponent),
      },
      {
        path: 'admin/partner-categories/new',
        canActivate: [roleGuard(['ADMINISTRATOR', 'TEAMLEITER'])],
        loadComponent: () =>
          import(
            './admin/partner-categories/partner-category-form/partner-category-form'
          ).then((m) => m.PartnerCategoryFormComponent),
      },
      {
        path: 'admin/partner-categories/:id',
        canActivate: [roleGuard(['ADMINISTRATOR', 'TEAMLEITER'])],
        loadComponent: () =>
          import(
            './admin/partner-categories/partner-category-form/partner-category-form'
          ).then((m) => m.PartnerCategoryFormComponent),
      },
      {
        path: 'admin/store-members',
        canActivate: [roleGuard(['ADMINISTRATOR', 'TEAMLEITER'])],
        loadComponent: () =>
          import('./admin/store-members/store-members').then((m) => m.StoreMembersComponent),
      },
      {
        path: 'admin/zertifikate',
        canActivate: [roleGuard(['ADMINISTRATOR', 'TEAMLEITER'])],
        loadComponent: () =>
          import(
            './hygiene-certificate/admin-hygiene-certificates/admin-hygiene-certificates.component'
          ).then((m) => m.AdminHygieneCertificatesComponent),
      },
      {
        path: 'admin/applications',
        canActivate: [roleGuard(['ADMINISTRATOR', 'TEAMLEITER'])],
        loadComponent: () =>
          import('./partner-applications/admin-applications/admin-applications.component').then(
            (m) => m.AdminApplicationsComponent,
          ),
      },
      {
        path: 'my-applications',
        loadComponent: () =>
          import('./partner-applications/my-applications/my-applications.component').then(
            (m) => m.MyApplicationsComponent,
          ),
      },
      {
        path: 'admin/system-log',
        canActivate: [roleGuard(['ADMINISTRATOR'])],
        loadComponent: () =>
          import('./admin/system-log/admin-system-log').then((m) => m.AdminSystemLogComponent),
      },
      {
        path: 'admin/stores/deleted',
        canActivate: [roleGuard(['ADMINISTRATOR'])],
        loadComponent: () =>
          import('./admin/deleted-stores/deleted-stores').then((m) => m.DeletedStoresComponent),
      },
      {
        path: 'admin/quiz/questions',
        canActivate: [quizAdminRoles],
        loadComponent: () =>
          import('./quiz/admin/quiz-questions').then((m) => m.QuizQuestionsComponent),
      },
      {
        path: 'admin/quiz/questions/new',
        canActivate: [quizAdminRoles],
        loadComponent: () =>
          import('./quiz/admin/question-edit').then((m) => m.QuestionEditComponent),
      },
      {
        path: 'admin/quiz/questions/edit/:id',
        canActivate: [quizAdminRoles],
        loadComponent: () =>
          import('./quiz/admin/question-edit').then((m) => m.QuestionEditComponent),
      },
      {
        path: 'admin/quiz/categories',
        canActivate: [quizAdminRoles],
        loadComponent: () =>
          import('./quiz/admin/quiz-categories').then((m) => m.QuizCategoriesComponent),
      },
      {
        path: 'admin/quiz/attempts',
        canActivate: [quizAdminRoles],
        loadComponent: () =>
          import('./quiz/admin/quiz-attempts').then((m) => m.QuizAttemptsComponent),
      },
    ],
  },
];
