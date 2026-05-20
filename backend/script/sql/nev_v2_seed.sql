-- =============================================================================
-- NEV-v2 种子数据：业务角色 + 业务菜单 + 角色菜单绑定
-- =============================================================================
-- 项目：NEV-v2（动力电池全产业链溯源平台）
-- 数据库：nev_v2
-- 依赖：ry_vue_5.X.sql 已导入（sys_role / sys_menu / sys_role_menu 已建表）
-- 内容：
--   - 6 个业务角色（admin 已存在为 superadmin，复用即可）
--   - 6 个一级目录 + 15 个二级菜单 = 21 项业务菜单
--   - 各角色对应菜单授权（按需求文档 §3.3 角色权限矩阵）
-- 编号占用：role_id 11-16 / menu_id 2001-2061
-- 注：demo 用户、排放因子库、商品分类等业务种子数据放在 D5-D6 完成
-- =============================================================================

SET NAMES utf8mb4;

-- =============================================================================
-- 1. 业务角色（role_id 11-16）
-- =============================================================================
-- data_scope: 5 = 仅本人数据权限（业务角色默认仅看自己数据，admin 走 superadmin）
-- role_key 与 RuoYi 注解 @SaCheckRole 对应

delete from sys_role where role_id in (11, 12, 13, 14, 15, 16);

insert into sys_role values (11, '000000', '电池生产商',   'producer',    11, 5, 1, 1, '0', '0', 103, 1, sysdate(), null, null, '电池厂商角色，可注册电池 + 出厂上链');
insert into sys_role values (12, '000000', '经销商',       'distributor', 12, 5, 1, 1, '0', '0', 103, 1, sysdate(), null, null, '电池流通环节角色，记录 IN_USE 事件');
insert into sys_role values (13, '000000', '零售商',       'retailer',    13, 5, 1, 1, '0', '0', 103, 1, sysdate(), null, null, '4S 店 / 终端门店角色，记录 SOLD 事件');
insert into sys_role values (14, '000000', '商城商家',     'merchant',    14, 5, 1, 1, '0', '0', 103, 1, sysdate(), null, null, '商城运营角色，管理商品 + 订单');
insert into sys_role values (15, '000000', '终端消费者',   'consumer',    15, 5, 1, 1, '0', '0', 103, 1, sysdate(), null, null, '消费者角色，下单 + 以旧换新 + 碳积分');
insert into sys_role values (16, '000000', '回收处理商',   'recycler',    16, 5, 1, 1, '0', '0', 103, 1, sysdate(), null, null, '回收商角色，评估 + 回收 + 记录 RECYCLED 事件');

-- =============================================================================
-- 2. 业务菜单（menu_id 2001-2061）
-- =============================================================================
-- 字段顺序：(menu_id, menu_name, parent_id, order_num, path, component, query_param,
--          is_frame, is_cache, menu_type, visible, status, perms, icon,
--          create_dept, create_by, create_time, update_by, update_time, remark)

delete from sys_menu where menu_id between 2001 and 2999;

-- ---------- 2001 电池溯源 ----------
insert into sys_menu values (2001, '电池溯源', 0,    10, 'battery',         null, '', 1, 0, 'M', '0', '0', '',                          'battery',  103, 1, sysdate(), null, null, '电池溯源目录');
insert into sys_menu values (2010, '电池注册', 2001, 1,  'register',        'nev/battery/register/index',  '', 1, 0, 'C', '0', '0', 'nev:battery:register', 'add',      103, 1, sysdate(), null, null, '生产商注册电池数字身份');
insert into sys_menu values (2011, '电池流通', 2001, 2,  'transfer',        'nev/battery/transfer/index',  '', 1, 0, 'C', '0', '0', 'nev:battery:transfer', 'swap',     103, 1, sysdate(), null, null, '记录 IN_USE / SOLD 流通事件');
insert into sys_menu values (2012, '电池查询', 2001, 3,  'query',           'nev/battery/query/index',     '', 1, 0, 'C', '0', '0', 'nev:battery:query',    'search',   103, 1, sysdate(), null, null, '查询电池数字身份与生命周期');

