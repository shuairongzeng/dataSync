<template>
  <div class="cached-table-list">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="searchText"
          placeholder="搜索表名..."
          :prefix-icon="Search"
          clearable
          @input="handleSearch"
          @clear="handleSearch"
          class="search-input"
        />
      </div>
      
      <div class="toolbar-right">
        <el-tooltip content="刷新表数据" placement="top">
          <el-button
            :icon="Refresh"
            :loading="refreshing"
            @click="refreshTables"
            size="small"
            type="primary"
            plain
          >
            刷新
          </el-button>
        </el-tooltip>
        
        <el-tooltip content="缓存状态" placement="top">
          <el-tag
            :type="cacheStatus.type"
            size="small"
            class="cache-status"
          >
            {{ cacheStatus.text }}
          </el-tag>
        </el-tooltip>
      </div>
    </div>

    <!-- 缓存信息 -->
    <div class="cache-info" v-if="cacheMetadata">
      <el-text size="small" type="info">
        <el-icon><Clock /></el-icon>
        最后更新：{{ formatTime(cacheMetadata.lastUpdated) }}
        (共 {{ cacheMetadata.totalCount }} 张表)
      </el-text>
    </div>

    <!-- 表格容器 -->
    <div class="table-container" ref="containerRef">
      <el-auto-resizer>
        <template #default="{ height, width }">
          <el-table-v2
            :key="tableKey"
            :columns="columns"
            :data="displayData"
            :width="width"
            :height="height"
            :row-height="68"
            :header-height="48"
            :estimated-row-height="68"
            :row-class="getRowClass"
            class="virtual-table"
          >
            <template #empty>
              <el-empty 
                :description="getEmptyDescription()"
                :image-size="100"
              >
                <template #default>
                  <div class="empty-actions" v-if="!loading && !hasCache">
                    <el-button 
                      type="primary" 
                      @click="refreshTables"
                      :loading="refreshing"
                    >
                      加载表数据
                    </el-button>
                  </div>
                </template>
              </el-empty>
            </template>
          </el-table-v2>
        </template>
      </el-auto-resizer>
    </div>

    <!-- 详细加载状态 -->
    <div class="loading-overlay" v-if="loading">
      <div class="loading-content">
        <el-skeleton v-if="!showDetailedProgress" :rows="5" animated />
        
        <!-- 详细进度显示 -->
        <div v-else class="detailed-loading">
          <div class="loading-header">
            <el-icon class="loading-icon" size="24">
              <Loading />
            </el-icon>
            <h3 class="loading-title">{{ loadingStage }}</h3>
          </div>
          
          <div class="loading-body">
            <el-progress 
              v-if="estimatedTotal > 0"
              :percentage="loadingProgress" 
              :stroke-width="8"
              status="success"
              striped
              striped-flow
            >
              <template #default="{ percentage }">
                <span class="progress-text">{{ percentage }}%</span>
              </template>
            </el-progress>
            
            <div class="loading-details" v-if="loadingDetails">
              {{ loadingDetails }}
            </div>
            
            <div class="loading-stats" v-if="loadedCount > 0 || estimatedTotal > 0">
              <span>已加载：{{ loadedCount }}</span>
              <span v-if="estimatedTotal > 0">/ {{ estimatedTotal }} 张表</span>
            </div>
          </div>
          
          <div class="loading-tips">
            <el-text size="small" type="info">
              💡 首次加载较慢，后续访问将使用缓存快速响应
            </el-text>
          </div>
        </div>
      </div>
    </div>

    <!-- 右键菜单 -->
    <div v-if="showContextMenu" class="context-menu-overlay" @click="hideContextMenu">
      <div class="context-menu" :style="contextMenuStyle" @click.stop>
        <div class="context-menu-item" @click.stop="handleContextMenuCommand('select')">
          <el-icon><Search /></el-icon>
          SELECT 查询
        </div>
        <div class="context-menu-item" @click.stop="handleContextMenuCommand('count')">
          <el-icon><Document /></el-icon>
          COUNT 查询
        </div>
        <div class="context-menu-item" @click.stop="handleContextMenuCommand('describe')">
          <el-icon><View /></el-icon>
          表结构
        </div>
        <div class="context-menu-divider"></div>
        <div class="context-menu-item" @click.stop="handleContextMenuCommand('insert')">
          <el-icon><Document /></el-icon>
          INSERT 模板
        </div>
        <div class="context-menu-item" @click.stop="handleContextMenuCommand('update')">
          <el-icon><Document /></el-icon>
          UPDATE 模板
        </div>
        <div class="context-menu-item" @click.stop="handleContextMenuCommand('delete')">
          <el-icon><Document /></el-icon>
          DELETE 模板
        </div>
        <div class="context-menu-divider"></div>
        <div class="context-menu-item" @click.stop="handleContextMenuCommand('viewFields')">
          <el-icon><View /></el-icon>
          查看字段
        </div>
      </div>
    </div>

    <!-- SQL 菜单 -->
    <div v-if="showSqlMenu" class="sql-menu-overlay" @click="hideSqlMenu">
      <div class="sql-menu" :style="sqlMenuStyle" @click.stop>
        <div class="sql-menu-item" @click="handleSqlMenuCommand('select')">
          <el-icon><Search /></el-icon>
          SELECT 查询
        </div>
        <div class="sql-menu-item" @click="handleSqlMenuCommand('count')">
          <el-icon><Document /></el-icon>
          COUNT 查询
        </div>
        <div class="sql-menu-item" @click="handleSqlMenuCommand('describe')">
          <el-icon><View /></el-icon>
          表结构
        </div>
        <div class="sql-menu-divider"></div>
        <div class="sql-menu-item" @click="handleSqlMenuCommand('insert')">
          <el-icon><Document /></el-icon>
          INSERT 模板
        </div>
        <div class="sql-menu-item" @click="handleSqlMenuCommand('update')">
          <el-icon><Document /></el-icon>
          UPDATE 模板
        </div>
        <div class="sql-menu-item" @click="handleSqlMenuCommand('delete')">
          <el-icon><Document /></el-icon>
          DELETE 模板
        </div>
      </div>
    </div>

    <!-- 字段列表弹框 -->
    <el-dialog
      v-model="showFieldDialog"
      :title="dialogTitle"
      width="900px"
      :close-on-click-modal="false"
      class="field-dialog"
    >
      <div class="field-dialog-header">
        <el-input
          v-model="fieldSearchText"
          placeholder="搜索字段名..."
          :prefix-icon="Search"
          clearable
          class="field-search"
        />
        <div class="field-stats">
          <el-tag type="info" size="small">
            共 {{ filteredFields.length }} 个字段
          </el-tag>
        </div>
      </div>
      
      <div class="field-dialog-content">
        <el-table
          :data="filteredFields"
          v-loading="fieldDialogLoading"
          stripe
          border
          height="450"
          class="field-table"
        >
          <el-table-column prop="columnName" label="字段名" width="160">
            <template #default="{ row }">
              <div class="field-name-cell">
                <el-icon v-if="row.isPrimaryKey" class="pk-icon"><Key /></el-icon>
                <span class="field-name" :class="{ 'is-primary': row.isPrimaryKey }">
                  {{ row.columnName }}
                </span>
                <el-button
                  size="small"
                  text
                  :icon="CopyDocument"
                  @click="copyFieldName(row.columnName)"
                  class="copy-btn"
                />
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="dataType" label="类型" width="120">
            <template #default="{ row }">
              <el-tag size="small" class="type-tag">{{ row.dataType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="columnSize" label="长度" width="80" align="center" />
          <el-table-column prop="nullable" label="可空" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.nullable ? 'success' : 'danger'" size="small">
                {{ row.nullable ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="isPrimaryKey" label="主键" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.isPrimaryKey" type="warning" size="small">
                <el-icon><Key /></el-icon>
                主键
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="isAutoIncrement" label="自增" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.isAutoIncrement" type="info" size="small">自增</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remarks" label="备注" min-width="150">
            <template #default="{ row }">
              <span class="field-remarks">{{ row.remarks || '-' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="copyAllFields" :icon="CopyDocument">
            复制所有字段名
          </el-button>
          <el-button @click="showFieldDialog = false" type="primary">
            关闭
          </el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch, h } from 'vue'
import { ElMessage, ElIcon, ElTag, ElText, ElTooltip, ElButton } from 'element-plus'
import { Search, Refresh, Clock, Document, View, Close, Key, CopyDocument, Loading } from '@element-plus/icons-vue'
import { tableCacheManager, type TableInfo, type CacheMetadata } from '@/utils/TableCacheManager'
import { getTablesWithPaginationApi, getTableColumnsApi, getBasicTablesApi, getBasicTablesWithPaginationApi, getTableDetailsApi, getBatchTableStatsApi, getConnectionByIdApi } from '@/api/database'
import { generateSelectQuery, generateInsertQuery, generateUpdateQuery, generateDeleteQuery, generateDescribeQuery, getPaginationSyntaxInfo } from '@/utils/SqlGenerator'

// Props
interface Props {
  connectionId: string
  schema?: string
}

const props = defineProps<Props>()

// Emits
interface Emits {
  (e: 'table-click', tableName: string): void
  (e: 'sql-generated', sql: string): void
}

const emit = defineEmits<Emits>()

// 响应式数据
const searchText = ref('')
const loading = ref(false)
const refreshing = ref(false)
const tableData = ref<TableInfo[]>([])
const filteredData = ref<TableInfo[]>([])
const cacheMetadata = ref<CacheMetadata | null>(null)
const tableKey = ref(0) // 用于强制重新渲染表格

// 新增：详细的加载状态管理
const loadingProgress = ref(0)
const loadingStage = ref('')
const loadingDetails = ref('')
const showDetailedProgress = ref(false)
const estimatedTotal = ref(0)
const loadedCount = ref(0)

// 数据库连接信息
const connectionInfo = ref<any>(null)
const dbType = ref('')

// SQL 菜单相关状态
const showSqlMenu = ref(false)
const sqlMenuStyle = ref({})
const sqlMenuTable = ref<TableInfo | null>(null)

// 右键菜单相关状态
const showContextMenu = ref(false)
const contextMenuStyle = ref({})
const contextMenuTable = ref<TableInfo | null>(null)

// 字段弹框相关状态
const showFieldDialog = ref(false)
const fieldDialogLoading = ref(false)
const tableFields = ref<any[]>([])
const dialogTitle = ref('')
const fieldSearchText = ref('')

// 搜索防抖
const searchTimeout = ref<any>(null)

// 容器引用
const containerRef = ref()

// 计算属性：过滤后的字段列表
const filteredFields = computed(() => {
  if (!fieldSearchText.value.trim()) {
    return tableFields.value
  }
  
  const searchLower = fieldSearchText.value.toLowerCase()
  return tableFields.value.filter(field => 
    field.columnName.toLowerCase().includes(searchLower) ||
    (field.remarks && field.remarks.toLowerCase().includes(searchLower))
  )
})

// 计算属性
const displayData = computed(() => filteredData.value)

const hasCache = computed(() => {
  return tableCacheManager.hasCache(props.connectionId, props.schema)
})

const cacheStatus = computed(() => {
  if (!hasCache.value) {
    return { type: 'info' as const, text: '无缓存' }
  }
  
  if (cacheMetadata.value) {
    const age = Date.now() - cacheMetadata.value.lastUpdated
    const hours = Math.floor(age / (1000 * 60 * 60))
    
    if (hours < 1) {
      return { type: 'success' as const, text: '最新' }
    } else if (hours < 12) {
      return { type: 'warning' as const, text: `${hours}小时前` }
    } else {
      return { type: 'danger' as const, text: '较旧' }
    }
  }
  
  return { type: 'info' as const, text: '已缓存' }
})

// 表格列定义 - 简化为普通展示模式
const columns = computed(() => [
  {
    key: 'tableName',
    title: '表名',
    dataKey: 'tableName',
    width: 350,
    cellRenderer: ({ rowData }: any) => {
      // 统一的表名显示，带详细 tooltip
      const tooltipContent = h('div', { class: 'tooltip-content' }, [
        h('div', { class: 'tooltip-item' }, [
          h('span', { class: 'tooltip-label' }, '类型：'),
          h('span', { class: 'tooltip-value' }, rowData.tableType === 'VIEW' ? '视图' : '数据表')
        ]),
        h('div', { class: 'tooltip-item' }, [
          h('span', { class: 'tooltip-label' }, '字段数：'),
          h('span', { class: 'tooltip-value' }, `${rowData.columnCount || 0} 个`)
        ]),
        h('div', { class: 'tooltip-item' }, [
          h('span', { class: 'tooltip-label' }, '主键：'),
          h('span', { class: 'tooltip-value' }, rowData.hasPrimaryKey ? '有' : '无')
        ]),
        rowData.remarks ? h('div', { class: 'tooltip-item' }, [
          h('span', { class: 'tooltip-label' }, '备注：'),
          h('span', { class: 'tooltip-value' }, rowData.remarks)
        ]) : null
      ].filter(Boolean))
      
      return h(ElTooltip, {
        placement: 'right',
        showAfter: 300,
        hideAfter: 100,
        popperClass: 'table-info-tooltip'
      }, {
        content: () => tooltipContent,
        default: () => h('div', { 
          class: 'table-row-cell full-width-cell',
          'data-table-name': rowData.tableName,
          style: { width: '100%', minHeight: '68px' },
          onContextmenu: (e: MouseEvent) => {
            e.preventDefault()
            e.stopPropagation()
            handleRowContextMenu(e, rowData)
          }
        }, [
          h('div', { class: 'table-name-content' }, [
            h('span', { class: 'table-name' }, rowData.tableName),
            h('div', { class: 'table-meta-tags' }, [
              h(ElTag, { 
                size: 'small', 
                type: rowData.tableType === 'VIEW' ? 'info' : 'success',
                class: 'table-type-tag'
              }, () => rowData.tableType === 'VIEW' ? '视图' : '表'),
              h('span', { class: 'column-count' }, `${rowData.columnCount || 0}列`),
              rowData.hasPrimaryKey ? h(ElTag, { 
                size: 'small', 
                type: 'warning',
                class: 'pk-tag'
              }, () => 'PK') : null
            ].filter(Boolean))
          ])
        ])
      })
    }
  },
  {
    key: 'remarks',
    title: '备注',
    dataKey: 'remarks',
    width: 200,
    cellRenderer: ({ rowData }: any) => {
      const remarks = rowData.remarks || '-'
      return h('div', { 
        class: 'table-row-cell table-remarks-cell full-width-cell',
        style: { width: '100%', minHeight: '68px' },
        onContextmenu: (e: MouseEvent) => {
          e.preventDefault()
          e.stopPropagation()
          handleRowContextMenu(e, rowData)
        }
      }, [
        h('span', {
          class: 'table-remarks',
          title: remarks.length > 30 ? remarks : undefined
        }, remarks.length > 30 ? `${remarks.substring(0, 30)}...` : remarks)
      ])
    }
  }
])

// 方法 - 移除了左键点击选中逻辑

// 查看字段功能
const handleViewFields = async (table: TableInfo) => {
  dialogTitle.value = `${table.tableName}${table.remarks ? ` - ${table.remarks}` : ''}`
  showFieldDialog.value = true
  fieldDialogLoading.value = true
  
  try {
    const fields = await getTableColumnsApi(props.connectionId, table.tableName, props.schema)
    tableFields.value = fields
  } catch (error: any) {
    ElMessage.error('获取字段信息失败：' + (error.message || '未知错误'))
    tableFields.value = []
  } finally {
    fieldDialogLoading.value = false
  }
}


// 处理 SQL 命令
const handleSqlCommand = async (command: string, table: TableInfo) => {
  if (!table || !table.tableName) {
    console.error('Invalid table object:', table)
    ElMessage.error('表信息无效，请重新选择')
    return
  }
  
  let sql = ''
  try {
    switch (command) {
      case 'select':
        sql = await generateSelectSql(table.tableName)
        break
      case 'count':
        sql = `SELECT COUNT(*) FROM ${table.tableName};`
        break
      case 'describe':
        sql = await generateDescribeSql(table.tableName)
        break
      case 'insert':
        sql = await generateInsertSql(table.tableName)
        break
      case 'update':
        sql = await generateUpdateSql(table.tableName)
        break
      case 'delete':
        sql = await generateDeleteSql(table.tableName)
        break
      default:
        throw new Error(`未知的 SQL 命令：${command}`)
    }
    
    if (sql) {
      emit('sql-generated', sql)
      ElMessage.success(`已生成 ${getCommandDisplayName(command)} 语句`)
    } else {
      throw new Error('生成的 SQL 为空')
    }
  } catch (error: any) {
    console.error('SQL generation error:', error)
    ElMessage.error('生成 SQL 失败：' + (error.message || '未知错误'))
  }
}

// 获取命令显示名称
const getCommandDisplayName = (command: string): string => {
  const names: Record<string, string> = {
    'select': 'SELECT 查询',
    'count': 'COUNT 查询', 
    'describe': '表结构',
    'insert': 'INSERT 模板',
    'update': 'UPDATE 模板',
    'delete': 'DELETE 模板'
  }
  return names[command] || command.toUpperCase()
}

// 获取主键列名
const getPrimaryKeyColumn = async (tableName: string): Promise<string> => {
  try {
    const columns = await getTableColumnsApi(props.connectionId, tableName, props.schema)
    const pkColumn = columns.find((col: any) => col.isPrimaryKey)
    return pkColumn?.columnName || 'id'
  } catch (error) {
    return 'id'
  }
}

// 显示 SQL 菜单
const showSqlMenuHandler = (event: Event, table: TableInfo) => {
  if (!table || !table.tableName) {
    console.error('Invalid table data for SQL menu:', table)
    ElMessage.error('表数据无效')
    return
  }
  
  const target = event.target as HTMLElement
  const rect = target.getBoundingClientRect()
  
  sqlMenuStyle.value = {
    left: rect.left + 'px',
    top: (rect.bottom + 5) + 'px'
  }
  sqlMenuTable.value = table
  showSqlMenu.value = true
}

// 隐藏 SQL 菜单
const hideSqlMenu = () => {
  showSqlMenu.value = false
  sqlMenuTable.value = null
}

// 处理行右键菜单
const handleRowContextMenu = (event: MouseEvent, table: TableInfo) => {
  if (!table || !table.tableName) {
    console.error('Invalid table data for context menu:', table)
    ElMessage.error('表数据无效')
    return
  }
  
  // 先关闭现有的菜单
  hideContextMenu()
  
  // 使用 nextTick 确保菜单完全关闭后再打开新菜单
  nextTick(() => {
    const { clientX, clientY } = event
    
    // 调整菜单位置，确保不超出屏幕边界
    const menuWidth = 180
    const menuHeight = 240
    const windowWidth = window.innerWidth
    const windowHeight = window.innerHeight
    
    let left = clientX
    let top = clientY
    
    if (left + menuWidth > windowWidth) {
      left = windowWidth - menuWidth - 10
    }
    
    if (top + menuHeight > windowHeight) {
      top = windowHeight - menuHeight - 10
    }
    
    contextMenuStyle.value = {
      left: `${left}px`,
      top: `${top}px`
    }
    
    contextMenuTable.value = table
    showContextMenu.value = true
  })
}

// 隐藏右键菜单
const hideContextMenu = () => {
  showContextMenu.value = false
  contextMenuTable.value = null
}

// 处理右键菜单命令
const handleContextMenuCommand = async (command: string) => {
  if (!contextMenuTable.value) {
    ElMessage.error('未选择表，请重新右键点击')
    return
  }
  
  // 立即保存表格引用，避免异步操作中丢失
  const tableRef = { ...contextMenuTable.value }
  
  // 隐藏菜单
  hideContextMenu()
  
  try {
    if (command === 'viewFields') {
      await handleViewFields(tableRef)
    } else {
      await handleSqlCommand(command, tableRef)
    }
  } catch (error: any) {
    console.error('Command execution failed:', error)
    ElMessage.error('操作失败：' + (error.message || '未知错误'))
  }
}

// 处理 SQL 菜单命令
const handleSqlMenuCommand = async (command: string) => {
  if (!sqlMenuTable.value) {
    ElMessage.error('未选择表，请重新点击 SQL 按钮')
    return
  }
  
  // 先保存表格引用，再隐藏菜单
  const tableRef = { ...sqlMenuTable.value }
  hideSqlMenu()
  await handleSqlCommand(command, tableRef)
}

// 获取行样式类 - 移除选中状态判断
const getRowClass = ({ rowData }: any) => {
  return 'table-row'
}

// 复制字段名
const copyFieldName = async (fieldName: string) => {
  try {
    await navigator.clipboard.writeText(fieldName)
    ElMessage.success(`已复制字段名：${fieldName}`)
  } catch (error) {
    // 降级方案
    const textArea = document.createElement('textarea')
    textArea.value = fieldName
    document.body.appendChild(textArea)
    textArea.select()
    document.execCommand('copy')
    document.body.removeChild(textArea)
    ElMessage.success(`已复制字段名：${fieldName}`)
  }
}

// 复制所有字段名
const copyAllFields = async () => {
  try {
    const fieldNames = filteredFields.value.map(field => field.columnName).join(', ')
    await navigator.clipboard.writeText(fieldNames)
    ElMessage.success(`已复制 ${filteredFields.value.length} 个字段名`)
  } catch (error) {
    // 降级方案
    const fieldNames = filteredFields.value.map(field => field.columnName).join(', ')
    const textArea = document.createElement('textarea')
    textArea.value = fieldNames
    document.body.appendChild(textArea)
    textArea.select()
    document.execCommand('copy')
    document.body.removeChild(textArea)
    ElMessage.success(`已复制 ${filteredFields.value.length} 个字段名`)
  }
}

// 重置字段搜索
const resetFieldSearch = () => {
  fieldSearchText.value = ''
}

// 监听字段弹框关闭，重置搜索
watch(showFieldDialog, (newVal) => {
  if (!newVal) {
    resetFieldSearch()
  }
})

// 重置加载状态
const resetLoadingState = () => {
  loadingProgress.value = 0
  loadingStage.value = ''
  loadingDetails.value = ''
  showDetailedProgress.value = false
  estimatedTotal.value = 0
  loadedCount.value = 0
}

// 更新加载进度
const updateLoadingProgress = (stage: string, details: string, loaded: number = 0, total: number = 0) => {
  loadingStage.value = stage
  loadingDetails.value = details
  loadedCount.value = loaded
  if (total > 0) {
    estimatedTotal.value = total
    loadingProgress.value = Math.round((loaded / total) * 100)
  }
  
  // 当开始显示详细进度时，启用详细进度显示
  if (stage && details) {
    showDetailedProgress.value = true
  }
}

// 优化后的快速加载方法 - 使用分层加载策略
const loadTables = async (useCache = true) => {
  if (!props.connectionId) return

  // 重置加载状态
  resetLoadingState()

  // 如果允许使用缓存且有缓存，直接使用缓存数据
  if (useCache) {
    const cachedTables = tableCacheManager.getCachedTables(props.connectionId, props.schema)
    if (cachedTables) {
      updateLoadingProgress('加载缓存数据', '从本地缓存快速加载表信息...')
      
      // 稍微延迟以显示加载状态
      await new Promise(resolve => setTimeout(resolve, 100))
      
      tableData.value = cachedTables
      cacheMetadata.value = tableCacheManager.getCacheMetadata(props.connectionId, props.schema)
      applySearch()
      return
    }
  }

  loading.value = true

  try {
    updateLoadingProgress('初始化连接', '正在连接数据库并准备获取表列表...')
    
    // 使用快速API分批加载基础表信息（只包含表名和备注）
    const allTables: TableInfo[] = []
    let currentPage = 1
    let hasMore = true
    const pageSize = 200 // 增加页面大小，因为只获取基础信息

    // 先获取第一页以了解总数
    updateLoadingProgress('获取表列表', '正在获取数据库表信息...')
    
    while (hasMore) {
      const params = {
        page: currentPage,
        size: pageSize,
        sortBy: 'name',
        sortOrder: 'asc',
        schema: props.schema
      }

      updateLoadingProgress(
        '获取表列表', 
        `正在获取第 ${currentPage} 页表信息...`,
        allTables.length,
        estimatedTotal.value
      )

      const response = await getBasicTablesWithPaginationApi(props.connectionId, params)
      
      if (response.data && response.data.length > 0) {
        // 如果是第一页，设置预估总数
        if (currentPage === 1 && response.total) {
          estimatedTotal.value = response.total
        }
        
        // 转换基础数据格式，详细信息设为默认值
        const tables: TableInfo[] = response.data.map((table: any) => ({
          tableName: table.tableName || table.name,
          tableType: table.tableType || 'TABLE',
          columnCount: 0, // 默认值，将在需要时懒加载
          remarks: table.remarks || table.comment || '',
          hasPrimaryKey: false, // 默认值，将在需要时懒加载
          schema: props.schema,
          connectionId: props.connectionId,
          lastUpdated: Date.now(),
          // 添加标志位，表示详细信息尚未加载
          _detailsLoaded: false
        }))
        
        allTables.push(...tables)
        
        // 更新进度
        updateLoadingProgress(
          '获取表列表', 
          `已获取 ${allTables.length} 张表的基础信息...`,
          allTables.length,
          estimatedTotal.value || allTables.length
        )
      }

      hasMore = response.hasNext || false
      currentPage++

      // 防止无限循环
      if (currentPage > 200) {
        console.warn('Too many pages, stopping table loading')
        break
      }
    }

    updateLoadingProgress('保存缓存', '正在保存表信息到本地缓存...', allTables.length, allTables.length)
    
    // 缓存基础数据
    tableCacheManager.cacheTables(props.connectionId, props.schema, allTables)
    
    tableData.value = allTables
    cacheMetadata.value = tableCacheManager.getCacheMetadata(props.connectionId, props.schema)
    
    applySearch()
    
    ElMessage.success(`快速加载 ${allTables.length} 张表（基础信息）`)
    
    // 异步批量加载可见表的详细信息
    setTimeout(() => {
      loadVisibleTableDetails()
    }, 500) // 延迟500ms开始加载详细信息
    
  } catch (error: any) {
    console.error('加载表列表失败：', error)
    
    // 回退到旧的API
    try {
      updateLoadingProgress('尝试传统方式', '快速加载失败，正在尝试传统加载方式...')
      ElMessage.warning('正在尝试传统加载方式...')
      await loadTablesLegacy(false)
    } catch (fallbackError: any) {
      ElMessage.error('加载表列表失败：' + (error.message || '未知错误'))
    }
  } finally {
    loading.value = false
    resetLoadingState()
  }
}

// 传统加载方法（作为回退方案）
const loadTablesLegacy = async (useCache = true) => {
  if (!props.connectionId) return

  if (useCache) {
    const cachedTables = tableCacheManager.getCachedTables(props.connectionId, props.schema)
    if (cachedTables) {
      tableData.value = cachedTables
      cacheMetadata.value = tableCacheManager.getCacheMetadata(props.connectionId, props.schema)
      applySearch()
      return
    }
  }

  const allTables: TableInfo[] = []
  let currentPage = 1
  let hasMore = true
  const pageSize = 100

  while (hasMore) {
    const params = {
      page: currentPage,
      size: pageSize,
      sortBy: 'name',
      sortOrder: 'asc',
      schema: props.schema
    }

    const response = await getTablesWithPaginationApi(props.connectionId, params)
    
    if (response.data && response.data.length > 0) {
      const tables: TableInfo[] = response.data.map((table: any) => ({
        tableName: table.tableName || table.name,
        tableType: table.tableType || 'TABLE',
        columnCount: table.columnCount || 0,
        remarks: table.remarks || table.comment,
        hasPrimaryKey: table.hasPrimaryKey || false,
        schema: props.schema,
        connectionId: props.connectionId,
        lastUpdated: Date.now(),
        _detailsLoaded: true
      }))
      
      allTables.push(...tables)
    }

    hasMore = response.hasNext || false
    currentPage++

    if (currentPage > 100) {
      console.warn('Too many pages, stopping table loading')
      break
    }
  }

  tableCacheManager.cacheTables(props.connectionId, props.schema, allTables)
  tableData.value = allTables
  cacheMetadata.value = tableCacheManager.getCacheMetadata(props.connectionId, props.schema)
  applySearch()
  
  ElMessage.success(`成功加载 ${allTables.length} 张表（完整信息）`)
}

// 懒加载可见表的详细信息
const loadVisibleTableDetails = async () => {
  try {
    // 获取前50个表的详细信息（这些通常是用户最关心的）
    const tablesToLoad = filteredData.value.slice(0, 50).filter(table => !table._detailsLoaded)
    
    if (tablesToLoad.length === 0) return
    
    // 批量获取表统计信息
    const tableNames = tablesToLoad.map(table => table.tableName)
    const stats = await getBatchTableStatsApi(props.connectionId, {
      tableNames,
      schema: props.schema
    })
    
    // 更新表信息
    tablesToLoad.forEach(table => {
      const stat = stats[table.tableName]
      if (stat && !stat.error) {
        table.columnCount = stat.columnCount || 0
        table.hasPrimaryKey = stat.hasPrimaryKey || false
        table._detailsLoaded = true
      }
    })
    
    // 更新缓存
    tableCacheManager.cacheTables(props.connectionId, props.schema, tableData.value)
    
    // 触发表格更新
    tableKey.value++
    
  } catch (error: any) {
    console.warn('批量加载表详细信息失败：', error)
    // 静默失败，不影响基础功能
  }
}

// 获取数据库连接信息
const loadConnectionInfo = async () => {
  try {
    if (!connectionInfo.value) {
      const result = await getConnectionByIdApi(props.connectionId)
      connectionInfo.value = result
      dbType.value = result.dbType || 'mysql'
    }
  } catch (error: any) {
    console.warn('获取数据库连接信息失败：', error)
    // 默认使用MySQL语法
    dbType.value = 'mysql'
  }
}

const refreshTables = async () => {
  refreshing.value = true
  try {
    // 清除缓存并重新加载
    tableCacheManager.clearCache(props.connectionId, props.schema)
    await loadTables(false)
  } finally {
    refreshing.value = false
  }
}

const applySearch = () => {
  if (!searchText.value.trim()) {
    filteredData.value = tableData.value
    return
  }

  const searchLower = searchText.value.toLowerCase()
  filteredData.value = tableData.value.filter(table => 
    table.tableName.toLowerCase().includes(searchLower) ||
    (table.remarks && table.remarks.toLowerCase().includes(searchLower))
  )
}

const handleSearch = () => {
  // 防抖处理
  clearTimeout(searchTimeout.value)
  searchTimeout.value = setTimeout(() => {
    applySearch()
  }, 300)
}

const generateSelectSql = async (tableName: string) => {
  try {
    // 确保已加载数据库连接信息
    await loadConnectionInfo()
    
    const columns = await getTableColumnsApi(props.connectionId, tableName, props.schema)
    // 如果字段数量较多，只选择前 10 个字段；否则使用所有字段
    const columnNames = columns.length > 10 
      ? columns.slice(0, 10).map((col: any) => col.columnName)
      : columns.map((col: any) => col.columnName)
      
    return generateSelectQuery(dbType.value, tableName, columnNames, props.schema)
  } catch (error) {
    // 确保有默认的数据库类型
    await loadConnectionInfo()
    return generateSelectQuery(dbType.value, tableName, [], props.schema)
  }
}

const generateInsertSql = async (tableName: string) => {
  try {
    // 确保已加载数据库连接信息
    await loadConnectionInfo()
    
    const columns = await getTableColumnsApi(props.connectionId, tableName, props.schema)
    // 过滤掉自增字段，构建列信息
    const insertColumns = columns
      .filter((col: any) => !col.isAutoIncrement)
      .map((col: any) => ({
        columnName: col.columnName,
        dataType: col.dataType || 'VARCHAR',
        nullable: col.nullable !== false
      }))
      
    return generateInsertQuery(dbType.value, tableName, insertColumns, props.schema)
  } catch (error) {
    await loadConnectionInfo()
    return generateInsertQuery(dbType.value, tableName, [], props.schema)
  }
}

const generateUpdateSql = async (tableName: string) => {
  try {
    // 确保已加载数据库连接信息
    await loadConnectionInfo()
    
    const columns = await getTableColumnsApi(props.connectionId, tableName, props.schema)
    // 构建列信息，包含主键标识
    const updateColumns = columns.map((col: any) => ({
      columnName: col.columnName,
      dataType: col.dataType || 'VARCHAR',
      isPrimaryKey: col.isPrimaryKey === true
    }))
      
    return generateUpdateQuery(dbType.value, tableName, updateColumns, props.schema)
  } catch (error) {
    await loadConnectionInfo()
    return generateUpdateQuery(dbType.value, tableName, [], props.schema)
  }
}

const generateDeleteSql = async (tableName: string) => {
  try {
    // 确保已加载数据库连接信息
    await loadConnectionInfo()
    
    const columns = await getTableColumnsApi(props.connectionId, tableName, props.schema)
    // 获取主键字段
    const primaryKeys = columns
      .filter((col: any) => col.isPrimaryKey === true)
      .map((col: any) => ({
        columnName: col.columnName,
        dataType: col.dataType || 'VARCHAR'
      }))
      
    return generateDeleteQuery(dbType.value, tableName, primaryKeys, props.schema)
  } catch (error) {
    await loadConnectionInfo()
    return generateDeleteQuery(dbType.value, tableName, [], props.schema)
  }
}

const generateDescribeSql = async (tableName: string) => {
  try {
    // 确保已加载数据库连接信息
    await loadConnectionInfo()
    
    return generateDescribeQuery(dbType.value, tableName, props.schema)
  } catch (error) {
    await loadConnectionInfo()
    return generateDescribeQuery(dbType.value, tableName, props.schema)
  }
}

const getEmptyDescription = () => {
  if (loading.value) return '加载中...'
  if (searchText.value) return '未找到匹配的表'
  if (!hasCache.value) return '暂无表数据，点击下方按钮加载'
  return '暂无数据'
}

const formatTime = (timestamp: number) => {
  return new Date(timestamp).toLocaleString()
}

// 监听器
watch(() => props.connectionId, () => {
  if (props.connectionId) {
    loadTables(true)
  }
}, { immediate: true })

watch(() => props.schema, () => {
  if (props.connectionId) {
    loadTables(true)
  }
})

// 全局点击事件处理器
const handleGlobalClick = (e: MouseEvent) => {
  const target = e.target as Element
  
  // 如果点击的不是右键菜单，则隐藏菜单
  if (showContextMenu.value) {
    const contextMenuEl = document.querySelector('.context-menu')
    const overlayEl = document.querySelector('.context-menu-overlay')
    
    if (contextMenuEl && !contextMenuEl.contains(target) && 
        overlayEl && !overlayEl.contains(target)) {
      hideContextMenu()
    }
  }
  
  if (showSqlMenu.value) {
    const sqlMenuEl = document.querySelector('.sql-menu')
    const overlayEl = document.querySelector('.sql-menu-overlay')
    
    // 不隐藏 SQL 菜单，如果点击的是 SQL 按钮或其子元素
    const isSqlButton = target.closest('.action-buttons') !== null
    
    if (!isSqlButton && sqlMenuEl && !sqlMenuEl.contains(target) && 
        overlayEl && !overlayEl.contains(target)) {
      hideSqlMenu()
    }
  }
}

// 生命周期
onMounted(() => {
  if (props.connectionId) {
    loadConnectionInfo() // 先加载连接信息
    loadTables(true)
  }
  
  // 延迟添加全局点击事件监听器，避免与初始化点击冲突
  setTimeout(() => {
    document.addEventListener('click', handleGlobalClick)
    document.addEventListener('contextmenu', (e) => {
      // 如果右键菜单已显示，阻止默认的浏览器右键菜单
      if (showContextMenu.value) {
        e.preventDefault()
      }
    })
  }, 100)
})

// 组件卸载时清理事件监听器
onUnmounted(() => {
  document.removeEventListener('click', handleGlobalClick)
})
</script>

<style scoped>
.cached-table-list {
  height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-light);
  background: var(--el-bg-color-page);
}

.toolbar-left {
  flex: 1;
  max-width: 300px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-input {
  width: 100%;
}

.cache-status {
  cursor: help;
}

.cache-info {
  padding: 8px 16px;
  background: var(--el-fill-color-lighter);
  border-bottom: 1px solid var(--el-border-color-light);
}

.table-container {
  flex: 1;
  min-height: 400px;
  position: relative;
}

.virtual-table {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
}

/* 表格行统一样式 */
.table-row-cell {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 12px 0;
  border-radius: 8px;
  transition: all 0.2s ease;
  cursor: context-menu;
  height: 100%;
  min-height: 68px;
  box-sizing: border-box;
}

/* 确保整行区域都能响应右键事件 */
.full-width-cell {
  width: 100% !important;
  position: relative;
  display: flex;
  align-items: center;
}

.table-row-cell:hover {
  background-color: var(--el-fill-color-light);
  transform: translateX(2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

/* 表格内容样式 */
.table-name-content {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
  height: 100%;
}

.table-name {
  font-weight: 600;
  font-size: 15px;
  color: var(--el-text-color-primary);
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-row-cell:hover .table-name {
  color: var(--el-color-primary);
}

.table-meta-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 2px;
}

.table-type-tag {
  font-weight: 500;
  font-size: 12px;
  height: 20px;
  padding: 0 6px;
}

.column-count {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
  white-space: nowrap;
}

.pk-tag {
  font-weight: 600;
  font-size: 11px;
  height: 18px;
  padding: 0 5px;
}

.table-remarks-cell {
  padding: 12px 0;
  height: 100%;
  min-height: 68px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  box-sizing: border-box;
}

.table-remarks {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  line-height: 1.4;
  display: block;
}

/* Tooltip 样式优化 */
.tooltip-content {
  padding: 8px 0;
}

.tooltip-item {
  display: flex;
  align-items: center;
  margin: 4px 0;
  font-size: 13px;
  line-height: 1.4;
}

.tooltip-label {
  color: var(--el-text-color-secondary);
  min-width: 40px;
  font-weight: 500;
}

.tooltip-value {
  color: var(--el-text-color-primary);
  font-weight: 400;
}

/* 右键菜单样式 */
.context-menu-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  background: transparent;
}

.context-menu {
  position: fixed;
  z-index: 10000;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
  padding: 6px 0;
  min-width: 180px;
  backdrop-filter: blur(10px);
  animation: contextMenuFadeIn 0.15s ease-out;
}

@keyframes contextMenuFadeIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(-5px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.context-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  font-size: 14px;
  color: var(--el-text-color-primary);
  transition: all 0.15s ease;
  user-select: none;
}

.context-menu-item:hover {
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.context-menu-item .el-icon {
  font-size: 16px;
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
}

.context-menu-item:hover .el-icon {
  color: var(--el-color-primary);
}

.context-menu-divider {
  height: 1px;
  background-color: var(--el-border-color-lighter);
  margin: 6px 0;
}

/* SQL 菜单样式 */
.sql-menu-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9998;
  background: transparent;
}

.sql-menu {
  position: fixed;
  z-index: 9999;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 4px 0;
  min-width: 160px;
  backdrop-filter: blur(8px);
}

.sql-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  cursor: pointer;
  font-size: 14px;
  color: var(--el-text-color-primary);
  transition: all 0.2s ease;
}

.sql-menu-item:hover {
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.sql-menu-item .el-icon {
  font-size: 16px;
  color: var(--el-text-color-secondary);
}

.sql-menu-item:hover .el-icon {
  color: var(--el-color-primary);
}

.sql-menu-divider {
  height: 1px;
  background-color: var(--el-border-color-lighter);
  margin: 4px 0;
}

/* 字段弹框样式 */
.field-dialog {
  --el-dialog-border-radius: 12px;
}

.field-dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 0 4px;
}

.field-search {
  width: 300px;
}

.field-stats {
  display: flex;
  align-items: center;
  gap: 8px;
}

.field-dialog-content {
  max-height: 500px;
  border-radius: 8px;
  overflow: hidden;
}

.field-table {
  --el-table-border-radius: 8px;
}

.field-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
}

.pk-icon {
  color: var(--el-color-warning);
  font-size: 14px;
}

.field-name {
  font-weight: 500;
  flex: 1;
}

.field-name.is-primary {
  color: var(--el-color-warning);
  font-weight: 600;
}

.copy-btn {
  opacity: 0;
  transition: opacity 0.2s ease;
  padding: 4px;
  margin-left: auto;
}

.field-name-cell:hover .copy-btn {
  opacity: 1;
}

.type-tag {
  font-family: 'Consolas', 'Monaco', monospace;
  font-weight: 500;
}

.field-remarks {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 字段表格行 hover 效果 */
:global(.field-table .el-table__row:hover) {
  background-color: var(--el-fill-color-light) !important;
}

/* 主键行特殊样式 */
:global(.field-table .el-table__row:has(.is-primary)) {
  background-color: var(--el-color-warning-light-9) !important;
}

:global(.field-table .el-table__row:has(.is-primary):hover) {
  background-color: var(--el-color-warning-light-8) !important;
}

/* 全局 tooltip 样式 */
:global(.table-info-tooltip) {
  max-width: 200px;
}

:global(.table-info-tooltip .el-popper__arrow::before) {
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
}

/* 表格行 hover 效果 */
:global(.el-table-v2__row:hover) {
  background-color: var(--el-fill-color-lighter) !important;
}

/* 表格行样式 */
:global(.table-row) {
  transition: all 0.2s ease;
}

:global(.table-row:hover) {
  background-color: var(--el-fill-color-lighter) !important;
}

/* 确保表格单元格填满整个可用空间 */
:global(.el-table-v2__row-cell) {
  padding: 0 16px !important;
  display: flex;
  align-items: center;
}

:global(.el-table-v2__row-cell > *) {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
}

/* 表格头部样式优化 */
:global(.el-table-v2__header-row) {
  background-color: var(--el-fill-color-light);
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.loading-content {
  width: 100%;
  max-width: 500px;
  margin: 0 auto;
}

.detailed-loading {
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  padding: 32px;
  text-align: center;
  border: 1px solid var(--el-border-color-lighter);
}

.loading-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.loading-icon {
  color: var(--el-color-primary);
  animation: spin 2s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0;
  line-height: 1.3;
}

.loading-body {
  margin-bottom: 24px;
}

.loading-details {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  margin: 16px 0;
  line-height: 1.5;
}

.loading-stats {
  display: flex;
  justify-content: center;
  gap: 8px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  margin-top: 12px;
}

.loading-stats span {
  padding: 4px 8px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  font-weight: 500;
}

.progress-text {
  font-weight: 600;
  color: var(--el-color-primary);
}

.loading-tips {
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  border-left: 4px solid var(--el-color-primary);
}

/* 进度条样式优化 */
:global(.el-progress__text) {
  font-weight: 600 !important;
}

:global(.el-progress-bar__outer) {
  background-color: var(--el-fill-color-light) !important;
}

:global(.el-progress-bar__inner) {
  background: linear-gradient(90deg, var(--el-color-primary), var(--el-color-primary-light-3)) !important;
}

.empty-actions {
  margin-top: 16px;
}

/* 响应式优化 */
@media (max-width: 768px) {
  .table-row-cell {
    padding: 10px 12px;
  }
  
  .table-name {
    font-size: 14px;
  }
  
  .table-name-content {
    gap: 4px;
  }
  
  .table-meta-tags {
    gap: 6px;
  }
  
  .table-type-tag {
    font-size: 11px;
    height: 18px;
    padding: 0 5px;
  }
  
  .column-count {
    font-size: 11px;
  }
  
  .pk-tag {
    font-size: 10px;
    height: 16px;
    padding: 0 4px;
  }
  
  .table-remarks {
    font-size: 13px;
  }
  
  .context-menu {
    min-width: 160px;
  }
  
  .context-menu-item {
    padding: 10px 16px;
    font-size: 16px;
  }
  
  .sql-menu {
    min-width: 160px;
  }
  
  .sql-menu-item {
    padding: 10px 16px;
    font-size: 16px;
  }
}
</style>
