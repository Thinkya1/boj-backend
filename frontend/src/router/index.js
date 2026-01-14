import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const routes = [
  { path: '/', redirect: '/questions' },
  { path: '/login', component: () => import('../pages/Login.vue'), meta: { guestOnly: true } },
  { path: '/register', component: () => import('../pages/Register.vue'), meta: { guestOnly: true } },
  { path: '/questions', component: () => import('../pages/QuestionList.vue') },
  { path: '/questions/new', component: () => import('../pages/QuestionForm.vue'), meta: { requiresAuth: true } },
  { path: '/questions/:id', component: () => import('../pages/QuestionDetail.vue'), meta: { fullBleed: true } },
  { path: '/questions/:id/edit', component: () => import('../pages/QuestionForm.vue'), meta: { requiresAuth: true } },
  { path: '/posts', component: () => import('../pages/PostList.vue') },
  { path: '/posts/new', component: () => import('../pages/PostForm.vue'), meta: { requiresAuth: true } },
  { path: '/posts/:id', component: () => import('../pages/PostDetail.vue') },
  { path: '/posts/:id/edit', component: () => import('../pages/PostForm.vue'), meta: { requiresAuth: true } },
  { path: '/submissions', component: () => import('../pages/SubmissionList.vue'), meta: { requiresAuth: true } },
  { path: '/submissions/:id', component: () => import('../pages/SubmissionDetail.vue'), meta: { requiresAuth: true } },
  { path: '/me', component: () => import('../pages/Profile.vue'), meta: { requiresAuth: true } },
  { path: '/admin/users', component: () => import('../pages/AdminUsers.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/admin/questions', component: () => import('../pages/AdminQuestions.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/:pathMatch(.*)*', component: () => import('../pages/NotFound.vue') },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 };
  },
});

router.beforeEach(async (to) => {
  const auth = useAuthStore();
  if (!auth.ready) {
    await auth.bootstrap();
  }
  if (to.meta.requiresAuth && !auth.user) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { path: '/questions' };
  }
  if (to.meta.guestOnly && auth.user) {
    return { path: '/questions' };
  }
  return true;
});

export default router;
