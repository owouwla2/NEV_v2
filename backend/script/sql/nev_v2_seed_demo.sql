-- =============================================================================
-- NEV-v2 演示种子数据：6 demo 用户 + 排放因子库 + 商品分类字典
-- =============================================================================
-- 项目：NEV-v2（动力电池全产业链溯源平台）
-- 数据库：nev_v2
-- 依赖：
--   - ry_vue_5.X.sql 已导入（sys_user / sys_user_role / sys_dict_type / sys_dict_data 已建表）
--   - nev_v2_business.sql 已导入（sys_nev_user_ext / nev_emission_factor 已建表）
--   - nev_v2_seed.sql 已导入（业务角色 11-16 已注册）
-- 内容：
--   - 6 个 demo 用户（producer1/distributor1/retailer1/merchant1/consumer1/recycler1），统一密码 admin123
--   - 15 条排放因子（5 阶段：RAW 5/MFG 4/TRANS 3/USE 2/EOL 1）
--   - 商品分类字典 nev_product_category（BATTERY/ACCESSORY/SERVICE）
-- 编号占用：
--   - user_id: 101-106
--   - sys_dict_type.dict_id: 100
--   - sys_dict_data.dict_code: 1001-1003
--   - nev_emission_factor.id: 由 MySQL 自增（业务层用 MyBatis-Plus 雪花，本处直接用 1-15）
-- 注：生产部署可以只导基础 seed（nev_v2_seed.sql），不导本文件
-- =============================================================================

SET NAMES utf8mb4;

-- =============================================================================
-- 1. 6 个 demo 用户（每个业务角色一名）
-- =============================================================================
-- password = admin123（BCrypt hash 复用 RuoYi 自带 admin 的 hash）
-- dept_id = 103（RuoYi 默认部门 ID）
-- create_by = 1（admin 创建）

delete from sys_user_role where user_id between 101 and 106;
delete from sys_nev_user_ext where user_id between 101 and 106;
delete from sys_user where user_id between 101 and 106;

insert into sys_user values (101, '000000', 103, 'producer1',    '宁德时代示范厂', 'sys_user', 'producer1@nev.demo',    '13800000101', '0', null, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), 103, 1, sysdate(), null, null, 'demo:电池生产商');
insert into sys_user values (102, '000000', 103, 'distributor1', '华东能源经销商', 'sys_user', 'distributor1@nev.demo', '13800000102', '0', null, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), 103, 1, sysdate(), null, null, 'demo:经销商');
insert into sys_user values (103, '000000', 103, 'retailer1',    '示范 4S 店',     'sys_user', 'retailer1@nev.demo',    '13800000103', '0', null, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), 103, 1, sysdate(), null, null, 'demo:零售商');
insert into sys_user values (104, '000000', 103, 'merchant1',    'NEV 配件商城',   'sys_user', 'merchant1@nev.demo',    '13800000104', '0', null, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), 103, 1, sysdate(), null, null, 'demo:商城商家');
insert into sys_user values (105, '000000', 103, 'consumer1',    '张三',           'sys_user', 'consumer1@nev.demo',    '13800000105', '0', null, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), 103, 1, sysdate(), null, null, 'demo:终端消费者');
insert into sys_user values (106, '000000', 103, 'recycler1',    '绿能回收处理厂', 'sys_user', 'recycler1@nev.demo',    '13800000106', '0', null, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), 103, 1, sysdate(), null, null, 'demo:回收处理商');

-- 用户-角色绑定
insert into sys_user_role values (101, 11);
insert into sys_user_role values (102, 12);
insert into sys_user_role values (103, 13);
insert into sys_user_role values (104, 14);
insert into sys_user_role values (105, 15);
insert into sys_user_role values (106, 16);

