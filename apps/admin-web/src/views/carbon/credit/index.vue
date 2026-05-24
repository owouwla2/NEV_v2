<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table'
import { Leaf, RefreshCw, TrendingUp, TrendingDown, Wallet } from 'lucide-vue-next'
import { toast } from 'vue-sonner'
import { consumerCarbonAccount, type CarbonAccountVO } from '@/api/carbon'

const account = ref<CarbonAccountVO | null>(null)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    account.value = await consumerCarbonAccount()
  } catch (err) {
    toast.error(err instanceof Error ? err.message : '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="space-y-6 max-w-5xl">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold flex items-center gap-2">
          <Leaf class="size-6 text-green-600" /> 碳积分账户
        </h1>
        <p class="text-sm text-muted-foreground mt-1">
          consumer 角色：每完成一笔含电池的订单，自动按"该电池 EOL 阶段减排量"发放积分
        </p>
      </div>
      <Button variant="outline" :disabled="loading" @click="load">
        <RefreshCw class="size-4" :class="loading ? 'animate-spin' : ''" /> 刷新
      </Button>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <Card>
        <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle class="text-sm font-medium text-muted-foreground">当前余额</CardTitle>
          <Wallet class="size-4 text-green-600" />
        </CardHeader>
        <CardContent>
          <div class="text-3xl font-bold">{{ account?.balance || '0.0000' }}</div>
          <p class="text-xs text-muted-foreground mt-1">kgCO2eq（可用）</p>
        </CardContent>
      </Card>
      <Card>
        <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle class="text-sm font-medium text-muted-foreground">累计获得</CardTitle>
          <TrendingUp class="size-4 text-blue-600" />
        </CardHeader>
        <CardContent>
          <div class="text-3xl font-bold">{{ account?.totalEarned || '0.0000' }}</div>
          <p class="text-xs text-muted-foreground mt-1">来自 ORDER_COMPLETE / TRADE_IN 等</p>
        </CardContent>
      </Card>
      <Card>
        <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle class="text-sm font-medium text-muted-foreground">累计消耗</CardTitle>
          <TrendingDown class="size-4 text-orange-600" />
        </CardHeader>
        <CardContent>
          <div class="text-3xl font-bold">{{ account?.totalSpent || '0.0000' }}</div>
          <p class="text-xs text-muted-foreground mt-1">冻结中: {{ account?.frozen || '0.0000' }}</p>
        </CardContent>
      </Card>
    </div>

    <Card>
      <CardHeader>
        <CardTitle class="text-base">最近流水</CardTitle>
        <CardDescription>展示最近 20 条 nev_carbon_credit_record 记录</CardDescription>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>时间</TableHead>
              <TableHead>原因</TableHead>
              <TableHead>关联</TableHead>
              <TableHead class="text-right">变更</TableHead>
              <TableHead class="text-right">变更后余额</TableHead>
              <TableHead>备注</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="!account || account.recentRecords.length === 0">
              <TableCell colspan="6" class="text-center text-muted-foreground py-8">
                {{ loading ? '加载中...' : '暂无流水' }}
              </TableCell>
            </TableRow>
            <TableRow v-for="r in account?.recentRecords || []" :key="r.id">
              <TableCell class="text-xs">{{ r.createTime }}</TableCell>
              <TableCell><Badge variant="outline">{{ r.reason }}</Badge></TableCell>
              <TableCell class="text-xs">
                <span v-if="r.relatedType">{{ r.relatedType }} #{{ String(r.relatedId).slice(-6) }}</span>
                <span v-else class="text-muted-foreground">—</span>
              </TableCell>
              <TableCell class="text-right">
                <span :class="Number(r.changeAmount) >= 0 ? 'text-green-600' : 'text-destructive'">
                  {{ Number(r.changeAmount) >= 0 ? '+' : '' }}{{ r.changeAmount }}
                </span>
              </TableCell>
              <TableCell class="text-right text-sm font-medium">{{ r.balanceAfter }}</TableCell>
              <TableCell class="text-xs text-muted-foreground max-w-xs truncate">{{ r.remark }}</TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  </div>
</template>