-- ---------- 2002 商城管理 ----------
insert into sys_menu values (2002, '商城管理', 0,    20, 'marketplace',     null, '', 1, 0, 'M', '0', '0', '',                          'shopping', 103, 1, sysdate(), null, null, '商城管理目录');
insert into sys_menu values (2020, '商品管理', 2002, 1,  'product',         'nev/marketplace/product/index', '', 1, 0, 'C', '0', '0', 'nev:marketplace:product', 'goods', 103, 1, sysdate(), null, null, '商家上下架商品');
insert into sys_menu values (2021, '订单管理', 2002, 2,  'order',           'nev/marketplace/order/index',   '', 1, 0, 'C', '0', '0', 'nev:marketplace:order',   'order', 103, 1, sysdate(), null, null, '订单查看与状态流转');

-- ---------- 2003 碳核算 ----------
insert into sys_menu values (2003, '碳核算',   0,    30, 'carbon',          null, '', 1, 0, 'M', '0', '0', '',                          'leaf',     103, 1, sysdate(), null, null, '碳核算目录');
insert into sys_menu values (2030, '碳足迹查询', 2003, 1, 'footprint',      'nev/carbon/footprint/index',  '', 1, 0, 'C', '0', '0', 'nev:carbon:footprint', 'chart',    103, 1, sysdate(), null, null, '查询电池全生命周期碳足迹');
insert into sys_menu values (2031, '排放因子库', 2003, 2, 'factor',         'nev/carbon/factor/index',     '', 1, 0, 'C', '0', '0', 'nev:carbon:factor',    'list',     103, 1, sysdate(), null, null, '维护排放因子库');
insert into sys_menu values (2032, '碳积分账户', 2003, 3, 'credit',         'nev/carbon/credit/index',     '', 1, 0, 'C', '0', '0', 'nev:carbon:credit',    'money',    103, 1, sysdate(), null, null, '查看碳积分账户与流水');

-- ---------- 2004 区块链 ----------
insert into sys_menu values (2004, '区块链',   0,    40, 'blockchain',      null, '', 1, 0, 'M', '0', '0', '',                          'lock',     103, 1, sysdate(), null, null, '区块链目录');
insert into sys_menu values (2040, '链上溯源', 2004, 1,  'trace',           'nev/blockchain/trace/index',  '', 1, 0, 'C', '0', '0', 'nev:blockchain:trace',  'eye',     103, 1, sysdate(), null, null, '查看链上交易与事件');
insert into sys_menu values (2041, '合约配置', 2004, 2,  'config',          'nev/blockchain/config/index', '', 1, 0, 'C', '0', '0', 'nev:blockchain:config', 'edit',    103, 1, sysdate(), null, null, '管理合约地址与 ABI');

-- ---------- 2005 以旧换新 ----------
insert into sys_menu values (2005, '以旧换新', 0,    50, 'tradein',         null, '', 1, 0, 'M', '0', '0', '',                          'refresh',  103, 1, sysdate(), null, null, '以旧换新目录');
insert into sys_menu values (2050, '换新申请', 2005, 1,  'request',         'nev/tradein/request/index',   '', 1, 0, 'C', '0', '0', 'nev:tradein:request',  'edit',     103, 1, sysdate(), null, null, '消费者提交换新申请');
insert into sys_menu values (2051, '评估处理', 2005, 2,  'evaluate',        'nev/tradein/evaluate/index',  '', 1, 0, 'C', '0', '0', 'nev:tradein:evaluate', 'checkbox', 103, 1, sysdate(), null, null, '回收商评估换新申请');

-- ---------- 2006 回收管理 ----------
insert into sys_menu values (2006, '回收管理', 0,    60, 'recycle',         null, '', 1, 0, 'M', '0', '0', '',                          'cascader', 103, 1, sysdate(), null, null, '回收管理目录');
insert into sys_menu values (2060, '回收申请', 2006, 1,  'request',         'nev/recycle/request/index',   '', 1, 0, 'C', '0', '0', 'nev:recycle:request', 'edit',     103, 1, sysdate(), null, null, '消费者直接提交回收申请');
insert into sys_menu values (2061, '回收处理', 2006, 2,  'process',         'nev/recycle/process/index',   '', 1, 0, 'C', '0', '0', 'nev:recycle:process', 'tool',     103, 1, sysdate(), null, null, '回收商处理回收单');