-- 业务扩展（钱包地址 / 企业名称 / 用户类型）
insert into sys_nev_user_ext (user_id, user_type, wallet_address, company_name, company_license, tenant_id, del_flag, create_dept, create_by, create_time, remark) values
(101, 'producer',    '0x1010000000000000000000000000000000000101', '宁德时代示范厂（demo）',     '91350000DEMO000001', '000000', '0', 103, 1, sysdate(), '本轮 wallet_address 为占位，Wave 2 D11 绑链时回填'),
(102, 'distributor', '0x1010000000000000000000000000000000000102', '华东能源经销商（demo）',     '91310000DEMO000002', '000000', '0', 103, 1, sysdate(), null),
(103, 'retailer',    '0x1010000000000000000000000000000000000103', '示范 4S 店（demo）',         '91310000DEMO000003', '000000', '0', 103, 1, sysdate(), null),
(104, 'merchant',    '0x1010000000000000000000000000000000000104', 'NEV 配件商城（demo）',       '91310000DEMO000004', '000000', '0', 103, 1, sysdate(), null),
(105, 'consumer',    '0x1010000000000000000000000000000000000105', null,                          null,                '000000', '0', 103, 1, sysdate(), 'consumer 不强制企业信息'),
(106, 'recycler',    '0x1010000000000000000000000000000000000106', '绿能回收处理厂（demo）',     '91320000DEMO000006', '000000', '0', 103, 1, sysdate(), null);

-- =============================================================================
-- 2. 排放因子库（5 阶段共 15 条）
-- =============================================================================
-- value 单位见 unit 列；本轮值为 LCA 文献近似值，仅供演示
-- Wave 3 D20 碳计算引擎按 (battery 用料/产能/运距) × 对应因子求和
-- source 标注关键引用：CLCD（中国生命周期数据库）/ Ecoinvent / IPCC 等

delete from nev_emission_factor where id between 1 and 15;

insert into nev_emission_factor (id, factor_code, factor_name, unit, value, source, applicable_stage, valid_from, valid_to, tenant_id, del_flag, create_dept, create_by, create_time, remark) values
-- ---- RAW 原材料（5 条） ----
( 1, 'RAW-LITHIUM',          '锂（金属/化合物折算）',     'kgCO2eq/kg',      15.000000, 'CLCD-China 2022',           'RAW',   '2024-01-01', null, '000000', '0', 103, 1, sysdate(), '电池正极活性材料关键金属'),
( 2, 'RAW-COBALT',           '钴',                         'kgCO2eq/kg',       7.800000, 'Ecoinvent v3.8',            'RAW',   '2024-01-01', null, '000000', '0', 103, 1, sysdate(), 'NCM/NCA 体系用'),
( 3, 'RAW-NICKEL',           '镍',                         'kgCO2eq/kg',       6.500000, 'Ecoinvent v3.8',            'RAW',   '2024-01-01', null, '000000', '0', 103, 1, sysdate(), '高镍体系用'),
( 4, 'RAW-COPPER',           '铜（负极集流体）',           'kgCO2eq/kg',       3.200000, 'CLCD-China 2022',           'RAW',   '2024-01-01', null, '000000', '0', 103, 1, sysdate(), null),
( 5, 'RAW-ALUMINUM',         '铝（外壳/正极集流体）',      'kgCO2eq/kg',       8.500000, 'IPCC 2019',                 'RAW',   '2024-01-01', null, '000000', '0', 103, 1, sysdate(), null),
-- ---- MFG 制造（4 条） ----
( 6, 'MFG-CELL-ASSEMBLY',    '电芯组装能耗',               'kgCO2eq/kWh',     30.000000, 'China battery LCA 2023',    'MFG',   '2024-01-01', null, '000000', '0', 103, 1, sysdate(), '每 kWh 电池容量制造能耗折算'),
( 7, 'MFG-MODULE-ASSEMBLY',  '模组组装能耗',               'kgCO2eq/kWh',      8.000000, 'China battery LCA 2023',    'MFG',   '2024-01-01', null, '000000', '0', 103, 1, sysdate(), null),
( 8, 'MFG-PACK-ASSEMBLY',    'PACK 组装能耗',              'kgCO2eq/kWh',      5.000000, 'China battery LCA 2023',    'MFG',   '2024-01-01', null, '000000', '0', 103, 1, sysdate(), null),
( 9, 'MFG-ELECTRICITY-CN',   '中国电网平均电力',           'kgCO2eq/kWh',      0.570300, '生态环境部 2022 年度因子',  'MFG',   '2024-01-01', null, '000000', '0', 103, 1, sysdate(), '通用电力因子，MFG/USE 共用'),
-- ---- TRANS 运输（3 条） ----
(10, 'TRANS-TRUCK-DIESEL',   '公路运输（柴油重卡）',       'kgCO2eq/(t·km)',   0.090000, 'IPCC 2019',                 'TRANS', '2024-01-01', null, '000000', '0', 103, 1, sysdate(), null),
(11, 'TRANS-RAIL-CN',        '铁路运输（中国电气化）',     'kgCO2eq/(t·km)',   0.018000, 'CLCD-China 2022',           'TRANS', '2024-01-01', null, '000000', '0', 103, 1, sysdate(), null),
(12, 'TRANS-SEA-OCEAN',      '远洋海运',                   'kgCO2eq/(t·km)',   0.010000, 'IMO 2020',                  'TRANS', '2024-01-01', null, '000000', '0', 103, 1, sysdate(), null),
-- ---- USE 使用（2 条） ----
(13, 'USE-CHARGE-CN-GRID',   '充电（中国电网）',           'kgCO2eq/kWh',      0.570300, '生态环境部 2022',           'USE',   '2024-01-01', null, '000000', '0', 103, 1, sysdate(), '与 MFG-ELECTRICITY-CN 数值相同，但语义在 USE 阶段'),
(14, 'USE-EV-AVOID',         '替代燃油车减排参考',         'kgCO2eq/100km',  -10.500000, '蔚来白皮书 2023',           'USE',   '2024-01-01', null, '000000', '0', 103, 1, sysdate(), '负值表示减排，作为 USE 阶段抵扣参考'),
-- ---- EOL 报废回收（1 条） ----
(15, 'EOL-RECYCLE-CREDIT',   '梯次/拆解回收抵扣',          'kgCO2eq/kg',      -2.500000, 'China battery recycle LCA 2022','EOL', '2024-01-01', null, '000000', '0', 103, 1, sysdate(), '负值表示通过回收避免的等量原材料生产排放');

