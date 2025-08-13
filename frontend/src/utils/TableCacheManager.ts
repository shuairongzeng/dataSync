/**
 * 表缓存管理器
 * 提供多级缓存策略：内存缓存 + LocalStorage + 可选的SQLite持久化
 */

export interface TableInfo {
  tableName: string
  tableType: 'TABLE' | 'VIEW'
  columnCount: number
  remarks?: string
  hasPrimaryKey: boolean
  schema?: string
  connectionId: string
  lastUpdated: number
  // 标志位：表示详细信息（如列数、主键等）是否已加载
  _detailsLoaded?: boolean
}

export interface CacheMetadata {
  connectionId: string
  schema?: string
  totalCount: number
  lastUpdated: number
  version: string
}

export interface CacheEntry {
  metadata: CacheMetadata
  tables: TableInfo[]
}

class TableCacheManager {
  private memoryCache = new Map<string, CacheEntry>()
  private readonly CACHE_VERSION = '1.0.0'
  private readonly DEFAULT_EXPIRE_TIME = 24 * 60 * 60 * 1000 // 24小时
  private readonly STORAGE_PREFIX = 'table_cache_'
  private readonly METADATA_PREFIX = 'table_meta_'

  /**
   * 生成缓存键
   */
  private getCacheKey(connectionId: string, schema?: string): string {
    return `${connectionId}_${schema || 'default'}`
  }

  /**
   * 生成存储键
   */
  private getStorageKey(connectionId: string, schema?: string): string {
    return `${this.STORAGE_PREFIX}${this.getCacheKey(connectionId, schema)}`
  }

  /**
   * 生成元数据存储键
   */
  private getMetadataKey(connectionId: string, schema?: string): string {
    return `${this.METADATA_PREFIX}${this.getCacheKey(connectionId, schema)}`
  }

  /**
   * 检查缓存是否过期
   */
  private isExpired(lastUpdated: number, expireTime = this.DEFAULT_EXPIRE_TIME): boolean {
    return Date.now() - lastUpdated > expireTime
  }

  /**
   * 从LocalStorage加载缓存
   */
  private loadFromStorage(connectionId: string, schema?: string): CacheEntry | null {
    try {
      const storageKey = this.getStorageKey(connectionId, schema)
      const metadataKey = this.getMetadataKey(connectionId, schema)
      
      const cachedData = localStorage.getItem(storageKey)
      const cachedMetadata = localStorage.getItem(metadataKey)
      
      if (!cachedData || !cachedMetadata) {
        return null
      }

      const tables: TableInfo[] = JSON.parse(cachedData)
      const metadata: CacheMetadata = JSON.parse(cachedMetadata)

      // 检查版本兼容性
      if (metadata.version !== this.CACHE_VERSION) {
        this.clearCache(connectionId, schema)
        return null
      }

      // 检查是否过期
      if (this.isExpired(metadata.lastUpdated)) {
        this.clearCache(connectionId, schema)
        return null
      }

      return { metadata, tables }
    } catch (error) {
      console.warn('Failed to load cache from storage:', error)
      return null
    }
  }

  /**
   * 保存到LocalStorage
   */
  private saveToStorage(connectionId: string, schema: string | undefined, entry: CacheEntry): void {
    try {
      const storageKey = this.getStorageKey(connectionId, schema)
      const metadataKey = this.getMetadataKey(connectionId, schema)
      
      localStorage.setItem(storageKey, JSON.stringify(entry.tables))
      localStorage.setItem(metadataKey, JSON.stringify(entry.metadata))
    } catch (error) {
      console.warn('Failed to save cache to storage:', error)
      // 如果存储空间不足，尝试清理旧缓存
      this.cleanupOldCaches()
    }
  }

  /**
   * 获取缓存的表列表
   */
  public getCachedTables(connectionId: string, schema?: string): TableInfo[] | null {
    const cacheKey = this.getCacheKey(connectionId, schema)
    
    // 首先检查内存缓存
    let cacheEntry = this.memoryCache.get(cacheKey)
    
    // 如果内存中没有，尝试从LocalStorage加载
    if (!cacheEntry) {
      cacheEntry = this.loadFromStorage(connectionId, schema)
      if (cacheEntry) {
        this.memoryCache.set(cacheKey, cacheEntry)
      }
    }

    if (!cacheEntry) {
      return null
    }

    // 检查是否过期
    if (this.isExpired(cacheEntry.metadata.lastUpdated)) {
      this.clearCache(connectionId, schema)
      return null
    }

    return cacheEntry.tables
  }

  /**
   * 缓存表列表
   */
  public cacheTables(
    connectionId: string, 
    schema: string | undefined, 
    tables: TableInfo[]
  ): void {
    const cacheKey = this.getCacheKey(connectionId, schema)
    const now = Date.now()

    const metadata: CacheMetadata = {
      connectionId,
      schema,
      totalCount: tables.length,
      lastUpdated: now,
      version: this.CACHE_VERSION
    }

    const cacheEntry: CacheEntry = {
      metadata,
      tables: tables.map(table => ({
        ...table,
        lastUpdated: now
      }))
    }

    // 保存到内存缓存
    this.memoryCache.set(cacheKey, cacheEntry)
    
    // 保存到LocalStorage
    this.saveToStorage(connectionId, schema, cacheEntry)
  }

  /**
   * 获取缓存元数据
   */
  public getCacheMetadata(connectionId: string, schema?: string): CacheMetadata | null {
    const cacheKey = this.getCacheKey(connectionId, schema)
    const cacheEntry = this.memoryCache.get(cacheKey) || this.loadFromStorage(connectionId, schema)
    
    return cacheEntry?.metadata || null
  }

