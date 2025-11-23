import { route } from 'quasar/wrappers';
import { createRouter, createWebHistory, NavigationGuardWithThis } from 'vue-router';
// Import the exported Supabase client
import { supabase } from 'boot/supabase';
import routes from './routes';

export default route(function (/* { store, ssrContext } */) {
  const createHistory = createWebHistory;

  const Router = createRouter({
    scrollBehavior: () => ({ left: 0, top: 0 }),
    routes, // Defined in src/router/routes.ts
    history: createHistory(process.env.VUE_ROUTER_BASE),
  });

  // ----------------------------------------------------------------------
  // NAVIGATION GUARD (Middleware for Authentication)
  // ----------------------------------------------------------------------

  // Define the type for the auth guard function
  const authGuard: NavigationGuardWithThis<undefined> = async (to, from, next) => {
    // 1. Check if the destination route requires authentication
    const requiresAuth = to.matched.some(record => record.meta.requiresAuth);

    // 2. Get the current user session
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
