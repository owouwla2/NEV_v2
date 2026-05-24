import { http, httpPost } from '@/lib/http'

// === footprint ===
export interface CarbonStageDetail {
  stage: string
  co2Kg: string
  breakdown: Array<{
    factorCode: string
    factorName: string
    input: string
    inputUnit: string
    factorValue: string
    factorUnit: string
    co2Kg: string
    note?: string
  }>
}

export interface CarbonFootprintVO {
  batteryId: number
  traceNumber: string
  batteryModel: string
  cellType: string
  capacityKwh: string
  totalCo2Kg: string
  calcMethod: string
  calcVersion: string
  calcTime: string | null
  stages: CarbonStageDetail[]
}

export function adminCalcCarbon(traceNumber: string) {
  return httpPost<CarbonFootprintVO>(`/admin/carbon/calc/${encodeURIComponent(traceNumber)}`)
}

export function publicCarbon(traceNumber: string) {
  return http<CarbonFootprintVO>(`/public/carbon/${encodeURIComponent(traceNumber)}`)
}

// === credit ===
export interface CarbonAccountVO {
  userId: number
  balance: string
  frozen: string
  totalEarned: string
  totalSpent: string
  recentRecords: Array<{
    id: number
    changeAmount: string
    balanceAfter: string
    reason: string
    relatedId: number | null
    relatedType: string | null
    remark: string | null
    createTime: string
  }>
}

export function consumerCarbonAccount() {
  return http<CarbonAccountVO>('/consumer/carbon/account')
}
