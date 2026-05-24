<script setup lang="ts">
import { reactive, ref } from 'vue'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Separator } from '@/components/ui/separator'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Loader2, Zap, CheckCircle2, Battery, Link2, RotateCcw } from 'lucide-vue-next'
import { toast } from 'vue-sonner'
import { registerBattery, type BatteryRegisterVO } from '@/api/battery'
import Copyable from '@/components/Copyable.vue'

interface FormState {
  traceNumber: string
  model: string
  serialNo: string
  capacityKwh: string
  voltage: string
  cellType: string
  cellSupplier: string
  bmsInfo: string
  remark: string
}

const form = reactive<FormState>({
  traceNumber: `BAT-DEMO-${Date.now().toString().slice(-6)}`,
  model: 'NEV-LFP-280-2026',
  serialNo: `SN-${Date.now().toString().slice(-8)}`,
  capacityKwh: '85.000',
  voltage: '400.00',
  cellType: 'LFP',
  cellSupplier: '宁德时代',
  bmsInfo: 'BYD-BMS-v3.2',
  remark: '',
})

const loading = ref(false)
const result = ref<BatteryRegisterVO | null>(null)
const errorMsg = ref('')

function validate(): string | null {
  if (!form.traceNumber.trim()) return '溯源编号必填'
  if (!form.model.trim()) return '电池型号必填'
  if (!form.serialNo.trim()) return '电芯序列号必填'
  const cap = Number(form.capacityKwh)
  if (!cap || cap <= 0) return '容量必须 > 0 kWh'
  return null
}

async function onSubmit(e: Event) {
  e.preventDefault()
  const v = validate()
  if (v) {
    toast.error(v)
    return
  }
  loading.value = true
  errorMsg.value = ''
  result.value = null
  try {
    const data = await registerBattery({
      traceNumber: form.traceNumber.trim(),
      model: form.model.trim(),
      serialNo: form.serialNo.trim(),
      capacityKwh: Number(form.capacityKwh),
      voltage: form.voltage ? Number(form.voltage) : undefined,
      cellType: form.cellType || undefined,
      cellSupplier: form.cellSupplier || undefined,
      bmsInfo: form.bmsInfo || undefined,
      remark: form.remark || undefined,
    })
    result.value = data
    toast.success(`电池 ${data.traceNumber} 已注册并上链`)
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : String(err)
    errorMsg.value = msg
    toast.error(`注册失败：${msg}`)
  } finally {
    loading.value = false
  }
}

function resetForm() {
  result.value = null
  errorMsg.value = ''
  form.traceNumber = `BAT-DEMO-${Date.now().toString().slice(-6)}`
  form.serialNo = `SN-${Date.now().toString().slice(-8)}`
}
</script>

