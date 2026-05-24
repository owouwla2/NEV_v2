<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import type { RouterEntry } from '@/stores/user'
import { resolveIcon } from '@/lib/icon-map'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'
import { ChevronRight, ExternalLink } from 'lucide-vue-next'
import { cn } from '@/lib/utils'

const props = defineProps<{
  entry: RouterEntry
  parentPath?: string
  depth?: number
}>()

const route = useRoute()
const depth = computed(() => props.depth ?? 0)
const padLeft = computed(() => `${depth.value * 12 + 12}px`)

const fullPath = computed(() => {
  if (props.entry.path.startsWith('/')) return props.entry.path
  if (!props.parentPath) return props.entry.path
  return `${props.parentPath.replace(/\/$/, '')}/${props.entry.path}`
})

const isExternal = computed(() => /^https?:\/\//.test(props.entry.path))
const visibleChildren = computed(() =>
  (props.entry.children || []).filter((c) => !c.hidden),
)
const hasChildren = computed(() => visibleChildren.value.length > 0)
const isActive = computed(() => route.path === fullPath.value || route.path.startsWith(fullPath.value + '/'))

const IconCmp = computed(() => resolveIcon(props.entry.meta?.icon))
</script>

<template>
  <!-- 外链 -->
  <a
    v-if="isExternal"
    :href="entry.path"
    target="_blank"
    rel="noopener"
    class="flex items-center gap-2 rounded-md py-1.5 text-sm hover:bg-sidebar-accent hover:text-sidebar-accent-foreground transition-colors"
    :style="{ paddingLeft: padLeft, paddingRight: '12px' }"
  >
    <component :is="IconCmp" class="size-4 shrink-0 opacity-70" />
    <span class="flex-1 truncate">{{ entry.meta?.title || entry.name }}</span>
    <ExternalLink class="size-3 opacity-50" />
  </a>

  <!-- 叶子（无 children，跳路由）-->
  <RouterLink
    v-else-if="!hasChildren"
    :to="fullPath"
    :class="cn(
      'flex items-center gap-2 rounded-md py-1.5 text-sm transition-colors',
      isActive
        ? 'bg-sidebar-primary text-sidebar-primary-foreground'
        : 'hover:bg-sidebar-accent hover:text-sidebar-accent-foreground'
    )"
    :style="{ paddingLeft: padLeft, paddingRight: '12px' }"
  >
    <component :is="IconCmp" class="size-4 shrink-0 opacity-80" />
    <span class="flex-1 truncate">{{ entry.meta?.title || entry.name }}</span>
  </RouterLink>

  <!-- 目录（可折叠）-->
  <Collapsible v-else :default-open="isActive">
    <CollapsibleTrigger
      :class="cn(
        'group/coll w-full flex items-center gap-2 rounded-md py-1.5 text-sm transition-colors',
        'hover:bg-sidebar-accent hover:text-sidebar-accent-foreground'
      )"
      :style="{ paddingLeft: padLeft, paddingRight: '12px' }"
    >
      <component :is="IconCmp" class="size-4 shrink-0 opacity-80" />
      <span class="flex-1 truncate text-left">{{ entry.meta?.title || entry.name }}</span>
      <ChevronRight class="size-3 transition-transform group-data-[state=open]/coll:rotate-90 opacity-60" />
    </CollapsibleTrigger>
    <CollapsibleContent class="space-y-0.5 mt-0.5">
      <SidebarNavItem
        v-for="child in visibleChildren"
        :key="child.path"
        :entry="child"
        :parent-path="fullPath"
        :depth="depth + 1"
      />
    </CollapsibleContent>
  </Collapsible>
</template>
