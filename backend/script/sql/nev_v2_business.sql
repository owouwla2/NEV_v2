-- =============================================================================
-- NEV-v2 业务表建表脚本
-- =============================================================================
-- 项目：NEV-v2（动力电池全产业链溯源平台）
-- 数据库：nev_v2（MySQL 8.0+ / utf8mb4）
-- 表数量：20 张（需求文档 §5.2）
-- 命名规范：nev_*（业务表） / sys_nev_*（RuoYi 扩展）
-- 主键策略：bigint(20)，MyBatis-Plus 雪花 ID 生成（不使用 AUTO_INCREMENT）
-- 公共字段：tenant_id / del_flag / create_dept / create_by / create_time / update_by / update_time / remark
-- 字段填充：create_by / update_by 为 bigint(20)（与 sys_user.user_id 对齐，配合 MyBatis-Plus MetaObjectHandler 自动填充）
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================================================
-- 分组 1：用户与角色扩展（2 张）
-- =============================================================================

-- ----------------------------
-- 1. RuoYi 用户业务扩展：钱包地址、用户类型、微信 OpenID
-- ----------------------------
drop table if exists sys_nev_user_ext;
create table sys_nev_user_ext (
    user_id          bigint(20)      not null                    comment '用户ID（FK sys_user.user_id）',
    user_type        varchar(20)     not null                    comment '业务用户类型（producer/distributor/retailer/merchant/consumer/recycler/admin）',
    wallet_address   varchar(64)     default null                comment '区块链钱包地址（FISCO BCOS account）',
    wx_openid        varchar(64)     default null                comment '微信 OpenID（小程序登录用）',
    phone_verified   char(1)         default '0'                 comment '手机号是否已验证（0未验证 1已验证）',
    real_name        varchar(50)     default null                comment '实名认证姓名',
    id_card_no       varchar(20)     default null                comment '身份证号（加密存储）',
    company_name     varchar(100)    default null                comment '企业名称（producer/distributor/retailer/merchant/recycler 必填）',
    company_license  varchar(50)     default null                comment '统一社会信用代码',
    tenant_id        varchar(20)     default '000000'            comment '租户编号',
    del_flag         char(1)         default '0'                 comment '删除标志（0代表存在 1代表删除）',
    create_dept      bigint(20)      default null                comment '创建部门',
    create_by        bigint(20)      default null                comment '创建者',
    create_time      datetime                                    comment '创建时间',
    update_by        bigint(20)      default null                comment '更新者',
    update_time      datetime                                    comment '更新时间',
    remark           varchar(500)    default null                comment '备注',
    primary key (user_id),
    unique key uk_wallet_address (wallet_address),
    unique key uk_wx_openid (wx_openid)
) engine=innodb comment = 'RuoYi 用户业务扩展表（钱包地址 / 用户类型 / 企业信息）';

-- ----------------------------
-- 2. 碳积分账户（每用户 1 个，可用余额 + 冻结余额）
-- ----------------------------
drop table if exists nev_carbon_credit_account;
create table nev_carbon_credit_account (
    id               bigint(20)      not null                    comment '主键ID',
    user_id          bigint(20)      not null                    comment '用户ID（FK sys_user.user_id）',
    balance          decimal(18,4)   default 0.0000              comment '可用余额（碳积分单位 kgCO2eq）',
    frozen           decimal(18,4)   default 0.0000              comment '冻结余额（待结算）',
    total_earned     decimal(18,4)   default 0.0000              comment '累计获得',
    total_spent      decimal(18,4)   default 0.0000              comment '累计消耗',
    tenant_id        varchar(20)     default '000000'            comment '租户编号',
    del_flag         char(1)         default '0'                 comment '删除标志',
    create_dept      bigint(20)      default null                comment '创建部门',
    create_by        bigint(20)      default null                comment '创建者',
    create_time      datetime                                    comment '创建时间',
    update_by        bigint(20)      default null                comment '更新者',
    update_time      datetime                                    comment '更新时间',
    remark           varchar(500)    default null                comment '备注',
    primary key (id),
    unique key uk_user_id (user_id)
) engine=innodb comment = '碳积分账户表（每用户 1 个）';