-- =============================================================================
-- 3. 角色-菜单绑定（按需求文档 §3.3 角色权限矩阵）
-- =============================================================================
-- 共用菜单：所有业务角色都能看"区块链 → 链上溯源"（2004, 2040），并可查看自己相关数据
-- admin（role_id=1）默认 superadmin，菜单权限通过 sys_role_menu 已覆盖全部；不在此处重复

-- 先清空旧绑定，避免重复执行报错
delete from sys_role_menu where role_id in (11, 12, 13, 14, 15, 16) and menu_id between 2001 and 2999;

-- ---------- producer（11）：电池注册 + 电池查询 + 碳足迹查询 + 区块链浏览 ----------
insert into sys_role_menu values (11, 2001);
insert into sys_role_menu values (11, 2010);
insert into sys_role_menu values (11, 2012);
insert into sys_role_menu values (11, 2003);
insert into sys_role_menu values (11, 2030);
insert into sys_role_menu values (11, 2004);
insert into sys_role_menu values (11, 2040);

-- ---------- distributor（12）：电池流通 + 电池查询 + 区块链浏览 ----------
insert into sys_role_menu values (12, 2001);
insert into sys_role_menu values (12, 2011);
insert into sys_role_menu values (12, 2012);
insert into sys_role_menu values (12, 2004);
insert into sys_role_menu values (12, 2040);

-- ---------- retailer（13）：电池流通 + 电池查询 + 商品管理 + 订单管理 + 区块链浏览 ----------
insert into sys_role_menu values (13, 2001);
insert into sys_role_menu values (13, 2011);
insert into sys_role_menu values (13, 2012);
insert into sys_role_menu values (13, 2002);
insert into sys_role_menu values (13, 2020);
insert into sys_role_menu values (13, 2021);
insert into sys_role_menu values (13, 2004);
insert into sys_role_menu values (13, 2040);

-- ---------- merchant（14）：商品管理 + 订单管理 + 区块链浏览 ----------
insert into sys_role_menu values (14, 2002);
insert into sys_role_menu values (14, 2020);
insert into sys_role_menu values (14, 2021);
insert into sys_role_menu values (14, 2004);
insert into sys_role_menu values (14, 2040);

-- ---------- consumer（15）：订单查看 + 以旧换新申请 + 回收申请 + 碳积分账户 + 区块链浏览 ----------
insert into sys_role_menu values (15, 2002);
insert into sys_role_menu values (15, 2021);
insert into sys_role_menu values (15, 2005);
insert into sys_role_menu values (15, 2050);
insert into sys_role_menu values (15, 2006);
insert into sys_role_menu values (15, 2060);
insert into sys_role_menu values (15, 2003);
insert into sys_role_menu values (15, 2032);
insert into sys_role_menu values (15, 2004);
insert into sys_role_menu values (15, 2040);

-- ---------- recycler（16）：评估处理 + 回收处理 + 碳足迹查询 + 区块链浏览 ----------
insert into sys_role_menu values (16, 2005);
insert into sys_role_menu values (16, 2051);
insert into sys_role_menu values (16, 2006);
insert into sys_role_menu values (16, 2061);
insert into sys_role_menu values (16, 2003);
insert into sys_role_menu values (16, 2030);
insert into sys_role_menu values (16, 2004);
insert into sys_role_menu values (16, 2040);

-- ---------- admin（1）：为新业务菜单全部授权 ----------
delete from sys_role_menu where role_id = 1 and menu_id between 2001 and 2999;
insert into sys_role_menu (role_id, menu_id)
select 1, menu_id from sys_menu where menu_id between 2001 and 2999;

-- =============================================================================
-- 验证查询（执行完后可手工跑）：
-- 1. 角色清单：       select role_id, role_name, role_key from sys_role order by role_id;
-- 2. 业务菜单清单：   select menu_id, menu_name, parent_id, menu_type, perms from sys_menu where menu_id between 2001 and 2999 order by parent_id, order_num;
-- 3. 角色权限矩阵：   select r.role_key, m.menu_name from sys_role_menu rm
--                       join sys_role r on r.role_id = rm.role_id
--                       join sys_menu m on m.menu_id = rm.menu_id
--                       where r.role_id in (1,11,12,13,14,15,16) and m.menu_id between 2001 and 2999
--                       order by r.role_id, m.menu_id;
-- =============================================================================
