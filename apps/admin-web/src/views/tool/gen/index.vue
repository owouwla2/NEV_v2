<script setup lang="ts">
import RuoYiListPage from '@/components/system/RuoYiListPage.vue'

</script>

<template>
  <RuoYiListPage
    title="代码生成"
    description="RuoYi 代码生成器（gen_table）；本演示未使用生成产物，列表为空属正常"
    endpoint="/tool/gen/list"
    :columns="[
      { key: 'tableName', label: '表名' },
      { key: 'tableComment', label: '说明' },
      { key: 'className', label: '类名' },
      { key: 'createTime', label: '创建时间' },
    ]"
  />
</template>