-- =============================================================================
-- 分组 2：电池溯源（4 张）
-- =============================================================================

-- ----------------------------
-- 3. 电池主表（数字身份）
-- ----------------------------
drop table if exists nev_battery;
create table nev_battery (
    id               bigint(20)      not null                    comment '主键ID',
    trace_number     varchar(64)     not null                    comment '溯源编号（业务唯一，对应链上 traceNumber）',
    model            varchar(50)     not null                    comment '电池型号',
    serial_no        varchar(64)     not null                    comment '电芯序列号',
    capacity_kwh     decimal(10,3)   not null                    comment '电池容量（kWh）',
    voltage          decimal(8,2)    default null                comment '额定电压（V）',
    producer_id      bigint(20)      not null                    comment '生产商用户ID（FK sys_user.user_id, role=producer）',
    current_owner_id bigint(20)      default null                comment '当前持有者用户ID',
    current_role     varchar(20)     default 'producer'          comment '当前持有者角色（producer/distributor/retailer/consumer/recycler）',
    current_status   varchar(20)     default 'PRODUCED'          comment '当前生命周期状态（PRODUCED/IN_USE/SOLD/REPAIRED/RECYCLED/DISMANTLED）',
    qr_code_path     varchar(255)    default null                comment '二维码图片 OSS 路径',
    chain_address    varchar(64)     default null                comment '链上合约地址（BatteryRegistry 注册后回填）',
    produced_at      datetime                                    comment '出厂时间',
    tenant_id        varchar(20)     default '000000'            comment '租户编号',
    del_flag         char(1)         default '0'                 comment '删除标志',
    create_dept      bigint(20)      default null                comment '创建部门',
    create_by        bigint(20)      default null                comment '创建者',
    create_time      datetime                                    comment '创建时间',
    update_by        bigint(20)      default null                comment '更新者',
    update_time      datetime                                    comment '更新时间',
    remark           varchar(500)    default null                comment '备注',
    primary key (id),
    unique key uk_trace_number (trace_number),
    key idx_producer_id (producer_id),
    key idx_current_owner (current_owner_id),
    key idx_current_status (current_status)
) engine=innodb comment = '电池主表（数字身份）';

-- ----------------------------
-- 4. 电池生命周期事件（AppendOnly，每事件一行 + 版本号）
-- ----------------------------
drop table if exists nev_battery_lifecycle;
create table nev_battery_lifecycle (
    id               bigint(20)      not null                    comment '主键ID',
    battery_id       bigint(20)      not null                    comment '电池ID（FK nev_battery.id）',
    event_type       varchar(20)     not null                    comment '事件类型（PRODUCED/IN_USE/SOLD/REPAIRED/RECYCLED/DISMANTLED）',
    operator_id      bigint(20)      not null                    comment '操作者用户ID',
    operator_role    varchar(20)     not null                    comment '操作者角色',
    data_hash        varchar(66)     not null                    comment '本次事件数据哈希（keccak256，对应链上）',
    tx_hash          varchar(66)     default null                comment '链上交易哈希（上链成功后回填）',
    block_number     bigint(20)      default null                comment '区块高度',
    version          int(11)         not null    default 1       comment '事件版本号（按 battery_id 单调递增）',
    payload          json            default null                comment '事件附加数据（json，含 location/handover_to 等）',
    event_time       datetime        not null                    comment '事件发生时间',
    tenant_id        varchar(20)     default '000000'            comment '租户编号',
    del_flag         char(1)         default '0'                 comment '删除标志',
    create_dept      bigint(20)      default null                comment '创建部门',
    create_by        bigint(20)      default null                comment '创建者',
    create_time      datetime                                    comment '创建时间',
    update_by        bigint(20)      default null                comment '更新者',
    update_time      datetime                                    comment '更新时间',
    remark           varchar(500)    default null                comment '备注',
    primary key (id),
    unique key uk_battery_version (battery_id, version),
    key idx_event_type (event_type),
    key idx_tx_hash (tx_hash)
) engine=innodb comment = '电池生命周期事件表（AppendOnly + 版本号）';

