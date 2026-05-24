import type { RouteRecordRaw, Router } from 'vue-router'
import type { RouterEntry } from '@/stores/user'

/**
 * 用 Vite 的 import.meta.glob 把所有 views 下的页面预加载映射成"component-key -> dynamic-import"
 *
 * 后端 menu.component 字段格式："system/user/index" 或 "Layout" 等
 * 我们把它当作相对 src/views/ 的路径，去找 src/views/{component}.vue
 *
 * 找不到的页面 fallback 到 PlaceholderView（避免控制台报错）
 */
const modules = import.meta.glob('@/views/**/*.vue')

// 把 modules 的 key 从 '/src/views/system/user/index.vue' 标准化为 'system/user/index'
const componentRegistry: Record<string, () => Promise<unknown>> = {}
for (const fullPath in modules) {
  const key = fullPath
    .replace(/^.*\/views\//, '')
    .replace(/\.vue$/, '')
  componentRegistry[key] = modules[fullPath] as () => Promise<unknown>
}

function loadComponent(componentName: string | undefined): () => Promise<unknown> {
  if (!componentName) return () => import('@/views/PlaceholderView.vue')
  if (componentName === 'Layout' || componentName === 'ParentView') {
    // 顶级菜单的 Layout 占位，展平后不会真渲染
    return () => import('@/views/PlaceholderView.vue')
  }
  // 兼容三种写法：'system/user/index'、'system/user'、'/system/user/index'
  const normalized = componentName.replace(/^\/+/, '')
  const candidates = [normalized, `${normalized}/index`]
  for (const c of candidates) {
    if (componentRegistry[c]) return componentRegistry[c]
  }
  console.warn(`[dynamic-routes] 找不到组件 ${componentName}，使用占位页`)
  return () => import('@/views/PlaceholderView.vue')
}

/**
 * 把后端菜单树**展平**为单层路由（所有页面都挂在 Layout 下）
 *
 * 输入：[{ path: '/system', children: [{ path: 'user', component: 'system/user/index' }] }]
 * 输出：[{ path: 'system/user', name: 'system-user', component: ... }]
 */
function flattenRoutes(entries: RouterEntry[], parentPath = ''): RouteRecordRaw[] {
  const flat: RouteRecordRaw[] = []
  for (const e of entries) {
    if (e.hidden) continue
    const childPath = joinPath(parentPath, e.path)
    if (e.children && e.children.length > 0) {
      // 目录节点：若 component 是 Layout/ParentView，跳过自己；若是真页面，作为一个 route
      if (e.component && e.component !== 'Layout' && e.component !== 'ParentView') {
        flat.push(buildRoute(e, stripLeadingSlash(childPath)))
      }
      flat.push(...flattenRoutes(e.children, childPath))
    } else if (e.component) {
      flat.push(buildRoute(e, stripLeadingSlash(childPath)))
    }
  }
  return flat
}

function buildRoute(e: RouterEntry, fullPath: string): RouteRecordRaw {
  const route = {
    path: fullPath, // 相对路径（挂在 Layout 下）
    name: e.name || fullPath.replace(/\//g, '-'),
    component: loadComponent(e.component),
    meta: {
      title: e.meta?.title,
      icon: e.meta?.icon,
      keepAlive: !e.meta?.noCache,
      link: e.meta?.link,
    },
  }
  return route as unknown as RouteRecordRaw
}

function joinPath(parent: string, child: string): string {
  if (!child) return parent
  if (child.startsWith('/')) return child
  return parent ? `${parent}/${child}` : child
}

function stripLeadingSlash(p: string): string {
  return p.startsWith('/') ? p.slice(1) : p
}

/** 注册标志（防重） */
let dynamicRoutesRegistered = false

export function dynamicRoutesAdded() {
  return dynamicRoutesRegistered
}

export function registerDynamicRoutes(router: Router, entries: RouterEntry[]) {
  if (dynamicRoutesRegistered) return
  const flat = flattenRoutes(entries)
  for (const r of flat) {
    router.addRoute('layout', r)
  }
  dynamicRoutesRegistered = true
}

export function resetDynamicRoutes() {
  dynamicRoutesRegistered = false
}
