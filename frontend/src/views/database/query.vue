<template>
  <div class="query-page">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-select
          v-model="selectedConnectionId"
          placeholder="选择数据库连接"
          @change="handleConnectionChange"
          style="width: 200px"
        >
          <el-option
            v-for="conn in connections"
            :key="conn.id"
            :label="conn.name"
            :value="conn.id.toString()"
          />
        </el-select>

        <el-select
          v-if="schemas.length > 0"
          v-model="selectedSchema"
          placeholder="选择Schema"
          @change="handleSchemaChange"
          style="width: 150px; margin-left: 10px"
        >
          <el-option
            v-for="schema in schemas"
            :key="schema"
            :label="schema"
            :value="schema"
          />
        </el-select>
      </div>

      <div class="toolbar-right">
        <el-button @click="executeQuery" :loading="executing" type="primary" :disabled="!sqlText.trim()">
          <el-icon><CaretRight /></el-icon>
          执行查询
        </el-button>
        <el-button @click="clearQuery">
          <el-icon><Delete /></el-icon>
          清空
        </el-button>
        <el-button @click="showHistory = !showHistory">
          <el-icon><Clock /></el-icon>
          历史记录
        </el-button>
        
        <!-- Cache Controls -->
        <div class="cache-controls">
          <el-checkbox v-model="useCaching" @change="onCacheSettingChange">
            启用缓存
          </el-checkbox>
          <el-button 
            @click="clearConnectionCache" 
            :disabled="!selectedConnectionId"
            size="small"
            type="warning"
          >
            清空缓存
          </el-button>
          <el-button 
            @click="warmupCache" 
            :disabled="!selectedConnectionId"
            size="small"
            type="info"
          >
            预热缓存
          </el-button>
        </div>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧面板 -->
      <div class="left-panel">
        <!-- 缓存表格列表 -->
        <div class="table-list-container">
          <CachedTableList
            v-if="selectedConnectionId"
            :connection-id="selectedConnectionId"
            :schema="selectedSchema"
            @table-click="handleTableClick"
            @sql-generated="handleSqlGenerated"
          />
          <el-empty
            v-else
            description="请先选择数据库连接"
            :image-size="100"
          />
        </div>
      </div>

      <!-- 右侧面板 -->
      <div class="right-panel">
        <!-- SQL 编辑器 -->
        <div class="sql-editor-container">
          <div class="editor-header">
            <span class="editor-title">SQL 编辑器</span>
            <div class="editor-actions">
              <el-button size="small" @click="formatSql">格式化</el-button>
              <el-button size="small" @click="validateSql">验证</el-button>
            </div>
          </div>

          <div class="editor-content">
            <SqlEditor
              v-model="sqlText"
              :height="'300px'"
              :tables="availableTables"
              :tableColumns="tableColumnsMap"
              :enable-completion="true"
              placeholder="请输入SQL查询语句..."
              @change="handleSqlChange"
            />
          </div>
        </div>

        <!-- 查询结果 -->
        <div class="result-container">
          <div class="result-header">
            <span class="result-title">查询结果</span>
            <div class="result-info" v-if="queryResult">
              <span>执行时间：{{ queryResult.executionTime }}ms</span>
              <span>返回行数：{{ queryResult.totalRows }}</span>
              <span v-if="lastQueryFromCache" class="cache-indicator">
                📦 来自缓存
              </span>
              <span v-else class="cache-indicator">
                🔄 实时查询
              </span>
            </div>
          </div>

          <div class="result-content">
            <!-- 加载状态 -->
            <div v-if="executing" class="loading-state">
              <el-skeleton :rows="5" animated />
            </div>

            <!-- 查询结果表格 -->
            <div v-if="queryResult && queryResult.data && queryResult.data.length > 0" class="table-container" ref="tableContainer">
              <el-table
                :data="queryResult.data"
                stripe
                class="result-table"
                :max-height="380"
              >
              <el-table-column
                v-for="column in queryResult.columns"
                :key="column"
                :prop="column"
                :label="getColumnDisplayName(column)"
                :min-width="200"
                show-overflow-tooltip
                resizable
              >
                <template #header>
                  <div class="column-header">
                    <span class="column-display-name">{{ getColumnDisplayName(column) }}</span>
                    <el-tooltip 
                      v-if="hasChineseName(column)" 
                      :content="`原始字段名: ${column}`" 
                      placement="top"
                    >
                      <el-icon class="column-info-icon"><InfoFilled /></el-icon>
                    </el-tooltip>
                  </div>
                </template>
              </el-table-column>
              </el-table>
            </div>

            <!-- 查询成功但无数据 -->
            <div
              v-else-if="queryResult && queryResult.data && queryResult.data.length === 0"
              class="empty-result"
            >
              <el-empty description="查询成功，但没有返回数据" :image-size="80" />
              <div class="result-summary">
                <span>执行时间：{{ queryResult.executionTime }}ms</span>
                <span>返回行数：0</span>
              </div>
            </div>

            <!-- 非查询操作结果（如 INSERT/UPDATE/DELETE） -->
            <div
              v-else-if="queryResult && queryResult.message"
              class="operation-result"
            >
              <el-result
                icon="success"
                :title="queryResult.message"
                :sub-title="`执行时间：${queryResult.executionTime}ms`"
              />
            </div>

            <!-- 默认空状态 -->
            <el-empty
              v-else-if="!executing"
              description="请输入SQL语句并点击执行查询"
              :image-size="100"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 历史记录抽屉 -->
    <el-drawer
      v-model="showHistory"
      title="查询历史"
      direction="rtl"
      size="400px"
    >
      <div class="history-content">
        <div class="history-search">
          <el-input
            v-model="historySearch"
            placeholder="搜索历史记录..."
            :prefix-icon="Search"
            clearable
          />
        </div>

        <div class="history-list">
          <div
            v-for="item in filteredHistory"
            :key="item.id"
            class="history-item"
            @click="loadHistoryQuery(item)"
          >
            <div class="history-sql">{{ item.sql }}</div>
            <div class="history-meta">
              <span class="history-time">{{ formatTime(item.createdAt) }}</span>
              <span class="history-status" :class="item.status.toLowerCase()">
                {{ item.status }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { CaretRight, Delete, Clock, Search, InfoFilled } from '@element-plus/icons-vue'
import CachedTableList from '@/components/CachedTableList.vue'
import SqlEditor from '@/components/SqlEditor/SqlEditor.vue'
import {
  getConnectionsApi,
  getSchemasApi,
  executeQueryApi,
  getQueryHistoryApi,
  getTablesApi,
  getTableColumnsApi
} from '@/api/database'
import { http } from '@/utils/http'

// 响应式数据
const connections = ref<any[]>([])
const selectedConnectionId = ref('')
const schemas = ref<string[]>([])
const selectedSchema = ref('')
const sqlText = ref('')
const executing = ref(false)
const queryResult = ref<any>(null)
const showHistory = ref(false)
const historySearch = ref('')
const queryHistory = ref<any[]>([])

// Cache-related data
const useCaching = ref(true)
const lastQueryFromCache = ref(false)

// 智能补全相关数据
const availableTables = ref<string[]>([])
const tableColumnsMap = ref<Map<string, string[]>>(new Map())

// 计算属性
const filteredHistory = computed(() => {
  if (!historySearch.value) return queryHistory.value
  return queryHistory.value.filter(item =>
    item.sql.toLowerCase().includes(historySearch.value.toLowerCase())
  )
})

// Enhanced Query Service Methods
const executeEnhancedQuery = async (connectionId: string, sql: string, schema?: string, options = {}) => {
  const response = await http.request('post', '/api/enhanced-query/execute', {
    data: {
      connectionId: parseInt(connectionId),
      sql,
      schema,
      useCache: useCaching.value,
      saveHistory: true,
      ...options
    }
  })
  return response
}

const loadTablesWithCache = async (connectionId: string, schema?: string) => {
  const params = new URLSearchParams()
  if (schema) params.append('schema', schema)
  params.append('useCache', 'true')
  
  const response = await http.request('get', `/api/enhanced-query/tables/${connectionId}?${params.toString()}`)
  return response
}

const loadTableColumnsWithCache = async (connectionId: string, tableName: string, schema?: string) => {
  const params = new URLSearchParams()
  if (schema) params.append('schema', schema)
  params.append('useCache', 'true')
  
  const response = await http.request('get', `/api/enhanced-query/tables/${connectionId}/${tableName}/columns?${params.toString()}`)
  return response
}

// Cache Management Methods
const clearConnectionCache = async () => {
  try {
    await http.request('delete', `/api/enhanced-query/cache/${selectedConnectionId.value}`)
    ElMessage.success('缓存清空成功')
    
    // Refresh table list after cache clear
    await loadTablesForCompletion()
  } catch (error: any) {
    ElMessage.error('清空缓存失败：' + (error.message || '未知错误'))
  }
}

const warmupCache = async () => {
  try {
    const params = selectedSchema.value ? `?schema=${selectedSchema.value}` : ''
    await http.request('post', `/api/enhanced-query/cache/warmup/${selectedConnectionId.value}${params}`)
    ElMessage.success('缓存预热完成')
  } catch (error: any) {
    ElMessage.error('缓存预热失败：' + (error.message || '未知错误'))
  }
}

const onCacheSettingChange = () => {
  // Save cache preference to localStorage
  localStorage.setItem('dbsync-cache-enabled', useCaching.value.toString())
}

// 方法
const loadConnections = async () => {
  try {
    connections.value = await getConnectionsApi()
  } catch (error: any) {
    ElMessage.error('加载数据库连接失败：' + (error.message || '未知错误'))
  }
}

const handleConnectionChange = async () => {
  if (!selectedConnectionId.value) return

  try {
    schemas.value = await getSchemasApi(selectedConnectionId.value)
    selectedSchema.value = ''
    queryResult.value = null
    lastQueryFromCache.value = false

    // 清空补全数据
    availableTables.value = []
    tableColumnsMap.value.clear()
  } catch (error: any) {
    ElMessage.error('加载 Schema 失败：' + (error.message || '未知错误'))
  }
}

const handleSchemaChange = async () => {
  queryResult.value = null
  lastQueryFromCache.value = false
  // 加载表名和字段数据用于智能补全
  await loadTablesForCompletion()
}

const handleTableClick = (tableName: string) => {
  // 可以在这里添加表点击的逻辑，比如显示表结构
  console.log('Table clicked:', tableName)
}

const handleSqlGenerated = (sql: string) => {
  sqlText.value = sql
}

// 加载表名和字段数据用于智能补全 (Enhanced with caching)
const loadTablesForCompletion = async () => {
  if (!selectedConnectionId.value) return

  try {
    // Use enhanced API with caching
    const tables = await loadTablesWithCache(selectedConnectionId.value, selectedSchema.value)
    availableTables.value = Array.isArray(tables) ? tables.map((table: any) => table.name || table.tableName || table) : []

    // 清空之前的字段映射
    tableColumnsMap.value.clear()

    // 获取每个表的字段信息（限制前 10 个表以避免性能问题）
    const tablesToLoad = availableTables.value.slice(0, 10)
    for (const tableName of tablesToLoad) {
      try {
        const columns = await loadTableColumnsWithCache(selectedConnectionId.value, tableName, selectedSchema.value)
        const columnNames = Array.isArray(columns) ? columns.map((col: any) => col.name || col.columnName || col) : []
        tableColumnsMap.value.set(tableName.toLowerCase(), columnNames)
      } catch (error) {
        console.warn(`Failed to load columns for table ${tableName}:`, error)
      }
    }
  } catch (error: any) {
    console.error('Failed to load tables for completion:', error)
    // Fallback to original API if enhanced API fails
    try {
      const tables = await getTablesApi(selectedConnectionId.value, selectedSchema.value)
      availableTables.value = tables.map((table: any) => table.name || table.tableName || table)
    } catch (fallbackError) {
      console.error('Fallback table loading also failed:', fallbackError)
    }
  }
}

// SQL 编辑器变化处理
const handleSqlChange = (sql: string) => {
  // 可以在这里添加 SQL 变化的处理逻辑
  console.log('SQL changed:', sql)
}

const executeQuery = async () => {
  if (!selectedConnectionId.value) {
    ElMessage.warning('请先选择数据库连接')
    return
  }

  if (!sqlText.value.trim()) {
    ElMessage.warning('请输入 SQL 查询语句')
    return
  }

  executing.value = true
  lastQueryFromCache.value = false

  try {
    // Use enhanced query API with caching
    const result = await executeEnhancedQuery(
      selectedConnectionId.value,
      sqlText.value,
      selectedSchema.value
    )

    // Check if result came from cache (this would need to be added to backend response)
    lastQueryFromCache.value = (result as any)?.fromCache || false

    // 转换后端返回的数据结构为前端表格需要的格式
    if (result && (result as any).columns && (result as any).rows) {
      const resultData = result as any
      
      // 获取列显示映射和增强信息
      const columnDisplayMapping = resultData.columnDisplayMapping || {}
      const displayColumns = resultData.displayColumns || resultData.columns
      const chineseColumnCount = resultData.chineseColumnCount || 0
      const enableChineseColumnNames = resultData.enableChineseColumnNames !== false
      
      const transformedResult = {
        ...resultData,
        columnDisplayMapping,
        displayColumns,
        chineseColumnCount,
        enableChineseColumnNames,
        data: resultData.rows.map((row: any[]) => {
          const rowObj: any = {}
          resultData.columns.forEach((column: string, index: number) => {
            rowObj[column] = row[index]
          })
          return rowObj
        })
      }
      
      queryResult.value = transformedResult
      
      // 显示中文字段统计信息
      if (enableChineseColumnNames && chineseColumnCount > 0) {
        const totalColumns = resultData.columns?.length || 0
        const coverage = totalColumns > 0 ? (chineseColumnCount / totalColumns * 100).toFixed(1) : 0
        ElMessage.success(`查询成功，${chineseColumnCount}/${totalColumns} 个字段显示中文名称 (${coverage}%)`)
      } else {
        ElMessage.success('查询执行成功')
      }
    } else {
      queryResult.value = result
      ElMessage.success('查询执行成功')
    }

    // 刷新历史记录
    loadQueryHistory()
  } catch (error: any) {
    ElMessage.error('查询执行失败：' + (error.message || '未知错误'))
    queryResult.value = null
    
    // Fallback to original API if enhanced API fails
    try {
      const fallbackResult = await executeQueryApi(selectedConnectionId.value, {
        sql: sqlText.value,
        schema: selectedSchema.value
      })
      
      if (fallbackResult && fallbackResult.columns && fallbackResult.rows) {
        const transformedResult = {
          ...fallbackResult,
          data: fallbackResult.rows.map((row: any[]) => {
            const rowObj: any = {}
            fallbackResult.columns.forEach((column: string, index: number) => {
              rowObj[column] = row[index]
            })
            return rowObj
          })
        }
        queryResult.value = transformedResult
        ElMessage.success('查询执行成功（使用备用接口）')
        loadQueryHistory()
      }
    } catch (fallbackError) {
      console.error('Fallback query execution also failed:', fallbackError)
    }
  } finally {
    executing.value = false
  }
}

const clearQuery = () => {
  sqlText.value = ''
  queryResult.value = null
  lastQueryFromCache.value = false
}

const formatSql = () => {
  // 简单的 SQL 格式化
  if (sqlText.value) {
    sqlText.value = sqlText.value
      .replace(/\s+/g, ' ')
      .replace(/,/g, ',\n  ')
      .replace(/\bFROM\b/gi, '\nFROM')
      .replace(/\bWHERE\b/gi, '\nWHERE')
      .replace(/\bORDER BY\b/gi, '\nORDER BY')
      .replace(/\bGROUP BY\b/gi, '\nGROUP BY')
      .replace(/\bHAVING\b/gi, '\nHAVING')
      .trim()
  }
}

const validateSql = () => {
  if (!sqlText.value.trim()) {
    ElMessage.warning('请输入 SQL 语句')
    return
  }

  // 简单的 SQL 验证
  const sql = sqlText.value.trim().toLowerCase()
  if (sql.includes('drop') || sql.includes('delete') || sql.includes('truncate')) {
    ElMessage.warning('检测到危险操作，请谨慎执行')
  } else {
    ElMessage.success('SQL 语法检查通过')
  }
}

const loadQueryHistory = async () => {
  if (!selectedConnectionId.value) return

  try {
    queryHistory.value = await getQueryHistoryApi(selectedConnectionId.value)
  } catch (error: any) {
    console.error('加载查询历史失败：', error)
  }
}

const loadHistoryQuery = (item: any) => {
  sqlText.value = item.sql
  showHistory.value = false
}

const formatTime = (timeStr: string) => {
  return new Date(timeStr).toLocaleString()
}

// 获取列显示名称的方法
const getColumnDisplayName = (column: string) => {
  if (!queryResult.value?.columnDisplayMapping) {
    return column
  }
  return queryResult.value.columnDisplayMapping[column] || column
}

// 判断列是否有中文名称
const hasChineseName = (column: string) => {
  if (!queryResult.value?.enableChineseColumnNames) {
    return false
  }
  const displayName = getColumnDisplayName(column)
  return displayName !== column
}

// 根据列名长度计算列宽
const getColumnWidth = (column: string) => {
  const displayName = getColumnDisplayName(column)
  const baseWidth = 180  // 增加基础宽度
  const charWidth = 14   // 增加字符宽度
  const padding = 50     // 增加 padding
  const iconWidth = hasChineseName(column) ? 25 : 0 // 图标宽度
  
  // 计算文字宽度（中文字符宽度约为英文的 1.6 倍）
  const textWidth = displayName.split('').reduce((width, char) => {
    return width + (char.match(/[\u4e00-\u9fff]/) ? charWidth * 1.6 : charWidth)
  }, 0)
  
  const calculatedWidth = Math.max(baseWidth, textWidth + padding + iconWidth)
  
  // 设置更大的最大宽度，确保内容能完整显示
  return Math.min(calculatedWidth, 350)
}

// 计算预期表格最小宽度
const getExpectedTableWidth = () => {
  if (!queryResult.value?.columns) {
    return 1300
  }
  
  const columns = queryResult.value.columns
  // 每列最小 200px 宽度
  const minWidth = columns.length * 200
  
  return minWidth
}

// 表格容器引用
const tableContainer = ref<HTMLDivElement>()

// 检查滚动条状态（调试用）
const checkScrollBars = () => {
  if (tableContainer.value && queryResult.value) {
    const element = tableContainer.value
    const table = element.querySelector('.el-table')
    const hasHorizontalScroll = element.scrollWidth > element.clientWidth
    const hasVerticalScroll = element.scrollHeight > element.clientHeight
    const columnCount = queryResult.value.columns?.length || 0
    const expectedWidth = getExpectedTableWidth()
    
    console.log('滚动条状态检查：', {
      容器宽度: element.clientWidth,
      容器滚动宽度: element.scrollWidth,
      表格实际宽度: table ? table.offsetWidth : 'N/A',
      预期最小宽度: expectedWidth,
      列数: columnCount,
      每列最小宽度: '200px',
      hasHorizontalScroll,
      hasVerticalScroll
    })
  }
}

// 监听查询结果变化，检查滚动条
watch(queryResult, () => {
  if (queryResult.value) {
    // 延迟执行，确保 DOM 更新完成
    nextTick(() => {
      setTimeout(checkScrollBars, 100)
    })
  }
})

// 生命周期
onMounted(() => {
  loadConnections()
  
  // Load cache preference from localStorage
  const savedCachePreference = localStorage.getItem('dbsync-cache-enabled')
  if (savedCachePreference !== null) {
    useCaching.value = savedCachePreference === 'true'
  }
})
</script>

<style scoped>
.query-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  padding: 16px;
  gap: 16px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cache-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 16px;
  padding-left: 16px;
  border-left: 1px solid #e4e7ed;
}