-- ----------------------------
-- 5. 电池详细规格（1:1 battery）
-- ----------------------------
drop table if exists nev_battery_spec;
create table nev_battery_spec (
    id               bigint(20)      not null                    comment '主键ID',
    battery_id       bigint(20)      not null                    comment '电池ID（FK nev_battery.id）',
    cell_supplier    varchar(100)    default null                comment '电芯供应商',
    cell_type        varchar(50)     default null                comment '电芯类型（LFP/NCM/NCA 等）',
    module_structure varchar(100)    default null                comment '模组结构描述',
    bms_info         varchar(255)    default null                comment 'BMS 信息（厂商 / 型号 / 固件版本）',
    safety_cert      varchar(255)    default null                comment '安全认证编号列表（逗号分隔）',
    cycle_life       int(11)         default null                comment '循环寿命（次）',
    energy_density   decimal(8,2)    default null                comment '能量密度（Wh/kg）',
    weight_kg        decimal(8,2)    default null                comment '重量（kg）',
    tenant_id        varchar(20)     default '000000'            comment '租户编号',
    del_flag         char(1)         default '0'                 comment '删除标志',
    create_dept      bigint(20)      default null                comment '创建部门',
    create_by        bigint(20)      default null                comment '创建者',
    create_time      datetime                                    comment '创建时间',
    update_by        bigint(20)      default null                comment '更新者',
    update_time      datetime                                    comment '更新时间',
    remark           varchar(500)    default null                comment '备注',
    primary key (id),
    unique key uk_battery_id (battery_id)
) engine=innodb comment = '电池详细规格表（1:1 nev_battery）';

-- ----------------------------
-- 6. 电池认证证书（N:1 battery）
-- ----------------------------
drop table if exists nev_certification;
create table nev_certification (
    id               bigint(20)      not null                    comment '主键ID',
    battery_id       bigint(20)      not null                    comment '电池ID（FK nev_battery.id）',
    cert_type        varchar(50)     not null                    comment '证书类型（QUALITY/SAFETY/ENVIRONMENT/CARBON 等）',
    cert_no          varchar(100)    not null                    comment '证书编号',
    issuer           varchar(100)    not null                    comment '颁发机构',
    issue_date       date                                        comment '颁发日期',
    expire_date      date            default null                comment '到期日期',
    file_path        varchar(255)    default null                comment '证书文件 OSS 路径',
    tenant_id        varchar(20)     default '000000'            comment '租户编号',
    del_flag         char(1)         default '0'                 comment '删除标志',
    create_dept      bigint(20)      default null                comment '创建部门',
    create_by        bigint(20)      default null                comment '创建者',
    create_time      datetime                                    comment '创建时间',
    update_by        bigint(20)      default null                comment '更新者',
    update_time      datetime                                    comment '更新时间',
    remark           varchar(500)    default null                comment '备注',
    primary key (id),
    key idx_battery_id (battery_id),
    key idx_cert_type (cert_type)
) engine=innodb comment = '电池认证证书表（N:1 nev_battery）';

-- =============================================================================
-- 分组 3：商城（6 张）
-- =============================================================================

-- ----------------------------
-- 7. 商家档案（merchant 角色用户 1:1）
-- ----------------------------
drop table if exists nev_merchant;
create table nev_merchant (
    id               bigint(20)      not null                    comment '主键ID',
    user_id          bigint(20)      not null                    comment '用户ID（FK sys_user.user_id, role=merchant）',
    merchant_name    varchar(100)    not null                    comment '商家名称',
    business_license varchar(50)     default null                comment '营业执照号',
    contact          varchar(50)     default null                comment '联系人',
    contact_phone    varchar(20)     default null                comment '联系电话',
    address          varchar(255)    default null                comment '商家地址',
    status           varchar(20)     default 'ACTIVE'            comment '状态（ACTIVE/SUSPENDED/CLOSED）',
    tenant_id        varchar(20)     default '000000'            comment '租户编号',
    del_flag         char(1)         default '0'                 comment '删除标志',
    create_dept      bigint(20)      default null                comment '创建部门',
    create_by        bigint(20)      default null                comment '创建者',
    create_time      datetime                                    comment '创建时间',
    update_by        bigint(20)      default null                comment '更新者',
    update_time      datetime                                    comment '更新时间',
    remark           varchar(500)    default null                comment '备注',
    primary key (id),
    unique key uk_user_id (user_id)
) engine=innodb comment = '商家档案表（merchant 角色 1:1）';

