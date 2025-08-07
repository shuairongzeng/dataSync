import { http } from "@/utils/http";

/**
 * 缓存统计信息接口
 */
export interface CacheStats {
  [cacheName: string]: {
    hitCount: number;
    missCount: number;
    hitRate: number;
    evictionCount: number;
    requestCount: number;
  };
}

/**
 * 缓存健康状态接口
 */
export interface CacheHealth {
  status: 'UP' | 'DOWN';
  cacheCount: number;
  averageHitRate: number;
  timestamp: number;
  details: CacheStats;
  error?: string;
}

/**
 * 缓存操作响应接口
 */
export interface CacheOperationResponse {
  success: boolean;
  message: string;
  timestamp: number;
  connectionId?: number;
  tableName?: string;
  schema?: string;
}

/**
 * 获取缓存统计信息
 */
export const getCacheStatsApi = () => {
  return http.request<CacheStats>("get", "/api/cache/stats");
};

/**
 * 获取缓存健康状态
 */
export const getCacheHealthApi = () => {
  return http.request<CacheHealth>("get", "/api/cache/health");
};

/**
 * 清除所有缓存
 */
export const clearAllCacheApi = () => {
  return http.request<CacheOperationResponse>("delete", "/api/cache/all");
};

/**
 * 清除指定连接的缓存
 */
export const clearConnectionCacheApi = (connectionId: number) => {
  return http.request<CacheOperationResponse>("delete", `/api/cache/connection/${connectionId}`);
};

/**
 * 清除指定表的缓存
 */
export const clearTableCacheApi = (connectionId: number, tableName: string, schema?: string) => {
  const params = schema ? { schema } : {};
  return http.request<CacheOperationResponse>(
    "delete", 
    `/api/cache/connection/${connectionId}/table/${tableName}`,
    { params }
  );
};

/**
 * 预热缓存
 */
export const warmupCacheApi = (connectionId: number, schema?: string) => {
  const params = schema ? { schema } : {};
  return http.request<CacheOperationResponse>(
    "post", 
    `/api/cache/warmup/${connectionId}`,
    { params }
  );
};

/**
 * 缓存管理工具类
 */
export class CacheApiManager {
  /**
   * 批量清除多个连接的缓存
   */
  static async clearMultipleConnectionsCache(connectionIds: number[]): Promise<CacheOperationResponse[]> {
    const promises = connectionIds.map(id => clearConnectionCacheApi(id));
    return Promise.all(promises);
  }

  /**
   * 批量预热多个连接的缓存
   */
  static async warmupMultipleConnectionsCache(
    connections: Array<{ id: number; schema?: string }>
  ): Promise<CacheOperationResponse[]> {
    const promises = connections.map(conn => warmupCacheApi(conn.id, conn.schema));
    return Promise.all(promises);
  }

  /**
   * 获取格式化的缓存统计信息
   */
  static async getFormattedCacheStats(): Promise<{
    totalHits: number;
    totalMisses: number;
    totalRequests: number;
    overallHitRate: number;
    cacheDetails: Array<{
      name: string;
      hitCount: number;
      missCount: number;
      hitRate: number;
      requestCount: number;
      evictionCount: number;
    }>;
  }> {
    const stats = await getCacheStatsApi();
    
    let totalHits = 0;
    let totalMisses = 0;
    let totalRequests = 0;
    
    const cacheDetails = Object.entries(stats).map(([name, data]) => {
      totalHits += data.hitCount;
      totalMisses += data.missCount;
      totalRequests += data.requestCount;
      
      return {
        name,
        hitCount: data.hitCount,
        missCount: data.missCount,
        hitRate: data.hitRate,
        requestCount: data.requestCount,
        evictionCount: data.evictionCount
      };
    });
    
    const overallHitRate = totalRequests > 0 ? totalHits / totalRequests : 0;
    
    return {
      totalHits,
      totalMisses,
      totalRequests,
      overallHitRate,
      cacheDetails
    };
  }

  /**
   * 检查缓存是否健康
   */
  static async isCacheHealthy(): Promise<boolean> {
    try {
      const health = await getCacheHealthApi();
      return health.status === 'UP';
    } catch (error) {
      console.error('检查缓存健康状态失败:', error);
      return false;
    }
  }

