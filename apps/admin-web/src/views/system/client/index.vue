<script setup lang="ts">
import RuoYiListPage from '@/components/system/RuoYiListPage.vue'
const statusFmt = (v: unknown) => (v === '0' ? '正常' : v === '1' ? '停用' : String(v ?? '-'))
</script>

<template>
  <RuoYiListPage
    title="客户端管理"
    description="OAuth 客户端（sys_client）；admin-web 使用 pc 客户端（dual-header 鉴权）"
    endpoint="/system/client/list"
    :columns="[
      { key: 'clientKey', label: '客户端' },
      { key: 'clientId', label: 'clientId' },
      { key: 'grantTypeList', label: '授权类型', format: (v: unknown) => (Array.isArray(v) ? v.join(', ') : String(v ?? '-')) },
      { key: 'status', label: '状态', format: statusFmt },
    ]"
  />
</template>