import { http, httpPost } from '@/lib/http'

export interface TradeInVO {
  id: number
  requestNo: string
  consumerId: number
  oldBatteryId: number
  oldBatteryTraceNumber?: string | null
  newProductId?: number | null
  evaluatedAmount?: string | null
  recyclerId?: number | null
  evaluatorId?: number | null
  status: 'SUBMITTED' | 'EVALUATED' | 'ACCEPTED' | 'COMPLETED' | 'REJECTED'
  soh?: number | null
  evaluationSummary?: string | null
  submittedAt?: string | null
  evaluatedAt?: string | null
  acceptedAt?: string | null
  recycledAt?: string | null
  completedAt?: string | null
  chainTxHash?: string | null
}

export interface TradeInSubmitDTO {
  oldBatteryTraceNumber: string
  newProductId?: number
  remark?: string
}

export interface TradeInEvaluateDTO {
  requestId: number
  soh: number
  evaluatedAmount: number
  summary?: string
}

// consumer
export function consumerTradeInSubmit(dto: TradeInSubmitDTO) {
  return httpPost<TradeInVO>('/consumer/trade-in/submit', dto)
}
export function consumerTradeInAccept(id: number) {
  return httpPost<TradeInVO>(`/consumer/trade-in/accept/${id}`)
}
export function consumerTradeInReject(id: number, reason?: string) {
  const q = reason ? `?reason=${encodeURIComponent(reason)}` : ''
  return httpPost<TradeInVO>(`/consumer/trade-in/reject/${id}${q}`)
}
export function consumerTradeInList() {
  return http<TradeInVO[]>('/consumer/trade-in/list')
}
export function consumerTradeInDetail(id: number) {
  return http<TradeInVO>(`/consumer/trade-in/detail/${id}`)
}

// recycler
export function recyclerPending() {
  return http<TradeInVO[]>('/recycler/trade-in/pending')
}
export function recyclerEvaluate(dto: TradeInEvaluateDTO) {
  return httpPost<TradeInVO>('/recycler/trade-in/evaluate', dto)
}
export function recyclerTradeInDetail(id: number) {
  return http<TradeInVO>(`/recycler/trade-in/detail/${id}`)
}
