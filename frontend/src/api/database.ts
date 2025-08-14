import { http } from "@/utils/http";
import { 
  executeEnhancedQueryApi, 
  getCachedTablesApi, 
  getCachedTableColumnsApi,
  type EnhancedQueryRequest 
} from "./enhanced-query";

// 数据库配置类型
export interface DbConfig {
  id?: number;
  name: string;
  dbType: string;
  host: string;
  port: number;
  database: string;
  username: string;
  password: string;
  schema?: string;
  description?: string;
  enabled?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

// 数据库连接测试请求
export interface DbTestRequest {
  dbType: string;
  host: string;
  port: number;
  database: string;
  username: string;
  password: string;
  schema?: string;
}

// 数据库连接测试响应
export interface DbTestResponse {
  success: boolean;
  message: string;
  connectionTime?: number;
}

// 支持的数据库类型
export const DB_TYPES = [
  { value: "mysql", label: "MySQL", port: 3306 },
  { value: "postgresql", label: "PostgreSQL", port: 5432 },
  { value: "oracle", label: "Oracle", port: 1521 },
  { value: "sqlserver", label: "SQL Server", port: 1433 },
  { value: "dameng", label: "达梦数据库", port: 5236 },
  { value: "vastbase", label: "海量数据库", port: 5432 }
];

/** 获取数据库连接列表 */
export const getDbConnectionsApi = () => {
  return http.request<DbConfig[]>("get", "/api/database/connections");
};

/** 创建数据库连接 */
export const createDbConnectionApi = (data: DbConfig) => {
  return http.request<DbConfig>("post", "/api/database/connections", { data });
};

/** 更新数据库连接 */
export const updateDbConnectionApi = (id: string, data: DbConfig) => {
  return http.request<DbConfig>("put", `/api/database/connections/${id}`, { data });
};

/** 删除数据库连接 */
export const deleteDbConnectionApi = (id: string) => {
  return http.request<any>("delete", `/api/database/connections/${id}`);
};

/** 测试数据库连接 */
export const testDbConnectionApi = (data: DbTestRequest) => {
  return http.request<DbTestResponse>("post", "/api/database/test-connection", { data });
};

/** 获取数据库表列表 */
export const getDbTablesApi = (connectionId: string, schema?: string) => {
  const params = schema ? { schema } : {};
  // 数据库表列表加载可能需要更长时间，特别是对于大型数据库
  return http.request<string[]>("get", `/api/database/connections/${connectionId}/tables`, {
    params,
    timeout: 60000 // 60 秒超时，专门用于表列表加载
  });
};

/** 获取表结构信息 */
export const getTableStructureApi = (connectionId: string, tableName: string, schema?: string) => {
  const params = schema ? { schema } : {};
  return http.request<any>("get", `/api/database/connections/${connectionId}/tables/${tableName}/structure`, { params });
};

// 同步任务相关类型定义
export interface SyncTask {
  id?: string;
  name: string;
  sourceConnectionId: string;
  targetConnectionId: string;
  sourceSchemaName?: string;
  targetSchemaName?: string;
  tables: string[];
  truncateBeforeSync: boolean;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED_SUCCESS' | 'COMPLETED_WITH_ERRORS' | 'FAILED';
  progress?: number;
  totalTables?: number;
  completedTables?: number;
  errorMessage?: string;
  createdAt?: string;
  updatedAt?: string;
  lastRunAt?: string;
}

export interface SyncTaskProgress {
  taskId: string;
  status: string;
  progress: number;
  totalTables: number;
  completedTables: number;
  currentTable?: string;
  message?: string;
}

/** 获取同步任务列表 */
export const getSyncTasksApi = () => {
  return http.request<SyncTask[]>("get", "/api/sync/tasks");
};

/** 创建同步任务 */
export const createSyncTaskApi = (data: SyncTask) => {
  return http.request<SyncTask>("post", "/api/sync/tasks", { data });
};

/** 更新同步任务 */
export const updateSyncTaskApi = (id: string, data: SyncTask) => {
  return http.request<SyncTask>("put", `/api/sync/tasks/${id}`, { data });
};

/** 删除同步任务 */
export const deleteSyncTaskApi = (id: string) => {
  return http.request<any>("delete", `/api/sync/tasks/${id}`);
};

/** 执行同步任务 */
export const executeSyncTaskApi = (id: string) => {
  return http.request<any>("post", `/api/sync/tasks/${id}/execute`);
};

/** 停止同步任务 */
export const stopSyncTaskApi = (id: string) => {
  return http.request<any>("post", `/api/sync/tasks/${id}/stop`);
};

/** 获取同步任务进度 */
export const getSyncTaskProgressApi = (id: string) => {
  return http.request<SyncTaskProgress>("get", `/api/sync/tasks/${id}/progress`);
};

/** 获取同步任务日志 */
export const getSyncTaskLogsApi = (id: string) => {
  return http.request<string[]>("get", `/api/sync/tasks/${id}/logs`);
};

// 自定义查询相关类型定义
export interface CustomQueryRequest {
  sourceDbConfig: DbConfig;
  targetDbConfig: DbConfig;
  customSql: string;
  targetTableName: string;
  targetSchemaName?: string;
}

export interface QueryResult {
  columns: string[];
  rows: any[][];
  data?: any[]; // 转换后的对象数组格式，用于前端表格显示
  totalRows: number;
  executionTime: number;
  message?: string;
}

export interface QueryHistory {
  id: string;
  sql: string;
  sourceConnection: string;
  targetConnection?: string;
  targetTable?: string;
  executedAt: string;
  executionTime: number;
  status: 'SUCCESS' | 'ERROR';
  errorMessage?: string;
}

/** 执行自定义查询并保存结果 */
export const executeCustomQueryApi = (data: CustomQueryRequest) => {
  return http.request<any>("post", "/api/custom-query/execute-and-save", { data });
};

/** 执行 SQL 查询（仅查询，不保存） */
export const executeSqlQueryApi = (connectionId: string, sql: string, schema?: string) => {
  const data = { sql, schema };
  return http.request<QueryResult>("post", `/api/database/connections/${connectionId}/query`, { data });
};

/** 获取查询历史 */
export const getQueryHistoryApi = (connectionId?: string) => {
  const params = connectionId ? { connectionId } : {};
  return http.request<QueryHistory[]>("get", "/api/query/history", { params });
};

/** 保存查询到历史 */
export const saveQueryHistoryApi = (data: Omit<QueryHistory, 'id' | 'executedAt'>) => {
  return http.request<QueryHistory>("post", "/api/query/history", { data });
};

/** 删除查询历史 */
export const deleteQueryHistoryApi = (id: string) => {
  return http.request<any>("delete", `/api/query/history/${id}`);
};

/** 获取数据库表列表 */
// 分页获取表列表
export const getTablesWithPaginationApi = (connectionId: string, params?: {
  page?: number;
  size?: number;
  search?: string;
  sortBy?: string;
  sortOrder?: string;
  schema?: string;
}) => {
  return http.request<any>("get", `/api/database/connections/${connectionId}/tables/page`, {
    params,
    timeout: 450000 // 450 秒超时，用于分页表列表加载
  });
};

// 获取所有表列表（保持向后兼容）
export const getTablesApi = (connectionId: string, schema?: string) => {
  const params = schema ? { schema } : {};
  return http.request<string[]>("get", `/api/database/connections/${connectionId}/tables`, { params });
};

/** 获取表结构信息 */
export const getTableColumnsApi = (connectionId: string, tableName: string, schema?: string) => {
  const params = schema ? { schema } : {};
  return http.request<any[]>("get", `/api/database/connections/${connectionId}/tables/${tableName}/columns`, {
    params,
    timeout: 30000 // 30 秒超时，用于表结构获取
  });
};

/** 获取数据库 Schema 列表 */
export const getSchemasApi = (connectionId: string) => {
  return http.request<string[]>("get", `/api/database/connections/${connectionId}/schemas`, {
    timeout: 30000 // 30 秒超时，用于 Schema 列表获取
  });
};

/** 获取数据库连接列表 */
export const getConnectionsApi = () => {
  return http.request<DbConfig[]>("get", "/api/database/connections");
};

/** 根据ID获取单个数据库连接信息 */
export const getConnectionByIdApi = (connectionId: string) => {
  return http.request<DbConfig>("get", `/api/database/connections/${connectionId}`);
};

/** 检查数据库连接健康状态 */
export const checkConnectionHealthApi = (connectionId: string) => {
  return http.request<{healthy: boolean, message: string, timestamp: number}>(
    "get",
    `/api/database/connections/${connectionId}/health`,
    {
      timeout: 10000 // 10 秒超时，用于健康检查
    }
  );
};

/** 执行 SQL 查询 */
export const executeQueryApi = (connectionId: string, data: { sql: string; schema?: string }) => {
  return http.request<QueryResult>("post", `/api/database/connections/${connectionId}/query`, {
    data,
    timeout: 60000 // 60 秒超时，用于 SQL 查询执行
  });
};

// ================ 新增快速加载API ================

/** 快速获取基础表信息列表（只包含表名和备注，不含详细信息） */
export const getBasicTablesApi = (connectionId: string, schema?: string) => {
  const params = schema ? { schema } : {};
  return http.request<any[]>("get", `/api/database/connections/${connectionId}/tables/basic`, {
    params,
    timeout: 30000 // 30 秒超时，快速查询
  });
};

/** 快速分页获取基础表信息列表 */
export const getBasicTablesWithPaginationApi = (connectionId: string, params?: {
  page?: number;
  size?: number;
  search?: string;
  sortBy?: string;
  sortOrder?: string;
  schema?: string;
}) => {
  return http.request<any>("get", `/api/database/connections/${connectionId}/tables/basic/page`, {
    params,
    timeout: 30000 // 30 秒超时，快速查询
  });
};

/** 获取单个表的详细信息（按需加载） */
export const getTableDetailsApi = (connectionId: string, tableName: string, schema?: string) => {
  const params = schema ? { schema } : {};
  return http.request<any>("get", `/api/database/connections/${connectionId}/tables/${tableName}/details`, {
    params,
    timeout: 15000 // 15 秒超时，单表详情
  });
};

/** 批量获取多个表的基础统计信息 */
export const getBatchTableStatsApi = (connectionId: string, data: {
  tableNames: string[];
  schema?: string;
}) => {
  return http.request<Record<string, any>>("post", `/api/database/connections/${connectionId}/tables/batch-stats`, {
    data,
    timeout: 45000 // 45 秒超时，批量查询
  });
};

// ================ SQL脚本执行API ================

/** 脚本执行请求类型 */
export interface ScriptExecutionRequest {
  script: string;
  schema?: string;
  executeInTransaction?: boolean;
}

/** 脚本解析请求类型 */
export interface ScriptParseRequest {
  script: string;
  dbType: string;
}

/** 语句执行结果类型 */
export interface StatementResult {
  statement: {
    sql: string;
    type: string;
    category: string;
    startLine: number;
    endLine: number;
  };
  success: boolean;
  message: string;
  affectedRows: number;
  executionTime: number;
  lineNumber: number;
  statementPreview: string;
}

/** 脚本执行结果类型 */
export interface ScriptExecutionResult {
  success: boolean;
  message: string;
  totalCount: number;
  successCount: number;
  failedCount: number;
  totalTime: number;
  statementResults: StatementResult[];
  timestamp: number;
}

/** 执行SQL脚本 */
export const executeScriptApi = (connectionId: string, data: ScriptExecutionRequest) => {
  return http.request<ScriptExecutionResult>("post", `/api/database/connections/${connectionId}/script/execute`, {
    data,
    timeout: 600000 // 10 分钟超时，脚本执行可能需要较长时间
  });
};

/** 解析SQL脚本（不执行） */
export const parseScriptApi = (data: ScriptParseRequest) => {
  return http.request<any>("post", `/api/database/script/parse`, {
    data,
    timeout: 30000 // 30 秒超时，解析速度较快
  });
};