-- ----------------------------
-- 8. 商品（可绑定电池实现"电池即商品"）
-- ----------------------------
drop table if exists nev_product;
create table nev_product (
    id               bigint(20)      not null                    comment '主键ID',
    merchant_id      bigint(20)      not null                    comment '商家ID（FK nev_merchant.id）',
    category         varchar(50)     not null                    comment '商品类目（BATTERY/ACCESSORY/SERVICE 等）',
    title            varchar(200)    not null                    comment '商品标题',
    subtitle         varchar(255)    default null                comment '商品副标题',
    price            decimal(12,2)   not null                    comment '单价（元）',
    stock            int(11)         not null    default 0       comment '库存数量',
    sales_count      int(11)         not null    default 0       comment '已售数量',
    battery_id       bigint(20)      default null                comment '关联电池ID（可选，电池即商品场景）',
    images           json            default null                comment '商品图片列表（json 数组）',
    description      text            default null                comment '商品详情（富文本）',
    status           varchar(20)     default 'ON_SALE'           comment '状态（ON_SALE/OFF_SHELF/SOLD_OUT）',
    tenant_id        varchar(20)     default '000000'            comment '租户编号',
    del_flag         char(1)         default '0'                 comment '删除标志',
    create_dept      bigint(20)      default null                comment '创建部门',
    create_by        bigint(20)      default null                comment '创建者',
    create_time      datetime                                    comment '创建时间',
    update_by        bigint(20)      default null                comment '更新者',
    update_time      datetime                                    comment '更新时间',
    remark           varchar(500)    default null                comment '备注',
    primary key (id),
    key idx_merchant_id (merchant_id),
    key idx_category_status (category, status),
    key idx_battery_id (battery_id)
) engine=innodb comment = '商品表（可绑定电池）';

-- ----------------------------
-- 9. 购物车（每用户每商家 1 个）
-- ----------------------------
drop table if exists nev_cart;
create table nev_cart (
    id               bigint(20)      not null                    comment '主键ID',
    user_id          bigint(20)      not null                    comment '用户ID（FK sys_user.user_id）',
    merchant_id      bigint(20)      not null                    comment '商家ID（FK nev_merchant.id）',
    tenant_id        varchar(20)     default '000000'            comment '租户编号',
    del_flag         char(1)         default '0'                 comment '删除标志',
    create_dept      bigint(20)      default null                comment '创建部门',
    create_by        bigint(20)      default null                comment '创建者',
    create_time      datetime                                    comment '创建时间',
    update_by        bigint(20)      default null                comment '更新者',
    update_time      datetime                                    comment '更新时间',
    remark           varchar(500)    default null                comment '备注',
    primary key (id),
    unique key uk_user_merchant (user_id, merchant_id)
) engine=innodb comment = '购物车表';

-- ----------------------------
-- 10. 购物车明细
-- ----------------------------
drop table if exists nev_cart_item;
create table nev_cart_item (
    id               bigint(20)      not null                    comment '主键ID',
    cart_id          bigint(20)      not null                    comment '购物车ID（FK nev_cart.id）',
    product_id       bigint(20)      not null                    comment '商品ID（FK nev_product.id）',
    quantity         int(11)         not null    default 1       comment '数量',
    unit_price       decimal(12,2)   not null                    comment '加入时单价（避免价格变动追溯困难）',
    selected         char(1)         default '1'                 comment '是否选中结算（0未选 1已选）',
    tenant_id        varchar(20)     default '000000'            comment '租户编号',
    del_flag         char(1)         default '0'                 comment '删除标志',
    create_dept      bigint(20)      default null                comment '创建部门',
    create_by        bigint(20)      default null                comment '创建者',
    create_time      datetime                                    comment '创建时间',
    update_by        bigint(20)      default null                comment '更新者',
    update_time      datetime                                    comment '更新时间',
    remark           varchar(500)    default null                comment '备注',
    primary key (id),
    unique key uk_cart_product (cart_id, product_id)
) engine=innodb comment = '购物车明细表';