-- =============================================================================
-- 3. 商品分类字典（nev_product_category）
-- =============================================================================
-- Wave 3 D17 商城下拉框用
-- 字段：(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)

delete from sys_dict_data where dict_type = 'nev_product_category';
delete from sys_dict_type where dict_type = 'nev_product_category';

insert into sys_dict_type values (100, '000000', '商品分类', 'nev_product_category', 103, 1, sysdate(), null, null, 'NEV 商城商品分类');

insert into sys_dict_data values (1001, '000000', 1, '电池', 'BATTERY',   'nev_product_category', '', 'primary', 'Y', 103, 1, sysdate(), null, null, '电池本体（含 PACK / 模组 / 电芯）');
insert into sys_dict_data values (1002, '000000', 2, '配件', 'ACCESSORY', 'nev_product_category', '', 'success', 'N', 103, 1, sysdate(), null, null, '充电器 / BMS / 线缆等配件');
insert into sys_dict_data values (1003, '000000', 3, '服务', 'SERVICE',   'nev_product_category', '', 'warning', 'N', 103, 1, sysdate(), null, null, '检测 / 维修 / 安装等服务');

-- =============================================================================
-- 验证查询（导入后手工跑）：
-- 1. demo 用户：
--    select u.user_id, u.user_name, u.nick_name, r.role_key, e.user_type, e.wallet_address
--    from sys_user u
--    join sys_user_role ur on ur.user_id = u.user_id
--    join sys_role r on r.role_id = ur.role_id
--    left join sys_nev_user_ext e on e.user_id = u.user_id
--    where u.user_id between 101 and 106 order by u.user_id;
--
-- 2. 排放因子库：
--    select applicable_stage, count(*) as cnt, group_concat(factor_code order by id separator ', ') as codes
--    from nev_emission_factor where id between 1 and 15
--    group by applicable_stage order by field(applicable_stage,'RAW','MFG','TRANS','USE','EOL');
--
-- 3. 商品分类字典：
--    select d.dict_code, d.dict_sort, d.dict_label, d.dict_value, t.dict_name
--    from sys_dict_data d join sys_dict_type t on t.dict_type = d.dict_type
--    where d.dict_type = 'nev_product_category' order by d.dict_sort;
-- =============================================================================
