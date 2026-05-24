<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'

import {
  Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle,
} from '@/components/ui/dialog'
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table'
import { Repeat2, Plus, RefreshCw, Loader2, Check, X } from 'lucide-vue-next'
import { toast } from 'vue-sonner'
import Copyable from '@/components/Copyable.vue'
import {
  consumerTradeInList, consumerTradeInSubmit, consumerTradeInAccept, consumerTradeInReject,
  type TradeInVO,
} from '@/api/tradein'

const list = ref<TradeInVO[]>([])
const loading = ref(false)
const dialogOpen = ref(false)
const submitting = ref(false)
const acting = ref<Set<string>>(new Set())

const form = reactive({
  oldBatteryTraceNumber: '',
  remark: '',
})

async function load() {
  loading.value = true
  try {
    list.value = (await consumerTradeInList()) || []
  } catch (err) {
    toast.error(err instanceof Error ? err.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.oldBatteryTraceNumber = ''
  form.remark = ''
  dialogOpen.value = true
}

async function submit() {
  if (!form.oldBatteryTraceNumber.trim()) return toast.error('请输入老电池溯源编号')
  submitting.value = true
  try {
    await consumerTradeInSubmit({
      oldBatteryTraceNumber: form.oldBatteryTraceNumber.trim(),
      remark: form.remark || undefined,
    })
    toast.success('申请已提交，等待 recycler 评估')
    dialogOpen.value = false
    await load()
  } catch (err) {
    toast.error(err instanceof Error ? err.message : '提交失败')
  } finally {
    submitting.value = false
  }
}

function actKey(id: number, action: string) { return `${id}-${action}` }

async function accept(id: number) {
  const k = actKey(id, 'accept')
  acting.value.add(k)
  try {
    await consumerTradeInAccept(id)
    toast.success('已接受 - 链上 RECYCLED 已写入')
    await load()
  } catch (err) {
    toast.error(err instanceof Error ? err.message : '操作失败')
  } finally {
    acting.value.delete(k)
  }
}

async function reject(id: number) {
  const k = actKey(id, 'reject')
  acting.value.add(k)
  try {
    await consumerTradeInReject(id, 'user-reject')
    toast.success('已拒绝')
    await load()
  } catch (err) {
    toast.error(err instanceof Error ? err.message : '操作失败')
  } finally {
    acting.value.delete(k)
  }
}

const statusColors: Record<string, string> = {
  SUBMITTED: 'bg-zinc-100 text-zinc-700',
  EVALUATED: 'bg-amber-100 text-amber-700',
  ACCEPTED: 'bg-blue-100 text-blue-700',
  COMPLETED: 'bg-green-100 text-green-700',
  REJECTED: 'bg-red-100 text-red-700',
}

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold flex items-center gap-2">
          <Repeat2 class="size-6" /> 以旧换新申请
        </h1>
        <p class="text-sm text-muted-foreground mt-1">
          consumer 角色：提交老电池换新申请 → recycler 评估 → 接受后链上自动写入 RECYCLED → COMPLETED
        </p>
      </div>
      <div class="flex gap-2">
        <Button variant="outline" :disabled="loading" @click="load">
          <RefreshCw class="size-4" :class="loading ? 'animate-spin' : ''" /> 刷新
        </Button>
        <Button @click="openCreate"><Plus class="size-4" /> 提交新申请</Button>
      </div>
    </div>

    <Card>
      <CardHeader><CardTitle class="text-base">我的换新申请（共 {{ list.length }}）</CardTitle></CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>申请单号</TableHead>
              <TableHead>电池ID</TableHead>
              <TableHead>评估 SOH</TableHead>
              <TableHead class="text-right">评估金额</TableHead>
              <TableHead>状态</TableHead>
              <TableHead>提交</TableHead>
              <TableHead>评估</TableHead>
              <TableHead>完成</TableHead>
              <TableHead>链上 tx</TableHead>
              <TableHead>操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="list.length === 0 && !loading">
              <TableCell colspan="10" class="text-center text-muted-foreground py-8">暂无申请</TableCell>
            </TableRow>
            <TableRow v-for="r in list" :key="r.id">
              <TableCell class="font-mono text-xs">{{ r.requestNo }}</TableCell>
              <TableCell class="font-mono text-[10px]">{{ String(r.oldBatteryId).slice(-6) }}</TableCell>
              <TableCell>{{ r.soh != null ? `${r.soh}%` : '—' }}</TableCell>
              <TableCell class="text-right">{{ r.evaluatedAmount ? `¥${r.evaluatedAmount}` : '—' }}</TableCell>
              <TableCell>
                <span :class="['inline-flex items-center text-xs font-medium px-2 py-0.5 rounded', statusColors[r.status]]">
                  {{ r.status }}
                </span>
              </TableCell>
              <TableCell class="text-xs">{{ r.submittedAt || '—' }}</TableCell>
              <TableCell class="text-xs">{{ r.evaluatedAt || '—' }}</TableCell>
              <TableCell class="text-xs">{{ r.completedAt || '—' }}</TableCell>
              <TableCell>
                <Copyable v-if="r.chainTxHash" :value="r.chainTxHash" short />
                <span v-else class="text-muted-foreground text-xs">—</span>
              </TableCell>
              <TableCell class="flex gap-1">
                <template v-if="r.status === 'EVALUATED'">
                  <Button size="sm" :disabled="acting.has(actKey(r.id, 'accept'))" @click="accept(r.id)">
                    <Loader2 v-if="acting.has(actKey(r.id, 'accept'))" class="size-3 animate-spin" />
                    <Check v-else class="size-3" /> 接受
                  </Button>
                  <Button variant="outline" size="sm" :disabled="acting.has(actKey(r.id, 'reject'))" @click="reject(r.id)">
                    <X class="size-3" /> 拒绝
                  </Button>
                </template>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </CardContent>
    </Card>

    <!-- 提交 Dialog -->
    <Dialog v-model:open="dialogOpen">
      <DialogContent class="max-w-md">
        <DialogHeader>
          <DialogTitle>提交以旧换新申请</DialogTitle>
          <DialogDescription>需输入您当前持有的电池溯源编号；后端会校验电池属于您且未在换新流程中</DialogDescription>
        </DialogHeader>
        <div class="space-y-3">
          <div class="space-y-1.5">
            <Label for="trace">老电池溯源编号 *</Label>
            <Input id="trace" v-model="form.oldBatteryTraceNumber" placeholder="BAT-DEMO-XXX" :disabled="submitting" />
          </div>
          <div class="space-y-1.5">
            <Label for="remark">备注</Label>
            <Textarea id="remark" v-model="form.remark" rows="3" :disabled="submitting" />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="dialogOpen = false">取消</Button>
          <Button :disabled="submitting" @click="submit">
            <Loader2 v-if="submitting" class="size-4 animate-spin" />
            <span>{{ submitting ? '提交中...' : '提交' }}</span>
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>