-- ----------------------------
-- 11. 订单主表
-- ----------------------------
drop table if exists nev_order;
create table nev_order (
    id               bigint(20)      not null                    comment '主键ID',
    order_no         varchar(32)     not null                    comment '订单号（业务唯一，规则：年月日+雪花后8位）',
    user_id          bigint(20)      not null                    comment '下单用户ID（FK sys_user.user_id）',
    merchant_id      bigint(20)      not null                    comment '商家ID（FK nev_merchant.id）',
    total_amount     decimal(12,2)   not null                    comment '订单总金额（元）',
    pay_amount       decimal(12,2)   default null                comment '实付金额（含优惠扣减）',
    status           varchar(20)     not null    default 'PENDING' comment '状态（PENDING/PAID/SHIPPED/DELIVERED/COMPLETED/CANCELLED/REFUNDED）',
    address_snapshot json            default null                comment '收货地址快照（json，下单时定格）',
    paid_at          datetime        default null                comment '支付时间',
    shipped_at       datetime        default null                comment '发货时间',
    delivered_at     datetime        default null                comment '送达时间',
    completed_at     datetime        default null                comment '完成时间',
    cancelled_at     datetime        default null                comment '取消时间',
    cancel_reason    varchar(255)    default null                comment '取消原因',
    tenant_id        varchar(20)     default '000000'            comment '租户编号',
    del_flag         char(1)         default '0'                 comment '删除标志',
    create_dept      bigint(20)      default null                comment '创建部门',
    create_by        bigint(20)      default null                comment '创建者',
    create_time      datetime                                    comment '创建时间',
    update_by        bigint(20)      default null                comment '更新者',
    update_time      datetime                                    comment '更新时间',
    remark           varchar(500)    default null                comment '备注',
    primary key (id),
    unique key uk_order_no (order_no),
    key idx_user_status (user_id, status),
    key idx_merchant_status (merchant_id, status)
) engine=innodb comment = '订单主表';

-- ----------------------------
-- 12. 订单明细
-- ----------------------------
drop table if exists nev_order_item;
create table nev_order_item (
    id               bigint(20)      not null                    comment '主键ID',
    order_id         bigint(20)      not null                    comment '订单ID（FK nev_order.id）',
    product_id       bigint(20)      not null                    comment '商品ID（FK nev_product.id）',
    product_snapshot json            not null                    comment '商品快照（json，下单时定格 title/category/images/battery_id）',
    quantity         int(11)         not null                    comment '数量',
    unit_price       decimal(12,2)   not null                    comment '成交单价（元）',
    subtotal         decimal(12,2)   not null                    comment '小计（quantity * unit_price）',
    tenant_id        varchar(20)     default '000000'            comment '租户编号',
    del_flag         char(1)         default '0'                 comment '删除标志',
    create_dept      bigint(20)      default null                comment '创建部门',
    create_by        bigint(20)      default null                comment '创建者',
    create_time      datetime                                    comment '创建时间',
    update_by        bigint(20)      default null                comment '更新者',
    update_time      datetime                                    comment '更新时间',
    remark           varchar(500)    default null                comment '备注',
    primary key (id),
    key idx_order_id (order_id),
    key idx_product_id (product_id)
) engine=innodb comment = '订单明细表';

-- =============================================================================
-- 分组 4：支付与地址（2 张）
-- =============================================================================

