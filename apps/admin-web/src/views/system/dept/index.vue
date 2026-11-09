<script setup lang="ts">
import RuoYiListPage from '@/components/system/RuoYiListPage.vue'
const statusFmt = (v: unknown) => (v === '0' ? '正常' : v === '1' ? '停用' : String(v ?? '-'))
</script>

<template>
  <RuoYiListPage
    title="部门管理"
    description="组织架构（sys_dept）；demo 账号统一挂在研发部门 103 下"
    endpoint="/system/dept/list"
    :columns="[
      { key: 'deptName', label: '部门' },
      { key: 'orderNum', label: '排序' },
      { key: 'status', label: '状态', format: statusFmt },
      { key: 'createTime', label: '创建时间' },
    ]"
  />
</template>