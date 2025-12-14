import type { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      // Protect the main app pages
      { path: '', redirect: '/dashboard' },
      {
        path: 'dashboard',
        component: () => import('pages/DashboardPage.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'trips',
        component: () => import('pages/TripsPage.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'trips/:tripId',
        component: () => import('pages/TripDetailsPage.vue'),
        meta: { requiresAuth: true },
      },
      // Expense editor routes removed
      {
        path: '/settings',
        component: () => import('pages/SettingsPage.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: '/people',
        component: () => import('pages/PeoplePage.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: '/trips/:tripId/settlement',
        component: () => import('pages/SettlementPage.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: '/trips/:tripId/settlement',
        component: () => import('pages/SettlementPage.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'settings',
        component: () => import('pages/SettingsPage.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'documents-vault',
        component: () => import('pages/DocumentsVaultPage.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'expense-analytics',
        component: () => import('pages/ExpenseAnalyticsPage.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'packing-list',
        component: () => import('pages/PackingListPage.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'itinerary-templates',
        component: () => import('pages/ItineraryTemplatesPage.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'user-profile',
        component: () => import('pages/UserProfilePage.vue'),
        meta: { requiresAuth: true },
      },
    ],
  },
  {
    path: '/login',
    component: () => import('pages/LoginPage.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/signup',
    component: () => import('pages/SignupPage.vue'),
    meta: { requiresAuth: false },
  },
  // Always leave this as last one
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue'),
  },
];

export default routes;
