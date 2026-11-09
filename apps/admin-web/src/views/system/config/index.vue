<script setup lang="ts">
import RuoYiListPage from '@/components/system/RuoYiListPage.vue'

</script>

<template>
  <RuoYiListPage
    title="参数设置"
    description="系统参数（sys_config，Redis 缓存）"
    endpoint="/system/config/list"
    :columns="[
      { key: 'configName', label: '参数名' },
      { key: 'configKey', label: '键' },
      { key: 'configValue', label: '值' },
    ]"
  />
</template>