.cache-indicator {
  font-size: 12px;
  color: #409eff;
  font-weight: 500;
}

.main-content {
  display: flex;
  flex: 1;
  gap: 16px;
  min-height: 0;
}

.left-panel {
  width: 300px;
  display: flex;
  flex-direction: column;
}

.table-list-container {
  flex: 1;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.sql-editor-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.editor-title {
  font-weight: 500;
  color: #303133;
}

.editor-actions {
  display: flex;
  gap: 8px;
}

.editor-content {
  flex: 1;
  padding: 16px;
}

.result-container {
  height: 350px;
  display: flex;
  flex-direction: column;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.result-title {
  font-weight: 500;
  color: #303133;
}

.result-info {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
  align-items: center;
}

.result-content {
  flex: 1;
  overflow: hidden;
}

.loading-state {
  padding: 20px;
}

.table-container {
  width: 100%;
  height: 400px;
  overflow: auto;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  position: relative;
  background: white;
}

/* 滚动条样式 */
.table-container::-webkit-scrollbar {
  width: 14px;
  height: 14px;
}

.table-container::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 7px;
}

.table-container::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 7px;
  border: 2px solid #f1f1f1;
}

.table-container::-webkit-scrollbar-thumb:hover {
  background: #555;
}

.table-container::-webkit-scrollbar-corner {
  background: #f1f1f1;
}

