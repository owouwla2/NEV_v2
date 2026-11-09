<script setup lang="ts">
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
</script>

<template>
  <div class="space-y-4">
    <div>
      <h1 class="text-2xl font-bold">Admin 监控</h1>
      <p class="text-sm text-muted-foreground mt-1">Spring Boot Admin（SBA）</p>
    </div>
    <Card>
      <CardHeader>
        <CardTitle class="text-base">本项目未启用 SBA Server</CardTitle>
        <CardDescription>说明</CardDescription>
      </CardHeader>
      <CardContent class="text-sm space-y-2 text-muted-foreground">
        <p>后端自带 SBA Client，默认注册到 localhost:9090；本项目未部署 SBA Server，因此启动日志会出现一条
          <code>Failed to register at spring-boot-admin</code> 的 ERROR——<b>业务零影响，直接忽略</b>（README「启动日志两条红字 ERROR」一节）。</p>
        <p>如需启用监控，另行部署 SBA Server 并放开 ruoyi-admin 的 spring-boot-admin 配置即可。</p>
      </CardContent>
    </Card>
  </div>
</template>