-- ----------------------------
-- 13. 支付记录（1:1 订单）
-- ----------------------------
drop table if exists nev_payment_record;
create table nev_payment_record (
    id               bigint(20)      not null                    comment '主键ID',
    order_id         bigint(20)      not null                    comment '订单ID（FK nev_order.id）',
    payment_no       varchar(32)     not null                    comment '支付单号（业务唯一）',
    amount           decimal(12,2)   not null                    comment '支付金额（元）',
    method           varchar(20)     not null                    comment '支付方式（ALIPAY/WECHAT/CARBON_CREDIT/BALANCE/MOCK）',
    status           varchar(20)     not null    default 'PENDING' comment '状态（PENDING/SUCCESS/FAILED/REFUNDED）',
    trade_no         varchar(64)     default null                comment '第三方交易号（支付宝/微信回调）',
    paid_at          datetime        default null                comment '支付完成时间',
    callback_payload json            default null                comment '支付回调原始数据',
    tenant_id        varchar(20)     default '000000'            comment '租户编号',
    del_flag         char(1)         default '0'                 comment '删除标志',
    create_dept      bigint(20)      default null                comment '创建部门',
    create_by        bigint(20)      default null                comment '创建者',
    create_time      datetime                                    comment '创建时间',
    update_by        bigint(20)      default null                comment '更新者',
    update_time      datetime                                    comment '更新时间',
    remark           varchar(500)    default null                comment '备注',
    primary key (id),
    unique key uk_payment_no (payment_no),
    key idx_order_id (order_id),
    key idx_trade_no (trade_no)
) engine=innodb comment = '支付记录表（1:1 nev_order）';

-- ----------------------------
-- 14. 收货地址
-- ----------------------------
drop table if exists nev_address;
create table nev_address (
    id               bigint(20)      not null                    comment '主键ID',
    user_id          bigint(20)      not null                    comment '用户ID（FK sys_user.user_id）',
    recipient        varchar(50)     not null                    comment '收件人',
    phone            varchar(20)     not null                    comment '收件人电话',
    province         varchar(50)     not null                    comment '省',
    city             varchar(50)     not null                    comment '市',
    district         varchar(50)     not null                    comment '区/县',
    detail           varchar(255)    not null                    comment '详细地址',
    is_default       char(1)         default '0'                 comment '是否默认地址（0否 1是）',
    tenant_id        varchar(20)     default '000000'            comment '租户编号',
    del_flag         char(1)         default '0'                 comment '删除标志',
    create_dept      bigint(20)      default null                comment '创建部门',
    create_by        bigint(20)      default null                comment '创建者',
    create_time      datetime                                    comment '创建时间',
    update_by        bigint(20)      default null                comment '更新者',
    update_time      datetime                                    comment '更新时间',
    remark           varchar(500)    default null                comment '备注',
    primary key (id),
    key idx_user_id (user_id)
) engine=innodb comment = '收货地址表';

-- =============================================================================
-- 分组 5：以旧换新（1 张）
-- =============================================================================

-- ----------------------------
-- 15. 以旧换新申请
-- ----------------------------
drop table if exists nev_trade_in_request;
create table nev_trade_in_request (
    id                 bigint(20)    not null                    comment '主键ID',
    request_no         varchar(32)   not null                    comment '换新单号（业务唯一）',
    consumer_id        bigint(20)    not null                    comment '申请用户ID（FK sys_user.user_id, role=consumer）',
    old_battery_id     bigint(20)    not null                    comment '旧电池ID（FK nev_battery.id）',
    new_product_id     bigint(20)    default null                comment '换新目标商品ID（FK nev_product.id）',
    evaluated_amount   decimal(12,2) default null                comment '评估抵扣金额（元）',
    recycler_id        bigint(20)    default null                comment '回收商用户ID（FK sys_user.user_id, role=recycler）',
    evaluator_id       bigint(20)    default null                comment '评估员用户ID',
    status             varchar(20)   not null    default 'SUBMITTED' comment '状态（SUBMITTED/EVALUATED/ACCEPTED/REJECTED/RECYCLED/COMPLETED）',
    evaluation_payload json          default null                comment '评估详情（json，SOH/外观/异常项）',
    linked_order_id    bigint(20)    default null                comment '关联订单ID（接受后创建的新订单，FK nev_order.id）',
    submitted_at       datetime      default null                comment '提交时间',
    evaluated_at       datetime      default null                comment '评估完成时间',
    accepted_at        datetime      default null                comment '消费者确认时间',
    recycled_at        datetime      default null                comment '回收商确认接收时间',
    completed_at       datetime      default null                comment '换新完成时间',
    tenant_id          varchar(20)   default '000000'            comment '租户编号',
    del_flag           char(1)       default '0'                 comment '删除标志',
    create_dept        bigint(20)    default null                comment '创建部门',
    create_by          bigint(20)    default null                comment '创建者',
    create_time        datetime                                  comment '创建时间',
    update_by          bigint(20)    default null                comment '更新者',
    update_time        datetime                                  comment '更新时间',
    remark             varchar(500)  default null                comment '备注',
    primary key (id),
    unique key uk_request_no (request_no),
    key idx_consumer_status (consumer_id, status),
    key idx_recycler_status (recycler_id, status),
    key idx_old_battery (old_battery_id)
) engine=innodb comment = '以旧换新申请表';