  /**
   * 检查是否有缓存
   */
  public hasCache(connectionId: string, schema?: string): boolean {
    return this.getCachedTables(connectionId, schema) !== null
  }

  /**
   * 清除指定连接的缓存
   */
  public clearCache(connectionId: string, schema?: string): void {
    const cacheKey = this.getCacheKey(connectionId, schema)
    const storageKey = this.getStorageKey(connectionId, schema)
    const metadataKey = this.getMetadataKey(connectionId, schema)
    
    // 清除内存缓存
    this.memoryCache.delete(cacheKey)
    
    // 清除LocalStorage
    localStorage.removeItem(storageKey)
    localStorage.removeItem(metadataKey)
  }

  /**
   * 清除所有缓存
   */
  public clearAllCaches(): void {
    // 清除内存缓存
    this.memoryCache.clear()
    
    // 清除LocalStorage中的所有表缓存
    const keysToRemove: string[] = []
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key && (key.startsWith(this.STORAGE_PREFIX) || key.startsWith(this.METADATA_PREFIX))) {
        keysToRemove.push(key)
      }
    }
    
    keysToRemove.forEach(key => localStorage.removeItem(key))
  }

  /**
   * 清理过期的缓存
   */
  public cleanupExpiredCaches(): void {
    const now = Date.now()
    
    // 清理内存缓存
    for (const [key, entry] of this.memoryCache.entries()) {
      if (this.isExpired(entry.metadata.lastUpdated)) {
        this.memoryCache.delete(key)
      }
    }
    
    // 清理LocalStorage中的过期缓存
    const keysToRemove: string[] = []
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key && key.startsWith(this.METADATA_PREFIX)) {
        try {
          const metadata: CacheMetadata = JSON.parse(localStorage.getItem(key) || '{}')
          if (this.isExpired(metadata.lastUpdated)) {
            keysToRemove.push(key)
            // 同时删除对应的数据缓存
            const dataKey = key.replace(this.METADATA_PREFIX, this.STORAGE_PREFIX)
            keysToRemove.push(dataKey)
          }
        } catch (error) {
          // 如果解析失败，也删除这个键
          keysToRemove.push(key)
        }
      }
    }
    
    keysToRemove.forEach(key => localStorage.removeItem(key))
  }

  /**
   * 清理旧缓存（当存储空间不足时）
   */
  private cleanupOldCaches(): void {
    const cacheEntries: Array<{ key: string; lastUpdated: number }> = []
    
    // 收集所有缓存条目的时间戳
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key && key.startsWith(this.METADATA_PREFIX)) {
        try {
          const metadata: CacheMetadata = JSON.parse(localStorage.getItem(key) || '{}')
          cacheEntries.push({ key, lastUpdated: metadata.lastUpdated })
        } catch (error) {
          // 解析失败的条目也加入清理列表
          cacheEntries.push({ key, lastUpdated: 0 })
        }
      }
    }
    
    // 按时间排序，删除最旧的一半缓存
    cacheEntries.sort((a, b) => a.lastUpdated - b.lastUpdated)
    const toDelete = cacheEntries.slice(0, Math.ceil(cacheEntries.length / 2))
    
    toDelete.forEach(({ key }) => {
      localStorage.removeItem(key)
      // 同时删除对应的数据缓存
      const dataKey = key.replace(this.METADATA_PREFIX, this.STORAGE_PREFIX)
      localStorage.removeItem(dataKey)
    })
  }

  /**
   * 获取缓存统计信息
   */
  public getCacheStats(): {
    memoryCount: number
    storageCount: number
    totalSize: number
  } {
    let storageCount = 0
    let totalSize = 0
    
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key && (key.startsWith(this.STORAGE_PREFIX) || key.startsWith(this.METADATA_PREFIX))) {
        storageCount++
        const value = localStorage.getItem(key) || ''
        totalSize += key.length + value.length
      }
    }
    
    return {
      memoryCount: this.memoryCache.size,
      storageCount: storageCount / 2, // 每个缓存有两个键（数据和元数据）
      totalSize
    }
  }

  /**
   * 搜索缓存的表
   */
  public searchCachedTables(
    connectionId: string, 
    schema: string | undefined, 
    searchText: string
  ): TableInfo[] | null {
    const tables = this.getCachedTables(connectionId, schema)
    if (!tables) {
      return null
    }

    if (!searchText.trim()) {
      return tables
    }

    const searchLower = searchText.toLowerCase()
    return tables.filter(table => 
      table.tableName.toLowerCase().includes(searchLower) ||
      (table.remarks && table.remarks.toLowerCase().includes(searchLower))
    )
  }

  /**
   * 更新单个表的信息
   */
  public updateTableInfo(
    connectionId: string, 
    schema: string | undefined, 
    tableName: string, 
    updates: Partial<TableInfo>
  ): boolean {
    const cacheKey = this.getCacheKey(connectionId, schema)
    const cacheEntry = this.memoryCache.get(cacheKey)
    
    if (!cacheEntry) {
      return false
    }

    const tableIndex = cacheEntry.tables.findIndex(t => t.tableName === tableName)
    if (tableIndex === -1) {
      return false
    }

    // 更新表信息
    cacheEntry.tables[tableIndex] = {
      ...cacheEntry.tables[tableIndex],
      ...updates,
      lastUpdated: Date.now()
    }

    // 更新元数据
    cacheEntry.metadata.lastUpdated = Date.now()

    // 保存到存储
    this.saveToStorage(connectionId, schema, cacheEntry)
    
    return true
  }
}

// 导出单例实例
export const tableCacheManager = new TableCacheManager()

// 定期清理过期缓存（每小时执行一次）
setInterval(() => {
  tableCacheManager.cleanupExpiredCaches()
}, 60 * 60 * 1000)