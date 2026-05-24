<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import {
  Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle,
} from '@/components/ui/dialog'
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table'
import { RefreshCw, Loader2, Receipt, Truck, Eye, CreditCard, X, CheckCircle2 } from 'lucide-vue-next'
import { toast } from 'vue-sonner'
import {
  merchantOrderList, merchantOrderDetail, merchantShip,
  consumerOrderList, consumerOrderDetail, consumerOrderPay, consumerOrderConfirm, consumerOrderCancel,
  type OrderVO,
} from '@/api/marketplace'

const userStore = useUserStore()
const isMerchant = computed(() => userStore.roles.includes('merchant'))
const isConsumer = computed(() => userStore.roles.includes('consumer'))

const list = ref<OrderVO[]>([])
const total = ref(0)
const loading = ref(false)
const status = ref<string>('ALL')
const detail = ref<OrderVO | null>(null)
const detailOpen = ref(false)
const acting = ref<Set<string>>(new Set())

async function loadList() {
  loading.value = true
  try {
    const params = {
      status: status.value === 'ALL' ? undefined : status.value,
      pageNum: 1,
      pageSize: 50,
    }
    const page = isMerchant.value
      ? await merchantOrderList(params)
      : await consumerOrderList(params)
    list.value = page.records || []
    total.value = page.total || 0
  } catch (err) {
    toast.error(err instanceof Error ? err.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function viewDetail(id: number) {
  try {
    detail.value = isMerchant.value
      ? await merchantOrderDetail(id)
      : await consumerOrderDetail(id)
    detailOpen.value = true
  } catch (err) {
    toast.error(err instanceof Error ? err.message : '加载失败')
  }
}

function actKey(id: number, action: string) {
  return `${id}-${action}`
}

async function withAction(id: number, action: string, fn: () => Promise<unknown>, ok: string) {
  const key = actKey(id, action)
  acting.value.add(key)
  try {
    await fn()
    toast.success(ok)
    await loadList()
  } catch (err) {
    toast.error(err instanceof Error ? err.message : '操作失败')
  } finally {
    acting.value.delete(key)
  }
}

const ship = (id: number) => withAction(id, 'ship', () => merchantShip(id), `订单 #${String(id).slice(-6)} 已发货`)
const pay = (id: number) => withAction(id, 'pay', () => consumerOrderPay(id), '支付成功')
const cancel = (id: number) => withAction(id, 'cancel', () => consumerOrderCancel(id, 'user-cancel'), '订单已取消')
const confirm = (id: number) => withAction(id, 'confirm', () => consumerOrderConfirm(id), '已确认收货（含链上 SOLD + 碳积分）')

const statusColors: Record<string, string> = {
  PENDING: 'bg-zinc-100 text-zinc-700',
  PAID: 'bg-blue-100 text-blue-700',
  SHIPPED: 'bg-amber-100 text-amber-700',
  DELIVERED: 'bg-purple-100 text-purple-700',
  COMPLETED: 'bg-green-100 text-green-700',
  CANCELLED: 'bg-red-100 text-red-700',
  REFUNDED: 'bg-red-100 text-red-700',
}

onMounted(loadList)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold flex items-center gap-2">
          <Receipt class="size-6" /> 订单管理
        </h1>
        <p v-if="isMerchant" class="text-sm text-muted-foreground mt-1">
          merchant 角色：查看自家订单 + 发货（PAID → SHIPPED）；消费者确认收货时自动触发链上 SOLD + 发碳积分
        </p>
        <p v-else class="text-sm text-muted-foreground mt-1">
          consumer 角色：我的订单 4 态推进（PENDING → PAID → SHIPPED → COMPLETED），含取消 / 确认收货
        </p>
      </div>
      <div class="flex items-center gap-2">
        <Select v-model="status" @update:model-value="loadList">
          <SelectTrigger class="w-36"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">全部状态</SelectItem>
            <SelectItem value="PENDING">PENDING</SelectItem>
            <SelectItem value="PAID">PAID</SelectItem>
            <SelectItem value="SHIPPED">SHIPPED</SelectItem>
            <SelectItem value="COMPLETED">COMPLETED</SelectItem>
            <SelectItem value="CANCELLED">CANCELLED</SelectItem>
          </SelectContent>
        </Select>
        <Button variant="outline" :disabled="loading" @click="loadList">
          <RefreshCw class="size-4" :class="loading ? 'animate-spin' : ''" /> 刷新
        </Button>
      </div>
    </div>

    <Card>
      <CardHeader><CardTitle class="text-base">订单列表（共 {{ total }} 单）</CardTitle></CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>订单号</TableHead>
              <TableHead>{{ isMerchant ? '买家 userId' : '商家 ID' }}</TableHead>
              <TableHead class="text-right">金额</TableHead>
              <TableHead>状态</TableHead>
              <TableHead>下单</TableHead>
              <TableHead>操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="list.length === 0 && !loading">
              <TableCell colspan="6" class="text-center text-muted-foreground py-8">暂无订单</TableCell>
            </TableRow>
            <TableRow v-for="o in list" :key="o.id">
              <TableCell class="font-mono text-xs">{{ o.orderNo }}</TableCell>
              <TableCell>{{ isMerchant ? o.userId : String(o.merchantId).slice(-6) }}</TableCell>
              <TableCell class="text-right">¥{{ o.totalAmount }}</TableCell>
              <TableCell>
                <span :class="['inline-flex items-center text-xs font-medium px-2 py-0.5 rounded', statusColors[o.status]]">
                  {{ o.status }}
                </span>
              </TableCell>
              <TableCell class="text-xs">{{ o.createTime || '—' }}</TableCell>
              <TableCell class="flex gap-1 flex-wrap">
                <Button variant="ghost" size="sm" @click="viewDetail(o.id)"><Eye class="size-3" /> 详情</Button>

                <!-- merchant 操作 -->
                <Button v-if="isMerchant && o.status === 'PAID'" size="sm"
                  :disabled="acting.has(actKey(o.id, 'ship'))" @click="ship(o.id)">
                  <Loader2 v-if="acting.has(actKey(o.id, 'ship'))" class="size-3 animate-spin" />
                  <Truck v-else class="size-3" /> 发货
                </Button>

                <!-- consumer 操作 -->
                <template v-if="isConsumer">
                  <Button v-if="o.status === 'PENDING'" size="sm"
                    :disabled="acting.has(actKey(o.id, 'pay'))" @click="pay(o.id)">
                    <Loader2 v-if="acting.has(actKey(o.id, 'pay'))" class="size-3 animate-spin" />
                    <CreditCard v-else class="size-3" /> 支付
                  </Button>
                  <Button v-if="o.status === 'PENDING'" variant="outline" size="sm"
                    :disabled="acting.has(actKey(o.id, 'cancel'))" @click="cancel(o.id)">
                    <X class="size-3" /> 取消
                  </Button>
                  <Button v-if="o.status === 'SHIPPED'" size="sm"
                    :disabled="acting.has(actKey(o.id, 'confirm'))" @click="confirm(o.id)">
                    <Loader2 v-if="acting.has(actKey(o.id, 'confirm'))" class="size-3 animate-spin" />
                    <CheckCircle2 v-else class="size-3" /> 确认收货
                  </Button>
                </template>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </CardContent>
    </Card>

    <Dialog v-model:open="detailOpen">
      <DialogContent class="max-w-2xl max-h-[90vh] overflow-auto">
        <DialogHeader>
          <DialogTitle>订单详情 {{ detail?.orderNo }}</DialogTitle>
          <DialogDescription>状态: {{ detail?.status }}</DialogDescription>
        </DialogHeader>
        <div v-if="detail" class="space-y-4">
          <div class="grid grid-cols-2 gap-3 text-sm">
            <div><span class="text-muted-foreground">总额：</span>¥{{ detail.totalAmount }}</div>
            <div><span class="text-muted-foreground">实付：</span>{{ detail.payAmount ? `¥${detail.payAmount}` : '—' }}</div>
            <div><span class="text-muted-foreground">下单：</span>{{ detail.createTime || '—' }}</div>
            <div><span class="text-muted-foreground">支付：</span>{{ detail.paidAt || '—' }}</div>
            <div><span class="text-muted-foreground">发货：</span>{{ detail.shippedAt || '—' }}</div>
            <div><span class="text-muted-foreground">送达：</span>{{ detail.deliveredAt || '—' }}</div>
            <div><span class="text-muted-foreground">完成：</span>{{ detail.completedAt || '—' }}</div>
            <div v-if="detail.cancelledAt"><span class="text-muted-foreground">取消：</span>{{ detail.cancelledAt }}（{{ detail.cancelReason }}）</div>
          </div>
          <div class="space-y-1.5">
            <div class="text-sm font-medium">收货地址</div>
            <div v-if="detail.address" class="text-xs text-muted-foreground p-3 bg-muted rounded">
              <div>{{ detail.address.recipient }} / {{ detail.address.phone }}</div>
              <div>{{ detail.address.province }} {{ detail.address.city }} {{ detail.address.district }}</div>
              <div>{{ detail.address.detail }}</div>
            </div>
            <div v-else class="text-xs text-muted-foreground">—</div>
          </div>
          <div class="space-y-2">
            <div class="text-sm font-medium">商品明细（{{ detail.items?.length || 0 }} 项）</div>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>商品</TableHead>
                  <TableHead>类目</TableHead>
                  <TableHead class="text-right">单价</TableHead>
                  <TableHead class="text-right">数量</TableHead>
                  <TableHead class="text-right">小计</TableHead>
                  <TableHead>电池</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-for="it in detail.items || []" :key="it.productId">
                  <TableCell class="text-xs">{{ it.title || `#${String(it.productId).slice(-6)}` }}</TableCell>
                  <TableCell><Badge variant="outline">{{ it.category || '—' }}</Badge></TableCell>
                  <TableCell class="text-right text-xs">¥{{ it.unitPrice }}</TableCell>
                  <TableCell class="text-right text-xs">{{ it.quantity }}</TableCell>
                  <TableCell class="text-right text-xs">¥{{ it.subtotal }}</TableCell>
                  <TableCell><code v-if="it.batteryId" class="text-[10px]">{{ String(it.batteryId).slice(-6) }}</code><span v-else class="text-muted-foreground text-xs">—</span></TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  </div>
</template>
