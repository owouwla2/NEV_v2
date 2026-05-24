<script setup lang="ts">
import { reactive, ref } from 'vue'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Loader2, Recycle, RotateCcw } from 'lucide-vue-next'
import { toast } from 'vue-sonner'
import { recyclerReceive, type BatteryEventVO } from '@/api/battery'
import EventResultCard from '@/components/battery/EventResultCard.vue'

const form = reactive({
  traceNumber: '',
  soh: '70',
  remark: '',
})

const loading = ref(false)
const result = ref<BatteryEventVO | null>(null)
const errorMsg = ref('')

function validate() {
  if (!form.traceNumber.trim()) return '溯源编号必填'
  const s = Number(form.soh)
  if (Number.isNaN(s) || s < 0 || s > 100) return 'SOH 必须在 0-100 之间'
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
    const data = await recyclerReceive({
      traceNumber: form.traceNumber.trim(),
      soh: Number(form.soh),
      remark: form.remark || undefined,
    })
    result.value = data
    toast.success(`电池 ${data.traceNumber} 已记录 RECYCLED 事件 (v=${data.version})`)
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
  form.soh = '70'
  form.remark = ''
}
</script>

<template>
  <div class="space-y-6 max-w-5xl">
    <div>
      <h1 class="text-2xl font-bold flex items-center gap-2">
        <Recycle class="size-6" /> 回收申请
      </h1>
      <p class="text-sm text-muted-foreground mt-1">
        recycler 角色：接收消费者送回的旧电池 → 评估 SOH → 链上写入 RECYCLED 事件 + 当前持有者转 recycler
      </p>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <Card class="lg:col-span-2">
        <CardHeader>
          <CardTitle class="text-base">回收信息</CardTitle>
          <CardDescription>SOH (State of Health) 范围 0-100；负值不接受</CardDescription>
        </CardHeader>
        <CardContent>
          <form @submit="onSubmit" class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="space-y-1.5 md:col-span-2">
              <Label for="traceNumber">溯源编号 *</Label>
              <Input
                id="traceNumber"
                v-model="form.traceNumber"
                placeholder="BAT-DEMO-XXX"
                :disabled="loading"
              />
            </div>
            <div class="space-y-1.5">
              <Label for="soh">SOH（%）*</Label>
              <Input
                id="soh"
                v-model="form.soh"
                type="number"
                min="0"
                max="100"
                :disabled="loading"
              />
              <p class="text-xs text-muted-foreground">
                State of Health：电池健康度（≥70% 可梯次利用 / <70% 拆解）
              </p>
            </div>
            <div class="space-y-1.5">
              <Label for="remark">评估备注</Label>
              <Textarea id="remark" v-model="form.remark" rows="1" :disabled="loading" />
            </div>
            <div class="md:col-span-2 flex items-center justify-end gap-2 pt-2">
              <Button type="button" variant="outline" :disabled="loading" @click="resetForm">
                <RotateCcw class="size-4" /> 重置
              </Button>
              <Button type="submit" :disabled="loading">
                <Loader2 v-if="loading" class="size-4 animate-spin" />
                <Recycle v-else class="size-4" />
                <span>{{ loading ? '上链中...' : '回收 + 写入 RECYCLED' }}</span>
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
            <Recycle class="size-8 mx-auto opacity-30" />
            <p>填写左侧后点击 "回收 + 写入 RECYCLED"</p>
            <p class="text-xs">上链后这里显示 txHash + 状态变更为 RECYCLED</p>
          </CardContent>
        </Card>
      </div>
    </div>
  </div>
</template>
