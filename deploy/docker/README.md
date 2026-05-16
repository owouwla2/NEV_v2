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
| Redis | 6379 | **6379** | 与老仓 Redis 端口相同；本仓 backend 用 db=1 隔离 |

如老仓 Redis 已占用 6379：编辑 `.env` 把 `REDIS_PORT=6479`，并同步改 `backend/ruoyi-admin/src/main/resources/application-dev.yml` 的 `spring.data.redis.port`。

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

## 导入 RuoYi 系统表

```bash
# 1. 等 MySQL 健康（docker compose ps 显示 (healthy)）
docker exec -i nev-v2-mysql mysql -uroot -pnev_v2_root_2026 nev_v2 \
  < ../../backend/script/sql/ry_vue_5.X.sql

# 2. 导入 SnailJob 调度表（RuoYi-Plus 5.6.1 默认依赖）
docker exec -i nev-v2-mysql mysql -uroot -pnev_v2_root_2026 nev_v2 \
  < ../../backend/script/sql/ry_job.sql

# 3. 验证（应看到一批 sys_* 与 sj_* 表）
docker exec -i nev-v2-mysql mysql -uroot -pnev_v2_root_2026 -e "SHOW TABLES;" nev_v2
```

> **不**导入 `ry_workflow.sql`：本项目需求已明确不用 warm-flow 工作流。

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
- Redis 共用 6379 但 db 隔离（老仓 db=0，本仓 db=1）
