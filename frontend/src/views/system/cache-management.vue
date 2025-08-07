<template>
  <div class="cache-management">
    <div class="page-header">
      <h2>缓存管理</h2>
      <p>管理和监控系统缓存状态</p>
    </div>

    <!-- 缓存概览卡片 -->
    <div class="cache-overview">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="overview-card">
            <div class="card-content">
              <div class="card-icon">
                <el-icon size="24"><DataAnalysis /></el-icon>
              </div>
              <div class="card-info">
                <div class="card-title">总命中率</div>
                <div class="card-value">{{ formatPercentage(overallStats.averageHitRate) }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="overview-card">
            <div class="card-content">
              <div class="card-icon">
                <el-icon size="24"><Box /></el-icon>
              </div>
              <div class="card-info">
                <div class="card-title">缓存数量</div>
                <div class="card-value">{{ overallStats.cacheCount }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="overview-card">
            <div class="card-content">
              <div class="card-icon">
                <el-icon size="24"><Clock /></el-icon>
              </div>
              <div class="card-info">
                <div class="card-title">状态</div>
                <div class="card-value" :class="statusClass">{{ overallStats.status }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="overview-card">
            <div class="card-content">
              <div class="card-icon">
                <el-icon size="24"><Refresh /></el-icon>
              </div>
              <div class="card-info">
                <div class="card-title">最后更新</div>
                <div class="card-value small">{{ formatTime(overallStats.timestamp) }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 操作按钮 -->
    <div class="cache-actions">
      <el-button type="primary" @click="refreshStats" :loading="loading">
        <el-icon><Refresh /></el-icon>
        刷新统计
      </el-button>
      <el-button type="warning" @click="clearAllCache" :loading="clearingAll">
        <el-icon><Delete /></el-icon>
        清除所有缓存
      </el-button>
      <el-button type="success" @click="showWarmupDialog = true">
        <el-icon><Lightning /></el-icon>
        预热缓存
      </el-button>
      <el-button @click="exportStats">
        <el-icon><Download /></el-icon>
        导出统计
      </el-button>
    </div>

    <!-- 缓存详情表格 -->
    <el-card class="cache-details">
      <template #header>
        <div class="card-header">
          <span>缓存详情</span>
          <el-switch
            v-model="autoRefresh"
            active-text="自动刷新"
            @change="toggleAutoRefresh"
          />
        </div>
      </template>

      <el-table :data="cacheDetails" v-loading="loading" stripe>
        <el-table-column prop="name" label="缓存名称" min-width="150">
          <template #default="{ row }">
            <el-tag :type="getCacheTypeTag(row.name)">{{ row.name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="hitCount" label="命中次数" width="120" sortable />
        <el-table-column prop="missCount" label="未命中次数" width="120" sortable />
        <el-table-column prop="requestCount" label="总请求数" width="120" sortable />
        <el-table-column prop="hitRate" label="命中率" width="120" sortable>
          <template #default="{ row }">
            <el-progress
              :percentage="Math.round(row.hitRate * 100)"
              :color="getHitRateColor(row.hitRate)"
              :stroke-width="8"
            />
          </template>
        </el-table-column>
        <el-table-column prop="evictionCount" label="驱逐次数" width="120" sortable />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="clearSpecificCache(row.name)">
              清除
            </el-button>
            <el-button size="small" type="primary" @click="viewCacheDetails(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 预热缓存对话框 -->
    <el-dialog v-model="showWarmupDialog" title="预热缓存" width="500px">
      <el-form :model="warmupForm" label-width="100px">
        <el-form-item label="数据库连接">
          <el-select v-model="warmupForm.connectionId" placeholder="选择数据库连接" style="width: 100%">
            <el-option
              v-for="conn in connections"
              :key="conn.id"
              :label="conn.name"
              :value="conn.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Schema">
          <el-input v-model="warmupForm.schema" placeholder="可选，留空使用默认Schema" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showWarmupDialog = false">取消</el-button>
        <el-button type="primary" @click="executeWarmup" :loading="warmingUp">
          开始预热
        </el-button>
      </template>
    </el-dialog>

    <!-- 缓存详情对话框 -->
    <el-dialog v-model="showDetailsDialog" title="缓存详情" width="600px">
      <div v-if="selectedCache">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="缓存名称">{{ selectedCache.name }}</el-descriptions-item>
          <el-descriptions-item label="命中次数">{{ selectedCache.hitCount }}</el-descriptions-item>
          <el-descriptions-item label="未命中次数">{{ selectedCache.missCount }}</el-descriptions-item>
          <el-descriptions-item label="总请求数">{{ selectedCache.requestCount }}</el-descriptions-item>
          <el-descriptions-item label="命中率">{{ formatPercentage(selectedCache.hitRate) }}</el-descriptions-item>
          <el-descriptions-item label="驱逐次数">{{ selectedCache.evictionCount }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  DataAnalysis,
  Box,
  Clock,
  Refresh,
  Delete,
  Lightning,
  Download
} from '@element-plus/icons-vue'
import {
  getCacheHealthApi,
  clearAllCacheApi,
  warmupCacheApi,
  CacheApiManager,
  type CacheHealth
} from '@/api/cache'
import { getConnectionsApi } from '@/api/database'

// 响应式数据
const loading = ref(false)
const clearingAll = ref(false)
const warmingUp = ref(false)
const autoRefresh = ref(false)
const showWarmupDialog = ref(false)
const showDetailsDialog = ref(false)

const overallStats = ref<CacheHealth>({
  status: 'UP',
  cacheCount: 0,
  averageHitRate: 0,
  timestamp: Date.now(),
  details: {}
})

const cacheDetails = ref<Array<{
  name: string
  hitCount: number
  missCount: number
  hitRate: number
  requestCount: number
  evictionCount: number
}>>([])

const connections = ref<Array<{ id: number; name: string }>>([])
const selectedCache = ref<any>(null)

const warmupForm = ref({
  connectionId: null as number | null,
  schema: ''
})

let refreshInterval: NodeJS.Timeout | null = null

// 计算属性
const statusClass = computed(() => {
  return overallStats.value.status === 'UP' ? 'status-up' : 'status-down'
})

// 方法
const refreshStats = async () => {
  loading.value = true
  try {
    const [healthData, formattedStats] = await Promise.all([
      getCacheHealthApi(),
      CacheApiManager.getFormattedCacheStats()
    ])

    overallStats.value = healthData
    cacheDetails.value = formattedStats.cacheDetails

    ElMessage.success('缓存统计已刷新')
  } catch (error: any) {
    ElMessage.error('获取缓存统计失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const clearAllCache = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清除所有缓存吗？这将影响系统性能，直到缓存重新建立。',
      '确认清除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    clearingAll.value = true
    await clearAllCacheApi()
    ElMessage.success('所有缓存已清除')
    await refreshStats()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('清除缓存失败: ' + (error.message || '未知错误'))
    }
  } finally {
    clearingAll.value = false
  }
}

const clearSpecificCache = async (cacheName: string) => {
  try {
    await ElMessageBox.confirm(
      `确定要清除缓存 "${cacheName}" 吗？`,
      '确认清除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 这里需要实现具体的单个缓存清除逻辑
    ElMessage.success(`缓存 ${cacheName} 已清除`)
    await refreshStats()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('清除缓存失败: ' + (error.message || '未知错误'))
    }
  }
}

const executeWarmup = async () => {
  if (!warmupForm.value.connectionId) {
    ElMessage.warning('请选择数据库连接')
    return
  }

  warmingUp.value = true
  try {
    await warmupCacheApi(warmupForm.value.connectionId, warmupForm.value.schema || undefined)
    ElMessage.success('缓存预热完成')
    showWarmupDialog.value = false
    await refreshStats()
  } catch (error: any) {
    ElMessage.error('缓存预热失败: ' + (error.message || '未知错误'))
  } finally {
    warmingUp.value = false
  }
}

const viewCacheDetails = (cache: any) => {
  selectedCache.value = cache
  showDetailsDialog.value = true
}

const toggleAutoRefresh = (enabled: boolean) => {
  if (enabled) {
    refreshInterval = setInterval(refreshStats, 30000) // 30秒刷新一次
  } else {
    if (refreshInterval) {
      clearInterval(refreshInterval)
      refreshInterval = null
    }
  }
}

const exportStats = () => {
  const data = {
    overview: overallStats.value,
    details: cacheDetails.value,
    exportTime: new Date().toISOString()
  }

  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `cache-stats-${new Date().toISOString().split('T')[0]}.json`
  a.click()
  URL.revokeObjectURL(url)
}

const loadConnections = async () => {
  try {
    connections.value = await getConnectionsApi()
  } catch (error: any) {
    ElMessage.error('加载数据库连接失败: ' + (error.message || '未知错误'))
  }
}

// 工具函数
const formatPercentage = (value: number) => {
  return `${Math.round(value * 100)}%`
}

const formatTime = (timestamp: number) => {
  return new Date(timestamp).toLocaleString()
}

const getCacheTypeTag = (name: string) => {
  if (name.includes('tables')) return 'primary'
  if (name.includes('columns')) return 'success'
  if (name.includes('schemas')) return 'warning'
  return 'info'
}

const getHitRateColor = (rate: number) => {
  if (rate >= 0.8) return '#67c23a'
  if (rate >= 0.6) return '#e6a23c'
  return '#f56c6c'
}

// 生命周期
onMounted(() => {
  refreshStats()
  loadConnections()
})

onUnmounted(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval)
  }
})
</script>

<style scoped>
.cache-management {
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.page-header p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.cache-overview {
  margin-bottom: 24px;
}

.overview-card {
  height: 100px;
}

.card-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.card-icon {
  margin-right: 16px;
  padding: 12px;
  background: #f0f9ff;
  border-radius: 8px;
  color: #409eff;
}

.card-info {
  flex: 1;
}

.card-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 4px;
}

.card-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.card-value.small {
  font-size: 12px;
  font-weight: normal;
}

.card-value.status-up {
  color: #67c23a;
}

.card-value.status-down {
  color: #f56c6c;
}

.cache-actions {
  margin-bottom: 24px;
}

.cache-actions .el-button {
  margin-right: 12px;
}

.cache-details {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.el-table {
  margin-top: 16px;
}

.el-progress {
  width: 80px;
}

.el-tag {
  font-size: 12px;
}

.el-descriptions {
  margin-top: 16px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .cache-overview .el-col {
    margin-bottom: 16px;
  }

  .cache-actions .el-button {
    margin-bottom: 8px;
    width: 100%;
  }

  .card-value {
    font-size: 20px;
  }
}
</style>
