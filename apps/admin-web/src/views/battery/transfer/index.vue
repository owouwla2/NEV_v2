<script setup lang="ts">
import { reactive, ref } from 'vue'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Loader2, Truck, RotateCcw } from 'lucide-vue-next'
import { toast } from 'vue-sonner'
import { distributorTransferIn, type BatteryEventVO } from '@/api/battery'
import EventResultCard from '@/components/battery/EventResultCard.vue'

const form = reactive({
  traceNumber: '',
  fromOwnerId: '101',
  remark: '',
})

const loading = ref(false)
const result = ref<BatteryEventVO | null>(null)
const errorMsg = ref('')

function validate() {
  if (!form.traceNumber.trim()) return '溯源编号必填'
  const fid = Number(form.fromOwnerId)
  if (!fid || fid <= 0) return '前持有者用户ID 必须 > 0'
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
    const data = await distributorTransferIn({
      traceNumber: form.traceNumber.trim(),
      fromOwnerId: Number(form.fromOwnerId),
      remark: form.remark || undefined,
    })
    result.value = data
    toast.success(`电池 ${data.traceNumber} 已记录 IN_USE 事件 (v=${data.version})`)
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : String(err)
    errorMsg.value = msg
    toast.error(`提交失败：${msg}`)
  } finally {
    loading.value = false
  }
}

function resetForm() {
  result.value = null
  errorMsg.value = ''
  form.traceNumber = ''
  form.fromOwnerId = '101'
  form.remark = ''
}
</script>

<template>
  <div class="space-y-6 max-w-5xl">
    <div>
      <h1 class="text-2xl font-bold flex items-center gap-2">
        <Truck class="size-6" /> 电池流通
      </h1>
      <p class="text-sm text-muted-foreground mt-1">
        distributor 角色：从生产商接收电池 → 链上写入 IN_USE 事件 + 电池当前持有者切换到当前用户
      </p>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <Card class="lg:col-span-2">
        <CardHeader>
          <CardTitle class="text-base">流通信息</CardTitle>
          <CardDescription>填写老电池溯源编号 + 上一持有者用户ID（producer1 = 101）</CardDescription>
        </CardHeader>
        <CardContent>
          <form @submit="onSubmit" class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="space-y-1.5 md:col-span-2">
              <Label for="traceNumber">溯源编号 *</Label>
              <Input
                id="traceNumber"
                v-model="form.traceNumber"
                placeholder="BAT-DEMO-XXX（从 producer 那拿到的）"
                :disabled="loading"
              />
            </div>
            <div class="space-y-1.5">
              <Label for="fromOwnerId">前持有者用户ID *</Label>
              <Input
                id="fromOwnerId"
                v-model="form.fromOwnerId"
                type="number"
                placeholder="101 = producer1"
                :disabled="loading"
              />
              <p class="text-xs text-muted-foreground">
                通常是 producer 的 user_id（demo 默认 101）
              </p>
            </div>
            <div class="space-y-1.5">
              <Label for="remark">备注</Label>
              <Textarea id="remark" v-model="form.remark" rows="1" :disabled="loading" />
            </div>
            <div class="md:col-span-2 flex items-center justify-end gap-2 pt-2">
              <Button type="button" variant="outline" :disabled="loading" @click="resetForm">
                <RotateCcw class="size-4" /> 重置
              </Button>
              <Button type="submit" :disabled="loading">
                <Loader2 v-if="loading" class="size-4 animate-spin" />
                <Truck v-else class="size-4" />
                <span>{{ loading ? '上链中...' : '接收 + 写入 IN_USE' }}</span>
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      <div class="space-y-4">
        <Alert v-if="errorMsg" variant="destructive">
          <AlertTitle>提交失败</AlertTitle>
          <AlertDescription>{{ errorMsg }}</AlertDescription>
        </Alert>

        <EventResultCard v-if="result" :result="result" />

        <Card v-if="!result && !errorMsg" class="border-dashed">
          <CardContent class="py-10 text-center text-sm text-muted-foreground space-y-2">
            <Truck class="size-8 mx-auto opacity-30" />
            <p>填写左侧表单后点击 "接收 + 写入 IN_USE"</p>
            <p class="text-xs">链上写入后这里显示 txHash + 当前持有者</p>
          </CardContent>
        </Card>
      </div>
    </div>
  </div>
</template>
