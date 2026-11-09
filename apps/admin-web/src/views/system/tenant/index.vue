<script setup lang="ts">
import RuoYiListPage from '@/components/system/RuoYiListPage.vue'
const statusFmt = (v: unknown) => (v === '0' ? '正常' : v === '1' ? '停用' : String(v ?? '-'))
</script>

<template>
  <RuoYiListPage
    title="租户管理"
    description="多租户（sys_tenant）；本项目单租户 000000 运行"
    endpoint="/system/tenant/list"
    :columns="[
      { key: 'tenantId', label: '租户ID' },
      { key: 'companyName', label: '企业' },
      { key: 'contactUserName', label: '联系人' },
      { key: 'status', label: '状态', format: statusFmt },
    ]"
  />
</template>