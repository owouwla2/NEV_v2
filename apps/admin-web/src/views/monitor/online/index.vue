<script setup lang="ts">
import RuoYiListPage from '@/components/system/RuoYiListPage.vue'

</script>

<template>
  <RuoYiListPage
    title="在线用户"
    description="当前持有有效 token 的会话（Redis）；演示时会随登录账号增加"
    endpoint="/monitor/online/list"
    :columns="[
      { key: 'userName', label: '用户' },
      { key: 'ipaddr', label: 'IP' },
      { key: 'loginLocation', label: '位置' },
      { key: 'browser', label: '浏览器' },
      { key: 'loginTime', label: '登录时间' },
    ]"
  />
</template>