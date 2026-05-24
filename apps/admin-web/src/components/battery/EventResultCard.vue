<script setup lang="ts">
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import { CheckCircle2, Link2 } from 'lucide-vue-next'
import Copyable from '@/components/Copyable.vue'

/** 4 个事件页共用的"链上成功"结果卡 */
defineProps<{
  result: {
    traceNumber?: string
    eventType?: string
    version?: number
    dataHash?: string
    txHash?: string
    blockNumber?: number | null
    eventTime?: string
    currentStatus?: string
    currentRole?: string
    currentOwnerId?: number
    chainStatus?: string
    message?: string
  }
}>()
</script>

<template>
  <Card class="border-green-200 dark:border-green-900">
    <CardHeader>
      <div class="flex items-center gap-2">
        <CheckCircle2 class="size-5 text-green-600" />
        <CardTitle class="text-base">链上事件已写入</CardTitle>
      </div>
      <CardDescription>{{ result.message }}</CardDescription>
    </CardHeader>
    <CardContent class="space-y-3 text-sm">
      <div class="flex items-center justify-between">
        <span class="text-muted-foreground">traceNumber</span>
        <Copyable :value="result.traceNumber || ''" />
      </div>
      <div class="flex items-center justify-between">
        <span class="text-muted-foreground">eventType</span>
        <Badge>{{ result.eventType }}</Badge>
      </div>
      <div class="flex items-center justify-between">
        <span class="text-muted-foreground">version</span>
        <Badge variant="secondary">v={{ result.version }}</Badge>
      </div>
      <div class="flex items-center justify-between">
        <span class="text-muted-foreground">当前状态</span>
        <span class="text-xs">
          <Badge variant="outline">{{ result.currentStatus }}</Badge>
          <span class="text-muted-foreground"> / </span>
          <Badge variant="outline">{{ result.currentRole }}</Badge>
        </span>
      </div>
      <div class="flex items-center justify-between">
        <span class="text-muted-foreground">链上状态</span>
        <Badge variant="default" class="bg-green-600">{{ result.chainStatus }}</Badge>
      </div>
      <Separator />
      <div class="space-y-1">
        <div class="text-xs text-muted-foreground">dataHash</div>
        <Copyable :value="result.dataHash || ''" short />
      </div>
      <div class="space-y-1">
        <div class="text-xs text-muted-foreground">txHash</div>
        <Copyable :value="result.txHash || ''" short />
      </div>
      <div class="flex items-center justify-between">
        <span class="text-muted-foreground">eventTime</span>
        <span class="text-xs">{{ result.eventTime }}</span>
      </div>
      <Separator />
      <a
        :href="`/scan/${encodeURIComponent(result.traceNumber || '')}`"
        target="_blank"
        class="text-xs inline-flex items-center gap-1 text-primary hover:underline"
      >
        <Link2 class="size-3" /> 公开扫码页（消费者视角）
      </a>
    </CardContent>
  </Card>
</template>
