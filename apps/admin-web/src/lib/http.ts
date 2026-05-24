import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'

/**
 * 后端 RuoYi-Vue-Plus 5.6.1 的全局 R 响应包装
 * code: 200 = 成功；其他 = 业务/系统错误
 */
export interface R<T = unknown> {
  code: number
  msg: string
  data: T
  rows?: T
  total?: number
}

/** clientId 哈希（D2 验证过：sys_client 表 "sys_client" 行的 client_id 字段） */
export const CLIENT_ID = 'e5cd7e4891bf95d1d19206ce24a7b32e'

const STORAGE_TOKEN_KEY = 'nev:token'

const instance: AxiosInstance = axios.create({
  baseURL: '/',
  timeout: 30000,
})

/** 请求拦截：注入 Authorization Bearer + clientid 双 header */
instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem(STORAGE_TOKEN_KEY)
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  config.headers.set('clientid', CLIENT_ID)
  return config
})

/** 响应拦截：剥 R{code,msg,data} 外壳 + 401 自动跳登录 */
instance.interceptors.response.use(
  (response) => {
    const body = response.data as R
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) {
        return response
      }
      if (body.code === 401) {
        localStorage.removeItem(STORAGE_TOKEN_KEY)
        if (location.pathname !== '/login') {
          location.replace('/login')
        }
      }
      return Promise.reject(new Error(body.msg || `业务异常 code=${body.code}`))
    }
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(STORAGE_TOKEN_KEY)
      if (location.pathname !== '/login') {
        location.replace('/login')
      }
    }
    return Promise.reject(error)
  },
)

/** 通用 GET：返回 data 主体 */
export async function http<T = unknown>(
  url: string,
  config?: Parameters<AxiosInstance['get']>[1],
): Promise<T> {
  const resp = await instance.get<R<T>>(url, config)
  return resp.data.data
}

/** 通用 POST */
export async function httpPost<T = unknown>(
  url: string,
  body?: unknown,
  config?: Parameters<AxiosInstance['post']>[2],
): Promise<T> {
  const resp = await instance.post<R<T>>(url, body, config)
  return resp.data.data
}

/** 通用 PUT */
export async function httpPut<T = unknown>(url: string, body?: unknown): Promise<T> {
  const resp = await instance.put<R<T>>(url, body)
  return resp.data.data
}

/** 通用 DELETE */
export async function httpDelete<T = unknown>(url: string): Promise<T> {
  const resp = await instance.delete<R<T>>(url)
  return resp.data.data
}

export { instance as axiosInstance, STORAGE_TOKEN_KEY }