-- =============================================================================
-- 分组 6：碳模块（4 张）
-- =============================================================================

-- ----------------------------
-- 16. 电池碳足迹主表（1:1 battery）
-- ----------------------------
drop table if exists nev_carbon_footprint;
create table nev_carbon_footprint (
    id               bigint(20)      not null                    comment '主键ID',
    battery_id       bigint(20)      not null                    comment '电池ID（FK nev_battery.id）',
    total_co2_kg     decimal(14,4)   not null                    comment '全生命周期总碳排放（kgCO2eq）',
    calc_method      varchar(50)     not null    default 'GB-T-24067' comment '核算方法（GB-T-24067/ISO-14067/GHG-Protocol）',
    calc_version     varchar(20)     default 'v1'                comment '核算版本号（公式或因子库更新时递增）',
    calc_time        datetime        not null                    comment '核算时间',
    tenant_id        varchar(20)     default '000000'            comment '租户编号',
    del_flag         char(1)         default '0'                 comment '删除标志',
    create_dept      bigint(20)      default null                comment '创建部门',
    create_by        bigint(20)      default null                comment '创建者',
    create_time      datetime                                    comment '创建时间',
    update_by        bigint(20)      default null                comment '更新者',
    update_time      datetime                                    comment '更新时间',
    remark           varchar(500)    default null                comment '备注',
    primary key (id),
    unique key uk_battery_id (battery_id)
) engine=innodb comment = '电池碳足迹主表（1:1 nev_battery）';

-- ----------------------------
-- 17. 碳足迹分阶段明细（固定 5 阶段）
-- ----------------------------
drop table if exists nev_carbon_stage;
create table nev_carbon_stage (
    id               bigint(20)      not null                    comment '主键ID',
    footprint_id     bigint(20)      not null                    comment '碳足迹ID（FK nev_carbon_footprint.id）',
    stage            varchar(10)     not null                    comment '阶段（RAW原材料/MFG制造/TRANS运输/USE使用/EOL报废）',
    co2_kg           decimal(14,4)   not null                    comment '本阶段碳排放（kgCO2eq）',
    breakdown        json            default null                comment '细分明细（json，含主要排放源 + 因子引用）',
    tenant_id        varchar(20)     default '000000'            comment '租户编号',
    del_flag         char(1)         default '0'                 comment '删除标志',
    create_dept      bigint(20)      default null                comment '创建部门',
    create_by        bigint(20)      default null                comment '创建者',
    create_time      datetime                                    comment '创建时间',
    update_by        bigint(20)      default null                comment '更新者',
    update_time      datetime                                    comment '更新时间',
    remark           varchar(500)    default null                comment '备注',
    primary key (id),
    unique key uk_footprint_stage (footprint_id, stage)
) engine=innodb comment = '碳足迹分阶段明细表（5 阶段：RAW/MFG/TRANS/USE/EOL）';

