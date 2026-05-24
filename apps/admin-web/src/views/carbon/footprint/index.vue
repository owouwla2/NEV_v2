<script setup lang="ts">
import { computed, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table'
import { Leaf, Loader2, Search, Zap } from 'lucide-vue-next'
import { toast } from 'vue-sonner'
import { adminCalcCarbon, publicCarbon, type CarbonFootprintVO } from '@/api/carbon'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.roles.includes('superadmin'))

const traceNumber = ref('')
const loading = ref(false)
const calcing = ref(false)
const data = ref<CarbonFootprintVO | null>(null)
const errorMsg = ref('')

async function query() {
  if (!traceNumber.value.trim()) return toast.error('请输入溯源编号')
  loading.value = true
  errorMsg.value = ''
  try {
    data.value = await publicCarbon(traceNumber.value.trim())
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : String(err)
    data.value = null
  } finally {
    loading.value = false
  }
}

async function calc() {
  if (!traceNumber.value.trim()) return toast.error('请输入溯源编号')
  calcing.value = true
  errorMsg.value = ''
  try {
    data.value = await adminCalcCarbon(traceNumber.value.trim())
    toast.success(`已计算：${data.value.totalCo2Kg} kgCO2eq`)
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : String(err)
    toast.error(errorMsg.value)
  } finally {
    calcing.value = false
  }
}

const stageColors: Record<string, string> = {
  RAW: 'bg-amber-50 text-amber-700 dark:bg-amber-950 dark:text-amber-300',
  MFG: 'bg-blue-50 text-blue-700 dark:bg-blue-950 dark:text-blue-300',
  TRANS: 'bg-purple-50 text-purple-700 dark:bg-purple-950 dark:text-purple-300',
  USE: 'bg-orange-50 text-orange-700 dark:bg-orange-950 dark:text-orange-300',
  EOL: 'bg-green-50 text-green-700 dark:bg-green-950 dark:text-green-300',
}
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold flex items-center gap-2">
        <Leaf class="size-6 text-green-600" /> 碳足迹查询
      </h1>
      <p class="text-sm text-muted-foreground mt-1">
        按 GB-T 24067 标准 5 阶段碳排放：RAW（原材料）/ MFG（制造）/ TRANS（运输）/ USE（使用）/ EOL（报废回收）
      </p>
    </div>

    <Card>
      <CardContent class="pt-6">
        <form class="flex items-end gap-3" @submit.prevent="query">
          <div class="flex-1 space-y-1.5">
            <Label for="trace">溯源编号</Label>
            <Input id="trace" v-model="traceNumber" placeholder="BAT-DEMO-XXX" :disabled="loading || calcing" />
          </div>
          <Button type="submit" :disabled="loading || calcing">
            <Loader2 v-if="loading" class="size-4 animate-spin" />
            <Search v-else class="size-4" />
            查询
          </Button>
          <Button v-if="isAdmin" type="button" variant="outline" :disabled="loading || calcing" @click="calc">
            <Loader2 v-if="calcing" class="size-4 animate-spin" />
            <Zap v-else class="size-4" />
            {{ calcing ? '计算中...' : 'admin 触发计算' }}
          </Button>
        </form>
      </CardContent>
    </Card>

    <Alert v-if="errorMsg" variant="destructive">
      <AlertTitle>查询失败</AlertTitle>
      <AlertDescription>{{ errorMsg }}</AlertDescription>
    </Alert>

    <template v-if="data">
      <Card class="border-green-200 dark:border-green-900">
        <CardHeader>
          <div class="flex items-center justify-between">
            <div>
              <CardTitle class="text-base">{{ data.traceNumber }}</CardTitle>
              <CardDescription>
                {{ data.batteryModel }} · {{ data.cellType }} · {{ data.capacityKwh }} kWh
                · 算法 {{ data.calcMethod }} {{ data.calcVersion }}
              </CardDescription>
            </div>
            <div class="text-right">
              <div class="text-3xl font-bold text-green-700">{{ data.totalCo2Kg }}</div>
              <p class="text-xs text-muted-foreground mt-1">kgCO2eq · 全生命周期</p>
            </div>
          </div>
        </CardHeader>
      </Card>

      <div class="grid grid-cols-1 md:grid-cols-5 gap-3">
        <Card v-for="s in data.stages" :key="s.stage" :class="['border-0', stageColors[s.stage] || '']">
          <CardContent class="pt-6 pb-4">
            <Badge variant="outline" class="font-mono text-[10px]">{{ s.stage }}</Badge>
            <div class="mt-2 text-2xl font-bold">
              {{ Number(s.co2Kg) >= 0 ? '' : '' }}{{ s.co2Kg }}
            </div>
            <p class="text-[10px] opacity-70 mt-1">kgCO2eq · {{ s.breakdown.length }} 项因子</p>
          </CardContent>
        </Card>
      </div>

      <Card v-for="s in data.stages" :key="`detail-${s.stage}`">
        <CardHeader>
          <CardTitle class="text-base flex items-center gap-2">
            <Badge variant="outline" class="font-mono">{{ s.stage }}</Badge>
            <span>小计 {{ s.co2Kg }} kgCO2eq</span>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>因子编码</TableHead>
                <TableHead>名称</TableHead>
                <TableHead class="text-right">输入</TableHead>
                <TableHead class="text-right">因子值</TableHead>
                <TableHead class="text-right">CO2 (kg)</TableHead>
                <TableHead>说明</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-for="it in s.breakdown" :key="it.factorCode">
                <TableCell class="font-mono text-[10px]">{{ it.factorCode }}</TableCell>
                <TableCell class="text-xs">{{ it.factorName }}</TableCell>
                <TableCell class="text-right text-xs">{{ it.input }} {{ it.inputUnit }}</TableCell>
                <TableCell class="text-right text-xs">× {{ it.factorValue }} {{ it.factorUnit }}</TableCell>
                <TableCell class="text-right text-sm font-medium">{{ it.co2Kg }}</TableCell>
                <TableCell class="text-xs text-muted-foreground">{{ it.note }}</TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </template>

    <Card v-if="!data && !errorMsg" class="border-dashed">
      <CardContent class="py-12 text-center text-sm text-muted-foreground space-y-2">
        <Leaf class="size-12 mx-auto opacity-30 text-green-600" />
        <p>输入溯源编号查询完整碳足迹</p>
        <p v-if="isAdmin" class="text-xs">admin 可点 "触发计算" 按钮重算（覆盖已有结果）</p>
      </CardContent>
    </Card>
  </div>
</template>
