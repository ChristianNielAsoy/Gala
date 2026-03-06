import { route } from 'quasar/wrappers';
import { createRouter, createWebHistory } from 'vue-router';
import type { NavigationGuardWithThis } from 'vue-router';
import { useAuthStore } from 'src/stores/authStore';
import routes from './routes';

export default route(function (/* { store, ssrContext } */) {
  const createHistory = createWebHistory;

  const Router = createRouter({
    scrollBehavior: () => ({ left: 0, top: 0 }),
    routes,
    history: createHistory(process.env.VUE_ROUTER_BASE),
  });

  const authGuard: NavigationGuardWithThis<undefined> = async (to, _from, next) => {
    const requiresAuth = to.matched.some((record) => record.meta.requiresAuth);
    const authStore = useAuthStore();

    // Initialize once — subsequent calls are no-ops (cached session)
    if (!authStore.isInitialized) {
      await authStore.initialize();
    }

    if (requiresAuth && !authStore.isAuthenticated) {
      next('/login');
    } else if (!requiresAuth && authStore.isAuthenticated && (to.path === '/login' || to.path === '/')) {
      next('/dashboard');
    } else {
      next();
    }
  };

  Router.beforeEach(authGuard);

  return Router;
});
