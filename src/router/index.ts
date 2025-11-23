import { route } from 'quasar/wrappers';
import { createRouter, createWebHistory } from 'vue-router';
import type { NavigationGuardWithThis } from 'vue-router';
import { supabase } from 'boot/supabase';
import routes from './routes';

export default route(function (/* { store, ssrContext } */) {
  const createHistory = createWebHistory;

  const Router = createRouter({
    scrollBehavior: () => ({ left: 0, top: 0 }),
    routes,
    history: createHistory(process.env.VUE_ROUTER_BASE),
  });

  // Navigation Guard (Middleware for Authentication)
  const authGuard: NavigationGuardWithThis<undefined> = async (to, from, next) => {
    // Check if the destination route requires authentication
    const requiresAuth = to.matched.some(record => record.meta.requiresAuth);

    // Get the current user session (UPDATED API)
    const { data: { session } } = await supabase.auth.getSession();

    if (requiresAuth && !session) {
      // If auth is required and no session exists, redirect to login
      next('/login');
    } else if (!requiresAuth && session && (to.path === '/login' || to.path === '/')) {
      // If user is logged in and tries to access login or root, redirect to dashboard
      next('/dashboard');
    } else {
      // Proceed to the route
      next();
    }
  };

  Router.beforeEach(authGuard);

  return Router;
});
