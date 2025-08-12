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
            :columns="columns"
            :data="displayData"
            :width="width"
            :height="height"
            :row-height="56"
            :header-height="48"
            :estimated-row-height="56"
            @row-click="handleRowClick"
            @row-contextmenu="handleRowRightClick"
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

    <!-- 加载状态 -->
    <div class="loading-overlay" v-if="loading">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 右键菜单 -->
    <el-dropdown
      ref="contextMenuRef"
      trigger="contextmenu"
      :teleported="false"
      @command="handleContextMenuCommand"
    >
      <span></span>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="select">生成 SELECT 查询</el-dropdown-item>
          <el-dropdown-item command="count">生成 COUNT 查询</el-dropdown-item>
          <el-dropdown-item command="describe">查看表结构</el-dropdown-item>
          <el-dropdown-item command="insert">生成 INSERT 模板</el-dropdown-item>
          <el-dropdown-item command="update">生成 UPDATE 模板</el-dropdown-item>
          <el-dropdown-item command="delete">生成 DELETE 模板</el-dropdown-item>
          <el-dropdown-item divided command="refresh-single">刷新此表信息</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch, h } from 'vue'
import { ElMessage, ElIcon, ElTag, ElText, ElTooltip } from 'element-plus'
import { Search, Refresh, Clock, Document } from '@element-plus/icons-vue'
import { tableCacheManager, type TableInfo, type CacheMetadata } from '@/utils/TableCacheManager'
import { getTablesWithPaginationApi, getTableColumnsApi } from '@/api/database'

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
const selectedTable = ref<TableInfo | null>(null)
const cacheMetadata = ref<CacheMetadata | null>(null)

// 容器引用
const containerRef = ref()
const contextMenuRef = ref()

// 计算属性
const displayData = computed(() => filteredData.value)

const hasCache = computed(() => {
  return tableCacheManager.hasCache(props.connectionId, props.schema)
})

const cacheStatus = computed(() => {
  if (!hasCache.value) {
    return { type: 'info', text: '无缓存' }
  }
  
  if (cacheMetadata.value) {
    const age = Date.now() - cacheMetadata.value.lastUpdated
    const hours = Math.floor(age / (1000 * 60 * 60))
    
    if (hours < 1) {
      return { type: 'success', text: '最新' }
    } else if (hours < 12) {
      return { type: 'warning', text: `${hours}小时前` }
    } else {
      return { type: 'danger', text: '较旧' }
    }
  }
  
  return { type: 'info', text: '已缓存' }
})

// 表格列定义 - 简化为只显示表名和备注，其他信息通过tooltip显示
const columns = [
  {
    key: 'tableName',
    title: '表名',
    dataKey: 'tableName',
    width: 350,
    cellRenderer: ({ rowData }: any) => {
      const tooltipContent = h('div', { class: 'tooltip-content' }, [
        h('div', { class: 'tooltip-item' }, [
          h('span', { class: 'tooltip-label' }, '类型：'),
          h('span', { class: 'tooltip-value' }, rowData.tableType === 'VIEW' ? '视图' : '表')
        ]),
        h('div', { class: 'tooltip-item' }, [
          h('span', { class: 'tooltip-label' }, '列数：'),
          h('span', { class: 'tooltip-value' }, `${rowData.columnCount || 0} 列`)
        ]),
        h('div', { class: 'tooltip-item' }, [
          h('span', { class: 'tooltip-label' }, '主键：'),
          h('span', { class: 'tooltip-value' }, rowData.hasPrimaryKey ? '有' : '无')
        ])
      ])
      
      return h(ElTooltip, {
        placement: 'right',
        showAfter: 300,
        hideAfter: 100,
        popperClass: 'table-info-tooltip'
      }, {
        content: () => tooltipContent,
        default: () => h('div', { class: 'table-name-cell' }, [
          h(ElIcon, { class: 'table-icon' }, () => h(Document)),
          h('div', { class: 'table-name-content' }, [
            h('span', { class: 'table-name' }, rowData.tableName),
            rowData.tableType === 'VIEW' ? h(ElTag, { 
              size: 'small', 
              type: 'info',
              class: 'table-type-tag'
            }, () => '视图') : null
          ].filter(Boolean))
        ])
      })
    }
  },
  {
    key: 'remarks',
    title: '备注',
    dataKey: 'remarks',
    cellRenderer: ({ rowData }: any) => {
      const remarks = rowData.remarks || '-'
      return h('div', { class: 'table-remarks-cell' }, [
        h('span', {
          class: 'table-remarks',
          title: remarks.length > 30 ? remarks : undefined
        }, remarks.length > 30 ? `${remarks.substring(0, 30)}...` : remarks)
      ])
    }
  }
]

