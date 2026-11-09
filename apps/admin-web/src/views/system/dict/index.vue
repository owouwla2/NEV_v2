<script setup lang="ts">
import RuoYiListPage from '@/components/system/RuoYiListPage.vue'

</script>

<template>
  <RuoYiListPage
    title="字典管理"
    description="数据字典（sys_dict_type）；业务字典 nev_product_category 由演示种子创建"
    endpoint="/system/dict/type/list"
    :columns="[
      { key: 'dictName', label: '字典名' },
      { key: 'dictType', label: '字典键' },
      { key: 'remark', label: '备注' },
    ]"
  />
</template>