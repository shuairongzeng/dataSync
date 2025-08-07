/**
 * 前端缓存工具类
 * 提供统一的缓存管理功能
 */

export interface CacheItem<T> {
  data: T;
  timestamp: number;
  expireTime: number;
}

export interface CacheConfig {
  // 默认缓存时间（毫秒）
  defaultTTL: number;
  // 最大缓存条目数
  maxSize: number;
  // 是否启用缓存
  enabled: boolean;
}

export class CacheManager {
  private cache = new Map<string, CacheItem<any>>();
  private config: CacheConfig;

  constructor(config: Partial<CacheConfig> = {}) {
    this.config = {
      defaultTTL: 5 * 60 * 1000, // 5分钟
      maxSize: 100,
      enabled: true,
      ...config
    };
  }

  /**
   * 设置缓存
   */
  set<T>(key: string, data: T, ttl?: number): void {
    if (!this.config.enabled) return;

    const expireTime = ttl || this.config.defaultTTL;
    const item: CacheItem<T> = {
      data,
      timestamp: Date.now(),
      expireTime: Date.now() + expireTime
    };

    // 检查缓存大小限制
    if (this.cache.size >= this.config.maxSize) {
      this.evictOldest();
    }

    this.cache.set(key, item);
  }

  /**
   * 获取缓存
   */
  get<T>(key: string): T | null {
    if (!this.config.enabled) return null;

    const item = this.cache.get(key);
    if (!item) return null;

    // 检查是否过期
    if (Date.now() > item.expireTime) {
      this.cache.delete(key);
      return null;
    }

    return item.data as T;
  }

  /**
   * 检查缓存是否存在且有效
   */
  has(key: string): boolean {
    if (!this.config.enabled) return false;

    const item = this.cache.get(key);
    if (!item) return false;

    if (Date.now() > item.expireTime) {
      this.cache.delete(key);
      return false;
    }

    return true;
  }

  /**
   * 删除指定缓存
   */
  delete(key: string): boolean {
    return this.cache.delete(key);
  }

  /**
   * 清空所有缓存
   */
  clear(): void {
    this.cache.clear();
  }

  /**
   * 根据前缀删除缓存
   */
  deleteByPrefix(prefix: string): number {
    let count = 0;
    for (const key of this.cache.keys()) {
      if (key.startsWith(prefix)) {
        this.cache.delete(key);
        count++;
      }
    }
    return count;
  }

  /**
   * 获取缓存统计信息
   */
  getStats() {
    const now = Date.now();
    let validCount = 0;
    let expiredCount = 0;

    for (const [key, item] of this.cache.entries()) {
      if (now > item.expireTime) {
        expiredCount++;
      } else {
        validCount++;
      }
    }

    return {
      totalCount: this.cache.size,
      validCount,
      expiredCount,
      maxSize: this.config.maxSize,
      hitRate: this.getHitRate()
    };
  }

  /**
   * 清理过期缓存
   */
  cleanup(): number {
    const now = Date.now();
    let cleanedCount = 0;

    for (const [key, item] of this.cache.entries()) {
      if (now > item.expireTime) {
        this.cache.delete(key);
        cleanedCount++;
      }
    }

    return cleanedCount;
  }

  /**
   * 驱逐最旧的缓存项
   */
  private evictOldest(): void {
    let oldestKey: string | null = null;
    let oldestTime = Date.now();

    for (const [key, item] of this.cache.entries()) {
      if (item.timestamp < oldestTime) {
        oldestTime = item.timestamp;
        oldestKey = key;
      }
    }

    if (oldestKey) {
      this.cache.delete(oldestKey);
    }
  }

  /**
   * 计算命中率（简化版本）
   */
  private getHitRate(): number {
    // 这里可以实现更复杂的命中率统计
    return 0.0;
  }

  /**
   * 更新配置
   */
  updateConfig(newConfig: Partial<CacheConfig>): void {
    this.config = { ...this.config, ...newConfig };
  }

  /**
   * 获取所有缓存键
   */
  getKeys(): string[] {
    return Array.from(this.cache.keys());
  }

  /**
   * 获取缓存项详情
   */
  getItem(key: string): CacheItem<any> | null {
    return this.cache.get(key) || null;
  }
}

// 创建全局缓存实例
export const globalCache = new CacheManager({
  defaultTTL: 10 * 60 * 1000, // 10分钟
  maxSize: 200,
  enabled: true
});

// 数据库元数据专用缓存
export const dbMetadataCache = new CacheManager({
  defaultTTL: 30 * 60 * 1000, // 30分钟
  maxSize: 50,
  enabled: true
});

// 缓存键生成工具
export const CacheKeys = {
  // 表列表缓存键
  tables: (connectionId: string | number, schema?: string) => 
    `tables_${connectionId}_${schema || 'default'}`,
  
  // 表结构缓存键
  tableColumns: (connectionId: string | number, tableName: string, schema?: string) => 
    `columns_${connectionId}_${tableName}_${schema || 'default'}`,
  
  // Schema列表缓存键
  schemas: (connectionId: string | number) => 
    `schemas_${connectionId}`,
  
  // 分页表列表缓存键
  tablesPagination: (connectionId: string | number, page: number, size: number, search?: string, schema?: string) => 
    `tables_page_${connectionId}_${page}_${size}_${search || ''}_${schema || 'default'}`,
  
  // 连接相关的所有缓存前缀
  connectionPrefix: (connectionId: string | number) => 
    `${connectionId}_`
};

// 缓存装饰器函数
export function withCache<T>(
  cacheManager: CacheManager,
  keyGenerator: (...args: any[]) => string,
  ttl?: number
) {
  return function(target: any, propertyName: string, descriptor: PropertyDescriptor) {
    const method = descriptor.value;

    descriptor.value = async function(...args: any[]): Promise<T> {
      const cacheKey = keyGenerator(...args);
      
      // 尝试从缓存获取
      const cached = cacheManager.get<T>(cacheKey);
      if (cached !== null) {
        console.log(`Cache hit for key: ${cacheKey}`);
        return cached;
      }

      // 缓存未命中，执行原方法
      console.log(`Cache miss for key: ${cacheKey}`);
      const result = await method.apply(this, args);
      
      // 存储到缓存
      cacheManager.set(cacheKey, result, ttl);
      
      return result;
    };

    return descriptor;
  };
}
