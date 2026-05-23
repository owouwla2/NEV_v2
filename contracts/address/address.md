# Wave 2 D11 部署地址清单

> **链**：FISCO BCOS group_1
> **部署日期**：2026-05-22
> **部署入口**：WeBASE-Web 5000（→ 老仓 WeBASE 集群）
> **本地后端 SDK 入口**：WeBASE-Front 5002（/trans/handle）

## 合约

| 名称 | 地址 |
|---|---|
| `LifecycleTrace`（含 BatteryRegistry + RoleManager） | `0xf71701365b8b35d4a03a12ecc51edf5fd5797b08` |

## 签名用户钱包

| 用户名 | 地址 | 链上 Role | 对应 sys_user.user_id |
|---|---|---:|---:|
| `admin1` | `0x6933f6d76d71b7ca66f70f3faf6b108a10697aa2` | 1 (ADMIN) | — |
| `producer1` | `0x501d135cc0c493ea423b5799bea95d2b1bd55d8c` | 2 (PRODUCER) | 101 |
| `distrib1` | `0x133dc9f28eb0da0d595104659d4d686e1fdd294d` | 3 (DISTRIBUTOR) | 102 |
| `retailer1` | `0x7a02799727b975cdbde2a8b443b5b6ea7b24c0ce` | 4 (RETAILER) | 103 |
| `merchant1` | `0x4cae790a0f2393c37008147c937411720aac510f` | 5 (MERCHANT) | 104 |
| `consumer1` | `0x8385dd8e88641e7e50914369f9ab4cfcb65fc623` | 6 (CONSUMER) | 105 |
| `recycler1` | `0xe3ca5516bb6cfac3faaf1883ddf987c7d9ee5eb9` | 7 (RECYCLER) | 106 |

## 私钥所在位置

| 用户 | 位置 | 备注 |
|---|---|---|
| admin1 | `contracts/pk/admin1_key_*.p12`（已 gitignore）+ 已导入 WeBASE-Front 5002 本地 | 空密码，后端用 `/trans/handle` 调用 |
| 其余 6 个 | 仅在 WeBASE-Web 5000/NodeManager 数据库 | 暂未导入 Front；D12 需要 producer1 等签名时再导出导入 |

## 链上授权记录

通过 `/trans/handle` 调用 6 次 `grantRole(account, role)`，全部 HTTP 200 + `code:0`。
验证 `getRole`/`hasRole` 返回值与设计一致（见 HANDOFF.md §2.16 D11 完成清单）。

## 链上状态查询示例

```bash
# 用本地 Python 调 /trans/handle 查 producer1 角色
curl -X POST http://localhost:5002/WeBASE-Front/trans/handle \
  -H 'Content-Type: application/json' \
  -d '{
    "groupId": 1,
    "user": "0x6933f6d76d71b7ca66f70f3faf6b108a10697aa2",
    "contractName": "LifecycleTrace",
    "contractAddress": "0xf71701365b8b35d4a03a12ecc51edf5fd5797b08",
    "contractAbi": <见 contracts/build/LifecycleTrace.abi>,
    "funcName": "getRole",
    "funcParam": ["0x501d135cc0c493ea423b5799bea95d2b1bd55d8c"]
  }'
# 期望返回 ["2"]
```
