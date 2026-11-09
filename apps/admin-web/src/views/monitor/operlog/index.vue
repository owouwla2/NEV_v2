<script setup lang="ts">
import RuoYiListPage from '@/components/system/RuoYiListPage.vue'

</script>

<template>
  <RuoYiListPage
    title="操作日志"
    description="业务操作审计（sys_oper_log）；演示产生的电池注册/订单等操作会写入"
    endpoint="/monitor/operlog/list"
    :columns="[
      { key: 'title', label: '模块' },
      { key: 'operName', label: '操作人' },
      { key: 'operIp', label: 'IP' },
      { key: 'status', label: '结果', format: (v: unknown) => (v === 0 || v === '0' ? '成功' : '失败') },
      { key: 'operTime', label: '时间' },
    ]"
  />
</template>