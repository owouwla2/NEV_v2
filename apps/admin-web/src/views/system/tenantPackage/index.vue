<script setup lang="ts">
import RuoYiListPage from '@/components/system/RuoYiListPage.vue'
const statusFmt = (v: unknown) => (v === '0' ? '正常' : v === '1' ? '停用' : String(v ?? '-'))
</script>

<template>
  <RuoYiListPage
    title="租户套餐管理"
    description="租户菜单套餐（sys_tenant_package）；单租户演示未配置套餐"
    endpoint="/system/tenant/package/list"
    :columns="[
      { key: 'packageName', label: '套餐名' },
      { key: 'remark', label: '备注' },
      { key: 'status', label: '状态', format: statusFmt },
    ]"
  />
</template>