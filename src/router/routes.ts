import type { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      {
        path: 'dashboard',
        component: () => import('pages/main/DashboardPage.vue'),
        meta: { requiresAuth: true, title: 'Dashboard' },
      },
      {
        path: 'trips',
        component: () => import('pages/trips/TripsPage.vue'),
        meta: { requiresAuth: true, title: 'My Trips' },
      },
      {
        path: 'trips/:tripId',
        component: () => import('pages/trips/TripDetailsPage.vue'),
        meta: { requiresAuth: true, title: 'Trip Details' },
      },
      {
        path: 'trips/:tripId/expenses/new',
        component: () => import('pages/expenses/ExpenseEditorPage.vue'),
        meta: { requiresAuth: true, title: 'New Expense' },
      },
      {
        path: 'trips/:tripId/expenses/:expenseId',
        component: () => import('pages/expenses/ExpenseEditorPage.vue'),
        meta: { requiresAuth: true, title: 'Edit Expense' },
      },
      {
        path: 'settings',
        component: () => import('pages/settings/SettingsPage.vue'),
        meta: { requiresAuth: true, title: 'Settings' },
      },
      {
        path: 'trips/:tripId/settlement',
        component: () => import('pages/settlement/SettlementPage.vue'),
        meta: { requiresAuth: true, title: 'Settlement' },
      },
      {
        path: 'documents-vault',
        component: () => import('pages/documents/DocumentsVaultPage.vue'),
        meta: { requiresAuth: true, title: 'Documents Vault' },
      },
      {
        path: 'expense-analytics',
        component: () => import('pages/expenses/ExpenseAnalyticsPage.vue'),
        meta: { requiresAuth: true, title: 'Expense Analytics' },
      },
      {
        path: 'packing-list',
        component: () => import('pages/trips/PackingListPage.vue'),
        meta: { requiresAuth: true, title: 'Packing List' },
      },
      {
        path: 'user-profile',
        component: () => import('pages/settings/UserProfilePage.vue'),
        meta: { requiresAuth: true, title: 'User Profile' },
      },
    ],
  },
  {
    path: '/login',
    component: () => import('pages/auth/LoginPage.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/signup',
    component: () => import('pages/auth/SignupPage.vue'),
    meta: { requiresAuth: false },
  },
  // Always leave this as last one
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/error/ErrorNotFound.vue'),
  },
];

export default routes;
