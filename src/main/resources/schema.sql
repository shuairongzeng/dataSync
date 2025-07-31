-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- 创建数据库连接表
CREATE TABLE IF NOT EXISTS db_connections (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL,
    db_type VARCHAR(20) NOT NULL,
    host VARCHAR(255) NOT NULL,
    port INTEGER NOT NULL,
    database VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    schema VARCHAR(100),
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建数据库连接表索引
CREATE INDEX IF NOT EXISTS idx_db_connections_name ON db_connections(name);
CREATE INDEX IF NOT EXISTS idx_db_connections_type ON db_connections(db_type);
CREATE INDEX IF NOT EXISTS idx_db_connections_enabled ON db_connections(enabled);

-- 创建同步任务表
CREATE TABLE IF NOT EXISTS sync_tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(200) NOT NULL,
    source_connection_id INTEGER NOT NULL,
    target_connection_id INTEGER NOT NULL,
    source_schema_name VARCHAR(100),
    target_schema_name VARCHAR(100),
    tables TEXT NOT NULL, -- JSON 格式存储表名数组
    truncate_before_sync BOOLEAN NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    progress INTEGER DEFAULT 0,
    total_tables INTEGER DEFAULT 0,
    completed_tables INTEGER DEFAULT 0,
    error_message TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_run_at DATETIME,
    FOREIGN KEY (source_connection_id) REFERENCES db_connections(id),
    FOREIGN KEY (target_connection_id) REFERENCES db_connections(id)
);

-- 创建同步任务日志表
CREATE TABLE IF NOT EXISTS sync_task_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    level VARCHAR(10) NOT NULL, -- INFO, WARN, ERROR
    message TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES sync_tasks(id)
);

-- 创建同步任务表索引
CREATE INDEX IF NOT EXISTS idx_sync_tasks_status ON sync_tasks(status);
CREATE INDEX IF NOT EXISTS idx_sync_tasks_source_connection ON sync_tasks(source_connection_id);
CREATE INDEX IF NOT EXISTS idx_sync_tasks_target_connection ON sync_tasks(target_connection_id);
CREATE INDEX IF NOT EXISTS idx_sync_tasks_created_at ON sync_tasks(created_at);
CREATE INDEX IF NOT EXISTS idx_sync_task_logs_task_id ON sync_task_logs(task_id);
CREATE INDEX IF NOT EXISTS idx_sync_task_logs_created_at ON sync_task_logs(created_at);

-- 创建查询历史表
CREATE TABLE IF NOT EXISTS query_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sql TEXT NOT NULL,
    source_connection_id INTEGER NOT NULL,
    source_connection_name VARCHAR(100) NOT NULL,
    target_connection_id INTEGER,
    target_connection_name VARCHAR(100),
    target_table_name VARCHAR(100),
    target_schema_name VARCHAR(100),
    executed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time INTEGER NOT NULL DEFAULT 0, -- 执行时间（毫秒）
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS', -- SUCCESS, ERROR
    error_message TEXT,
    result_rows INTEGER DEFAULT 0, -- 结果行数
    created_by VARCHAR(50), -- 执行用户
    FOREIGN KEY (source_connection_id) REFERENCES db_connections(id),
    FOREIGN KEY (target_connection_id) REFERENCES db_connections(id)
);

-- 创建查询历史表索引
CREATE INDEX IF NOT EXISTS idx_query_history_source_connection ON query_history(source_connection_id);
CREATE INDEX IF NOT EXISTS idx_query_history_target_connection ON query_history(target_connection_id);
CREATE INDEX IF NOT EXISTS idx_query_history_executed_at ON query_history(executed_at);
CREATE INDEX IF NOT EXISTS idx_query_history_status ON query_history(status);
CREATE INDEX IF NOT EXISTS idx_query_history_created_by ON query_history(created_by);

-- 插入默认管理员用户（密码是 admin123，已经过 BCrypt 加密）
-- BCrypt hash for "admin123": $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM7lbdxOoRaaOIjQRaiK
INSERT OR IGNORE INTO users (username, password, email, role, enabled)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM7lbdxOoRaaOIjQRaiK', 'admin@dbsync.com', 'ADMIN', 1);
