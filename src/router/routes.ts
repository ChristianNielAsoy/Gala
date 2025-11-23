import type { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', component: () => import('pages/DashboardPage.vue'), meta: { requiresAuth: true } },
      { path: 'trips', component: () => import('pages/TripsPage.vue'), meta: { requiresAuth: true } },
      { path: 'trips/:tripId', component: () => import('pages/TripDetailsPage.vue'), meta: { requiresAuth: true } },
      { path: 'trips/:tripId/expense/:expenseId', component: () => import('pages/ExpenseEditorPage.vue'), meta: { requiresAuth: true } },
      { path: 'settings', component: () => import('pages/SettingsPage.vue'), meta: { requiresAuth: true } },
    ],
  },
  {
    path: '/login',
    component: () => import('pages/LoginPage.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/signup',
    component: () => import('pages/SignupPage.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue'),
  },
];

export default routes;
