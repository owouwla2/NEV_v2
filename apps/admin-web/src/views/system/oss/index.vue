<script setup lang="ts">
import RuoYiListPage from '@/components/system/RuoYiListPage.vue'

</script>

<template>
  <RuoYiListPage
    title="文件管理"
    description="对象存储文件（sys_oss）；本演示未上传文件，列表为空属正常"
    endpoint="/resource/oss/list"
    :columns="[
      { key: 'originalName', label: '文件名' },
      { key: 'fileSuffix', label: '类型' },
      { key: 'fileSize', label: '大小' },
      { key: 'createTime', label: '上传时间' },
    ]"
  />
</template>