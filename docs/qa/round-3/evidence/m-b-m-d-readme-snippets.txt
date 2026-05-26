71:**校验机制**：MySQL `nev_battery_lifecycle` 每条记录保存 `tx_hash + version + data_hash`；前端公开扫码页对每条事件调链 `verifyEvent()` 反验，整体 `overallVerified = (MySQL count - PRODUCED == 链上 count) && (每条 verifyEvent/verifyBattery == true)`。链上 `events` 数组不含 PRODUCED（PRODUCED 走 `registerBattery` 单独存），扫码服务根据 event_type 分流到 `verifyEvent` 或 `verifyBattery`（详见 `BatteryScanService`）。
115:## 路径速查（NEV-v2 fix P3-002 / round-3 M-D）
133:**业务表全部带 `nev_` 前缀**：`nev_battery`, `nev_battery_lifecycle`, `nev_product`, `nev_cart`, `nev_order`, `nev_trade_in_request`, `nev_carbon_*`。
147:    ├── nev-battery/           # nev_battery + nev_battery_lifecycle + 公开扫码 BatteryScanVO
224:| Redis | **16379** | ⚠️ Windows + Hyper-V 默认保留 6305-7404（含 6379/6479），改用 16379；业务通过 `database=1` 与老仓隔离 |
229:> **⚠️ Windows Hyper-V 端口保留陷阱（NEV-v2 fix B-1）**
231:> Windows 10/11 启用 Hyper-V 或 WSL2 时会动态保留一段端口（每次开机不同），常见保留范围包含
236:> 解决：使用 16379（已是当前默认）。排查命令：
258:MySQL 初始化 SQL 会通过 `volumes/mysql/init/*.sql` 自动导入：RuoYi 基础表 + 7 角色 sys_user / sys_role + 业务表（`nev_battery` / `nev_battery_lifecycle` / `nev_product` / `nev_cart` / `nev_order` / `nev_trade_in_request` / `nev_carbon_*`）。所有业务表统一带 `nev_` 前缀。
