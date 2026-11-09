<script setup lang="ts">
import RuoYiListPage from '@/components/system/RuoYiListPage.vue'
const statusFmt = (v: unknown) => (v === '0' ? '正常' : v === '1' ? '停用' : String(v ?? '-'))
</script>

<template>
  <RuoYiListPage
    title="通知公告"
    description="平台公告（sys_notice）"
    endpoint="/system/notice/list"
    :columns="[
      { key: 'noticeTitle', label: '标题' },
      { key: 'noticeType', label: '类型', format: (v: unknown) => (v === '1' ? '通知' : v === '2' ? '公告' : String(v ?? '-')) },
      { key: 'status', label: '状态', format: statusFmt },
      { key: 'createTime', label: '发布时间' },
    ]"
  />
</template>