/* 表格样式 */
.result-table {
  width: 100%;
}

.empty-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
}

.result-summary {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
  margin-top: 10px;
}

.operation-result {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  padding: 20px;
}

.history-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.history-search {
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.history-item {
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.history-item:hover {
  border-color: #409eff;
  background: #f0f9ff;
}

.history-sql {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 12px;
  color: #303133;
  margin-bottom: 8px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 60px;
  overflow: hidden;
}

.history-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  color: #909399;
}

.history-status {
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 500;
}

.history-status.success {
  background: #f0f9ff;
  color: #67c23a;
}

.history-status.error {
  background: #fef0f0;
  color: #f56c6c;
}

/* 中文列名相关样式 */
.column-header {
  display: flex;
  align-items: center;
  gap: 4px;
  justify-content: center;
  flex-wrap: nowrap;
}

.column-display-name {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.column-info-icon {
  font-size: 14px;
  color: var(--el-color-primary);
  cursor: help;
  opacity: 0.7;
}

.column-info-icon:hover {
  opacity: 1;
}

/* 表格列标题样式优化 */
:deep(.result-table .el-table__header-wrapper) {
  .el-table__header {
    th {
      background-color: var(--el-fill-color-light);
      
      .cell {
        font-weight: 600;
        color: var(--el-text-color-primary);
        padding: 8px 12px;
      }
    }
  }
}

/* 有中文名的列标题样式 */
:deep(.result-table .el-table__header-wrapper) {
  .el-table__header {
    th:has(.column-info-icon) {
      background-color: var(--el-color-primary-light-9);
      
      .cell {
        color: var(--el-color-primary);
      }
    }
  }
}
</style>