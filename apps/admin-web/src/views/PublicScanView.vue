<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { Separator } from '@/components/ui/separator'
import {
  Zap, ShieldCheck, ShieldX, Battery, Leaf, Link2, AlertCircle,
} from 'lucide-vue-next'
import Copyable from '@/components/Copyable.vue'
import { scanBattery, type BatteryScanVO } from '@/api/battery'

const route = useRoute()
const loading = ref(true)
const data = ref<BatteryScanVO | null>(null)
const errorMsg = ref('')

const eventLabels: Record<string, string> = {
  PRODUCED: '生产出厂',
  IN_USE: '经销商接收',
  SOLD: '零售售出',
  REPAIRED: '维修检测',
  RECYCLED: '回收处理',
  DISMANTLED: '拆解报废',
}

const eventColor: Record<string, string> = {
  PRODUCED: 'bg-blue-100 text-blue-700 dark:bg-blue-950 dark:text-blue-300',
  IN_USE: 'bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-300',
  SOLD: 'bg-purple-100 text-purple-700 dark:bg-purple-950 dark:text-purple-300',
  REPAIRED: 'bg-orange-100 text-orange-700 dark:bg-orange-950 dark:text-orange-300',
  RECYCLED: 'bg-green-100 text-green-700 dark:bg-green-950 dark:text-green-300',
  DISMANTLED: 'bg-zinc-200 text-zinc-700 dark:bg-zinc-800 dark:text-zinc-300',
}

const stageLabels: Record<string, string> = {
  RAW: '原材料',
  MFG: '制造',
  TRANS: '运输',
  USE: '使用',
  EOL: '回收',
}
const stageColor: Record<string, string> = {
  RAW: 'border-amber-200 dark:border-amber-900',
  MFG: 'border-blue-200 dark:border-blue-900',
  TRANS: 'border-purple-200 dark:border-purple-900',
  USE: 'border-orange-200 dark:border-orange-900',
  EOL: 'border-green-200 dark:border-green-900',
}

