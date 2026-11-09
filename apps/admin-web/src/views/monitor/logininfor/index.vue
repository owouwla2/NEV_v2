<script setup lang="ts">
import RuoYiListPage from '@/components/system/RuoYiListPage.vue'

</script>

<template>
  <RuoYiListPage
    title="登录日志"
    description="登录审计（sys_logininfor）"
    endpoint="/monitor/logininfor/list"
    :columns="[
      { key: 'userName', label: '账号' },
      { key: 'ipaddr', label: 'IP' },
      { key: 'status', label: '结果', format: (v: unknown) => (v === 0 || v === '0' ? '成功' : '失败') },
      { key: 'loginTime', label: '时间' },
    ]"
  />
</template>