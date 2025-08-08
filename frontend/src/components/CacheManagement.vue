<template>
  <div class="cache-management">
    <el-card class="cache-stats-card">
      <template #header>
        <div class="card-header">
          <span>缓存统计</span>
          <el-button size="small" @click="refreshStats" :loading="refreshing">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>
      
      <div class="stats-grid">
        <div class="stat-item">
          <div class="stat-value">{{ cacheStats.memoryCount }}</div>
          <div class="stat-label">内存缓存</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ cacheStats.storageCount }}</div>
          <div class="stat-label">持久缓存</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ cacheStats.formattedSize }}</div>
          <div class="stat-label">占用空间</div>
        </div>
        <div class="stat-item">
          <div class="stat-value" :class="healthStatus.class">
            {{ healthStatus.text }}
          </div>
          <div class="stat-label">健康状态</div>
        </div>
      </div>
    </el-card>

    <el-card class="cache-actions-card">
      <template #header>
        <span>缓存操作</span>
      </template>
      
      <div class="actions-grid">
        <el-button 
          type="primary" 
          @click="optimizeCache"
          :loading="optimizing"
        >
          <el-icon><Tools /></el-icon>
          优化缓存
        </el-button>
        
        <el-button 
          type="warning" 
          @click="cleanupExpired"
          :loading="cleaning"
        >
          <el-icon><Delete /></el-icon>
          清理过期
        </el-button>
        
        <el-button 
          type="danger" 
          @click="confirmClearAll"
        >
          <el-icon><DeleteFilled /></el-icon>
          清空所有
        </el-button>
        
        <el-button 
          type="info" 
          @click="exportCache"
        >
          <el-icon><Download /></el-icon>
          导出缓存
        </el-button>
        
        <el-button 
          type="info" 
          @click="showImportDialog = true"
        >
          <el-icon><Upload /></el-icon>
          导入缓存
        </el-button>
      </div>
    </el-card>

    <el-card class="cache-health-card" v-if="healthCheck && !healthCheck.isHealthy">
      <template #header>
        <span class="health-header">
          <el-icon color="#F56C6C"><WarningFilled /></el-icon>
          缓存健康问题
        </span>
      </template>
      
      <div class="health-content">
        <div class="issues-section">
          <h4>发现的问题：</h4>
          <ul>
            <li v-for="issue in healthCheck.issues" :key="issue">{{ issue }}</li>
          </ul>
        </div>
        
        <div class="recommendations-section">
          <h4>建议操作：</h4>
          <ul>
            <li v-for="rec in healthCheck.recommendations" :key="rec">{{ rec }}</li>
          </ul>
        </div>
      </div>
    </el-card>

    <!-- 导入对话框 -->
    <el-dialog
      v-model="showImportDialog"
      title="导入缓存数据"
      width="600px"
    >
      <div class="import-content">
        <el-alert
          title="注意"
          description="导入缓存数据将覆盖现有缓存，请确保数据来源可靠"
          type="warning"
          :closable="false"
          style="margin-bottom: 16px"
        />
        
        <el-input
          v-model="importData"
          type="textarea"
          :rows="10"
          placeholder="请粘贴缓存数据的JSON格式..."
        />
      </div>
      
      <template #footer>
        <el-button @click="showImportDialog = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="importCache"
          :disabled="!importData.trim()"
        >
          导入
        </el-button>
      </template>
    </el-dialog>

    <!-- 确认清空对话框 -->
    <el-dialog
      v-model="showClearConfirm"
      title="确认清空所有缓存"
      width="400px"
    >
      <div class="confirm-content">
        <el-icon color="#F56C6C" size="48"><WarningFilled /></el-icon>
        <p>此操作将清空所有表缓存数据，无法恢复。确定要继续吗？</p>
      </div>
      
      <template #footer>
        <el-button @click="showClearConfirm = false">取消</el-button>
        <el-button 
          type="danger" 
          @click="clearAllCache"
        >
          确认清空
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Refresh, 
  Tools, 
  Delete, 
  DeleteFilled, 
  Download, 
  Upload, 
  WarningFilled 
} from '@element-plus/icons-vue'
import { CacheUtils } from '@/utils/CacheUtils'

