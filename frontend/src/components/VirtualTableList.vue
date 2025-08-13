<template>
  <div class="virtual-table-list">
    <!-- 搜索框 -->
    <div class="search-container">
      <el-input
        v-model="searchText"
        placeholder="搜索表名..."
        :prefix-icon="Search"
        clearable
        @input="handleSearch"
        @clear="handleSearch"
        class="search-input"
      />
      <div class="search-stats" v-if="totalCount > 0">
        共找到 {{ totalCount }} 张表
      </div>
    </div>

    <!-- 虚拟滚动表格 -->
    <div class="table-container" ref="containerRef">
      <el-auto-resizer>
        <template #default="{ height, width }">
          <el-table-v2
            :columns="columns"
            :data="displayData"
            :width="width"
            :height="height"
            :row-height="60"
            :header-height="50"
            :estimated-row-height="60"
            @row-click="handleRowClick"
            @row-contextmenu="handleRowRightClick"
            class="virtual-table"
          >
            <template #empty>
              <el-empty 
                :description="loading ? '加载中...' : (searchText ? '未找到匹配的表' : '暂无数据')"
                :image-size="100"
              />
            </template>
          </el-table-v2>
        </template>
      </el-auto-resizer>
    </div>

    <!-- 加载更多按钮 -->
    <div class="load-more-container" v-if="hasMore && !loading">
      <el-button @click="loadMore" :loading="loadingMore" type="primary" plain>
        加载更多 ({{ loadedCount }}/{{ totalCount }})
      </el-button>
    </div>

    <!-- 加载状态 -->
    <div class="loading-container" v-if="loading">
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
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch, h } from 'vue'
import { ElMessage, ElIcon, ElTag } from 'element-plus'
import { Search, Document } from '@element-plus/icons-vue'
import { getTablesWithPaginationApi, getTableColumnsApi, getConnectionByIdApi } from '@/api/database'
import { generateSelectQuery, generateInsertQuery, generateUpdateQuery, generateDeleteQuery, generateDescribeQuery } from '@/utils/SqlGenerator'

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
const loadingMore = ref(false)
const tableData = ref<any[]>([])
const totalCount = ref(0)
const currentPage = ref(1)
const pageSize = ref(50)
const hasMore = ref(false)
const selectedTable = ref<any>(null)

// 数据库连接信息
const connectionInfo = ref<any>(null)
const dbType = ref('')

// 容器引用
const containerRef = ref()
const contextMenuRef = ref()

// 计算属性
const displayData = computed(() => tableData.value)
const loadedCount = computed(() => tableData.value.length)

// 表格列定义
const columns = [
  {
    key: 'tableName',
    title: '表名',
    dataKey: 'tableName',
    width: 200,
    cellRenderer: ({ rowData }: any) => {
      return h('div', { class: 'table-name-cell' }, [
        h(ElIcon, { class: 'table-icon' }, () => h(Document)),
        h('span', { class: 'table-name' }, rowData.tableName),
        rowData.tableType === 'VIEW' ? h(ElTag, { size: 'small', type: 'info' }, () => '视图') : null
      ].filter(Boolean))
    }
  },
  {
    key: 'columnCount',
    title: '列数',
    dataKey: 'columnCount',
    width: 80,
    align: 'center'
  },
  {
    key: 'tableType',
    title: '类型',
    dataKey: 'tableType',
    width: 80,
    align: 'center',
    cellRenderer: ({ rowData }: any) => {
      return h(ElTag, {
        size: 'small',
        type: rowData.tableType === 'VIEW' ? 'info' : 'primary'
      }, () => rowData.tableType === 'VIEW' ? '视图' : '表')
    }
  },
  {
    key: 'remarks',
    title: '备注',
    dataKey: 'remarks',
    width: 300,
    cellRenderer: ({ rowData }: any) => {
      return h('span', {
        class: 'table-remarks',
        title: rowData.remarks
      }, rowData.remarks || '-')
    }
  },
  {
    key: 'hasPrimaryKey',
    title: '主键',
    dataKey: 'hasPrimaryKey',
    width: 80,
    align: 'center',
    cellRenderer: ({ rowData }: any) => {
      return rowData.hasPrimaryKey
        ? h(ElIcon, { color: '#67C23A' }, () => '✓')
        : h(ElIcon, { color: '#F56C6C' }, () => '✗')
    }
  }
]

