<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Badge } from '@/components/ui/badge'
import {
  Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle,
} from '@/components/ui/dialog'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table'
import { Plus, Edit, RefreshCw, Loader2, Tag, ShoppingCart, Eye } from 'lucide-vue-next'
import { toast } from 'vue-sonner'
import {
  merchantMyProducts, merchantSaveProduct, publicProductList, consumerCartAdd,
  type ProductVO, type ProductSaveDTO,
} from '@/api/marketplace'

const userStore = useUserStore()
const isMerchant = computed(() => userStore.roles.includes('merchant'))
const isConsumer = computed(() => userStore.roles.includes('consumer'))

const merchantList = ref<ProductVO[]>([])
const publicList = ref<ProductVO[]>([])
const loading = ref(false)
const total = ref(0)

const filters = reactive({
  category: 'ALL',
  keyword: '',
})

const saving = ref(false)
const dialogOpen = ref(false)
const adding = ref<Set<number>>(new Set())

interface FormState {
  id: number | null
  title: string
  subtitle: string
  category: string
  price: string
  stock: string
  batteryId: string
  images: string
  description: string
  status: string
}
const blankForm: FormState = {
  id: null, title: '', subtitle: '', category: 'BATTERY',
  price: '0', stock: '0', batteryId: '', images: '', description: '', status: 'ON_SALE',
}
const form = reactive<FormState>({ ...blankForm })

