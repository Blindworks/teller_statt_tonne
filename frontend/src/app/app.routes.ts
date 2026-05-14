import { Routes } from '@angular/router';
import { DashboardComponent } from './dashboard/dashboard';
import { StoresComponent } from './stores/stores';
import { AppShellComponent } from './shell/app-shell';
import { authGuard } from './auth/auth.guard';
import { featureGuard } from './auth/feature.guard';
import { onboardingCompletedGuard, onboardingRequiredGuard } from './auth/onboarding.guard';
import { landingGuard } from './landing/landing.guard';

const plannerEdit = featureGuard('route.planner');
const plannerView = featureGuard('route.planner.view');
const userEdit = featureGuard('route.user.edit');
const quizAdmin = featureGuard('route.quiz.admin');
const adminArea = featureGuard('route.admin');

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
    path: 'onboarding',
    canActivate: [authGuard, onboardingRequiredGuard],
    loadComponent: () =>
      import('./onboarding/onboarding-page').then((m) => m.OnboardingPageComponent),
  },
  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard, onboardingCompletedGuard],
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
        path: 'events',
        loadComponent: () =>
          import('./events/events-list/events-list').then((m) => m.EventsListComponent),
      },
      {
        path: 'events/new',
        canActivate: [plannerEdit],
        loadComponent: () =>
          import('./events/event-form/event-form').then((m) => m.EventFormComponent),
      },
      {
        path: 'events/:id',
        canActivate: [plannerEdit],
        loadComponent: () =>
          import('./events/event-form/event-form').then((m) => m.EventFormComponent),
      },
      {
        path: 'pickups',
        canActivate: [plannerView],
        loadComponent: () =>
          import('./pickups/pickups/pickups').then((m) => m.PickupsComponent),
      },
      {
        path: 'pickups/new',
        canActivate: [plannerEdit],
        loadComponent: () =>
          import('./pickups/pickup-edit/pickup-edit').then((m) => m.PickupEditComponent),
      },
      {
        path: 'pickups/edit/:id',
        canActivate: [plannerEdit],
        loadComponent: () =>
          import('./pickups/pickup-edit/pickup-edit').then((m) => m.PickupEditComponent),
      },
      {
        path: 'pickups/:pickupId/run',
        canActivate: [plannerView],
        loadComponent: () =>
          import('./pickup-run/pickup-run').then((m) => m.PickupRunComponent),
      },
      {
        path: 'admin/food-categories',
        canActivate: [adminArea],
        loadComponent: () =>
          import('./admin/food-categories/food-categories-admin').then(
            (m) => m.FoodCategoriesAdminComponent,
          ),
      },
      {
        path: 'users',
        loadComponent: () =>
          import('./users/users-list/users-list').then((m) => m.UsersListComponent),
      },
      {
        path: 'users/new',
        canActivate: [userEdit],
        loadComponent: () =>
          import('./users/user-edit/user-edit').then((m) => m.UserEditComponent),
      },
      {
        path: 'users/edit/:id',
        canActivate: [userEdit],
        loadComponent: () =>
          import('./users/user-edit/user-edit').then((m) => m.UserEditComponent),
      },
      {
        path: 'profile',
        loadComponent: () => import('./profile/profile').then((m) => m.ProfileComponent),
      },
      {
        path: 'statistik',
        canActivate: [plannerEdit],
        loadComponent: () =>
          import('./stats/stats-overview').then((m) => m.StatsOverviewComponent),
      },
      {
        path: 'teamleitung',
        canActivate: [plannerEdit],
        loadComponent: () =>
          import('./teamleiter/teamleiter-dashboard/teamleiter-dashboard').then(
            (m) => m.TeamleiterDashboardComponent,
          ),
      },
      {
        path: 'admin',
        canActivate: [adminArea],
        loadComponent: () =>
          import('./admin/admin-dashboard/admin-dashboard').then((m) => m.AdminDashboardComponent),
      },
      {
        path: 'admin/roles',
        canActivate: [adminArea],
        loadComponent: () =>
          import('./admin/roles/roles-list/roles-list').then((m) => m.RolesListComponent),
      },
      {
        path: 'admin/roles/new',
        canActivate: [adminArea],
        loadComponent: () =>
          import('./admin/roles/role-form/role-form').then((m) => m.RoleFormComponent),
      },
      {
        path: 'admin/roles/:id',
        canActivate: [adminArea],
        loadComponent: () =>
          import('./admin/roles/role-form/role-form').then((m) => m.RoleFormComponent),
      },
      {
        path: 'admin/permissions',
        canActivate: [featureGuard('route.admin.permissions')],
        loadComponent: () =>
          import('./admin/permissions/permissions-matrix/permissions-matrix').then(
            (m) => m.PermissionsMatrixComponent,
          ),
      },
      {
        path: 'admin/distribution-points',
        canActivate: [plannerEdit],
        loadComponent: () =>
          import(
            './admin/distribution-points/distribution-points-list/distribution-points-list'
          ).then((m) => m.DistributionPointsListComponent),
      },
      {
        path: 'admin/distribution-points/new',
        canActivate: [plannerEdit],
        loadComponent: () =>
          import(
            './admin/distribution-points/distribution-point-form/distribution-point-form'
          ).then((m) => m.DistributionPointFormComponent),
      },
      {
        path: 'admin/distribution-points/:id',
        canActivate: [plannerEdit],
        loadComponent: () =>
          import(
            './admin/distribution-points/distribution-point-form/distribution-point-form'
          ).then((m) => m.DistributionPointFormComponent),
      },
      {
        path: 'admin/partner-categories',
        canActivate: [plannerEdit],
        loadComponent: () =>
          import(
            './admin/partner-categories/partner-categories-list/partner-categories-list'
          ).then((m) => m.PartnerCategoriesListComponent),
      },
      {
        path: 'admin/partner-categories/new',
        canActivate: [plannerEdit],
        loadComponent: () =>
          import(
            './admin/partner-categories/partner-category-form/partner-category-form'
          ).then((m) => m.PartnerCategoryFormComponent),
      },
      {
        path: 'admin/partner-categories/:id',
        canActivate: [plannerEdit],
        loadComponent: () =>
          import(
            './admin/partner-categories/partner-category-form/partner-category-form'
          ).then((m) => m.PartnerCategoryFormComponent),
      },
      {
        path: 'admin/store-members',
        canActivate: [plannerEdit],
        loadComponent: () =>
          import('./admin/store-members/store-members').then((m) => m.StoreMembersComponent),
      },
      {
        path: 'teamleitung/onboarding',
        canActivate: [plannerEdit],
        loadComponent: () =>
          import('./admin/onboarding/onboarding-admin').then((m) => m.OnboardingAdminComponent),
      },
      {
        path: 'teamleitung/zertifikate',
        canActivate: [plannerEdit],
        loadComponent: () =>
          import(
            './hygiene-certificate/admin-hygiene-certificates/admin-hygiene-certificates.component'
          ).then((m) => m.AdminHygieneCertificatesComponent),
      },
      {
        path: 'teamleitung/applications',
        canActivate: [plannerEdit],
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
        canActivate: [adminArea],
        loadComponent: () =>
          import('./admin/system-log/admin-system-log').then((m) => m.AdminSystemLogComponent),
      },
      {
        path: 'admin/stores/deleted',
        canActivate: [adminArea],
        loadComponent: () =>
          import('./admin/deleted-stores/deleted-stores').then((m) => m.DeletedStoresComponent),
      },
      {
        path: 'admin/system-settings',
        canActivate: [adminArea],
        loadComponent: () =>
          import('./admin/system-settings/system-settings.component').then(
            (m) => m.SystemSettingsComponent,
          ),
      },
      {
        path: 'admin/quiz/questions',
        canActivate: [quizAdmin],
        loadComponent: () =>
          import('./quiz/admin/quiz-questions').then((m) => m.QuizQuestionsComponent),
      },
      {
        path: 'admin/quiz/questions/new',
        canActivate: [quizAdmin],
        loadComponent: () =>
          import('./quiz/admin/question-edit').then((m) => m.QuestionEditComponent),
      },
      {
        path: 'admin/quiz/questions/edit/:id',
        canActivate: [quizAdmin],
        loadComponent: () =>
          import('./quiz/admin/question-edit').then((m) => m.QuestionEditComponent),
      },
      {
        path: 'admin/quiz/categories',
        canActivate: [quizAdmin],
        loadComponent: () =>
          import('./quiz/admin/quiz-categories').then((m) => m.QuizCategoriesComponent),
      },
      {
        path: 'admin/quiz/attempts',
        canActivate: [quizAdmin],
        loadComponent: () =>
          import('./quiz/admin/quiz-attempts').then((m) => m.QuizAttemptsComponent),
      },
    ],
  },
];
