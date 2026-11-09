<script setup lang="ts">
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
</script>

<template>
  <div class="space-y-4">
    <div>
      <h1 class="text-2xl font-bold">任务调度中心</h1>
      <p class="text-sm text-muted-foreground mt-1">SnailJob 分布式任务调度</p>
    </div>
    <Card>
      <CardHeader>
        <CardTitle class="text-base">本项目未启用 SnailJob Server</CardTitle>
        <CardDescription>说明</CardDescription>
      </CardHeader>
      <CardContent class="text-sm space-y-2 text-muted-foreground">
        <p>后端自带 SnailJob Client，默认连接 localhost:8800；本项目未部署 SnailJob Server，因此启动日志会出现
          SnailJob UNAVAILABLE 的 ERROR——<b>业务零影响，直接忽略</b>（README「启动日志两条红字 ERROR」一节）。</p>
        <p>NEV-v2 的定时任务（碳计算等）当前均为同步调用，不依赖调度中心。</p>
      </CardContent>
    </Card>
  </div>
</template>