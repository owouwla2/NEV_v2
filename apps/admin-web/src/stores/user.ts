import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { http, httpPost, STORAGE_TOKEN_KEY, CLIENT_ID } from '@/lib/http'

export interface LoginPayload {
  username: string
  password: string
}

export interface LoginResponse {
  access_token: string
  expire_in: number
}

export interface UserInfoResponse {
  user: {
    userId: number
    userName: string
    nickName: string
    deptName?: string
    avatar?: string
  }
  permissions: string[]
  roles: string[]
}

export interface RouterMeta {
  title?: string
  icon?: string
  noCache?: boolean
  link?: string | null
}

export interface RouterEntry {
  name: string
  path: string
  hidden?: boolean
  redirect?: string
  component?: string
  alwaysShow?: boolean
  meta?: RouterMeta
  children?: RouterEntry[]
}

export const useUserStore = defineStore(
  'user',
  () => {
    const token = ref<string | null>(localStorage.getItem(STORAGE_TOKEN_KEY))
    const nickName = ref('')
    const userName = ref('')
    const userId = ref<number | null>(null)
    const avatar = ref('')
    const roles = ref<string[]>([])
    const permissions = ref<string[]>([])
    const routers = ref<RouterEntry[]>([])

    const isLoggedIn = computed(() => !!token.value)
    const isSuperAdmin = computed(() => roles.value.includes('superadmin'))

    function hasRole(role: string) {
      return roles.value.includes(role) || roles.value.includes('superadmin')
    }

    function hasPerm(perm: string) {
      return (
        permissions.value.includes(perm) ||
        permissions.value.includes('*:*:*') ||
        roles.value.includes('superadmin')
      )
    }

    async function login(payload: LoginPayload) {
      const body = {
        clientId: CLIENT_ID,
        grantType: 'password',
        username: payload.username,
        password: payload.password,
        tenantId: '000000',
      }
      const data = await httpPost<LoginResponse>('/auth/login', body)
      token.value = data.access_token
      localStorage.setItem(STORAGE_TOKEN_KEY, data.access_token)
      return data
    }

    async function loadProfile() {
      const info = await http<UserInfoResponse>('/system/user/getInfo')
      userId.value = info.user.userId
      userName.value = info.user.userName
      nickName.value = info.user.nickName
      avatar.value = info.user.avatar ?? ''
      roles.value = info.roles ?? []
      permissions.value = info.permissions ?? []
    }

    async function loadRouters() {
      const data = await http<RouterEntry[]>('/system/menu/getRouters')
      routers.value = data ?? []
    }

    function logout() {
      token.value = null
      nickName.value = ''
      userName.value = ''
      userId.value = null
      avatar.value = ''
      roles.value = []
      permissions.value = []
      routers.value = []
      localStorage.removeItem(STORAGE_TOKEN_KEY)
      // 同时清持久化的 user store（不然刷新会从 localStorage 拿到旧 roles）
      localStorage.removeItem('nev:user')
    }

    return {
      token,
      userId,
      userName,
      nickName,
      avatar,
      roles,
      permissions,
      routers,
      isLoggedIn,
      isSuperAdmin,
      hasRole,
      hasPerm,
      login,
      loadProfile,
      loadRouters,
      logout,
    }
  },
  {
    persist: {
      key: 'nev:user',
      pick: ['nickName', 'userName', 'userId', 'avatar', 'roles', 'permissions'],
    },
  },
)
