import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      name: 'dashboard',
      component: () => import('@/views/DashboardView.vue'),
    },
    {
      path: '/scan/:trace',
      name: 'public-scan',
      component: () => import('@/views/PublicScanView.vue'),
      meta: { public: true },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  if (to.meta.public) {
    return true
  }
  if (!userStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  // 首次进入受保护页时，若 roles 还没拉就刷一次
  if (!userStore.roles.length) {
    try {
      await Promise.all([userStore.loadProfile(), userStore.loadRouters()])
    } catch {
      userStore.logout()
      return { path: '/login' }
    }
  }
  return true
})

export default router
