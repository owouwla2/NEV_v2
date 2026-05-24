<script setup lang="ts">
import { Copy, Check } from 'lucide-vue-next'
import { ref } from 'vue'
import { toast } from 'vue-sonner'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

const props = defineProps<{
  value: string
  short?: boolean
  class?: string
}>()

const copied = ref(false)

async function copy() {
  try {
    await navigator.clipboard.writeText(props.value)
    copied.value = true
    toast.success('已复制')
    setTimeout(() => (copied.value = false), 1500)
  } catch {
    toast.error('复制失败')
  }
}

function shorten(v: string) {
  if (!props.short) return v
  if (!v) return ''
  if (v.length <= 18) return v
  return `${v.slice(0, 10)}...${v.slice(-6)}`
}
</script>

<template>
  <span :class="cn('inline-flex items-center gap-1.5 group', props.class)">
    <code class="text-xs font-mono px-1.5 py-0.5 rounded bg-muted">{{ shorten(props.value) }}</code>
    <Button
      variant="ghost"
      size="icon"
      class="size-6 opacity-50 group-hover:opacity-100 transition-opacity"
      @click="copy"
    >
      <Check v-if="copied" class="size-3 text-green-600" />
      <Copy v-else class="size-3" />
    </Button>
  </span>
</template>
