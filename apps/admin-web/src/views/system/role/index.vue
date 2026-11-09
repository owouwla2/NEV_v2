<script setup lang="ts">
import RuoYiListPage from '@/components/system/RuoYiListPage.vue'
const statusFmt = (v: unknown) => (v === '0' ? '正常' : v === '1' ? '停用' : String(v ?? '-'))
</script>

<template>
  <RuoYiListPage
    title="角色管理"
    description="RuoYi 角色表；superadmin 为超管，producer/distributor/retailer/merchant/consumer/recycler 为业务角色"
    endpoint="/system/role/list"
    :columns="[
      { key: 'roleName', label: '角色名' },
      { key: 'roleKey', label: '权限字符' },
      { key: 'roleSort', label: '排序' },
      { key: 'status', label: '状态', format: statusFmt },
    ]"
  />
</template>