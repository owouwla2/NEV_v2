<script setup lang="ts">
import { onMounted, ref, computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle,
} from '@/components/ui/dialog'
import { ShoppingCart, RefreshCw, Trash2, Loader2, Plus, Minus, CreditCard } from 'lucide-vue-next'
import { toast } from 'vue-sonner'
import {
  consumerCartList, consumerCartUpdate, consumerCartRemove, consumerCartClear,
  consumerOrderCreate, type CartVO,
} from '@/api/marketplace'

const router = useRouter()
const cart = ref<CartVO | null>(null)
const loading = ref(false)
const busy = ref<Set<number>>(new Set())
const checkoutOpen = ref(false)
const submitting = ref(false)

const address = reactive({
  recipient: '张三',
  phone: '13800000105',
  province: '上海',
  city: '上海市',
  district: '浦东新区',
  detail: '陆家嘴示范门店 1F',
})
const remark = ref('')

const selectedItems = computed(() => {
  if (!cart.value) return [] as number[]
  const ids: number[] = []
  for (const g of cart.value.groups) {
    for (const it of g.items) if (it.selected === '1') ids.push(it.itemId)
  }
  return ids
})

async function loadCart() {
  loading.value = true
  try {
    cart.value = await consumerCartList()
  } catch (err) {
    toast.error(err instanceof Error ? err.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function updateQuantity(itemId: number, qty: number) {
  if (qty < 1) return
  busy.value.add(itemId)
  try {
    await consumerCartUpdate(itemId, qty)
    await loadCart()
  } catch (err) {
    toast.error(err instanceof Error ? err.message : '更新失败')
  } finally {
    busy.value.delete(itemId)
  }
}

async function toggleSelect(itemId: number, selected: boolean) {
  busy.value.add(itemId)
  try {
    await consumerCartUpdate(itemId, undefined, selected ? '1' : '0')
    await loadCart()
  } catch (err) {
    toast.error(err instanceof Error ? err.message : '更新失败')
  } finally {
    busy.value.delete(itemId)
  }
}

async function removeItem(itemId: number) {
  busy.value.add(itemId)
  try {
    await consumerCartRemove(itemId)
    toast.success('已删除')
    await loadCart()
  } catch (err) {
    toast.error(err instanceof Error ? err.message : '删除失败')
  } finally {
    busy.value.delete(itemId)
  }
}

async function clearAll() {
  if (!confirm('确定清空购物车？')) return
  try {
    await consumerCartClear()
    toast.success('已清空')
    await loadCart()
  } catch (err) {
    toast.error(err instanceof Error ? err.message : '清空失败')
  }
}

async function checkout() {
  if (selectedItems.value.length === 0) {
    toast.error('请至少选中一项')
    return
  }
  // 校验所有选中项是同一商家
  if (!cart.value) return
  let merchantId: number | null = null
  for (const g of cart.value.groups) {
    for (const it of g.items) {
      if (it.selected === '1') {
        if (merchantId === null) merchantId = g.merchantId
        else if (merchantId !== g.merchantId) {
          toast.error('一次下单仅支持单一商家的商品')
          return
        }
      }
    }
  }
  checkoutOpen.value = true
}

async function submitOrder() {
  submitting.value = true
  try {
    const order = await consumerOrderCreate({
      cartItemIds: selectedItems.value,
      address: { ...address },
      remark: remark.value || undefined,
    })
    toast.success(`下单成功，订单号 ${order.orderNo}`)
    checkoutOpen.value = false
    await loadCart()
    router.push('/marketplace/order')
  } catch (err) {
    toast.error(err instanceof Error ? err.message : '下单失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadCart)
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold flex items-center gap-2">
          <ShoppingCart class="size-6" /> 我的购物车
        </h1>
        <p class="text-sm text-muted-foreground mt-1">consumer 角色：按商家分组结算，单次下单仅支持同一商家</p>
      </div>
      <div class="flex gap-2">
        <Button variant="outline" :disabled="loading" @click="loadCart">
          <RefreshCw class="size-4" :class="loading ? 'animate-spin' : ''" /> 刷新
        </Button>
        <Button variant="outline" :disabled="loading || !cart || cart.groups.length === 0" @click="clearAll">
          <Trash2 class="size-4" /> 清空
        </Button>
      </div>
    </div>

    <Card v-if="cart && cart.groups.length === 0">
      <CardContent class="py-16 text-center text-sm text-muted-foreground space-y-3">
        <ShoppingCart class="size-12 mx-auto opacity-30" />
        <p>购物车空空如也</p>
        <Button variant="outline" size="sm" @click="router.push('/marketplace/product')">去逛逛</Button>
      </CardContent>
    </Card>

    <div v-else class="space-y-4">
      <Card v-for="g in cart?.groups || []" :key="g.cartId">
        <CardHeader>
          <CardTitle class="text-base flex items-center justify-between">
            <span>商家：{{ g.merchantName || `#${g.merchantId}` }}</span>
            <span class="text-sm font-normal text-muted-foreground">已选小计 ¥{{ g.selectedTotal }}</span>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div class="space-y-3">
            <div v-for="it in g.items" :key="it.itemId" class="flex items-center gap-3 py-2 border-b last:border-0">
              <input
                type="checkbox"
                :checked="it.selected === '1'"
                :disabled="busy.has(it.itemId)"
                class="size-4 accent-primary"
                @change="(e) => toggleSelect(it.itemId, (e.target as HTMLInputElement).checked)"
              />
              <div class="size-16 rounded bg-muted flex items-center justify-center shrink-0 overflow-hidden">
                <img v-if="it.images?.[0]" :src="it.images[0]" class="size-full object-cover" />
                <ShoppingCart v-else class="size-6 opacity-30" />
              </div>
              <div class="flex-1 min-w-0">
                <div class="text-sm font-medium truncate">{{ it.title || `#${String(it.productId).slice(-6)}` }}</div>
                <div class="text-xs text-muted-foreground">¥{{ it.unitPrice }} × {{ it.quantity }} = ¥{{ it.subtotal }}</div>
                <div v-if="it.productStatus !== 'ON_SALE'" class="text-xs text-destructive">
                  商品状态：{{ it.productStatus }}
                </div>
              </div>
              <div class="flex items-center gap-1">
                <Button variant="outline" size="icon" class="size-7"
                  :disabled="busy.has(it.itemId) || it.quantity <= 1"
                  @click="updateQuantity(it.itemId, it.quantity - 1)">
                  <Minus class="size-3" />
                </Button>
                <span class="w-8 text-center text-sm">{{ it.quantity }}</span>
                <Button variant="outline" size="icon" class="size-7"
                  :disabled="busy.has(it.itemId)"
                  @click="updateQuantity(it.itemId, it.quantity + 1)">
                  <Plus class="size-3" />
                </Button>
              </div>
              <Button variant="ghost" size="icon" class="size-8 text-destructive"
                :disabled="busy.has(it.itemId)" @click="removeItem(it.itemId)">
                <Trash2 class="size-4" />
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card v-if="cart && cart.groups.length > 0" class="sticky bottom-0">
        <CardContent class="py-4 flex items-center justify-between">
          <div class="text-sm">
            已选 {{ selectedItems.length }} 项
            <span class="ml-3 text-2xl font-bold text-primary">¥{{ cart.grandSelectedTotal }}</span>
          </div>
          <Button :disabled="selectedItems.length === 0" @click="checkout">
            <CreditCard class="size-4" /> 去结算
          </Button>
        </CardContent>
      </Card>
    </div>

    <!-- 结算 Dialog -->
    <Dialog v-model:open="checkoutOpen">
      <DialogContent class="max-w-lg">
        <DialogHeader>
          <DialogTitle>填写收货地址</DialogTitle>
          <DialogDescription>共 {{ selectedItems.length }} 项 · 应付 ¥{{ cart?.grandSelectedTotal }}</DialogDescription>
        </DialogHeader>
        <div class="grid grid-cols-2 gap-3">
          <div class="space-y-1"><Label>收件人 *</Label><Input v-model="address.recipient" :disabled="submitting" /></div>
          <div class="space-y-1"><Label>电话 *</Label><Input v-model="address.phone" :disabled="submitting" /></div>
          <div class="space-y-1"><Label>省</Label><Input v-model="address.province" :disabled="submitting" /></div>
          <div class="space-y-1"><Label>市</Label><Input v-model="address.city" :disabled="submitting" /></div>
          <div class="space-y-1"><Label>区</Label><Input v-model="address.district" :disabled="submitting" /></div>
          <div class="space-y-1 col-span-2"><Label>详细地址 *</Label><Input v-model="address.detail" :disabled="submitting" /></div>
          <div class="space-y-1 col-span-2"><Label>备注</Label><Textarea v-model="remark" rows="2" :disabled="submitting" /></div>
        </div>
        <DialogFooter>
          <Button variant="outline" :disabled="submitting" @click="checkoutOpen = false">取消</Button>
          <Button :disabled="submitting" @click="submitOrder">
            <Loader2 v-if="submitting" class="size-4 animate-spin" />
            <span>{{ submitting ? '下单中...' : '提交订单' }}</span>
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>