// 方法
const loadTables = async (useCache = true) => {
  if (!props.connectionId) return

  // 如果允许使用缓存且有缓存，直接使用缓存数据
  if (useCache) {
    const cachedTables = tableCacheManager.getCachedTables(props.connectionId, props.schema)
    if (cachedTables) {
      tableData.value = cachedTables
      cacheMetadata.value = tableCacheManager.getCacheMetadata(props.connectionId, props.schema)
      applySearch()
      return
    }
  }

  loading.value = true

  try {
    // 分批加载所有表数据
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
        // 转换数据格式
        const tables: TableInfo[] = response.data.map((table: any) => ({
          tableName: table.tableName || table.name,
          tableType: table.tableType || 'TABLE',
          columnCount: table.columnCount || 0,
          remarks: table.remarks || table.comment,
          hasPrimaryKey: table.hasPrimaryKey || false,
          schema: props.schema,
          connectionId: props.connectionId,
          lastUpdated: Date.now()
        }))
        
        allTables.push(...tables)
      }

      hasMore = response.hasNext || false
      currentPage++

      // 防止无限循环
      if (currentPage > 100) {
        console.warn('Too many pages, stopping table loading')
        break
      }
    }

    // 缓存数据
    tableCacheManager.cacheTables(props.connectionId, props.schema, allTables)
    
    tableData.value = allTables
    cacheMetadata.value = tableCacheManager.getCacheMetadata(props.connectionId, props.schema)
    
    applySearch()
    
    ElMessage.success(`成功加载 ${allTables.length} 张表`)
  } catch (error: any) {
    console.error('加载表列表失败:', error)
    ElMessage.error('加载表列表失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
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

const searchTimeout = ref<any>(null)

const handleRowClick = (row: TableInfo) => {
  selectedTable.value = row
  emit('table-click', row.tableName)
}

const handleRowRightClick = (event: MouseEvent, row: TableInfo) => {
  event.preventDefault()
  selectedTable.value = row
  
  nextTick(() => {
    const menu = contextMenuRef.value
    if (menu) {
      menu.handleOpen()
    }
  })
}

const handleContextMenuCommand = async (command: string) => {
  if (!selectedTable.value) return

  const tableName = selectedTable.value.tableName
  let sql = ''

  try {
    switch (command) {
      case 'select':
        sql = await generateSelectSql(tableName)
        break
      case 'count':
        sql = `SELECT COUNT(*) FROM ${tableName};`
        break
      case 'describe':
        sql = `DESC ${tableName};`
        break
      case 'insert':
        sql = await generateInsertSql(tableName)
        break
      case 'update':
        sql = await generateUpdateSql(tableName)
        break
      case 'delete':
        sql = `DELETE FROM ${tableName} WHERE ;`
        break
      case 'refresh-single':
        await refreshSingleTable(tableName)
        return
    }

    if (sql) {
      emit('sql-generated', sql)
    }
  } catch (error: any) {
    ElMessage.error('操作失败: ' + (error.message || '未知错误'))
  }
}

const generateSelectSql = async (tableName: string) => {
  try {
    const columns = await getTableColumnsApi(props.connectionId, tableName, props.schema)
    const columnNames = columns.slice(0, 10).map((col: any) => col.columnName).join(', ')
    return `SELECT ${columnNames}${columns.length > 10 ? ', ...' : ''} FROM ${tableName} LIMIT 100;`
  } catch (error) {
    return `SELECT * FROM ${tableName} LIMIT 100;`
  }
}

const generateInsertSql = async (tableName: string) => {
  try {
    const columns = await getTableColumnsApi(props.connectionId, tableName, props.schema)
    const columnNames = columns.filter((col: any) => !col.isAutoIncrement).map((col: any) => col.columnName)
    const values = columnNames.map(() => '?').join(', ')
    return `INSERT INTO ${tableName} (${columnNames.join(', ')}) VALUES (${values});`
  } catch (error) {
    return `INSERT INTO ${tableName} () VALUES ();`
  }
}

const generateUpdateSql = async (tableName: string) => {
  try {
    const columns = await getTableColumnsApi(props.connectionId, tableName, props.schema)
    const updateColumns = columns.filter((col: any) => !col.isPrimaryKey && !col.isAutoIncrement)
      .slice(0, 3).map((col: any) => `${col.columnName} = ?`).join(', ')
    const pkColumn = columns.find((col: any) => col.isPrimaryKey)?.columnName || 'id'
    return `UPDATE ${tableName} SET ${updateColumns} WHERE ${pkColumn} = ?;`
  } catch (error) {
    return `UPDATE ${tableName} SET  WHERE ;`
  }
}

const refreshSingleTable = async (tableName: string) => {
  try {
    // 这里可以实现单表刷新逻辑
    // 暂时使用全量刷新
    await refreshTables()
    ElMessage.success(`已刷新表 ${tableName} 的信息`)
  } catch (error: any) {
    ElMessage.error('刷新表信息失败: ' + (error.message || '未知错误'))
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

// 生命周期
onMounted(() => {
  if (props.connectionId) {
    loadTables(true)
  }
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

/* 全局tooltip样式 */
:global(.table-info-tooltip) {
  max-width: 200px;
}

:global(.table-info-tooltip .el-popper__arrow::before) {
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
}

/* 表格行hover效果 */
:global(.el-table-v2__row:hover) {
  background-color: var(--el-fill-color-lighter) !important;
}

/* 表格头部样式优化 */
:global(.el-table-v2__header-row) {
  background-color: var(--el-fill-color-light);
  font-weight: 600;
  color: var(--el-text-color-primary);
}

/* 响应式优化 */
@media (max-width: 768px) {
  .table-name-cell {
    padding: 6px 8px;
    gap: 6px;
  }
  
  .table-name {
    font-size: 14px;
  }
  
  .table-remarks {
    font-size: 12px;
  }
}

.table-name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 6px;
  transition: all 0.2s ease;
  cursor: pointer;
}

.table-name-cell:hover {
  background-color: var(--el-fill-color-light);
  transform: translateX(2px);
}

.table-icon {
  color: var(--el-color-primary);
  font-size: 16px;
  flex-shrink: 0;
}

.table-name-content {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.table-name {
  font-weight: 500;
  color: var(--el-text-color-primary);
  transition: color 0.2s ease;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-name-cell:hover .table-name {
  color: var(--el-color-primary);
}

.table-type-tag {
  flex-shrink: 0;
}

.table-remarks-cell {
  padding: 8px 12px;
}

.table-remarks {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.4;
  display: block;
}

/* Tooltip样式优化 */
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

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.empty-actions {
  margin-top: 16px;
}
</style>