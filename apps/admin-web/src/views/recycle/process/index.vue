<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { http } from '@/lib/http'
import { RefreshCw } from 'lucide-vue-next'

interface TradeInVO {
  id: number
  requestNo: string
  oldBatteryTraceNumber?: string | null
  status: string
  soh?: number | null
  evaluatedAmount?: string | number | null
  evaluationSummary?: string | null
  evaluatedAt?: string | null
}

const STATUS_LABEL: Record<string, string> = {
  EVALUATED: '已评估·待接受',
  COMPLETED: '已完成回收',
}

const rows = ref<TradeInVO[]>([])
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    rows.value = (await http<TradeInVO[]>('/recycler/trade-in/processed')) ?? []
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
      <h1 class="text-2xl font-bold">回收处理</h1>
      <p class="text-sm text-muted-foreground mt-1">
        recycler 处理历史（需 recycler 角色登录）：已评估待消费者接受 + 已完成回收；待评估队列见「以旧换新 → 评估处理」
      </p>
    </div>
    <Card>
      <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-3">
        <div>
          <CardTitle class="text-base">处理记录</CardTitle>
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
                <TableHead>申请单号</TableHead>
                <TableHead>旧电池</TableHead>
                <TableHead>SOH</TableHead>
                <TableHead>评估价</TableHead>
                <TableHead>状态</TableHead>
                <TableHead>评估意见</TableHead>
                <TableHead>评估时间</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-if="rows.length === 0">
                <TableCell :colspan="7" class="text-center text-muted-foreground py-8">暂无处理记录</TableCell>
              </TableRow>
              <TableRow v-for="r in rows" :key="r.id">
                <TableCell class="font-mono text-xs">{{ r.requestNo }}</TableCell>
                <TableCell class="font-mono text-xs">{{ r.oldBatteryTraceNumber ?? '-' }}</TableCell>
                <TableCell>{{ r.soh ?? '-' }}%</TableCell>
                <TableCell>¥{{ r.evaluatedAmount ?? '-' }}</TableCell>
                <TableCell>
                  <Badge :variant="r.status === 'COMPLETED' ? 'default' : 'secondary'">
                    {{ STATUS_LABEL[r.status] ?? r.status }}
                  </Badge>
                </TableCell>
                <TableCell class="text-muted-foreground max-w-64 truncate">{{ r.evaluationSummary ?? '-' }}</TableCell>
                <TableCell>{{ r.evaluatedAt ?? '-' }}</TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </div>
      </CardContent>
    </Card>
  </div>
</template>