async function loadList() {
  loading.value = true
  try {
    if (isMerchant.value) {
      merchantList.value = (await merchantMyProducts()) || []
    } else {
      const page = await publicProductList({
        category: filters.category === 'ALL' ? undefined : filters.category,
        keyword: filters.keyword || undefined,
        onSaleOnly: true,
        pageNum: 1,
        pageSize: 50,
      })
      publicList.value = page.records || []
      total.value = page.total || 0
    }
  } catch (err) {
    toast.error(err instanceof Error ? err.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, blankForm)
  dialogOpen.value = true
}

function openEdit(p: ProductVO) {
  form.id = p.id
  form.title = p.title
  form.subtitle = p.subtitle || ''
  form.category = p.category || 'BATTERY'
  form.price = String(p.price)
  form.stock = String(p.stock)
  form.batteryId = p.batteryId ? String(p.batteryId) : ''
  form.images = (p.images || []).join('\n')
  form.description = p.description || ''
  form.status = p.status
  dialogOpen.value = true
}

async function submitForm() {
  if (!form.title.trim()) return toast.error('标题必填')
  if (Number(form.price) <= 0) return toast.error('价格必须 > 0')
  const dto: ProductSaveDTO = {
    id: form.id ?? undefined,
    title: form.title.trim(),
    subtitle: form.subtitle || undefined,
    category: form.category,
    price: Number(form.price),
    stock: Number(form.stock),
    batteryId: form.batteryId ? Number(form.batteryId) : null,
    images: form.images.split('\n').map((s) => s.trim()).filter(Boolean),
    description: form.description || undefined,
    status: form.status,
  }
  saving.value = true
  try {
    await merchantSaveProduct(dto)
    toast.success(form.id ? '已更新' : '已上架')
    dialogOpen.value = false
    await loadList()
  } catch (err) {
    toast.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function addToCart(p: ProductVO) {
  adding.value.add(p.id)
  try {
    await consumerCartAdd(p.id, 1)
    toast.success(`已加入购物车：${p.title}`)
  } catch (err) {
    toast.error(err instanceof Error ? err.message : '加购失败')
  } finally {
    adding.value.delete(p.id)
  }
}

const statusVariant: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  ON_SALE: 'default', OFF_SHELF: 'outline', SOLD_OUT: 'destructive',
}

onMounted(loadList)
</script>

<template>
  <div class="space-y-6">
    <!-- merchant 视角 -->
    <template v-if="isMerchant">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold flex items-center gap-2">
            <Tag class="size-6" /> 商品管理
          </h1>
          <p class="text-sm text-muted-foreground mt-1">
            merchant 角色：上下架商品；可选绑定电池实现"电池即商品"，订单完成时自动触发链上 SOLD
          </p>
        </div>
        <div class="flex gap-2">
          <Button variant="outline" :disabled="loading" @click="loadList">
            <RefreshCw class="size-4" :class="loading ? 'animate-spin' : ''" /> 刷新
          </Button>
          <Button @click="openCreate"><Plus class="size-4" /> 上架新商品</Button>
        </div>
      </div>
      <Card>
        <CardHeader><CardTitle class="text-base">我的商品（共 {{ merchantList.length }}）</CardTitle></CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead><TableHead>类目</TableHead><TableHead>标题</TableHead>
                <TableHead class="text-right">价格</TableHead><TableHead class="text-right">库存</TableHead>
                <TableHead class="text-right">已售</TableHead><TableHead>绑定电池</TableHead>
                <TableHead>状态</TableHead><TableHead>操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-if="merchantList.length === 0 && !loading">
                <TableCell colspan="9" class="text-center text-muted-foreground py-8">
                  暂无商品，点右上角"上架新商品"开始
                </TableCell>
              </TableRow>
              <TableRow v-for="p in merchantList" :key="p.id">
                <TableCell class="font-mono text-xs">{{ String(p.id).slice(-6) }}</TableCell>
                <TableCell><Badge variant="outline">{{ p.category }}</Badge></TableCell>
                <TableCell class="max-w-xs">
                  <div class="truncate font-medium">{{ p.title }}</div>
                  <div v-if="p.subtitle" class="truncate text-xs text-muted-foreground">{{ p.subtitle }}</div>
                </TableCell>
                <TableCell class="text-right">¥{{ p.price }}</TableCell>
                <TableCell class="text-right">{{ p.stock }}</TableCell>
                <TableCell class="text-right">{{ p.salesCount ?? 0 }}</TableCell>
                <TableCell>
                  <code v-if="p.batteryId" class="text-[10px]">{{ String(p.batteryId).slice(-6) }}</code>
                  <span v-else class="text-muted-foreground text-xs">—</span>
                </TableCell>
                <TableCell><Badge :variant="statusVariant[p.status] || 'outline'">{{ p.status }}</Badge></TableCell>
                <TableCell><Button variant="ghost" size="sm" @click="openEdit(p)"><Edit class="size-3" /> 编辑</Button></TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </template>

    <!-- consumer / 其它 视角 -->
    <template v-else>
      <div class="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 class="text-2xl font-bold flex items-center gap-2">
            <ShoppingCart class="size-6" /> 商品浏览
          </h1>
          <p class="text-sm text-muted-foreground mt-1">
            浏览全平台在售商品；consumer 角色可加入购物车（D26）
          </p>
        </div>
        <div class="flex items-center gap-2">
          <Input v-model="filters.keyword" placeholder="搜索标题" class="w-40" />
          <Select v-model="filters.category">
            <SelectTrigger class="w-32"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">全部类目</SelectItem>
              <SelectItem value="BATTERY">BATTERY</SelectItem>
              <SelectItem value="ACCESSORY">ACCESSORY</SelectItem>
              <SelectItem value="SERVICE">SERVICE</SelectItem>
            </SelectContent>
          </Select>
          <Button variant="outline" :disabled="loading" @click="loadList">
            <RefreshCw class="size-4" :class="loading ? 'animate-spin' : ''" /> 搜索
          </Button>
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        <Card v-for="p in publicList" :key="p.id" class="overflow-hidden">
          <div class="aspect-[4/3] bg-muted flex items-center justify-center">
            <img v-if="p.images?.[0]" :src="p.images[0]" :alt="p.title" class="object-cover w-full h-full" @error="(e: any) => (e.target.style.display = 'none')" />
            <Tag v-else class="size-12 opacity-30" />
          </div>
          <CardContent class="p-3 space-y-2">
            <div class="flex items-center gap-2">
              <Badge variant="outline" class="text-[10px]">{{ p.category }}</Badge>
              <code v-if="p.batteryId" class="text-[10px] text-muted-foreground">电池 #{{ String(p.batteryId).slice(-6) }}</code>
            </div>
            <h3 class="text-sm font-medium line-clamp-2">{{ p.title }}</h3>
            <p v-if="p.subtitle" class="text-xs text-muted-foreground line-clamp-1">{{ p.subtitle }}</p>
            <div class="flex items-center justify-between pt-1">
              <span class="text-lg font-bold text-primary">¥{{ p.price }}</span>
              <span class="text-xs text-muted-foreground">库存 {{ p.stock }}</span>
            </div>
            <Button
              v-if="isConsumer"
              size="sm"
              class="w-full"
              :disabled="adding.has(p.id) || p.stock <= 0"
              @click="addToCart(p)"
            >
              <Loader2 v-if="adding.has(p.id)" class="size-3 animate-spin" />
              <ShoppingCart v-else class="size-3" />
              <span>{{ p.stock <= 0 ? '已售罄' : '加入购物车' }}</span>
            </Button>
            <Button v-else variant="outline" size="sm" class="w-full" disabled>
              <Eye class="size-3" /> 仅 consumer 可加购
            </Button>
          </CardContent>
        </Card>
        <Card v-if="publicList.length === 0 && !loading" class="col-span-full">
          <CardContent class="py-12 text-center text-sm text-muted-foreground">暂无在售商品</CardContent>
        </Card>
      </div>
    </template>

    <!-- merchant 编辑 Dialog -->
    <Dialog v-model:open="dialogOpen">
      <DialogContent class="max-w-2xl max-h-[90vh] overflow-auto">
        <DialogHeader>
          <DialogTitle>{{ form.id ? `编辑商品 #${String(form.id).slice(-6)}` : '上架新商品' }}</DialogTitle>
          <DialogDescription>类目：BATTERY 电池（绑 battery_id）/ ACCESSORY 配件 / SERVICE 服务</DialogDescription>
        </DialogHeader>
        <div class="grid grid-cols-2 gap-4">
          <div class="col-span-2 space-y-1.5">
            <Label for="title">标题 *</Label>
            <Input id="title" v-model="form.title" :disabled="saving" />
          </div>
          <div class="col-span-2 space-y-1.5">
            <Label for="subtitle">副标题</Label>
            <Input id="subtitle" v-model="form.subtitle" :disabled="saving" />
          </div>
          <div class="space-y-1.5">
            <Label>类目 *</Label>
            <Select v-model="form.category" :disabled="saving">
              <SelectTrigger><SelectValue /></SelectTrigger>
              <SelectContent>
                <SelectItem value="BATTERY">BATTERY 电池</SelectItem>
                <SelectItem value="ACCESSORY">ACCESSORY 配件</SelectItem>
                <SelectItem value="SERVICE">SERVICE 服务</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="space-y-1.5">
            <Label>状态</Label>
            <Select v-model="form.status" :disabled="saving">
              <SelectTrigger><SelectValue /></SelectTrigger>
              <SelectContent>
                <SelectItem value="ON_SALE">ON_SALE 在售</SelectItem>
                <SelectItem value="OFF_SHELF">OFF_SHELF 下架</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="space-y-1.5">
            <Label for="price">价格（¥）*</Label>
            <Input id="price" v-model="form.price" type="number" step="0.01" min="0" :disabled="saving" />
          </div>
          <div class="space-y-1.5">
            <Label for="stock">库存 *</Label>
            <Input id="stock" v-model="form.stock" type="number" min="0" :disabled="saving" />
          </div>
          <div class="col-span-2 space-y-1.5">
            <Label for="batteryId">绑定电池 ID（可空）</Label>
            <Input id="batteryId" v-model="form.batteryId" placeholder="留空=普通商品；填 nev_battery.id=电池即商品" :disabled="saving" />
          </div>
          <div class="col-span-2 space-y-1.5">
            <Label for="images">图片 URL（每行一个）</Label>
            <Textarea id="images" v-model="form.images" rows="3" :disabled="saving" />
          </div>
          <div class="col-span-2 space-y-1.5">
            <Label for="description">详情</Label>
            <Textarea id="description" v-model="form.description" rows="3" :disabled="saving" />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" :disabled="saving" @click="dialogOpen = false">取消</Button>
          <Button :disabled="saving" @click="submitForm">
            <Loader2 v-if="saving" class="size-4 animate-spin" />
            <span>{{ saving ? '保存中...' : (form.id ? '保存' : '上架') }}</span>
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>