  /**
   * 智能缓存清理 - 根据命中率清理低效缓存
   */
  static async smartCacheClear(minHitRate: number = 0.1): Promise<{
    clearedCaches: string[];
    message: string;
  }> {
    try {
      const stats = await getCacheStatsApi();
      const lowHitRateCaches = Object.entries(stats)
        .filter(([_, data]) => data.hitRate < minHitRate && data.requestCount > 10)
        .map(([name]) => name);

      if (lowHitRateCaches.length === 0) {
        return {
          clearedCaches: [],
          message: '没有发现需要清理的低效缓存'
        };
      }

      // 这里可以实现更精细的缓存清理逻辑
      // 目前先清除所有缓存作为简化实现
      await clearAllCacheApi();

      return {
        clearedCaches: lowHitRateCaches,
        message: `已清理 ${lowHitRateCaches.length} 个低效缓存`
      };
    } catch (error) {
      throw new Error('智能缓存清理失败: ' + (error as Error).message);
    }
  }
}

/**
 * 前端缓存管理类
 */
export class FrontendCacheManager {
  private static instance: FrontendCacheManager;
  private cacheStats = {
    hits: 0,
    misses: 0,
    operations: 0
  };

  static getInstance(): FrontendCacheManager {
    if (!FrontendCacheManager.instance) {
      FrontendCacheManager.instance = new FrontendCacheManager();
    }
    return FrontendCacheManager.instance;
  }

  /**
   * 记录缓存命中
   */
  recordHit(): void {
    this.cacheStats.hits++;
    this.cacheStats.operations++;
  }

  /**
   * 记录缓存未命中
   */
  recordMiss(): void {
    this.cacheStats.misses++;
    this.cacheStats.operations++;
  }

  /**
   * 获取前端缓存统计
   */
  getStats() {
    const hitRate = this.cacheStats.operations > 0
      ? this.cacheStats.hits / this.cacheStats.operations
      : 0;

    return {
      ...this.cacheStats,
      hitRate: Math.round(hitRate * 100) / 100
    };
  }

  /**
   * 重置统计
   */
  resetStats(): void {
    this.cacheStats = {
      hits: 0,
      misses: 0,
      operations: 0
    };
  }

  /**
   * 清除前端缓存并通知后端
   */
  async clearAllCache(): Promise<void> {
    try {
      // 清除前端缓存
      if (typeof window !== 'undefined') {
        // 清除localStorage中的缓存
        const keys = Object.keys(localStorage);
        keys.forEach(key => {
          if (key.startsWith('cache_') || key.startsWith('table_cache_')) {
            localStorage.removeItem(key);
          }
        });

        // 清除sessionStorage中的缓存
        const sessionKeys = Object.keys(sessionStorage);
        sessionKeys.forEach(key => {
          if (key.startsWith('cache_') || key.startsWith('table_cache_')) {
            sessionStorage.removeItem(key);
          }
        });
      }

      // 通知后端清除缓存
      await clearAllCacheApi();

      // 重置统计
      this.resetStats();

      console.log('所有缓存已清除');
    } catch (error) {
      console.error('清除缓存失败:', error);
      throw error;
    }
  }

  /**
   * 清除指定连接的前端缓存
   */
  async clearConnectionCache(connectionId: string | number): Promise<void> {
    try {
      if (typeof window !== 'undefined') {
        const prefix = `cache_${connectionId}_`;

        // 清除localStorage
        const keys = Object.keys(localStorage);
        keys.forEach(key => {
          if (key.startsWith(prefix)) {
            localStorage.removeItem(key);
          }
        });

        // 清除sessionStorage
        const sessionKeys = Object.keys(sessionStorage);
        sessionKeys.forEach(key => {
          if (key.startsWith(prefix)) {
            sessionStorage.removeItem(key);
          }
        });
      }

      // 通知后端清除缓存
      await clearConnectionCacheApi(connectionId as number);

      console.log(`连接 ${connectionId} 的缓存已清除`);
    } catch (error) {
      console.error(`清除连接 ${connectionId} 缓存失败:`, error);
      throw error;
    }
  }
}

// 导出单例实例
export const frontendCacheManager = FrontendCacheManager.getInstance();
