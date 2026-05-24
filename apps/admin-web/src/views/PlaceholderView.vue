<script setup lang="ts">
import { useRoute } from 'vue-router'
import { Construction } from 'lucide-vue-next'

const route = useRoute()
</script>

<template>
  <div class="flex flex-col items-center justify-center h-[60vh] gap-3 text-muted-foreground">
    <Construction class="size-16 opacity-50" />
    <p class="text-base font-medium text-foreground">{{ route.meta.title || '该页面' }}</p>
    <p class="text-sm">页面待开发（D24+）</p>
    <p class="text-xs">component path: <code class="px-1 py-0.5 rounded bg-muted">{{ String(route.matched.at(-1)?.path || route.path) }}</code></p>
  </div>
</template>
