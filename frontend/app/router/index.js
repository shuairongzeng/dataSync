import { createRouter, createWebHistory } from 'vue-router';
import Login from '@/views/Login.vue';
import authService from '@/services/authService';

// Placeholder for a protected component
const Dashboard = { template: '<div><h2>Dashboard (Protected)</h2><button @click="logout">Logout</button></div>', methods: { logout() { authService.logout(); this.$router.push("/login"); } } };
// Placeholder for a public component
const Home = { template: '<div><h2>Home Page (Public)</h2><router-link to="/login">Login</router-link> | <router-link to="/dashboard">Dashboard</router-link></div>' };


const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home,
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { guest: true } // For users who are not logged in
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    meta: { requiresAuth: true } // This route requires authentication
  }
  // Define other routes for Task Configuration and Task Progress later
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL || '/'), // Ensure BASE_URL is correctly handled
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
      next({ name: 'Dashboard' }); // Or wherever authenticated users should go
    } else {
      next();
    }
  } else {
    next(); // Make sure to always call next()!
  }
});

export default router;
