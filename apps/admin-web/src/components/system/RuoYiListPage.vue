<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { axiosInstance } from '@/lib/http'
import { RefreshCw } from 'lucide-vue-next'

export interface ListColumn {
  key: string
  label: string
  format?: (value: unknown, row: Record<string, unknown>) => string
}

const props = defineProps<{
  title: string
  description?: string
  /** RuoYi 列表接口，如 /system/user/list */
  endpoint: string
  columns: ListColumn[]
  /** 附加查询参数 */
  query?: Record<string, string | number>
}>()

const rows = ref<Record<string, unknown>[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = new URLSearchParams({ pageNum: String(pageNum.value), pageSize: String(pageSize.value) })
    for (const [k, v] of Object.entries(props.query ?? {})) params.set(k, String(v))
    const resp = await axiosInstance.get(`${props.endpoint}?${params.toString()}`)
    // 兼容两种返回：TableDataInfo{total,rows} 与 R{data:[...]}（menu/dept）
    const body = resp.data as { code?: number; msg?: string; rows?: unknown[]; total?: number; data?: unknown }
    if (Array.isArray(body.rows)) {
      rows.value = body.rows as Record<string, unknown>[]
      total.value = body.total ?? rows.value.length
    } else if (Array.isArray(body.data)) {
      rows.value = body.data as Record<string, unknown>[]
      total.value = rows.value.length
    } else if (body.code && body.code !== 200) {
      throw new Error(body.msg || `code=${body.code}`)
    } else {
      throw new Error('无法识别的响应结构')
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}

function fmt(v: unknown): string {
  if (v === null || v === undefined || v === '') return '-'
  return String(v)
}

onMounted(load)
</script>

<template>
  <div class="space-y-4">
    <div>
      <h1 class="text-2xl font-bold">{{ title }}</h1>
      <p v-if="description" class="text-sm text-muted-foreground mt-1">{{ description }}</p>
    </div>

    <Card>
      <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-3">
        <div>
          <CardTitle class="text-base">数据列表</CardTitle>
          <CardDescription>共 {{ total }} 条</CardDescription>
        </div>
        <Button variant="outline" size="sm" :disabled="loading" @click="load">
          <RefreshCw class="size-4 mr-1" :class="{ 'animate-spin': loading }" />
          刷新
        </Button>
      </CardHeader>
      <CardContent>
        <Alert v-if="error" variant="destructive">
          <AlertTitle>加载失败</AlertTitle>
          <AlertDescription>{{ error }}</AlertDescription>
        </Alert>
        <div v-else class="rounded-md border overflow-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead v-for="c in columns" :key="c.key">{{ c.label }}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-if="rows.length === 0">
                <TableCell :colspan="columns.length" class="text-center text-muted-foreground py-8">
                  暂无数据
                </TableCell>
              </TableRow>
              <TableRow v-for="(r, i) in rows" :key="i">
                <TableCell v-for="c in columns" :key="c.key" class="whitespace-nowrap">
                  {{ c.format ? c.format(r[c.key], r) : fmt(r[c.key]) }}
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </div>
      </CardContent>
    </Card>
  </div>
</template>
