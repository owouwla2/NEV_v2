<script setup lang="ts">
import RuoYiListPage from '@/components/system/RuoYiListPage.vue'
const statusFmt = (v: unknown) => (v === '0' ? '正常' : v === '1' ? '停用' : String(v ?? '-'))
</script>

<template>
  <RuoYiListPage
    title="岗位管理"
    description="岗位表（sys_post）"
    endpoint="/system/post/list"
    :columns="[
      { key: 'postName', label: '岗位' },
      { key: 'postCode', label: '编码' },
      { key: 'postSort', label: '排序' },
      { key: 'status', label: '状态', format: statusFmt },
    ]"
  />
</template>