-- ----------------------------
-- 18. 排放因子库
-- ----------------------------
drop table if exists nev_emission_factor;
create table nev_emission_factor (
    id                bigint(20)     not null                    comment '主键ID',
    factor_code       varchar(50)    not null                    comment '因子编码（业务唯一）',
    factor_name       varchar(100)   not null                    comment '因子名称',
    unit              varchar(20)    not null                    comment '单位（kgCO2eq/kg、kgCO2eq/kWh 等）',
    value             decimal(14,6)  not null                    comment '因子值',
    source            varchar(255)   default null                comment '数据来源（文献/标准/CLCD/Ecoinvent）',
    applicable_stage  varchar(10)    default null                comment '适用阶段（RAW/MFG/TRANS/USE/EOL，可空=通用）',
    valid_from        date           default null                comment '有效起始日期',
    valid_to          date           default null                comment '有效截止日期',
    tenant_id         varchar(20)    default '000000'            comment '租户编号',
    del_flag          char(1)        default '0'                 comment '删除标志',
    create_dept       bigint(20)     default null                comment '创建部门',
    create_by         bigint(20)     default null                comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)     default null                comment '更新者',
    update_time       datetime                                   comment '更新时间',
    remark            varchar(500)   default null                comment '备注',
    primary key (id),
    unique key uk_factor_code (factor_code),
    key idx_applicable_stage (applicable_stage)
) engine=innodb comment = '排放因子库表';

-- ----------------------------
-- 19. 碳积分流水
-- ----------------------------
drop table if exists nev_carbon_credit_record;
create table nev_carbon_credit_record (
    id               bigint(20)      not null                    comment '主键ID',
    user_id          bigint(20)      not null                    comment '用户ID（FK sys_user.user_id）',
    change_amount    decimal(18,4)   not null                    comment '变更金额（正数=增加，负数=扣减）',
    balance_after    decimal(18,4)   not null                    comment '变更后余额',
    reason           varchar(50)     not null                    comment '业务原因（TRADE_IN/RECYCLE/PURCHASE_DEDUCT/ADMIN_ADJUST 等）',
    related_id       bigint(20)      default null                comment '关联业务ID（订单ID / 换新单ID 等）',
    related_type     varchar(20)     default null                comment '关联业务类型（ORDER/TRADE_IN/MANUAL）',
    tenant_id        varchar(20)     default '000000'            comment '租户编号',
    del_flag         char(1)         default '0'                 comment '删除标志',
    create_dept      bigint(20)      default null                comment '创建部门',
    create_by        bigint(20)      default null                comment '创建者',
    create_time      datetime                                    comment '创建时间',
    update_by        bigint(20)      default null                comment '更新者',
    update_time      datetime                                    comment '更新时间',
    remark           varchar(500)    default null                comment '备注',
    primary key (id),
    key idx_user_created (user_id, create_time),
    key idx_related (related_type, related_id)
) engine=innodb comment = '碳积分流水表';

-- =============================================================================
-- 分组 7：区块链配置（1 张）
-- =============================================================================

-- ----------------------------
-- 20. 合约配置（每个合约部署后写一行）
-- ----------------------------
drop table if exists nev_contract_config;
create table nev_contract_config (
    id                bigint(20)     not null                    comment '主键ID',
    contract_name     varchar(50)    not null                    comment '合约名称（RoleManager/BatteryRegistry/LifecycleTrace）',
    contract_address  varchar(64)    not null                    comment '合约地址（0x 开头 42 字符）',
    abi               text           not null                    comment '合约 ABI（json 字符串）',
    deploy_block      bigint(20)     default null                comment '部署区块高度',
    deployed_at       datetime                                   comment '部署时间',
    network           varchar(20)    default 'fisco-bcos'        comment '链网络标识（fisco-bcos/ethereum 等）',
    group_id          varchar(20)    default '1'                 comment 'FISCO BCOS 群组ID',
    enabled           char(1)        default '1'                 comment '是否启用（0停用 1启用）',
    tenant_id         varchar(20)    default '000000'            comment '租户编号',
    del_flag          char(1)        default '0'                 comment '删除标志',
    create_dept       bigint(20)     default null                comment '创建部门',
    create_by         bigint(20)     default null                comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)     default null                comment '更新者',
    update_time       datetime                                   comment '更新时间',
    remark            varchar(500)   default null                comment '备注',
    primary key (id),
    unique key uk_contract_name_network (contract_name, network),
    key idx_enabled (enabled)
) engine=innodb comment = '合约配置表';

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 建表完成。预期表数量：20 张
-- 验证：select table_name, table_comment from information_schema.tables
--       where table_schema = 'nev_v2' and (table_name like 'nev_%' or table_name = 'sys_nev_user_ext')
--       order by table_name;
-- =============================================================================
