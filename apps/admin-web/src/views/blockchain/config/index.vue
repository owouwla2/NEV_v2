<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { http } from '@/lib/http'

interface ContractConfigVO {
  contractName: string
  contractAddress: string
  network: string
  groupId: string
  deployBlock: number | null
  deployedAt: string | null
  abiLength: number
}

const list = ref<ContractConfigVO[]>([])
const error = ref('')

onMounted(async () => {
  try {
    list.value = (await http<ContractConfigVO[]>('/public/chain/config')) ?? []
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  }
})
</script>

<template>
  <div class="space-y-4">
    <div>
      <h1 class="text-2xl font-bold">合约配置</h1>
      <p class="text-sm text-muted-foreground mt-1">链上合约登记（nev_contract_config）；后端经 WeBASE-Front /trans/handle 调用</p>
    </div>
    <div v-if="error" class="text-sm text-destructive">加载失败：{{ error }}</div>
    <Card v-for="c in list" :key="c.contractName">
      <CardHeader>
        <div class="flex items-center gap-2">
          <CardTitle class="text-base">{{ c.contractName }}</CardTitle>
          <Badge variant="secondary">{{ c.network }} · group {{ c.groupId }}</Badge>
        </div>
        <CardDescription>FISCO BCOS 合约，7 角色钱包已授权（RoleManager）</CardDescription>
      </CardHeader>
      <CardContent class="space-y-2 text-sm">
        <div>
          <div class="text-xs text-muted-foreground">合约地址</div>
          <div class="font-mono break-all">{{ c.contractAddress }}</div>
        </div>
        <div class="grid grid-cols-3 gap-4">
          <div>
            <div class="text-xs text-muted-foreground">部署时间</div>
            <div>{{ c.deployedAt ?? '-' }}</div>
          </div>
          <div>
            <div class="text-xs text-muted-foreground">部署区块</div>
            <div>{{ c.deployBlock ?? '-' }}</div>
          </div>
          <div>
            <div class="text-xs text-muted-foreground">ABI 长度</div>
            <div class="font-mono">{{ c.abiLength }}</div>
          </div>
        </div>
      </CardContent>
    </Card>
  </div>
</template>