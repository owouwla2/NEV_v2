<script setup lang="ts">
import RuoYiListPage from '@/components/system/RuoYiListPage.vue'
const statusFmt = (v: unknown) => (v === '0' ? '正常' : v === '1' ? '停用' : String(v ?? '-'))
</script>

<template>
  <RuoYiListPage
    title="用户管理"
    description="平台账号（sys_user）；demo 业务账号 producer1~recycler1 由演示种子创建，密码统一 admin123"
    endpoint="/system/user/list"
    :columns="[
      { key: 'userName', label: '账号' },
      { key: 'nickName', label: '昵称' },
      { key: 'phonenumber', label: '手机号' },
      { key: 'status', label: '状态', format: statusFmt },
      { key: 'createTime', label: '创建时间' },
    ]"
  />
</template>