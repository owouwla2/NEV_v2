import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { registerDynamicRoutes, dynamicRoutesAdded } from './dynamic'

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
      path: '/scan/:trace',
      name: 'public-scan',
      component: () => import('@/views/PublicScanView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      name: 'layout',
      component: () => import('@/components/layout/Layout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/DashboardView.vue'),
          meta: { title: '仪表盘' },
        },
      ],
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
  // 首次进入受保护页时拉 profile + routers
  if (!userStore.routers.length) {
    try {
      await Promise.all([userStore.loadProfile(), userStore.loadRouters()])
    } catch {
      userStore.logout()
      return { path: '/login' }
    }
  }
  // 注册动态路由（仅一次）
  if (!dynamicRoutesAdded() && userStore.routers.length > 0) {
    registerDynamicRoutes(router, userStore.routers)
    // 命中刚刚注册的路由，需要再触发一次导航解析
    return { ...to, replace: true }
  }
  return true
})

export default router
