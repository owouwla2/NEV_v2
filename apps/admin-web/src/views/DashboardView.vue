<script setup lang="ts">
import { useUserStore } from '@/stores/user'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

function logout() {
  userStore.logout()
  router.replace('/login')
}
</script>

<template>
  <div class="min-h-screen p-8 bg-background">
    <div class="max-w-6xl mx-auto space-y-6">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold">NEV-v2 管理后台</h1>
          <p class="text-sm text-muted-foreground mt-1">D22 登录跑通占位页 - 完整 layout 见 D23</p>
        </div>
        <Button variant="outline" @click="logout">登出</Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>当前用户</CardTitle>
          <CardDescription>从 /system/user/getInfo + /system/menu/getRouters 返回</CardDescription>
        </CardHeader>
        <CardContent class="space-y-4">
          <div class="grid grid-cols-2 gap-x-8 gap-y-2 text-sm">
            <div><span class="text-muted-foreground">userId:</span> {{ userStore.userId }}</div>
            <div><span class="text-muted-foreground">userName:</span> {{ userStore.userName }}</div>
            <div><span class="text-muted-foreground">nickName:</span> {{ userStore.nickName }}</div>
            <div><span class="text-muted-foreground">avatar:</span> {{ userStore.avatar || '(none)' }}</div>
          </div>
          <div class="space-y-1">
            <div class="text-sm text-muted-foreground">roles</div>
            <div class="flex flex-wrap gap-2">
              <Badge v-for="r in userStore.roles" :key="r" variant="secondary">{{ r }}</Badge>
            </div>
          </div>
          <div class="space-y-1">
            <div class="text-sm text-muted-foreground">permissions ({{ userStore.permissions.length }})</div>
            <div class="flex flex-wrap gap-1 text-xs">
              <Badge v-for="p in userStore.permissions.slice(0, 20)" :key="p" variant="outline">{{ p }}</Badge>
              <span v-if="userStore.permissions.length > 20" class="text-muted-foreground text-xs self-center">
                ...+{{ userStore.permissions.length - 20 }} more
              </span>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>动态菜单（一级）</CardTitle>
          <CardDescription>{{ userStore.routers.length }} 个顶级菜单（D23 将渲染为侧边栏）</CardDescription>
        </CardHeader>
        <CardContent>
          <ul class="space-y-1 text-sm">
            <li v-for="r in userStore.routers" :key="r.name" class="flex items-center gap-3">
              <Badge variant="outline">{{ r.path }}</Badge>
              <span>{{ r.meta?.title || r.name }}</span>
              <span class="text-xs text-muted-foreground">children: {{ r.children?.length ?? 0 }}</span>
            </li>
          </ul>
        </CardContent>
      </Card>
    </div>
  </div>
</template>
