<script setup lang="ts">
import RuoYiListPage from '@/components/system/RuoYiListPage.vue'

</script>

<template>
  <RuoYiListPage
    title="菜单管理"
    description="动态菜单表（sys_menu）：本前端按 component 路径经 import.meta.glob 动态注册路由"
    endpoint="/system/menu/list"
    :columns="[
      { key: 'menuName', label: '菜单名' },
      { key: 'orderNum', label: '排序' },
      { key: 'path', label: '路由' },
      { key: 'component', label: '组件' },
      { key: 'visible', label: '可见', format: (v: unknown) => (v === '0' ? '显示' : '隐藏') },
    ]"
  />
</template>