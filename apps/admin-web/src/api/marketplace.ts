import { http, httpPost } from '@/lib/http'

// === product ===
export interface ProductVO {
  id: number
  merchantId: number
  merchantName: string
  category: string
  title: string
  subtitle?: string
  price: string | number
  stock: number
  salesCount?: number
  batteryId?: number | null
  images?: string[]
  description?: string
  status: 'ON_SALE' | 'OFF_SHELF' | 'SOLD_OUT'
  createTime?: string
}

export interface ProductSaveDTO {
  id?: number
  title: string
  subtitle?: string
  category: string
  price: number
  stock: number
  batteryId?: number | null
  images?: string[]
  description?: string
  status?: string
}

export interface ProductQueryDTO {
  category?: string
  keyword?: string
  merchantId?: number
  onSaleOnly?: boolean
  pageNum?: number
  pageSize?: number
}

export interface Page<T> {
  records: T[]
  total: number
  size: number
  current: number
}

export function publicProductList(query: ProductQueryDTO) {
  const params = new URLSearchParams()
  for (const [k, v] of Object.entries(query)) {
    if (v !== undefined && v !== null && v !== '') params.set(k, String(v))
  }
  return http<Page<ProductVO>>(`/public/product/list?${params.toString()}`)
}

export function merchantSaveProduct(dto: ProductSaveDTO) {
  return httpPost<ProductVO>('/merchant/product/save', dto)
}

export function merchantMyProducts() {
  return http<ProductVO[]>('/merchant/product/mine')
}

// === order ===
export interface OrderItemVO {
  productId: number
  title?: string
  category?: string
  unitPrice: string | number
  quantity: number
  subtotal: string | number
  batteryId?: number | null
}

export interface AddressVO {
  recipient: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
}

export interface OrderVO {
  id: number
  orderNo: string
  userId: number
  merchantId: number
  merchantName?: string
  totalAmount: string | number
  payAmount?: string | number | null
  status: 'PENDING' | 'PAID' | 'SHIPPED' | 'DELIVERED' | 'COMPLETED' | 'CANCELLED' | 'REFUNDED'
  items?: OrderItemVO[]
  address?: AddressVO
  paidAt?: string | null
  shippedAt?: string | null
  deliveredAt?: string | null
  completedAt?: string | null
  cancelledAt?: string | null
  cancelReason?: string | null
  createTime?: string
}

export function merchantOrderList(params: { status?: string; pageNum?: number; pageSize?: number }) {
  const q = new URLSearchParams()
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== '') q.set(k, String(v))
  }
  return http<Page<OrderVO>>(`/merchant/order/list?${q.toString()}`)
}

export function merchantOrderDetail(id: number) {
  return http<OrderVO>(`/merchant/order/detail/${id}`)
}

export function merchantShip(id: number) {
  return httpPost<OrderVO>(`/merchant/order/ship/${id}`)
}

// === consumer ===
export interface CartItemVO {
  itemId: number
  productId: number
  title?: string
  category?: string
  images?: string[]
  unitPrice: string | number
  quantity: number
  subtotal: string | number
  selected: string
  stock?: number
  productStatus?: string
}

export interface CartMerchantGroup {
  cartId: number
  merchantId: number
  merchantName?: string
  items: CartItemVO[]
  selectedTotal: string | number
}

export interface CartVO {
  groups: CartMerchantGroup[]
  grandSelectedTotal: string | number
}

export function consumerCartList() {
  return http<CartVO>('/consumer/cart/list')
}
export function consumerCartAdd(productId: number, quantity: number) {
  return httpPost<unknown>('/consumer/cart/add', { productId, quantity })
}
export function consumerCartUpdate(itemId: number, quantity?: number, selected?: string) {
  return httpPost<unknown>('/consumer/cart/update', { itemId, quantity, selected })
}
export function consumerCartRemove(itemId: number) {
  return httpPost<unknown>(`/consumer/cart/remove/${itemId}`)
}
export function consumerCartClear() {
  return httpPost<unknown>('/consumer/cart/clear')
}

export interface OrderCreateDTO {
  cartItemIds: number[]
  address: AddressVO
  remark?: string
}
export function consumerOrderCreate(dto: OrderCreateDTO) {
  return httpPost<OrderVO>('/consumer/order/create', dto)
}
export function consumerOrderList(params: { status?: string; pageNum?: number; pageSize?: number }) {
  const q = new URLSearchParams()
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== '') q.set(k, String(v))
  }
  return http<Page<OrderVO>>(`/consumer/order/list?${q.toString()}`)
}
export function consumerOrderDetail(id: number) {
  return http<OrderVO>(`/consumer/order/detail/${id}`)
}
export function consumerOrderPay(id: number) {
  return httpPost<OrderVO>(`/consumer/order/pay/${id}`)
}
export function consumerOrderConfirm(id: number) {
  return httpPost<OrderVO>(`/consumer/order/confirm/${id}`)
}
export function consumerOrderCancel(id: number, reason?: string) {
  const q = reason ? `?reason=${encodeURIComponent(reason)}` : ''
  return httpPost<unknown>(`/consumer/order/cancel/${id}${q}`)
}
