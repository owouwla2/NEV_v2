import type { Component } from 'vue'
import {
  Circle,
  Battery,
  ShoppingCart,
  Leaf,
  Link2,
  Recycle,
  Repeat2,
  Settings,
  Building2,
  Activity,
  Wrench,
  Workflow,
  ListTodo,
  Globe,
  FlaskConical,
  User,
  Users,
  ShieldCheck,
  Menu,
  ServerCog,
  Database,
  Bell,
  Sliders,
  Code2,
  ScrollText,
  FileText,
  Eye,
  Search,
  Plus,
  Edit,
  Trash2,
  Save,
  Coins,
  RefreshCw,
  Truck,
  Tag,
  Receipt,
  ClipboardList,
  Gauge,
  CheckSquare,
  Building,
  KeyRound,
} from 'lucide-vue-next'

/**
 * 把后端 sys_menu.icon 字段（RuoYi element-icon 名字）映射到 lucide-vue-next 组件
 *
 * 缺失的 icon 渲染为 Circle 占位
 */
export const iconMap: Record<string, Component> = {
  // 业务
  battery: Battery,
  shopping: ShoppingCart,
  leaf: Leaf,
  lock: Link2, // 区块链
  refresh: Repeat2, // 以旧换新
  cascader: Recycle, // 回收
  // 系统
  system: Settings,
  monitor: Activity,
  tool: Wrench,
  workflow: Workflow,
  task: ListTodo,
  globe: Globe,
  guide: Building2,
  tenant: Building,
  // 通用
  user: User,
  peoples: Users,
  people: User,
  'user-1': Users,
  list: ScrollText,
  edit: Edit,
  add: Plus,
  delete: Trash2,
  search: Search,
  eye: Eye,
  view: Eye,
  save: Save,
  checkbox: CheckSquare,
  money: Coins,
  swap: RefreshCw,
  truck: Truck,
  goods: Tag,
  order: Receipt,
  chart: Gauge,
  log: ClipboardList,
  document: FileText,
  password: KeyRound,
  validCode: ShieldCheck,
  job: Sliders,
  build: Code2,
  redis: Database,
  server: ServerCog,
  message: Bell,
  online: Activity,
  druid: FlaskConical,
  example: Menu,
}

export function resolveIcon(icon?: string | null): Component {
  if (!icon) return Circle
  return iconMap[icon] ?? Circle
}
