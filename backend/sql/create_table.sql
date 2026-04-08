# 数据库初始化
-- 创建库
create database if not exists ai_creator_db;

-- 切换库
use ai_creator_db;

-- 用户表
create table if not exists user
(
    id            bigint auto_increment comment '主键ID' primary key,
    user_account  varchar(256)                           not null comment '用户账号',
    user_password varchar(512)                           not null comment '用户密码',
    user_name     varchar(256)                           null comment '用户昵称',
    user_avatar   varchar(1024)                          null comment '用户头像',
    user_profile  varchar(512)                           null comment '用户简介',
    user_role     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    edit_time     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    create_time   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete     tinyint      default 0                 not null comment '是否删除（0-未删除，1-已删除）',
    UNIQUE KEY uk_user_account (user_account),
    INDEX idx_user_name (user_name)
) comment '用户表' collate = utf8mb4_unicode_ci;

-- 应用表
CREATE TABLE IF NOT EXISTS app
(
    id            BIGINT AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    app_name      VARCHAR(256)                       NULL COMMENT '应用名称',
    cover         VARCHAR(512)                       NULL COMMENT '应用封面',
    init_prompt   TEXT                               NULL COMMENT '应用初始化的 prompt',
    code_gen_type VARCHAR(64)                        NULL COMMENT '代码生成类型（枚举）',
    deploy_key    VARCHAR(64)                        NULL COMMENT '部署标识',
    deployed_time DATETIME                           NULL COMMENT '部署时间',
    priority      INT      DEFAULT 0                 NOT NULL COMMENT '优先级',
    user_id       BIGINT                             NOT NULL COMMENT '创建用户ID',
    edit_time     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete     TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除',

    UNIQUE KEY uk_deploy_key (deploy_key), -- 唯一索引
    INDEX idx_app_name (app_name),         -- 应用名称索引
    INDEX idx_user_id (user_id)            -- 用户ID索引
) COMMENT ='应用表' COLLATE = utf8mb4_unicode_ci;


-- 对话历史表
create table if not exists chat_history
(
    id           bigint auto_increment comment 'id' primary key,
    message      text                               not null comment '消息',
    message_type varchar(32)                        not null comment 'user/ai',
    app_id       bigint                             not null comment '应用id',
    user_id      bigint                             not null comment '创建用户id',
    create_time  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete    tinyint  default 0                 not null comment '是否删除',
    INDEX idx_appId (app_id),                        -- 提升基于应用的查询性能
    INDEX idx_createTime (create_time),              -- 提升基于时间的查询性能
    INDEX idx_appId_createTime (app_id, create_time) -- 游标查询核心索引
) comment '对话历史' collate = utf8mb4_unicode_ci;
