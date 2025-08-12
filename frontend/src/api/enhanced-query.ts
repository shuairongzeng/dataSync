import { http } from "@/utils/http";

// Enhanced Query Request Interface
export interface EnhancedQueryRequest {
  connectionId: number;
  sql: string;
  schema?: string;
  useCache?: boolean;
  saveHistory?: boolean;
}

// Enhanced Query Response Interface
export interface EnhancedQueryResponse {
  columns: string[];
  rows: any[][];
  data?: any[];
  totalRows: number;
  executionTime: number;
  message?: string;
  fromCache?: boolean;
  cacheKey?: string;
  // Pagination fields
  currentPage?: number;
  pageSize?: number;
  totalPages?: number;
  hasMore?: boolean;
}

// Cache Statistics Interface
export interface CacheStats {
  totalEntries: number;
  hitRate: number;
  missRate: number;
  totalSize: number;
  connectionStats: { [connectionId: string]: any };
}

/** Execute SQL query with enhanced caching support */
export const executeEnhancedQueryApi = (data: EnhancedQueryRequest) => {
  return http.request<EnhancedQueryResponse>("post", "/api/enhanced-query/execute", { 
    data,
    timeout: 60000 // 60 seconds timeout for query execution
  });
};

/** Execute paginated SQL query with enhanced caching support */
export const executeEnhancedQueryPaginated = (
  connectionId: number,
  sql: string,
  page: number = 1,
  pageSize: number = 50,
  schema?: string,
  useCache: boolean = true,
  saveHistory: boolean = true
) => {
  return http.request<EnhancedQueryResponse>("post", "/api/enhanced-query/execute", { 
    data: {
      connectionId,
      sql,
      schema,
      useCache,
      saveHistory,
      page,
      pageSize
    },
    timeout: 60000 // 60 seconds timeout for query execution
  });
};

/** Get cached tables for a connection */
export const getCachedTablesApi = (connectionId: string, schema?: string, useCache = true) => {
  const params = new URLSearchParams();
  if (schema) params.append('schema', schema);
  params.append('useCache', useCache.toString());
  
  return http.request<string[]>("get", `/api/enhanced-query/tables/${connectionId}?${params.toString()}`, {
    timeout: 30000 // 30 seconds timeout
  });
};

/** Get cached table columns */
export const getCachedTableColumnsApi = (connectionId: string, tableName: string, schema?: string, useCache = true) => {
  const params = new URLSearchParams();
  if (schema) params.append('schema', schema);
  params.append('useCache', useCache.toString());
  
  return http.request<any[]>("get", `/api/enhanced-query/tables/${connectionId}/${tableName}/columns?${params.toString()}`, {
    timeout: 30000 // 30 seconds timeout
  });
};

/** Clear cache for a specific connection */
export const clearConnectionCacheApi = (connectionId: string) => {
  return http.request<{ message: string }>("delete", `/api/enhanced-query/cache/${connectionId}`);
};

/** Clear all cache */
export const clearAllCacheApi = () => {
  return http.request<{ message: string }>("delete", "/api/enhanced-query/cache");
};

/** Warmup cache for a connection */
export const warmupConnectionCacheApi = (connectionId: string, schema?: string) => {
  const params = schema ? `?schema=${schema}` : '';
  return http.request<{ message: string }>("post", `/api/enhanced-query/cache/warmup/${connectionId}${params}`);
};

/** Get cache statistics */
export const getCacheStatsApi = () => {
  return http.request<CacheStats>("get", "/api/enhanced-query/cache/stats");
};

/** Get cache statistics for a specific connection */
export const getConnectionCacheStatsApi = (connectionId: string) => {
  return http.request<any>("get", `/api/enhanced-query/cache/stats/${connectionId}`);
};

/** Check cache health */
export const checkCacheHealthApi = () => {
  return http.request<{ healthy: boolean; message: string; timestamp: number }>("get", "/api/enhanced-query/cache/health");
};

/** Get cache configuration */
export const getCacheConfigApi = () => {
  return http.request<any>("get", "/api/enhanced-query/cache/config");
};

/** Update cache configuration */
export const updateCacheConfigApi = (config: any) => {
  return http.request<any>("put", "/api/enhanced-query/cache/config", { data: config });
};

/** Export cache data */
export const exportCacheApi = (connectionId?: string) => {
  const params = connectionId ? `?connectionId=${connectionId}` : '';
  return http.request<Blob>("get", `/api/enhanced-query/cache/export${params}`, {
    responseType: 'blob'
  });
};

/** Import cache data */
export const importCacheApi = (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  
  return http.request<{ message: string }>("post", "/api/enhanced-query/cache/import", {
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
};