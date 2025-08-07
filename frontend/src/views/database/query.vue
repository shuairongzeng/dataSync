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
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧面板 -->
      <div class="left-panel">
        <!-- 虚拟滚动表格 -->
        <div class="table-list-container">
          <VirtualTableList
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
              :table-columns="tableColumnsMap"
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
            </div>
          </div>

          <div class="result-content">
            <!-- 加载状态 -->
            <div v-if="executing" class="loading-state">
              <el-skeleton :rows="5" animated />
            </div>

            <!-- 查询结果表格 -->
            <el-table
              v-else-if="queryResult && queryResult.data && queryResult.data.length > 0"
              :data="queryResult.data"
              border
              stripe
              height="300"
              class="result-table"
            >
              <el-table-column
                v-for="column in queryResult.columns"
                :key="column"
                :prop="column"
                :label="column"
                :min-width="120"
                show-overflow-tooltip
              />
            </el-table>

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

            <!-- 非查询操作结果（如INSERT/UPDATE/DELETE） -->
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
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { CaretRight, Delete, Clock, Search } from '@element-plus/icons-vue'
import VirtualTableList from '@/components/VirtualTableList.vue'
import SqlEditor from '@/components/SqlEditor/SqlEditor.vue'
import {
  getConnectionsApi,
  getSchemasApi,
  executeQueryApi,
  getQueryHistoryApi,
  getTablesApi,
  getTableColumnsApi
} from '@/api/database'

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

    // 清空补全数据
    availableTables.value = []
    tableColumnsMap.value.clear()
  } catch (error: any) {
    ElMessage.error('加载 Schema 失败：' + (error.message || '未知错误'))
  }
}

const handleSchemaChange = async () => {
  queryResult.value = null
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

// 加载表名和字段数据用于智能补全
const loadTablesForCompletion = async () => {
  if (!selectedConnectionId.value) return

  try {
    // 获取表列表
    const tables = await getTablesApi(selectedConnectionId.value, selectedSchema.value)
    availableTables.value = tables.map((table: any) => table.name || table.tableName || table)

    // 清空之前的字段映射
    tableColumnsMap.value.clear()

    // 获取每个表的字段信息（限制前10个表以避免性能问题）
    const tablesToLoad = availableTables.value.slice(0, 10)
    for (const tableName of tablesToLoad) {
      try {
        const columns = await getTableColumnsApi(selectedConnectionId.value, tableName, selectedSchema.value)
        const columnNames = columns.map((col: any) => col.name || col.columnName || col)
        tableColumnsMap.value.set(tableName.toLowerCase(), columnNames)
      } catch (error) {
        console.warn(`Failed to load columns for table ${tableName}:`, error)
      }
    }
  } catch (error: any) {
    console.error('Failed to load tables for completion:', error)
  }
}

// SQL编辑器变化处理
const handleSqlChange = (sql: string) => {
  // 可以在这里添加SQL变化的处理逻辑
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

  try {
    const result = await executeQueryApi(selectedConnectionId.value, {
      sql: sqlText.value,
      schema: selectedSchema.value
    })

    // 转换后端返回的数据结构为前端表格需要的格式
    if (result && result.columns && result.rows) {
      const transformedResult = {
        ...result,
        data: result.rows.map((row: any[]) => {
          const rowObj: any = {}
          result.columns.forEach((column: string, index: number) => {
            rowObj[column] = row[index]
          })
          return rowObj
        })
      }
      queryResult.value = transformedResult
    } else {
      queryResult.value = result
    }

    ElMessage.success('查询执行成功')

    // 刷新历史记录
    loadQueryHistory()
  } catch (error: any) {
    ElMessage.error('查询执行失败：' + (error.message || '未知错误'))
    queryResult.value = null
  } finally {
    executing.value = false
  }
}

const clearQuery = () => {
  sqlText.value = ''
  queryResult.value = null
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

// 生命周期
onMounted(() => {
  loadConnections()
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

.sql-textarea {
  height: 100%;
}

.sql-textarea :deep(.el-textarea__inner) {
  height: 100% !important;
  resize: none;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.5;
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
}

.result-content {
  flex: 1;
  overflow: hidden;
}

.loading-state {
  padding: 20px;
}

.result-table {
  height: 100%;
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
</style>


