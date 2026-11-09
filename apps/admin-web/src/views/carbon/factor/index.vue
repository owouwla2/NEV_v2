<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { http } from '@/lib/http'
import { RefreshCw } from 'lucide-vue-next'

interface FactorVO {
  id: number
  factorCode: string
  factorName: string
  unit: string
  value: string | number
  source: string
  applicableStage: string
  validFrom: string
  validTo: string
}

const STAGE_ORDER = ['RAW', 'MFG', 'TRANS', 'USE', 'EOL']
const STAGE_LABEL: Record<string, string> = {
  RAW: '原材料获取 RAW',
  MFG: '生产制造 MFG',
  TRANS: '运输流通 TRANS',
  USE: '使用阶段 USE',
  EOL: '回收再生 EOL',
}

const rows = ref<FactorVO[]>([])
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const list = await http<FactorVO[]>('/public/carbon/factors')
    rows.value = (list ?? []).sort((a, b) => STAGE_ORDER.indexOf(a.applicableStage) - STAGE_ORDER.indexOf(b.applicableStage))
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="space-y-4">
    <div>
      <h1 class="text-2xl font-bold">排放因子库</h1>
      <p class="text-sm text-muted-foreground mt-1">GB/T 24067 五阶段排放因子（演示种子 15 条）；/admin/carbon/calc 按阶段取用计算碳足迹</p>
    </div>
    <Card>
      <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-3">
        <div>
          <CardTitle class="text-base">因子列表</CardTitle>
          <CardDescription>共 {{ rows.length }} 条</CardDescription>
        </div>
        <Button variant="outline" size="sm" :disabled="loading" @click="load">
          <RefreshCw class="size-4 mr-1" :class="{ 'animate-spin': loading }" /> 刷新
        </Button>
      </CardHeader>
      <CardContent>
        <div v-if="error" class="text-sm text-destructive">加载失败：{{ error }}</div>
        <div v-else class="rounded-md border overflow-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>阶段</TableHead>
                <TableHead>因子编码</TableHead>
                <TableHead>因子名称</TableHead>
                <TableHead>数值</TableHead>
                <TableHead>单位</TableHead>
                <TableHead>数据来源</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-for="f in rows" :key="f.id">
                <TableCell><Badge variant="secondary">{{ STAGE_LABEL[f.applicableStage] ?? f.applicableStage }}</Badge></TableCell>
                <TableCell class="font-mono text-xs">{{ f.factorCode }}</TableCell>
                <TableCell>{{ f.factorName }}</TableCell>
                <TableCell class="font-mono">{{ f.value }}</TableCell>
                <TableCell>{{ f.unit }}</TableCell>
                <TableCell class="text-muted-foreground">{{ f.source }}</TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </div>
      </CardContent>
    </Card>
  </div>
</template>