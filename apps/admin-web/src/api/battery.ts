import { http, httpPost } from '@/lib/http'

export interface BatteryRegisterDTO {
  traceNumber: string
  model: string
  serialNo: string
  capacityKwh: number
  voltage?: number
  cellSupplier?: string
  cellType?: string
  bmsInfo?: string
  remark?: string
}

export interface BatteryRegisterVO {
  id: number
  traceNumber: string
  dataHash: string
  txHash: string
  blockNumber: number | null
  version: number
  producedAt: string
  chainStatus: string
  message: string
}

export interface BatteryScanEventVO {
  version: number
  eventType: string
  operatorId: number
  operatorRole: string
  dataHash: string
  txHash: string
  blockNumber: number | null
  eventTime: string
  chainVerified: boolean
}

export interface CarbonStageBriefVO {
  stage: string
  co2Kg: string
}

export interface CarbonSummaryVO {
  totalCo2Kg: string
  calcMethod: string
  calcVersion: string
  calcTime: string
  stages: CarbonStageBriefVO[]
}

export interface BatteryScanVO {
  traceNumber: string
  model: string
  capacityKwh: string
  voltage: string
  currentStatus: string
  currentRole: string
  producedAt: string
  overallVerified: boolean
  totalEvents: number
  verifiedEvents: number
  chainEventCount: number | null
  events: BatteryScanEventVO[]
  carbonFootprint: CarbonSummaryVO | null
}

// === producer ===
export function registerBattery(dto: BatteryRegisterDTO) {
  return httpPost<BatteryRegisterVO>('/producer/battery/register', dto)
}

// === public scan ===
export function scanBattery(traceNumber: string) {
  return http<BatteryScanVO>(`/public/scan/${encodeURIComponent(traceNumber)}`)
}

// === distributor / retailer / recycler events ===
export interface TransferInDTO {
  traceNumber: string
  fromOwnerId: number
  remark?: string
}
export interface SellDTO {
  traceNumber: string
  orderNo: string
  consumerId: number
  remark?: string
}
export interface ReceiveDTO {
  traceNumber: string
  soh: number
  remark?: string
}

export interface BatteryEventVO {
  batteryId: number
  traceNumber: string
  eventType: string
  version: number
  dataHash: string
  txHash: string
  blockNumber: number | null
  eventTime: string
  currentStatus: string
  currentOwnerId: number
  currentRole: string
  chainStatus: string
  message: string
}

export function distributorTransferIn(dto: TransferInDTO) {
  return httpPost<BatteryEventVO>('/distributor/battery/transfer-in', dto)
}

export function retailerSell(dto: SellDTO) {
  return httpPost<BatteryEventVO>('/retailer/battery/sell', dto)
}

export function recyclerReceive(dto: ReceiveDTO) {
  return httpPost<BatteryEventVO>('/recycler/battery/receive', dto)
}
