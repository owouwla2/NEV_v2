<script setup lang="ts">
import { ref } from 'vue'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Separator } from '@/components/ui/separator'
import { Loader2, Search, BatteryFull, ShieldCheck, ShieldX, Link2, Leaf } from 'lucide-vue-next'
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
        <Search class="size-6" /> 电池查询
      </h1>
      <p class="text-sm text-muted-foreground mt-1">
        按溯源编号查询电池基础信息 + 完整链上事件时间线 + 碳足迹摘要
      </p>
    </div>

    <!-- 搜索区 -->
    <Card>
      <CardContent class="pt-6">
        <form class="flex items-end gap-3" @submit.prevent="onSearch">
          <div class="flex-1 space-y-1.5">
            <Label for="trace">溯源编号</Label>
            <Input
              id="trace"
              v-model="traceNumber"
              placeholder="BAT-DEMO-XXX"
              :disabled="loading"
            />
          </div>
          <Button type="submit" :disabled="loading">
            <Loader2 v-if="loading" class="size-4 animate-spin" />
            <Search v-else class="size-4" />
            <span>查询</span>
          </Button>
        </form>
      </CardContent>
    </Card>

    <Alert v-if="errorMsg" variant="destructive">
      <AlertTitle>查询失败</AlertTitle>
      <AlertDescription>{{ errorMsg }}</AlertDescription>
    </Alert>

    <!-- 结果 -->
    <template v-if="data">
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- 电池基础 -->
        <Card class="lg:col-span-2">
          <CardHeader class="flex flex-row items-start justify-between">
            <div>
              <CardTitle class="flex items-center gap-2">
                <BatteryFull class="size-5" /> {{ data.traceNumber }}
              </CardTitle>
              <CardDescription>{{ data.model }} · {{ data.capacityKwh }} kWh · {{ data.voltage }} V</CardDescription>
            </div>
            <a
              :href="`/scan/${encodeURIComponent(data.traceNumber)}`"
              target="_blank"
              class="text-xs inline-flex items-center gap-1 text-primary hover:underline"
            >
              <Link2 class="size-3" /> 公开扫码页
            </a>
          </CardHeader>
          <CardContent class="space-y-3 text-sm">
            <div class="grid grid-cols-2 gap-x-4 gap-y-2">
              <div><span class="text-muted-foreground">当前状态:</span> <Badge variant="outline">{{ data.currentStatus }}</Badge></div>
              <div><span class="text-muted-foreground">当前角色:</span> <Badge variant="outline">{{ data.currentRole }}</Badge></div>
              <div><span class="text-muted-foreground">出厂时间:</span> {{ data.producedAt }}</div>
              <div><span class="text-muted-foreground">事件总数:</span> {{ data.totalEvents }}</div>
            </div>
            <Separator />
            <div class="flex items-center gap-2">
              <component
                :is="data.overallVerified ? ShieldCheck : ShieldX"
                :class="[
                  'size-5',
                  data.overallVerified ? 'text-green-600' : 'text-destructive',
                ]"
              />
              <span class="text-sm font-medium">
                链上一致性：
                <span :class="data.overallVerified ? 'text-green-700' : 'text-destructive'">
                  {{ data.overallVerified ? '通过' : '不通过' }}
                </span>
              </span>
              <span class="text-xs text-muted-foreground">
                ({{ data.verifiedEvents }}/{{ data.totalEvents }} 条 verifyEvent=true · 链上 count={{ data.chainEventCount }})
              </span>
            </div>
          </CardContent>
        </Card>

        <!-- 碳足迹摘要 -->
        <Card v-if="data.carbonFootprint">
          <CardHeader>
            <CardTitle class="text-base flex items-center gap-2">
              <Leaf class="size-5 text-green-600" /> 碳足迹
            </CardTitle>
            <CardDescription>{{ data.carbonFootprint.calcMethod }} · {{ data.carbonFootprint.calcVersion }}</CardDescription>
          </CardHeader>
          <CardContent class="space-y-2 text-sm">
            <div class="text-2xl font-bold">
              {{ data.carbonFootprint.totalCo2Kg }}
              <span class="text-xs font-normal text-muted-foreground">kgCO2eq</span>
            </div>
            <Separator />
            <ul class="space-y-1.5">
              <li
                v-for="s in data.carbonFootprint.stages"
                :key="s.stage"
                class="flex items-center justify-between"
              >
                <Badge variant="outline" class="font-mono text-[10px]">{{ s.stage }}</Badge>
                <span class="text-xs">{{ s.co2Kg }} kg</span>
              </li>
            </ul>
          </CardContent>
        </Card>

        <Card v-else class="border-dashed">
          <CardHeader>
            <CardTitle class="text-base flex items-center gap-2">
              <Leaf class="size-5 opacity-40" /> 碳足迹
            </CardTitle>
            <CardDescription>该电池尚未计算（admin 可在 /carbon/footprint 触发）</CardDescription>
          </CardHeader>
        </Card>
      </div>

      <!-- 事件时间线 -->
      <Card>
        <CardHeader>
          <CardTitle class="text-base">链上事件时间线</CardTitle>
          <CardDescription>每条事件都已通过链上 verifyEvent 二次校验</CardDescription>
        </CardHeader>
        <CardContent>
          <div class="relative pl-6">
            <div class="absolute left-2 top-2 bottom-2 w-px bg-border"></div>
            <ul class="space-y-4">
              <li
                v-for="e in data.events"
                :key="e.version"
                class="relative"
              >
                <span class="absolute -left-[18px] top-1.5 size-3 rounded-full ring-2 ring-background"
                  :class="e.chainVerified ? 'bg-green-600' : 'bg-destructive'"
                ></span>
                <div class="flex items-center gap-2 mb-1">
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
                    chainVerified = {{ e.chainVerified }}
                  </span>
                </div>
              </li>
            </ul>
          </div>
        </CardContent>
      </Card>
    </template>
  </div>
</template>
