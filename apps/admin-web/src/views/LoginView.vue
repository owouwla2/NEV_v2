<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { toast } from 'vue-sonner'
import { Loader2, Zap } from 'lucide-vue-next'

const username = ref('admin')
const password = ref('admin123')
const loading = ref(false)

const router = useRouter()
const userStore = useUserStore()

async function onSubmit(e: Event) {
  e.preventDefault()
  if (!username.value || !password.value) {
    toast.error('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    await userStore.login({ username: username.value, password: password.value })
    await Promise.all([userStore.loadProfile(), userStore.loadRouters()])
    toast.success(`欢迎，${userStore.nickName || userStore.userName}`)
    router.replace('/')
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : String(err)
    toast.error(`登录失败：${msg}`)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-zinc-50 via-zinc-100 to-zinc-200 dark:from-zinc-950 dark:via-zinc-900 dark:to-zinc-800 p-4">
    <Card class="w-full max-w-md shadow-2xl">
      <CardHeader class="space-y-3">
        <div class="flex items-center gap-2">
          <div class="size-10 rounded-lg bg-primary text-primary-foreground flex items-center justify-center">
            <Zap class="size-5" />
          </div>
          <div>
            <CardTitle class="text-xl">NEV-v2 管理后台</CardTitle>
            <CardDescription>动力电池全产业链溯源平台</CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <form @submit="onSubmit" class="space-y-4">
          <div class="space-y-2">
            <Label for="username">账号</Label>
            <Input
              id="username"
              v-model="username"
              placeholder="admin / producer1 / consumer1 ..."
              autocomplete="username"
              :disabled="loading"
            />
          </div>
          <div class="space-y-2">
            <Label for="password">密码</Label>
            <Input
              id="password"
              v-model="password"
              type="password"
              placeholder="admin123"
              autocomplete="current-password"
              :disabled="loading"
            />
          </div>
          <Button type="submit" class="w-full" :disabled="loading">
            <Loader2 v-if="loading" class="size-4 animate-spin" />
            <span>{{ loading ? '登录中...' : '登 录' }}</span>
          </Button>
        </form>
        <p class="text-xs text-muted-foreground mt-6 leading-relaxed">
          Demo 账号：admin / producer1 / distributor1 / retailer1 / merchant1 / consumer1 / recycler1
          <br />密码统一 <code class="px-1 py-0.5 rounded bg-muted text-foreground">admin123</code>
        </p>
      </CardContent>
    </Card>
  </div>
</template>
