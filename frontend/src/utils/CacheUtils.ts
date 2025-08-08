/**
 * 缓存工具类
 * 提供缓存相关的辅助功能
 */

import { tableCacheManager } from './TableCacheManager'
import { ElMessage } from 'element-plus'

export class CacheUtils {
  /**
   * 预热多个连接的缓存
   */
  static async warmupMultipleConnections(
    connections: Array<{ id: string; schema?: string }>
  ): Promise<void> {
    const promises = connections.map(async (conn) => {
      try {
        // 检查是否已有缓存
        if (!tableCacheManager.hasCache(conn.id, conn.schema)) {
          console.log(`Warming up cache for connection ${conn.id}, schema: ${conn.schema}`)
          // 这里可以调用API预热缓存，暂时跳过
        }
      } catch (error) {
        console.warn(`Failed to warmup cache for connection ${conn.id}:`, error)
      }
    })

    await Promise.allSettled(promises)
  }

  /**
   * 清理过期缓存
   */
  static cleanupExpiredCaches(): void {
    try {
      tableCacheManager.cleanupExpiredCaches()
      console.log('Expired caches cleaned up successfully')
    } catch (error) {
      console.error('Failed to cleanup expired caches:', error)
    }
  }

  /**
   * 获取缓存统计信息
   */
  static getCacheStats(): {
    memoryCount: number
    storageCount: number
    totalSize: number
    formattedSize: string
  } {
    const stats = tableCacheManager.getCacheStats()
    
    return {
      ...stats,
      formattedSize: this.formatBytes(stats.totalSize)
    }
  }

  /**
   * 格式化字节大小
   */
  private static formatBytes(bytes: number): string {
    if (bytes === 0) return '0 Bytes'
    
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  /**
   * 清理所有缓存
   */
  static clearAllCaches(): void {
    try {
      tableCacheManager.clearAllCaches()
      ElMessage.success('所有缓存已清理')
    } catch (error) {
      console.error('Failed to clear all caches:', error)
      ElMessage.error('清理缓存失败')
    }
  }

  /**
   * 导出缓存数据
   */
  static exportCacheData(): string {
    try {
      const cacheData: any = {}
      
      // 导出LocalStorage中的缓存数据
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i)
        if (key && (key.startsWith('table_cache_') || key.startsWith('table_meta_'))) {
          cacheData[key] = localStorage.getItem(key)
        }
      }
      
      return JSON.stringify(cacheData, null, 2)
    } catch (error) {
      console.error('Failed to export cache data:', error)
      throw new Error('导出缓存数据失败')
    }
  }

  /**
   * 导入缓存数据
   */
  static importCacheData(jsonData: string): void {
    try {
      const cacheData = JSON.parse(jsonData)
      
      Object.entries(cacheData).forEach(([key, value]) => {
        if (key.startsWith('table_cache_') || key.startsWith('table_meta_')) {
          localStorage.setItem(key, value as string)
        }
      })
      
      ElMessage.success('缓存数据导入成功')
    } catch (error) {
      console.error('Failed to import cache data:', error)
      ElMessage.error('导入缓存数据失败')
    }
  }

  /**
   * 检查缓存健康状态
   */
  static checkCacheHealth(): {
    isHealthy: boolean
    issues: string[]
    recommendations: string[]
  } {
    const issues: string[] = []
    const recommendations: string[] = []
    
    try {
      const stats = tableCacheManager.getCacheStats()
      
      // 检查缓存大小
      if (stats.totalSize > 10 * 1024 * 1024) { // 10MB
        issues.push('缓存占用空间过大')
        recommendations.push('建议清理部分旧缓存')
      }
      
      // 检查缓存数量
      if (stats.storageCount > 50) {
        issues.push('缓存条目过多')
        recommendations.push('建议清理不常用的连接缓存')
      }
      
      // 检查LocalStorage可用性
      try {
        const testKey = 'cache_health_test'
        localStorage.setItem(testKey, 'test')
        localStorage.removeItem(testKey)
      } catch (error) {
        issues.push('LocalStorage不可用或空间不足')
        recommendations.push('清理浏览器存储空间')
      }
      
      return {
        isHealthy: issues.length === 0,
        issues,
        recommendations
      }
    } catch (error) {
      return {
        isHealthy: false,
        issues: ['缓存系统检查失败'],
        recommendations: ['重启应用或清理所有缓存']
      }
    }
  }

  /**
   * 自动优化缓存
   */
  static async optimizeCache(): Promise<void> {
    try {
      // 清理过期缓存
      this.cleanupExpiredCaches()
      
      // 检查健康状态
      const health = this.checkCacheHealth()
      
      if (!health.isHealthy) {
        // 如果不健康，执行优化操作
        if (health.issues.includes('缓存占用空间过大') || health.issues.includes('缓存条目过多')) {
          // 清理最旧的一半缓存
          tableCacheManager.cleanupExpiredCaches()
        }
      }
      
      ElMessage.success('缓存优化完成')
    } catch (error) {
      console.error('Failed to optimize cache:', error)
      ElMessage.error('缓存优化失败')
    }
  }
}

// 定期执行缓存优化（每30分钟）
setInterval(() => {
  CacheUtils.cleanupExpiredCaches()
}, 30 * 60 * 1000)

// 页面卸载时清理内存缓存
window.addEventListener('beforeunload', () => {
  // 这里可以添加清理逻辑，但通常浏览器会自动处理
})