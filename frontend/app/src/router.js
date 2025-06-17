import { createRouter, createWebHistory } from 'vue-router';
import Login from '@/views/Login.vue';
import authService from '@/services/authService';
import TaskConfiguration from '@/views/TaskConfiguration.vue';
import TaskProgress from '@/views/TaskProgress.vue';

const Dashboard = { template: '<div><h2>Dashboard (Protected)</h2><button @click="logout">Logout</button></div>', methods: { logout() { authService.logout(); this.$router.push("/login"); } } };
const Home = { template: '<div><h2>Home Page (Public)</h2><router-link to="/login">Login</router-link> | <router-link to="/dashboard">Dashboard</router-link></div>' };

const routes = [
  { path: '/', name: 'Home', component: Home },
  { path: '/login', name: 'Login', component: Login, meta: { guest: true } },
  { path: '/dashboard', name: 'Dashboard', component: Dashboard, meta: { requiresAuth: true } },
  { path: '/task-configuration', name: 'TaskConfiguration', component: TaskConfiguration, meta: { requiresAuth: true } },
  { path: '/task-progress', name: 'TaskProgress', component: TaskProgress, meta: { requiresAuth: true } },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL || '/'),
  routes,
});

router.beforeEach((to, from, next) => {
  const isAuthenticated = authService.isAuthenticated();
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!isAuthenticated) {
      next({ name: 'Login' });
    } else {
      next();
    }
  } else if (to.matched.some(record => record.meta.guest)) {
    if (isAuthenticated) {
      next({ name: 'Dashboard' });
    } else {
      next();
    }
  } else {
    next();
  }
});

export default router;
