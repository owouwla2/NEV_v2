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
import { CheckSquare, RefreshCw, Loader2 } from 'lucide-vue-next'
import { toast } from 'vue-sonner'
import {
  recyclerPending, recyclerEvaluate, type TradeInVO,
} from '@/api/tradein'

const pending = ref<TradeInVO[]>([])
const loading = ref(false)
const dialogOpen = ref(false)
const submitting = ref(false)

const form = reactive({
  requestId: 0,
  requestNo: '',
  soh: '70',
  evaluatedAmount: '8000.00',
  summary: '',
})

async function load() {
  loading.value = true
  try {
    pending.value = (await recyclerPending()) || []
  } catch (err) {
    toast.error(err instanceof Error ? err.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function openEvaluate(r: TradeInVO) {
  form.requestId = r.id
  form.requestNo = r.requestNo
  form.soh = '70'
  form.evaluatedAmount = '8000.00'
  form.summary = ''
  dialogOpen.value = true
}

async function submit() {
  const soh = Number(form.soh)
  const amount = Number(form.evaluatedAmount)
  if (Number.isNaN(soh) || soh < 0 || soh > 100) return toast.error('SOH 必须在 0-100 之间')
  if (!amount || amount <= 0) return toast.error('评估金额必须 > 0')

  submitting.value = true
  try {
    await recyclerEvaluate({
      requestId: form.requestId,
      soh,
      evaluatedAmount: amount,
      summary: form.summary || undefined,
    })
    toast.success(`已评估 ${form.requestNo}（等 consumer 接受后自动上链）`)
    dialogOpen.value = false
    await load()
  } catch (err) {
    toast.error(err instanceof Error ? err.message : '评估失败')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold flex items-center gap-2">
          <CheckSquare class="size-6" /> 评估处理（recycler）
        </h1>
        <p class="text-sm text-muted-foreground mt-1">
          待评估的以旧换新申请（status=SUBMITTED）；评估后状态变 EVALUATED，等 consumer 接受触发链上 RECYCLED
        </p>
      </div>
      <Button variant="outline" :disabled="loading" @click="load">
        <RefreshCw class="size-4" :class="loading ? 'animate-spin' : ''" /> 刷新
      </Button>
    </div>

    <Card>
      <CardHeader><CardTitle class="text-base">待评估列表（共 {{ pending.length }}）</CardTitle></CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>申请单号</TableHead>
              <TableHead>消费者</TableHead>
              <TableHead>电池ID</TableHead>
              <TableHead>提交时间</TableHead>
              <TableHead>操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="pending.length === 0 && !loading">
              <TableCell colspan="5" class="text-center text-muted-foreground py-8">暂无待评估申请</TableCell>
            </TableRow>
            <TableRow v-for="r in pending" :key="r.id">
              <TableCell class="font-mono text-xs">{{ r.requestNo }}</TableCell>
              <TableCell>userId={{ r.consumerId }}</TableCell>
              <TableCell class="font-mono text-[10px]">{{ String(r.oldBatteryId).slice(-6) }}</TableCell>
              <TableCell class="text-xs">{{ r.submittedAt }}</TableCell>
              <TableCell>
                <Button size="sm" @click="openEvaluate(r)">
                  <CheckSquare class="size-3" /> 评估
                </Button>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </CardContent>
    </Card>

    <!-- 评估 Dialog -->
    <Dialog v-model:open="dialogOpen">
      <DialogContent class="max-w-md">
        <DialogHeader>
          <DialogTitle>评估 {{ form.requestNo }}</DialogTitle>
          <DialogDescription>填写 SOH 和评估金额；提交后等待消费者最终确认</DialogDescription>
        </DialogHeader>
        <div class="grid grid-cols-2 gap-3">
          <div class="space-y-1.5">
            <Label for="soh">SOH（%）*</Label>
            <Input id="soh" v-model="form.soh" type="number" min="0" max="100" :disabled="submitting" />
          </div>
          <div class="space-y-1.5">
            <Label for="amount">评估金额（¥）*</Label>
            <Input id="amount" v-model="form.evaluatedAmount" type="number" step="0.01" min="0" :disabled="submitting" />
          </div>
          <div class="space-y-1.5 col-span-2">
            <Label for="summary">评估说明</Label>
            <Textarea id="summary" v-model="form.summary" rows="3" placeholder="外观完好；电芯一致性 OK；可拆解回收" :disabled="submitting" />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="dialogOpen = false">取消</Button>
          <Button :disabled="submitting" @click="submit">
            <Loader2 v-if="submitting" class="size-4 animate-spin" />
            <span>{{ submitting ? '提交中...' : '提交评估' }}</span>
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>
