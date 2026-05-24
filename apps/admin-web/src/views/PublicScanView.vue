<script setup lang="ts">
// 占位：D28 实现完整公开扫码 H5
import { useRoute } from 'vue-router'
const route = useRoute()
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-background p-8">
    <div class="text-center space-y-2">
      <h1 class="text-xl font-semibold">公开扫码页（占位）</h1>
      <p class="text-muted-foreground text-sm">trace = <code class="px-1 py-0.5 rounded bg-muted">{{ route.params.trace }}</code></p>
      <p class="text-muted-foreground text-xs">D28 实现完整扫码视图（电池 + 链上事件 + 碳足迹）</p>
    </div>
  </div>
</template>
