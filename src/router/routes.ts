import type { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      // Protect the main app pages
      { path: '', redirect: '/home' },
      {
        path: 'home',
        component: () => import('pages/HomePage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'dashboard',
        component: () => import('pages/DashboardPage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'trips',
        component: () => import('pages/TripsPage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'trips/:tripId',
        component: () => import('pages/TripDetailsPage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'trips/:tripId/expense/new',
        component: () => import('pages/ExpenseEditorPage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'trips/:tripId/expense/:expenseId',
        component: () => import('pages/ExpenseEditorPage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: '/settings',
        component: () => import('pages/SettingsPage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: '/people',
        component: () => import('pages/PeoplePage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: '/trips/:tripId/settlement',
        component: () => import('pages/SettlementPage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: '/trips/create',
        component: () => import('pages/TripCreationPage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: '/trips/:tripId/settlement',
        component: () => import('pages/SettlementPage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'settings',
        component: () => import('pages/SettingsPage.vue'),
        meta: { requiresAuth: true }
      },
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
  // Always leave this as last one
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue'),
  },
];

export default routes;