<template>
  <div class="space-y-6 max-w-5xl">
    <!-- 标题 -->
    <div>
      <h1 class="text-2xl font-bold flex items-center gap-2">
        <Battery class="size-6" /> 电池注册
      </h1>
      <p class="text-sm text-muted-foreground mt-1">
        producer 角色：录入电池规格 → 计算 keccak256 dataHash → 调用
        <code class="text-xs px-1 py-0.5 rounded bg-muted">LifecycleTrace.registerBattery</code>
        + 写入 v=1 PRODUCED 事件
      </p>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- 左侧表单（占 2 列） -->
      <Card class="lg:col-span-2">
        <CardHeader>
          <CardTitle class="text-base">电池信息</CardTitle>
          <CardDescription>带 * 为必填项；规格字段会按规约拼成 keccak256 输入</CardDescription>
        </CardHeader>
        <CardContent>
          <form @submit="onSubmit" class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="space-y-1.5">
              <Label for="traceNumber">溯源编号 *</Label>
              <Input
                id="traceNumber"
                v-model="form.traceNumber"
                placeholder="BAT-DEMO-XXX"
                :disabled="loading"
              />
            </div>
            <div class="space-y-1.5">
              <Label for="model">电池型号 *</Label>
              <Input
                id="model"
                v-model="form.model"
                placeholder="NEV-LFP-280-2026"
                :disabled="loading"
              />
            </div>
            <div class="space-y-1.5">
              <Label for="serialNo">电芯序列号 *</Label>
              <Input id="serialNo" v-model="form.serialNo" :disabled="loading" />
            </div>
            <div class="space-y-1.5">
              <Label for="cellType">电芯类型</Label>
              <Select v-model="form.cellType" :disabled="loading">
                <SelectTrigger id="cellType">
                  <SelectValue placeholder="选择" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="LFP">LFP（磷酸铁锂）</SelectItem>
                  <SelectItem value="NCM">NCM（三元）</SelectItem>
                  <SelectItem value="NCA">NCA（镍钴铝）</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div class="space-y-1.5">
              <Label for="capacityKwh">容量（kWh） *</Label>
              <Input
                id="capacityKwh"
                v-model="form.capacityKwh"
                type="number"
                step="0.001"
                min="0"
                :disabled="loading"
              />
            </div>
            <div class="space-y-1.5">
              <Label for="voltage">额定电压（V）</Label>
              <Input
                id="voltage"
                v-model="form.voltage"
                type="number"
                step="0.01"
                :disabled="loading"
              />
            </div>
            <div class="space-y-1.5">
              <Label for="cellSupplier">电芯供应商</Label>
              <Input id="cellSupplier" v-model="form.cellSupplier" :disabled="loading" />
            </div>
            <div class="space-y-1.5">
              <Label for="bmsInfo">BMS 信息</Label>
              <Input id="bmsInfo" v-model="form.bmsInfo" :disabled="loading" />
            </div>
            <div class="space-y-1.5 md:col-span-2">
              <Label for="remark">备注</Label>
              <Textarea id="remark" v-model="form.remark" rows="2" :disabled="loading" />
            </div>

            <div class="md:col-span-2 flex items-center justify-end gap-2 pt-2">
              <Button type="button" variant="outline" :disabled="loading" @click="resetForm">
                <RotateCcw class="size-4" /> 重置
              </Button>
              <Button type="submit" :disabled="loading">
                <Loader2 v-if="loading" class="size-4 animate-spin" />
                <Zap v-else class="size-4" />
                <span>{{ loading ? '上链中...' : '注册 + 上链' }}</span>
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      <!-- 右侧结果区 -->
      <div class="space-y-4">
        <Alert v-if="errorMsg" variant="destructive">
          <AlertTitle>注册失败</AlertTitle>
          <AlertDescription>{{ errorMsg }}</AlertDescription>
        </Alert>

        <Card v-if="result" class="border-green-200 dark:border-green-900">
          <CardHeader>
            <div class="flex items-center gap-2">
              <CheckCircle2 class="size-5 text-green-600" />
              <CardTitle class="text-base">链上注册成功</CardTitle>
            </div>
            <CardDescription>{{ result.message }}</CardDescription>
          </CardHeader>
          <CardContent class="space-y-3 text-sm">
            <div class="flex items-center justify-between">
              <span class="text-muted-foreground">traceNumber</span>
              <Copyable :value="result.traceNumber" />
            </div>
            <div class="flex items-center justify-between">
              <span class="text-muted-foreground">version</span>
              <Badge variant="secondary">v={{ result.version }}</Badge>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-muted-foreground">链上状态</span>
              <Badge variant="default" class="bg-green-600">{{ result.chainStatus }}</Badge>
            </div>
            <Separator />
            <div class="space-y-1">
              <div class="text-xs text-muted-foreground">dataHash</div>
              <Copyable :value="result.dataHash" short class="break-all" />
            </div>
            <div class="space-y-1">
              <div class="text-xs text-muted-foreground">txHash</div>
              <Copyable :value="result.txHash" short class="break-all" />
            </div>
            <div v-if="result.blockNumber !== null" class="flex items-center justify-between">
              <span class="text-muted-foreground">blockNumber</span>
              <code class="text-xs">{{ result.blockNumber }}</code>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-muted-foreground">producedAt</span>
              <span class="text-xs">{{ result.producedAt }}</span>
            </div>
            <Separator />
            <a
              :href="`/scan/${encodeURIComponent(result.traceNumber)}`"
              target="_blank"
              class="text-xs inline-flex items-center gap-1 text-primary hover:underline"
            >
              <Link2 class="size-3" /> 公开扫码页（消费者视角）
            </a>
          </CardContent>
        </Card>

        <Card v-if="!result && !errorMsg" class="border-dashed">
          <CardContent class="py-10 text-center text-sm text-muted-foreground space-y-2">
            <Zap class="size-8 mx-auto opacity-30" />
            <p>填写左侧表单后点击 "注册 + 上链"</p>
            <p class="text-xs">
              成功后这里会显示 txHash / dataHash<br />供消费者扫码验证
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  </div>
</template>
