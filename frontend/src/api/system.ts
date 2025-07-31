import { http } from "@/utils/http";

// 系统日志类型定义
export interface SystemLog {
  id: string;
  timestamp: string;
  level: 'INFO' | 'WARN' | 'ERROR' | 'DEBUG';
  module: string;
  message: string;
  details?: string;
  userId?: string;
  username?: string;
  ip?: string;
  userAgent?: string;
}

// 操作日志类型定义
export interface OperationLog {
  id: string;
  timestamp: string;
  userId: string;
  username: string;
  operation: string;
  module: string;
  description: string;
  ip: string;
  userAgent: string;
  status: 'SUCCESS' | 'FAILED';
  duration: number;
  details?: any;
}

// 系统健康状态
export interface SystemHealth {
  status: 'UP' | 'DOWN' | 'DEGRADED';
  timestamp: string;
  components: {
    database: ComponentHealth;
    memory: ComponentHealth;
    disk: ComponentHealth;
    cpu: ComponentHealth;
  };
  details?: any;
}

export interface ComponentHealth {
  status: 'UP' | 'DOWN' | 'DEGRADED';
  details?: any;
}

// 系统指标
export interface SystemMetrics {
  timestamp: string;
  cpu: {
    usage: number;
    cores: number;
  };
  memory: {
    used: number;
    total: number;
    usage: number;
  };
  disk: {
    used: number;
    total: number;
    usage: number;
  };
  network: {
    bytesIn: number;
    bytesOut: number;
  };
}

// 日志查询参数
export interface LogQueryParams {
  page?: number;
  size?: number;
  level?: string;
  module?: string;
  startTime?: string;
  endTime?: string;
  keyword?: string;
}

// 日志级别常量
export const LOG_LEVELS = [
  { label: '全部', value: '' },
  { label: 'DEBUG', value: 'DEBUG' },
  { label: 'INFO', value: 'INFO' },
  { label: 'WARN', value: 'WARN' },
  { label: 'ERROR', value: 'ERROR' }
];

// 系统模块常量
export const SYSTEM_MODULES = [
  { label: '全部', value: '' },
  { label: '用户管理', value: 'USER' },
  { label: '角色管理', value: 'ROLE' },
  { label: '菜单管理', value: 'MENU' },
  { label: '部门管理', value: 'DEPT' },
  { label: '数据库连接', value: 'DATABASE' },
  { label: '查询执行', value: 'QUERY' },
  { label: '系统监控', value: 'MONITOR' }
];

type Result = {
  success: boolean;
  data?: Array<any>;
};

type ResultTable = {
  success: boolean;
  data?: {
    /** 列表数据 */
    list: Array<any>;
    /** 总条目数 */
    total?: number;
    /** 每页显示条目个数 */
    pageSize?: number;
    /** 当前页数 */
    currentPage?: number;
  };
};

/** 获取系统管理-用户管理列表 */
export const getUserList = (data?: object) => {
  return http.request<ResultTable>("post", "/user", { data });
};

/** 系统管理-用户管理-获取所有角色列表 */
export const getAllRoleList = () => {
  return http.request<Result>("get", "/list-all-role");
};

/** 系统管理-用户管理-根据userId，获取对应角色id列表（userId：用户id） */
export const getRoleIds = (data?: object) => {
  return http.request<Result>("post", "/list-role-ids", { data });
};

/** 获取系统管理-角色管理列表 */
export const getRoleList = (data?: object) => {
  return http.request<ResultTable>("post", "/role", { data });
};

/** 获取系统管理-菜单管理列表 */
export const getMenuList = (data?: object) => {
  return http.request<Result>("post", "/menu", { data });
};

/** 获取系统管理-部门管理列表 */
export const getDeptList = (data?: object) => {
  return http.request<Result>("post", "/dept", { data });
};

/** 获取系统监控-在线用户列表 */
export const getOnlineLogsList = (data?: object) => {
  return http.request<ResultTable>("post", "/online-logs", { data });
};

/** 获取系统监控-登录日志列表 */
export const getLoginLogsList = (data?: object) => {
  return http.request<ResultTable>("post", "/login-logs", { data });
};

/** 获取系统监控-操作日志列表 */
export const getOperationLogsList = (data?: object) => {
  return http.request<ResultTable>("post", "/operation-logs", { data });
};

/** 获取系统监控-系统日志列表 */
export const getSystemLogsList = (data?: object) => {
  return http.request<ResultTable>("post", "/system-logs", { data });
};

/** 获取系统监控-系统日志-根据 id 查日志详情 */
export const getSystemLogsDetail = (data?: object) => {
  return http.request<Result>("post", "/system-logs-detail", { data });
};

/** 获取角色管理-权限-菜单权限 */
export const getRoleMenu = (data?: object) => {
  return http.request<Result>("post", "/role-menu", { data });
};

/** 获取角色管理-权限-菜单权限-根据角色 id 查对应菜单 */
export const getRoleMenuIds = (data?: object) => {
  return http.request<Result>("post", "/role-menu-ids", { data });
};
