<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { http } from '@/lib/http'
import { Battery, ShoppingCart, Leaf, Link2 } from 'lucide-vue-next'

const userStore = useUserStore()

const stats = ref({
  batteryCount: 0,
  orderCount: 0,
  totalCarbon: 0,
  chainEvents: 0,
})

interface ProductPage {
  total: number
}

onMounted(async () => {
  // 仅 demo 用：拉公开商品总数（拿 total 字段）
  try {
    const r = await http<ProductPage>('/public/product/list?pageNum=1&pageSize=1')
    stats.value.orderCount = r?.total ?? 0
  } catch (e) {
    // ignore
  }
})
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold">仪表盘</h1>
      <p class="text-sm text-muted-foreground mt-1">
        欢迎，{{ userStore.nickName || userStore.userName }}！这里是 NEV-v2 全局概览
      </p>
    </div>

    <!-- 4 个统计卡 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <Card>
        <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle class="text-sm font-medium text-muted-foreground">商品总数</CardTitle>
          <ShoppingCart class="size-4 text-muted-foreground" />
        </CardHeader>
        <CardContent>
          <div class="text-2xl font-bold">{{ stats.orderCount }}</div>
          <p class="text-xs text-muted-foreground mt-1">公开列表 total</p>
        </CardContent>
      </Card>
      <Card>
        <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle class="text-sm font-medium text-muted-foreground">电池注册</CardTitle>
          <Battery class="size-4 text-muted-foreground" />
        </CardHeader>
        <CardContent>
          <div class="text-2xl font-bold">D24 接</div>
          <p class="text-xs text-muted-foreground mt-1">nev_battery 总数</p>
        </CardContent>
      </Card>
      <Card>
        <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle class="text-sm font-medium text-muted-foreground">碳积分发放</CardTitle>
          <Leaf class="size-4 text-muted-foreground" />
        </CardHeader>
        <CardContent>
          <div class="text-2xl font-bold">D24 接</div>
          <p class="text-xs text-muted-foreground mt-1">∑ totalEarned kgCO2eq</p>
        </CardContent>
      </Card>
      <Card>
        <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle class="text-sm font-medium text-muted-foreground">链上事件</CardTitle>
          <Link2 class="size-4 text-muted-foreground" />
        </CardHeader>
        <CardContent>
          <div class="text-2xl font-bold">D24 接</div>
          <p class="text-xs text-muted-foreground mt-1">LifecycleEventAdded count</p>
        </CardContent>
      </Card>
    </div>

    <!-- 当前用户 / 菜单状态（D22 验证保留）-->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
      <Card>
        <CardHeader>
          <CardTitle>当前用户</CardTitle>
          <CardDescription>来自 /system/user/getInfo</CardDescription>
        </CardHeader>
        <CardContent class="space-y-3 text-sm">
          <div class="grid grid-cols-2 gap-x-4 gap-y-2">
            <div><span class="text-muted-foreground">userId:</span> <code>{{ userStore.userId }}</code></div>
            <div><span class="text-muted-foreground">userName:</span> {{ userStore.userName }}</div>
            <div><span class="text-muted-foreground">nickName:</span> {{ userStore.nickName }}</div>
            <div><span class="text-muted-foreground">avatar:</span> {{ userStore.avatar || '(none)' }}</div>
          </div>
          <div class="space-y-1">
            <div class="text-xs text-muted-foreground">roles</div>
            <div class="flex flex-wrap gap-2">
              <Badge v-for="r in userStore.roles" :key="r" variant="secondary">{{ r }}</Badge>
            </div>
          </div>
          <div class="space-y-1">
            <div class="text-xs text-muted-foreground">permissions ({{ userStore.permissions.length }})</div>
            <div class="flex flex-wrap gap-1 max-h-24 overflow-auto">
              <Badge v-for="p in userStore.permissions.slice(0, 30)" :key="p" variant="outline" class="text-[10px] font-mono">{{ p }}</Badge>
              <span v-if="userStore.permissions.length > 30" class="text-muted-foreground text-xs self-center">
                ...+{{ userStore.permissions.length - 30 }}
              </span>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>动态路由</CardTitle>
          <CardDescription>{{ userStore.routers.length }} 个一级菜单</CardDescription>
        </CardHeader>
        <CardContent>
          <ul class="space-y-1 text-sm max-h-72 overflow-auto pr-2">
            <li v-for="r in userStore.routers" :key="r.name || r.path" class="flex items-center gap-2">
              <Badge variant="outline" class="font-mono text-[10px]">{{ r.path }}</Badge>
              <span>{{ r.meta?.title || r.name }}</span>
              <span class="text-xs text-muted-foreground ml-auto">{{ r.children?.length ?? 0 }} 子项</span>
            </li>
          </ul>
        </CardContent>
      </Card>
    </div>
  </div>
</template>
