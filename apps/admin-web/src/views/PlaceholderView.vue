<script setup lang="ts">
import { useRoute } from 'vue-router'
import { Construction } from 'lucide-vue-next'

const route = useRoute()
</script>

<template>
  <div class="flex flex-col items-center justify-center h-[60vh] gap-3 text-muted-foreground">
    <Construction class="size-16 opacity-50" />
    <p class="text-base font-medium text-foreground">{{ route.meta.title || '该页面' }}</p>
    <p class="text-sm">组件路径未注册：菜单 component 找不到对应页面（诊断信息，正常演示不应出现本页）</p>
    <p class="text-xs">component path: <code class="px-1 py-0.5 rounded bg-muted">{{ String(route.matched.at(-1)?.path || route.path) }}</code></p>
  </div>
</template>
