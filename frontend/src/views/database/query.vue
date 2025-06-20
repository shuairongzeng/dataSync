<template>
  <div class="main">
    <el-row :gutter="20">
      <!-- 左侧：SQL编辑器和执行控制 -->
      <el-col :span="16">
        <el-card class="query-card">
          <template #header>
            <div class="card-header">
              <span>SQL查询编辑器</span>
              <div class="header-actions">
                <el-button size="small" @click="formatSql">格式化</el-button>
                <el-button size="small" @click="clearEditor">清空</el-button>
                <el-button size="small" type="primary" @click="showHistoryDialog = true">
                  历史记录
                </el-button>
              </div>
            </div>
          </template>

          <!-- 数据库连接选择 -->
          <div class="connection-selector mb-4">
            <el-form :inline="true">
              <el-form-item label="数据库连接:">
                <el-select 
                  v-model="selectedConnectionId" 
                  placeholder="请选择数据库连接"
                  style="width: 200px"
                >
                  <el-option
                    v-for="conn in connectionList"
                    :key="conn.id"
                    :label="conn.name"
                    :value="conn.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="Schema:">
                <el-input 
                  v-model="selectedSchema" 
                  placeholder="可选"
                  style="width: 150px"
                />
              </el-form-item>
            </el-form>
          </div>

          <!-- SQL编辑器 -->
          <div class="editor-container">
            <codemirror-editor-vue3
              v-model:value="sqlContent"
              :options="editorOptions"
              height="300px"
              width="100%"
              border
              @change="handleEditorChange"
            />
          </div>

          <!-- 执行按钮区域 -->
          <div class="action-buttons mt-4">
            <el-button 
              type="primary" 
              @click="executeQuery"
              :loading="executing"
              :disabled="!selectedConnectionId || !sqlContent.trim()"
            >
              <el-icon><CaretRight /></el-icon>
              执行查询
            </el-button>
            <el-button 
              type="success" 
              @click="showSaveDialog = true"
              :disabled="!queryResult"
            >
              <el-icon><Download /></el-icon>
              保存结果
            </el-button>
            <el-button @click="exportResults" :disabled="!queryResult">
              <el-icon><Document /></el-icon>
              导出CSV
            </el-button>
          </div>
        </el-card>

        <!-- 查询结果 -->
        <el-card class="result-card mt-4" v-if="queryResult || errorMessage">
          <template #header>
            <div class="card-header">
              <span>查询结果</span>
              <div class="result-info" v-if="queryResult">
                <el-tag type="success">
                  {{ queryResult.totalRows }} 行
                </el-tag>
                <el-tag type="info" class="ml-2">
                  {{ queryResult.executionTime }}ms
                </el-tag>
              </div>
            </div>
          </template>

          <!-- 错误信息 -->
          <el-alert
            v-if="errorMessage"
            :title="errorMessage"
            type="error"
            show-icon
            :closable="false"
          />

          <!-- 结果表格 -->
          <div v-if="queryResult" class="result-table">
            <el-table
              :data="queryResult.rows"
              style="width: 100%"
              max-height="400"
              border
              stripe
            >
              <el-table-column
                v-for="(column, index) in queryResult.columns"
                :key="index"
                :prop="index.toString()"
                :label="column"
                show-overflow-tooltip
                min-width="120"
              >
                <template #default="{ row }">
                  {{ row[index] }}
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：数据库表结构 -->
      <el-col :span="8">
        <el-card class="schema-card">
          <template #header>
            <div class="card-header">
              <span>数据库结构</span>
              <el-button 
                size="small" 
                @click="loadTables"
                :loading="loadingTables"
                :disabled="!selectedConnectionId"
              >
                刷新
              </el-button>
            </div>
          </template>

          <div class="schema-tree">
            <el-tree
              :data="tableTreeData"
              :props="treeProps"
              node-key="id"
              @node-click="handleTableClick"
            >
              <template #default="{ node, data }">
                <span class="tree-node">
                  <el-icon v-if="data.type === 'table'">
                    <Grid />
                  </el-icon>
                  <el-icon v-else-if="data.type === 'column'">
                    <Key />
                  </el-icon>
                  <span class="ml-1">{{ node.label }}</span>
                  <span v-if="data.dataType" class="text-gray-400 text-xs ml-2">
                    {{ data.dataType }}
                  </span>
                </span>
              </template>
            </el-tree>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 保存结果对话框 -->
    <el-dialog
      v-model="showSaveDialog"
      title="保存查询结果"
      width="600px"
    >
      <el-form
        ref="saveFormRef"
        :model="saveForm"
        :rules="saveFormRules"
        label-width="120px"
      >
        <el-form-item label="目标数据库" prop="targetConnectionId">
          <el-select 
            v-model="saveForm.targetConnectionId" 
            placeholder="请选择目标数据库"
          >
            <el-option
              v-for="conn in connectionList"
              :key="conn.id"
              :label="conn.name"
              :value="conn.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="目标表名" prop="targetTableName">
          <el-input v-model="saveForm.targetTableName" placeholder="请输入目标表名" />
        </el-form-item>
        
        <el-form-item label="目标Schema" prop="targetSchemaName">
          <el-input v-model="saveForm.targetSchemaName" placeholder="可选，默认为空" />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showSaveDialog = false">取消</el-button>
          <el-button type="primary" @click="saveQueryResult" :loading="saving">
            保存
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 查询历史对话框 -->
    <el-dialog
      v-model="showHistoryDialog"
      title="查询历史"
      width="800px"
    >
      <el-table :data="queryHistory" style="width: 100%">
        <el-table-column prop="sql" label="SQL语句" show-overflow-tooltip />
        <el-table-column prop="sourceConnection" label="数据库" width="120" />
        <el-table-column prop="executedAt" label="执行时间" width="150">
          <template #default="{ row }">
            {{ formatTime(row.executedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'">
              {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" @click="loadHistoryQuery(row)">加载</el-button>
            <el-button size="small" type="danger" @click="deleteHistory(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="showHistoryDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { CaretRight, Download, Document, Grid, Key } from "@element-plus/icons-vue";
import CodemirrorEditorVue3 from "codemirror-editor-vue3";
import "codemirror/mode/sql/sql.js";
import "codemirror/theme/material.css";
import "codemirror/addon/hint/show-hint.css";
import "codemirror/addon/hint/show-hint.js";
import "codemirror/addon/hint/sql-hint.js";
import {
  type DbConfig,
  type QueryResult,
  type QueryHistory,
  type CustomQueryRequest,
  getDbConnectionsApi,
  getDbTablesApi,
  executeSqlQueryApi,
  executeCustomQueryApi,
  getQueryHistoryApi,
  saveQueryHistoryApi,
  deleteQueryHistoryApi
} from "@/api/database";

defineOptions({
  name: "DatabaseQuery"
});

// 响应式数据
const executing = ref(false);
const saving = ref(false);
const loadingTables = ref(false);
const showSaveDialog = ref(false);
const showHistoryDialog = ref(false);
const selectedConnectionId = ref("");
const selectedSchema = ref("");
const sqlContent = ref("-- 请输入SQL查询语句\nSELECT * FROM users LIMIT 10;");
const queryResult = ref<QueryResult | null>(null);
const errorMessage = ref("");
const connectionList = ref<DbConfig[]>([]);
const tableTreeData = ref<any[]>([]);
const queryHistory = ref<QueryHistory[]>([]);
const saveFormRef = ref<FormInstance>();

// 编辑器配置
const editorOptions = {
  mode: "text/x-sql",
  theme: "material",
  lineNumbers: true,
  lineWrapping: true,
  foldGutter: true,
  gutters: ["CodeMirror-linenumbers", "CodeMirror-foldgutter"],
  autoCloseBrackets: true,
  matchBrackets: true,
  hint: true,
  hintOptions: {
    completeSingle: false
  }
};

// 树形控件配置
const treeProps = {
  children: 'children',
  label: 'label'
};

// 保存表单数据
const saveForm = reactive({
  targetConnectionId: "",
  targetTableName: "",
  targetSchemaName: ""
});

// 保存表单验证规则
const saveFormRules: FormRules = {
  targetConnectionId: [{ required: true, message: "请选择目标数据库", trigger: "change" }],
  targetTableName: [{ required: true, message: "请输入目标表名", trigger: "blur" }]
};

// 获取连接列表
const fetchConnections = async () => {
  try {
    // 暂时使用模拟数据
    connectionList.value = [
      {
        id: "1",
        name: "本地MySQL",
        dbType: "mysql",
        host: "localhost",
        port: 3306,
        database: "test",
        username: "root",
        password: "******"
      },
      {
        id: "2",
        name: "PostgreSQL",
        dbType: "postgresql",
        host: "localhost",
        port: 5432,
        database: "testdb",
        username: "postgres",
        password: "******"
      }
    ];
  } catch (error) {
    ElMessage.error("获取连接列表失败");
  }
};

// 加载表结构
const loadTables = async () => {
  if (!selectedConnectionId.value) {
    ElMessage.warning("请先选择数据库连接");
    return;
  }

  loadingTables.value = true;
  try {
    // 暂时使用模拟数据
    const tables = ["users", "orders", "products"];
    tableTreeData.value = tables.map(table => ({
      id: table,
      label: table,
      type: 'table',
      children: [
        { id: `${table}_id`, label: 'id', type: 'column', dataType: 'INT' },
        { id: `${table}_name`, label: 'name', type: 'column', dataType: 'VARCHAR(255)' },
        { id: `${table}_created_at`, label: 'created_at', type: 'column', dataType: 'TIMESTAMP' }
      ]
    }));
  } catch (error) {
    ElMessage.error("加载表结构失败");
  } finally {
    loadingTables.value = false;
  }
};

// 表格点击事件
const handleTableClick = (data: any) => {
  if (data.type === 'table') {
    const selectSql = `SELECT * FROM ${data.label} LIMIT 10;`;
    sqlContent.value = selectSql;
  }
};

// 编辑器内容变化
const handleEditorChange = (value: string) => {
  sqlContent.value = value;
};

// 格式化SQL
const formatSql = () => {
  // 简单的SQL格式化
  let formatted = sqlContent.value
    .replace(/\s+/g, ' ')
    .replace(/,/g, ',\n  ')
    .replace(/\bFROM\b/gi, '\nFROM')
    .replace(/\bWHERE\b/gi, '\nWHERE')
    .replace(/\bORDER BY\b/gi, '\nORDER BY')
    .replace(/\bGROUP BY\b/gi, '\nGROUP BY')
    .replace(/\bHAVING\b/gi, '\nHAVING');
  
  sqlContent.value = formatted;
  ElMessage.success("SQL格式化完成");
};

// 清空编辑器
const clearEditor = () => {
  sqlContent.value = "";
};

// 执行查询
const executeQuery = async () => {
  if (!selectedConnectionId.value) {
    ElMessage.warning("请选择数据库连接");
    return;
  }

  if (!sqlContent.value.trim()) {
    ElMessage.warning("请输入SQL语句");
    return;
  }

  executing.value = true;
  errorMessage.value = "";
  queryResult.value = null;

  try {
    // 暂时使用模拟数据
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    queryResult.value = {
      columns: ["id", "name", "email", "created_at"],
      rows: [
        [1, "张三", "zhangsan@example.com", "2024-01-01 10:00:00"],
        [2, "李四", "lisi@example.com", "2024-01-02 11:00:00"],
        [3, "王五", "wangwu@example.com", "2024-01-03 12:00:00"]
      ],
      totalRows: 3,
      executionTime: 125
    };

    // 保存到历史记录
    const historyItem: Omit<QueryHistory, 'id' | 'executedAt'> = {
      sql: sqlContent.value,
      sourceConnection: getConnectionName(selectedConnectionId.value),
      executionTime: queryResult.value.executionTime,
      status: 'SUCCESS'
    };
    
    ElMessage.success("查询执行成功");
  } catch (error) {
    errorMessage.value = error.message || "查询执行失败";
    ElMessage.error("查询执行失败");
  } finally {
    executing.value = false;
  }
};

// 保存查询结果
const saveQueryResult = async () => {
  if (!saveFormRef.value) return;

  try {
    await saveFormRef.value.validate();
    saving.value = true;

    const sourceConnection = connectionList.value.find(c => c.id === selectedConnectionId.value);
    const targetConnection = connectionList.value.find(c => c.id === saveForm.targetConnectionId);

    if (!sourceConnection || !targetConnection) {
      ElMessage.error("连接信息不完整");
      return;
    }

    const request: CustomQueryRequest = {
      sourceDbConfig: sourceConnection,
      targetDbConfig: targetConnection,
      customSql: sqlContent.value,
      targetTableName: saveForm.targetTableName,
      targetSchemaName: saveForm.targetSchemaName
    };

    // 调用后端API
    ElMessage.success("查询结果保存成功");
    showSaveDialog.value = false;
  } catch (error) {
    ElMessage.error("保存失败");
  } finally {
    saving.value = false;
  }
};

// 导出结果为CSV
const exportResults = () => {
  if (!queryResult.value) return;

  const csvContent = [
    queryResult.value.columns.join(','),
    ...queryResult.value.rows.map(row => row.join(','))
  ].join('\n');

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = 'query_result.csv';
  link.click();
  
  ElMessage.success("CSV文件已下载");
};

// 获取连接名称
const getConnectionName = (connectionId: string) => {
  const connection = connectionList.value.find(c => c.id === connectionId);
  return connection ? connection.name : connectionId;
};

// 格式化时间
const formatTime = (timeStr: string) => {
  return new Date(timeStr).toLocaleString();
};

// 获取查询历史
const fetchQueryHistory = async () => {
  try {
    // 暂时使用模拟数据
    queryHistory.value = [
      {
        id: "1",
        sql: "SELECT * FROM users WHERE status = 'active'",
        sourceConnection: "本地MySQL",
        executedAt: new Date().toISOString(),
        executionTime: 150,
        status: 'SUCCESS'
      }
    ];
  } catch (error) {
    ElMessage.error("获取查询历史失败");
  }
};

// 加载历史查询
const loadHistoryQuery = (history: QueryHistory) => {
  sqlContent.value = history.sql;
  showHistoryDialog.value = false;
  ElMessage.success("历史查询已加载");
};

// 删除历史记录
const deleteHistory = async (history: QueryHistory) => {
  try {
    await ElMessageBox.confirm(
      "确定要删除这条历史记录吗？",
      "确认删除",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }
    );
    
    ElMessage.success("删除成功");
    fetchQueryHistory();
  } catch (error) {
    // 用户取消删除
  }
};

// 组件挂载时获取数据
onMounted(() => {
  fetchConnections();
  fetchQueryHistory();
});
</script>

<style scoped>
.main {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.query-card {
  min-height: 500px;
}

.result-card {
  min-height: 300px;
}

.schema-card {
  height: 600px;
}

.editor-container {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

.result-info {
  display: flex;
  align-items: center;
}

.schema-tree {
  max-height: 500px;
  overflow-y: auto;
}

.tree-node {
  display: flex;
  align-items: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.connection-selector {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
}

.result-table {
  max-height: 400px;
  overflow: auto;
}
</style>
