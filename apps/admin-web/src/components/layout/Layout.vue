<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, RouterLink, RouterView } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Button } from '@/components/ui/button'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Badge } from '@/components/ui/badge'
import { Zap, LogOut, Home, User, ShoppingCart } from 'lucide-vue-next'
import SidebarNavItem from './SidebarNavItem.vue'

const userStore = useUserStore()
const route = useRoute()

const isConsumer = computed(() => userStore.roles.includes('consumer'))

const userInitials = computed(() => {
  const n = userStore.nickName || userStore.userName || '?'
  return n.slice(0, 1).toUpperCase()
})

const breadcrumb = computed(() => {
  const trail: string[] = []
  for (const m of route.matched) {
    const t = m.meta?.title as string | undefined
    if (t) trail.push(t)
  }
  if (trail.length === 0 && route.meta?.title) trail.push(route.meta.title as string)
  return trail
})

const visibleRouters = computed(() => userStore.routers.filter((r) => !r.hidden))

function logout() {
  userStore.logout()
  location.replace('/login')
}
</script>

<template>
  <div class="min-h-screen flex bg-background">
    <!-- 侧边栏 -->
    <aside class="w-60 border-r border-sidebar-border bg-sidebar text-sidebar-foreground flex flex-col">
      <!-- Logo -->
      <RouterLink to="/" class="flex items-center gap-2 h-14 px-4 border-b border-sidebar-border shrink-0">
        <div class="size-8 rounded-lg bg-primary text-primary-foreground flex items-center justify-center">
          <Zap class="size-4" />
        </div>
        <div class="flex flex-col">
          <span class="text-sm font-semibold leading-none">NEV-v2</span>
          <span class="text-[10px] text-muted-foreground mt-0.5">动力电池溯源</span>
        </div>
      </RouterLink>

      <!-- 菜单（包括 Dashboard 入口）-->
      <ScrollArea class="flex-1">
        <nav class="p-2 space-y-0.5 text-sm">
          <RouterLink
            to="/dashboard"
            :class="[
              'flex items-center gap-2 rounded-md py-1.5 px-3 transition-colors',
              route.path === '/dashboard' || route.path === '/'
                ? 'bg-sidebar-primary text-sidebar-primary-foreground'
                : 'hover:bg-sidebar-accent hover:text-sidebar-accent-foreground',
            ]"
          >
            <Home class="size-4 shrink-0 opacity-80" />
            <span class="flex-1">仪表盘</span>
          </RouterLink>
          <SidebarNavItem
            v-for="r in visibleRouters"
            :key="r.path || r.name"
            :entry="r"
          />
        </nav>
      </ScrollArea>

      <!-- 底部信息 -->
      <div class="p-3 border-t border-sidebar-border shrink-0">
        <p class="text-[10px] text-muted-foreground leading-relaxed">
          v0.1.0 · Wave 4 D23<br />
          shadcn-vue + Tailwind 4
        </p>
      </div>
    </aside>

    <!-- 右侧 -->
    <div class="flex-1 flex flex-col min-w-0">
      <!-- 顶栏 -->
      <header class="h-14 border-b bg-background flex items-center justify-between px-6 shrink-0">
        <div class="flex items-center gap-2 text-sm text-muted-foreground">
          <Home class="size-4" />
          <template v-for="(t, i) in breadcrumb" :key="i">
            <span class="opacity-50">/</span>
            <span :class="i === breadcrumb.length - 1 ? 'text-foreground font-medium' : ''">{{ t }}</span>
          </template>
          <span v-if="breadcrumb.length === 0">首页</span>
        </div>
        <div class="flex items-center gap-3">
          <Button v-if="isConsumer" variant="ghost" size="sm" @click="$router.push('/marketplace/cart')">
            <ShoppingCart class="size-4" /> 购物车
          </Button>
          <Badge v-for="r in userStore.roles" :key="r" variant="secondary" class="font-normal">{{ r }}</Badge>
          <DropdownMenu>
            <DropdownMenuTrigger as-child>
              <Button variant="ghost" class="gap-2 px-2 h-9">
                <Avatar class="size-7">
                  <AvatarFallback class="text-xs bg-primary text-primary-foreground">{{ userInitials }}</AvatarFallback>
                </Avatar>
                <span class="text-sm">{{ userStore.nickName || userStore.userName }}</span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" class="w-44">
              <DropdownMenuLabel>
                <div class="flex flex-col">
                  <span class="text-sm font-medium">{{ userStore.nickName || userStore.userName }}</span>
                  <span class="text-xs text-muted-foreground">userId: {{ userStore.userId }}</span>
                </div>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem>
                <User class="size-4 mr-2" /> 个人中心
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem @click="logout" class="text-destructive focus:text-destructive">
                <LogOut class="size-4 mr-2" /> 退出登录
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </header>

      <!-- 内容 -->
      <main class="flex-1 overflow-auto bg-muted/30 p-6">
        <RouterView />
      </main>
    </div>
  </div>
</template>