onMounted(async () => {
  const trace = String(route.params.trace || '')
  if (!trace) {
    errorMsg.value = '溯源编号为空'
    loading.value = false
    return
  }
  try {
    data.value = await scanBattery(trace)
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : String(err)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="min-h-screen bg-gradient-to-b from-zinc-50 to-zinc-100 dark:from-zinc-950 dark:to-zinc-900 pb-12">
    <!-- 顶部 Hero -->
    <div class="bg-zinc-900 dark:bg-zinc-950 text-zinc-100 px-4 py-6 sm:py-8">
      <div class="max-w-4xl mx-auto flex items-center gap-3">
        <div class="size-10 sm:size-12 rounded-lg bg-primary-foreground text-primary flex items-center justify-center">
          <Zap class="size-5 sm:size-6" />
        </div>
        <div>
          <h1 class="text-lg sm:text-xl font-bold">NEV-v2 公开扫码</h1>
          <p class="text-xs sm:text-sm opacity-70">区块链溯源 + 全生命周期碳足迹</p>
        </div>
      </div>
    </div>

    <div class="max-w-4xl mx-auto px-4 -mt-4 sm:-mt-6 space-y-4 sm:space-y-6">
      <!-- Loading -->
      <Card v-if="loading">
        <CardContent class="pt-6 space-y-3">
          <Skeleton class="h-8 w-2/3" />
          <Skeleton class="h-4 w-1/2" />
          <Skeleton class="h-32 w-full" />
        </CardContent>
      </Card>

      <!-- Error -->
      <Card v-else-if="errorMsg" class="border-destructive">
        <CardContent class="py-12 text-center space-y-3">
          <AlertCircle class="size-12 mx-auto text-destructive" />
          <h2 class="text-lg font-bold">无法查询</h2>
          <p class="text-sm text-muted-foreground">{{ errorMsg }}</p>
          <p class="text-xs text-muted-foreground">trace = <code class="px-1 py-0.5 rounded bg-muted">{{ route.params.trace }}</code></p>
        </CardContent>
      </Card>

      <!-- Data -->
      <template v-else-if="data">
        <!-- 整体一致性大标识 -->
        <Card :class="data.overallVerified ? 'border-green-300 dark:border-green-800' : 'border-destructive'">
          <CardContent class="py-6 flex items-center gap-4">
            <div :class="[
              'size-14 rounded-full flex items-center justify-center shrink-0',
              data.overallVerified
                ? 'bg-green-100 text-green-700 dark:bg-green-950 dark:text-green-300'
                : 'bg-red-100 text-red-700 dark:bg-red-950 dark:text-red-300'
            ]">
              <ShieldCheck v-if="data.overallVerified" class="size-7" />
              <ShieldX v-else class="size-7" />
            </div>
            <div class="flex-1 min-w-0">
              <h2 class="text-lg font-bold">
                {{ data.overallVerified ? '链上数据校验通过' : '检测到链上链下不一致' }}
              </h2>
              <p class="text-xs text-muted-foreground mt-0.5">
                共 {{ data.totalEvents }} 条事件，{{ data.verifiedEvents }} 条 verifyEvent=true
                （链上 count={{ data.chainEventCount ?? '?' }}）
              </p>
            </div>
          </CardContent>
        </Card>

        <!-- 电池基础 -->
        <Card>
          <CardHeader class="pb-3">
            <div class="flex items-center gap-2">
              <Battery class="size-5 text-primary" />
              <CardTitle class="text-base">{{ data.traceNumber }}</CardTitle>
            </div>
          </CardHeader>
          <CardContent class="space-y-2 text-sm">
            <div class="flex justify-between"><span class="text-muted-foreground">电池型号</span><span class="font-medium">{{ data.model }}</span></div>
            <div class="flex justify-between"><span class="text-muted-foreground">容量</span><span>{{ data.capacityKwh }} kWh</span></div>
            <div class="flex justify-between"><span class="text-muted-foreground">额定电压</span><span>{{ data.voltage }} V</span></div>
            <div class="flex justify-between"><span class="text-muted-foreground">出厂时间</span><span class="text-xs">{{ data.producedAt }}</span></div>
            <Separator />
            <div class="flex justify-between items-center">
              <span class="text-muted-foreground">当前状态</span>
              <span class="text-xs">
                <Badge variant="outline">{{ data.currentStatus }}</Badge>
                <span class="text-muted-foreground mx-1">/</span>
                <Badge variant="outline">{{ data.currentRole }}</Badge>
              </span>
            </div>
          </CardContent>
        </Card>

        <!-- 碳足迹 -->
        <Card v-if="data.carbonFootprint">
          <CardHeader class="pb-3">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <Leaf class="size-5 text-green-600" />
                <CardTitle class="text-base">碳足迹</CardTitle>
              </div>
              <span class="text-xs text-muted-foreground">{{ data.carbonFootprint.calcMethod }} {{ data.carbonFootprint.calcVersion }}</span>
            </div>
          </CardHeader>
          <CardContent class="space-y-3">
            <div class="text-3xl font-bold text-green-700 dark:text-green-400">
              {{ data.carbonFootprint.totalCo2Kg }}
              <span class="text-sm font-normal text-muted-foreground">kgCO2eq</span>
            </div>
            <div class="grid grid-cols-5 gap-2">
              <div
                v-for="s in data.carbonFootprint.stages"
                :key="s.stage"
                :class="['border rounded p-2 text-center', stageColor[s.stage] || 'border-border']"
              >
                <div class="text-[10px] text-muted-foreground">{{ stageLabels[s.stage] || s.stage }}</div>
                <div class="text-xs font-bold mt-1">{{ s.co2Kg }}</div>
              </div>
            </div>
            <p class="text-[10px] text-muted-foreground">
              5 阶段：原材料 / 制造 / 运输 / 使用 / 回收（EOL 负值表示通过回收抵扣的减排量）
            </p>
          </CardContent>
        </Card>

        <!-- 事件时间线 -->
        <Card>
          <CardHeader class="pb-3">
            <div class="flex items-center gap-2">
              <Link2 class="size-5" />
              <CardTitle class="text-base">链上事件时间线</CardTitle>
            </div>
          </CardHeader>
          <CardContent>
            <div class="relative pl-6">
              <div class="absolute left-2 top-2 bottom-2 w-px bg-border" />
              <ul class="space-y-4">
                <li v-for="e in data.events" :key="e.version" class="relative">
                  <span
                    :class="['absolute -left-[18px] top-1.5 size-3 rounded-full ring-2 ring-background',
                      e.chainVerified ? 'bg-green-600' : 'bg-destructive']"
                  />
                  <div class="flex items-center gap-2 mb-1 flex-wrap">
                    <Badge variant="secondary" class="text-[10px]">v={{ e.version }}</Badge>
                    <span :class="['text-xs font-medium px-2 py-0.5 rounded', eventColor[e.eventType] || 'bg-muted']">
                      {{ eventLabels[e.eventType] || e.eventType }}
                    </span>
                    <span class="text-[10px] text-muted-foreground">{{ e.operatorRole }}</span>
                    <span class="text-[10px] text-muted-foreground ml-auto">{{ e.eventTime }}</span>
                  </div>
                  <div class="text-[10px] text-muted-foreground space-y-0.5">
                    <div class="flex items-center gap-1 flex-wrap">
                      <span>tx:</span><Copyable :value="e.txHash" short />
                    </div>
                    <div class="flex items-center gap-1">
                      <component :is="e.chainVerified ? ShieldCheck : ShieldX" :class="['size-3', e.chainVerified ? 'text-green-700' : 'text-destructive']" />
                      <span :class="e.chainVerified ? 'text-green-700' : 'text-destructive'">
                        verifyEvent = {{ e.chainVerified }}
                      </span>
                    </div>
                  </div>
                </li>
              </ul>
            </div>
          </CardContent>
        </Card>

        <p class="text-[10px] text-center text-muted-foreground pt-2">
          NEV-v2 区块链溯源 · 数据来自 FISCO BCOS 主链 + MySQL 双写校验
        </p>
      </template>
    </div>
  </div>
</template>