// 响应式数据
const refreshing = ref(false)
const optimizing = ref(false)
const cleaning = ref(false)
const showImportDialog = ref(false)
const showClearConfirm = ref(false)
const importData = ref('')

const cacheStats = ref({
  memoryCount: 0,
  storageCount: 0,
  totalSize: 0,
  formattedSize: '0 Bytes'
})

const healthCheck = ref<{
  isHealthy: boolean
  issues: string[]
  recommendations: string[]
} | null>(null)

// 计算属性
const healthStatus = computed(() => {
  if (!healthCheck.value) {
    return { text: '未知', class: 'unknown' }
  }
  
  if (healthCheck.value.isHealthy) {
    return { text: '良好', class: 'healthy' }
  } else {
    return { text: '异常', class: 'unhealthy' }
  }
})

// 方法
const refreshStats = async () => {
  refreshing.value = true
  try {
    cacheStats.value = CacheUtils.getCacheStats()
    healthCheck.value = CacheUtils.checkCacheHealth()
  } catch (error) {
    console.error('Failed to refresh cache stats:', error)
    ElMessage.error('刷新缓存统计失败')
  } finally {
    refreshing.value = false
  }
}

const optimizeCache = async () => {
  optimizing.value = true
  try {
    await CacheUtils.optimizeCache()
    await refreshStats()
  } catch (error) {
    console.error('Failed to optimize cache:', error)
  } finally {
    optimizing.value = false
  }
}

const cleanupExpired = async () => {
  cleaning.value = true
  try {
    CacheUtils.cleanupExpiredCaches()
    ElMessage.success('过期缓存清理完成')
    await refreshStats()
  } catch (error) {
    console.error('Failed to cleanup expired caches:', error)
    ElMessage.error('清理过期缓存失败')
  } finally {
    cleaning.value = false
  }
}

const confirmClearAll = () => {
  showClearConfirm.value = true
}

const clearAllCache = () => {
  try {
    CacheUtils.clearAllCaches()
    showClearConfirm.value = false
    refreshStats()
  } catch (error) {
    console.error('Failed to clear all caches:', error)
  }
}

const exportCache = () => {
  try {
    const data = CacheUtils.exportCacheData()
    
    // 创建下载链接
    const blob = new Blob([data], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `table-cache-${new Date().toISOString().split('T')[0]}.json`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    
    ElMessage.success('缓存数据导出成功')
  } catch (error) {
    console.error('Failed to export cache:', error)
    ElMessage.error('导出缓存失败')
  }
}

const importCache = () => {
  try {
    CacheUtils.importCacheData(importData.value)
    showImportDialog.value = false
    importData.value = ''
    refreshStats()
  } catch (error) {
    console.error('Failed to import cache:', error)
  }
}

// 生命周期
onMounted(() => {
  refreshStats()
})
</script>

<style scoped>
.cache-management {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 16px;
}

.stat-item {
  text-align: center;
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 8px;
}

.stat-value.healthy {
  color: var(--el-color-success);
}

.stat-value.unhealthy {
  color: var(--el-color-danger);
}

.stat-value.unknown {
  color: var(--el-color-info);
}

.stat-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 12px;
}

.health-header {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--el-color-danger);
}

.health-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.issues-section,
.recommendations-section {
  background: var(--el-fill-color-lighter);
  padding: 12px;
  border-radius: 6px;
}

.issues-section h4,
.recommendations-section h4 {
  margin: 0 0 8px 0;
  color: var(--el-text-color-primary);
}

.issues-section ul,
.recommendations-section ul {
  margin: 0;
  padding-left: 20px;
}

.issues-section li,
.recommendations-section li {
  margin-bottom: 4px;
  color: var(--el-text-color-regular);
}

.import-content {
  display: flex;
  flex-direction: column;
}

.confirm-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  text-align: center;
}

.confirm-content p {
  margin: 0;
  color: var(--el-text-color-regular);
}
</style>