// 方法
const loadTables = async (reset = false) => {
  if (reset) {
    currentPage.value = 1
    tableData.value = []
  }

  if (loading.value || loadingMore.value) return

  if (reset) {
    loading.value = true
  } else {
    loadingMore.value = true
  }

  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      search: searchText.value || undefined,
      sortBy: 'name',
      sortOrder: 'asc',
      schema: props.schema
    }

    const response = await getTablesWithPaginationApi(props.connectionId, params)
    
    if (reset) {
      tableData.value = response.data || []
    } else {
      tableData.value.push(...(response.data || []))
    }
    
    totalCount.value = response.total || 0
    hasMore.value = response.hasNext || false
    
    if (hasMore.value) {
      currentPage.value++
    }
  } catch (error: any) {
    console.error('加载表列表失败:', error)
    ElMessage.error('加载表列表失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const loadMore = () => {
  if (!hasMore.value || loadingMore.value) return
  loadTables(false)
}

const handleSearch = () => {
  // 防抖处理
  clearTimeout(searchTimeout.value)
  searchTimeout.value = setTimeout(() => {
    loadTables(true)
  }, 300)
}

const searchTimeout = ref<any>(null)

const handleRowClick = (row: any) => {
  selectedTable.value = row
  emit('table-click', row.tableName)
}

const handleRowRightClick = (event: MouseEvent, row: any) => {
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
        sql = await generateDescribeSql(tableName)
        break
      case 'insert':
        sql = await generateInsertSql(tableName)
        break
      case 'update':
        sql = await generateUpdateSql(tableName)
        break
      case 'delete':
        sql = await generateDeleteSql(tableName)
        break
    }

    if (sql) {
      emit('sql-generated', sql)
    }
  } catch (error: any) {
    ElMessage.error('生成SQL失败: ' + (error.message || '未知错误'))
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
    dbType.value = 'mysql'
  }
}

const generateSelectSql = async (tableName: string) => {
  try {
    await loadConnectionInfo()
    const columns = await getTableColumnsApi(props.connectionId, tableName, props.schema)
    const columnNames = columns.slice(0, 10).map((col: any) => col.columnName)
    return generateSelectQuery(dbType.value, tableName, columnNames, props.schema)
  } catch (error) {
    await loadConnectionInfo()
    return generateSelectQuery(dbType.value, tableName, [], props.schema)
  }
}

const generateInsertSql = async (tableName: string) => {
  try {
    await loadConnectionInfo()
    const columns = await getTableColumnsApi(props.connectionId, tableName, props.schema)
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
    await loadConnectionInfo()
    const columns = await getTableColumnsApi(props.connectionId, tableName, props.schema)
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
    await loadConnectionInfo()
    const columns = await getTableColumnsApi(props.connectionId, tableName, props.schema)
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
    await loadConnectionInfo()
    return generateDescribeQuery(dbType.value, tableName, props.schema)
  } catch (error) {
    await loadConnectionInfo()
    return generateDescribeQuery(dbType.value, tableName, props.schema)
  }
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
.virtual-table-list {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.search-container {
  padding: 16px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.search-input {
  margin-bottom: 8px;
}

.search-stats {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.table-container {
  flex: 1;
  min-height: 400px;
}

.virtual-table {
  border: 1px solid var(--el-border-color-light);
}

.table-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.table-icon {
  color: var(--el-color-primary);
}

.table-name {
  font-weight: 500;
  cursor: pointer;
}

.table-name:hover {
  color: var(--el-color-primary);
}

.table-remarks {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.load-more-container {
  padding: 16px;
  text-align: center;
  border-top: 1px solid var(--el-border-color-light);
}

.loading-container {
  padding: 16px;
}
</style>
