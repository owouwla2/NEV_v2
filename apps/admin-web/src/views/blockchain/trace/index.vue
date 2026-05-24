<script setup lang="ts">
import { ref } from 'vue'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Separator } from '@/components/ui/separator'
import { Loader2, Search, Link2, ShieldCheck, ShieldX } from 'lucide-vue-next'
import { toast } from 'vue-sonner'
import Copyable from '@/components/Copyable.vue'
import { scanBattery, type BatteryScanVO } from '@/api/battery'

const traceNumber = ref('')
const loading = ref(false)
const data = ref<BatteryScanVO | null>(null)
const errorMsg = ref('')

async function onSearch() {
  if (!traceNumber.value.trim()) {
    toast.error('请输入溯源编号')
    return
  }
  loading.value = true
  errorMsg.value = ''
  data.value = null
  try {
    data.value = await scanBattery(traceNumber.value.trim())
  } catch (err: unknown) {
    errorMsg.value = err instanceof Error ? err.message : String(err)
    toast.error(errorMsg.value)
  } finally {
    loading.value = false
  }
}

const eventColor: Record<string, string> = {
  PRODUCED: 'bg-blue-100 text-blue-700 dark:bg-blue-950 dark:text-blue-300',
  IN_USE: 'bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-300',
  SOLD: 'bg-purple-100 text-purple-700 dark:bg-purple-950 dark:text-purple-300',
  REPAIRED: 'bg-orange-100 text-orange-700 dark:bg-orange-950 dark:text-orange-300',
  RECYCLED: 'bg-green-100 text-green-700 dark:bg-green-950 dark:text-green-300',
  DISMANTLED: 'bg-zinc-200 text-zinc-700 dark:bg-zinc-800 dark:text-zinc-300',
}
</script>

<template>
  <div class="space-y-6 max-w-6xl">
    <div>
      <h1 class="text-2xl font-bold flex items-center gap-2">
        <Link2 class="size-6" /> 链上溯源
      </h1>
      <p class="text-sm text-muted-foreground mt-1">
        区块链浏览器视角：每条事件单独调链上 verifyEvent，dataHash 当场比对、txHash 可查
      </p>
    </div>

    <Card>
      <CardContent class="pt-6">
        <form class="flex items-end gap-3" @submit.prevent="onSearch">
          <div class="flex-1 space-y-1.5">
            <Label for="trace">溯源编号</Label>
            <Input id="trace" v-model="traceNumber" placeholder="BAT-DEMO-XXX" :disabled="loading" />
          </div>
          <Button type="submit" :disabled="loading">
            <Loader2 v-if="loading" class="size-4 animate-spin" />
            <Search v-else class="size-4" /> 查询链上
          </Button>
        </form>
      </CardContent>
    </Card>

    <Alert v-if="errorMsg" variant="destructive">
      <AlertTitle>查询失败</AlertTitle>
      <AlertDescription>{{ errorMsg }}</AlertDescription>
    </Alert>

    <template v-if="data">
      <Card>
        <CardHeader>
          <CardTitle class="text-base">{{ data.traceNumber }}</CardTitle>
          <CardDescription>
            链上事件总数 {{ data.totalEvents }} · verifyEvent 通过 {{ data.verifiedEvents }} 条 ·
            链上 count={{ data.chainEventCount }}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div class="flex items-center gap-2">
            <component :is="data.overallVerified ? ShieldCheck : ShieldX"
              :class="['size-5', data.overallVerified ? 'text-green-600' : 'text-destructive']" />
            <span class="text-sm font-medium">
              整体一致性：
              <span :class="data.overallVerified ? 'text-green-700' : 'text-destructive'">
                {{ data.overallVerified ? '通过' : '不通过' }}
              </span>
            </span>
          </div>
          <Separator class="my-4" />
          <div class="relative pl-6">
            <div class="absolute left-2 top-2 bottom-2 w-px bg-border" />
            <ul class="space-y-4">
              <li v-for="e in data.events" :key="e.version" class="relative">
                <span :class="['absolute -left-[18px] top-1.5 size-3 rounded-full ring-2 ring-background',
                  e.chainVerified ? 'bg-green-600' : 'bg-destructive']" />
                <div class="flex items-center gap-2 mb-1 flex-wrap">
                  <Badge variant="secondary">v={{ e.version }}</Badge>
                  <span :class="['text-xs font-medium px-2 py-0.5 rounded', eventColor[e.eventType] || 'bg-muted text-muted-foreground']">
                    {{ e.eventType }}
                  </span>
                  <span class="text-xs text-muted-foreground">{{ e.operatorRole }} (uid={{ e.operatorId }})</span>
                  <span class="text-xs text-muted-foreground ml-auto">{{ e.eventTime }}</span>
                </div>
                <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4 gap-y-1 text-xs text-muted-foreground">
                  <div class="flex items-center gap-1">dataHash: <Copyable :value="e.dataHash" short /></div>
                  <div class="flex items-center gap-1">txHash: <Copyable :value="e.txHash" short /></div>
                </div>
                <div class="text-[10px] mt-1">
                  <span :class="['inline-flex items-center gap-1', e.chainVerified ? 'text-green-700' : 'text-destructive']">
                    <component :is="e.chainVerified ? ShieldCheck : ShieldX" class="size-3" />
                    LifecycleTrace.verifyEvent(traceNumber, {{ e.version }}, dataHash) = {{ e.chainVerified }}
                  </span>
                </div>
              </li>
            </ul>
          </div>
        </CardContent>
      </Card>
    </template>

    <Card v-else-if="!errorMsg" class="border-dashed">
      <CardContent class="py-12 text-center text-sm text-muted-foreground space-y-2">
        <Link2 class="size-12 mx-auto opacity-30" />
        <p>输入溯源编号查看链上完整时间线</p>
      </CardContent>
    </Card>
  </div>
</template>
