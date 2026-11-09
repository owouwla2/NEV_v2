# NEV-v2 docker 依赖

本目录提供 NEV-v2 后端开发所依赖的 **MySQL 8 + Redis 7** 容器编排。

## 启动

```bash
cd deploy/docker
# 可选：cp .env.example .env 后改密码/端口
docker compose up -d
docker compose ps          # 看 health 状态
docker compose logs -f mysql   # 看 MySQL 启动日志
```

## 端口

| 服务 | 容器内 | 主机映射 | 说明 |
|---|---|---|---|
| MySQL | 3306 | **13306** | 避开老仓 WeBASE 23306 与 RuoYi 默认 3306 |
| Redis | 6379 | **16379** | ⚠️ Hyper-V 保留 6305-7404（含 6379/6479），改用 16379；本仓 backend 用 db=1 隔离 |

Redis 主机端口由 `.env` 的 `REDIS_PORT` 控制（当前 **16379**）。⚠️ 不要改回 6379/6479——两者都落在 Windows Hyper-V 动态保留区间（`netsh int ipv4 show excludedportrange protocol=tcp` 可查），bind 会报 access permissions 错误。改端口需同步改 `backend/ruoyi-admin/src/main/resources/application-dev.yml` 的 `spring.data.redis.port`。

## 凭据（默认）

| 项 | 值 |
|---|---|
| MySQL root 密码 | `nev_v2_root_2026` |
| MySQL 默认库 | `nev_v2`（首次启动自动创建） |
| Redis 密码 | `nev_v2_redis_2026` |

> 演示场景密码硬编码无敏感信息；生产环境请用 `.env` 覆盖。

## 数据持久化

- MySQL 数据 → `./volumes/mysql/data/`
- Redis 数据 → `./volumes/redis/data/`
- 已写进 `.gitignore`，不入库

## 销毁与重建

```bash
docker compose down              # 仅停止
docker compose down -v           # 停止并删除匿名 volume（注意：本配置用 bind mount，不会删 ./volumes）
rm -rf volumes/                  # 真正清除数据
```

## 数据库初始化

**首选：自动导入（2026-11-09 起已生效）**。6 个 SQL 已按序号放入 `volumes/mysql/init/`
（01 基础表 → 02 调度表 → 03 业务表 → 04 链上绑定 → 05 种子 → 06 演示种子），由 `backend/script/sql/`
同步而来（`.gitignore` 已放行该目录）。首次启动（空 `volumes/mysql/data/`）时 docker-entrypoint
按序号顺序自动执行，实测导入约 68 张表。schema 变更后需重新同步到 init/。

> 每个脚本头部都带 `SET NAMES utf8mb4;`：容器内 mysql 客户端默认会话字符集为 latin1，
> 会导致中文按字节膨胀、触发 `Data too long for column 'nick_name'` 中断（2026-11-09 实测踩坑）。

**兜底：手动导入**（`data/` 已有历史数据但缺表、或 init/ 与源脚本失同步时）：

```bash
# 1. RuoYi 基础表（sys_*）
docker exec -i nev-v2-mysql mysql -uroot -pnev_v2_root_2026 nev_v2 \
  < ../../backend/script/sql/ry_vue_5.X.sql

# 2. SnailJob 调度表（sj_*，RuoYi-Plus 5.6.1 默认依赖）
docker exec -i nev-v2-mysql mysql -uroot -pnev_v2_root_2026 nev_v2 \
  < ../../backend/script/sql/ry_job.sql

# 3. NEV 业务表（20 张 nev_* / sys_nev_*）
docker exec -i nev-v2-mysql mysql -uroot -pnev_v2_root_2026 nev_v2 \
  < ../../backend/script/sql/nev_v2_business.sql

# 4. 链上合约地址绑定（可重跑）
docker exec -i nev-v2-mysql mysql -uroot -pnev_v2_root_2026 nev_v2 \
  < ../../backend/script/sql/nev_v2_d11_chain_binding.sql

# 5. 业务角色 + 菜单种子
docker exec -i nev-v2-mysql mysql -uroot -pnev_v2_root_2026 nev_v2 \
  < ../../backend/script/sql/nev_v2_seed.sql

# 6. 演示种子（6 demo 用户 + 排放因子库 + 商品分类字典）
docker exec -i nev-v2-mysql mysql -uroot -pnev_v2_root_2026 nev_v2 \
  < ../../backend/script/sql/nev_v2_seed_demo.sql

# 验证（应看到约 68 张表：sys_* / sj_* / nev_*）
docker exec -i nev-v2-mysql mysql -uroot -pnev_v2_root_2026 -e "SHOW TABLES;" nev_v2
```

> **不**导入 `ry_workflow.sql`：本项目需求已明确不用 warm-flow 工作流。历史 `data/` 里若残留
> `flow_*` / `test_leave` 表（warm-flow 遗留），不影响运行，可忽略。

## 故障排查

| 现象 | 处理 |
|---|---|
| `port is already allocated` | 改 `.env` 的 `MYSQL_PORT` / `REDIS_PORT` |
| MySQL 启动后立即退出 | 多半是上一次的 `volumes/mysql/data/` 残留与新密码冲突，删了再起 |
| backend 报 `Communications link failure` | `docker compose ps` 看 mysql 是否 `(healthy)`；不健康看 `logs mysql` |
| backend Redis 报 `WRONGPASS` | application-*.yml 的 `password` 与 `.env` 不一致 |

## 与老仓 NEV 共存

- 老仓 backend 在 9180 / admin-web 在 8020 / WeBASE MySQL 在 23306
- 新仓 backend 在 9280 / admin-web 在 8120 / 本目录 MySQL 在 13306
- Redis 本仓映射 **16379**（老仓 6379）；本仓 backend 用 db=1 隔离
