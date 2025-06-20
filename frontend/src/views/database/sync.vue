<template>
  <div class="main">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>数据同步任务管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新建任务
          </el-button>
        </div>
      </template>

      <!-- 任务列表 -->
      <el-table :data="taskList" style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="任务名称" width="150" />
        <el-table-column label="源数据库" width="120">
          <template #default="{ row }">
            {{ getConnectionName(row.sourceConnectionId) }}
          </template>
        </el-table-column>
        <el-table-column label="目标数据库" width="120">
          <template #default="{ row }">
            {{ getConnectionName(row.targetConnectionId) }}
          </template>
        </el-table-column>
        <el-table-column label="同步表数量" width="100">
          <template #default="{ row }">
            {{ row.tables?.length || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="150">
          <template #default="{ row }">
            <div v-if="row.status === 'RUNNING'">
              <el-progress 
                :percentage="row.progress || 0" 
                :status="row.progress === 100 ? 'success' : ''" 
              />
              <div class="text-xs text-gray-500 mt-1">
                {{ row.completedTables || 0 }}/{{ row.totalTables || 0 }} 表
              </div>
            </div>
            <div v-else-if="row.status === 'COMPLETED_SUCCESS'">
              <el-progress :percentage="100" status="success" />
            </div>
            <div v-else-if="row.status === 'FAILED'">
              <el-progress :percentage="row.progress || 0" status="exception" />
            </div>
            <div v-else>
              <span class="text-gray-400">-</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="lastRunAt" label="最后执行时间" width="150">
          <template #default="{ row }">
            {{ row.lastRunAt ? formatTime(row.lastRunAt) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button 
              size="small" 
              type="success" 
              @click="handleExecute(row)"
              :disabled="row.status === 'RUNNING'"
            >
              执行
            </el-button>
            <el-button 
              size="small" 
              type="warning" 
              @click="handleStop(row)"
              :disabled="row.status !== 'RUNNING'"
            >
              停止
            </el-button>
            <el-button size="small" @click="handleViewLogs(row)">日志</el-button>
            <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑任务' : '新建任务'"
      width="800px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
      >
        <el-form-item label="任务名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入任务名称" />
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="源数据库" prop="sourceConnectionId">
              <el-select 
                v-model="formData.sourceConnectionId" 
                placeholder="请选择源数据库"
                @change="handleSourceChange"
              >
                <el-option
                  v-for="conn in connectionList"
                  :key="conn.id"
                  :label="conn.name"
                  :value="conn.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标数据库" prop="targetConnectionId">
              <el-select 
                v-model="formData.targetConnectionId" 
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
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="源Schema" prop="sourceSchemaName">
              <el-input v-model="formData.sourceSchemaName" placeholder="可选，默认为空" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标Schema" prop="targetSchemaName">
              <el-input v-model="formData.targetSchemaName" placeholder="可选，默认为空" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="同步表" prop="tables">
          <div class="w-full">
            <div class="flex items-center mb-2">
              <el-button size="small" @click="loadSourceTables" :loading="loadingTables">
                加载源表列表
              </el-button>
              <span class="ml-2 text-sm text-gray-500">
                已选择 {{ formData.tables.length }} 个表
              </span>
            </div>
            <el-transfer
              v-model="formData.tables"
              :data="sourceTableList"
              :titles="['可用表', '同步表']"
              filterable
              filter-placeholder="搜索表名"
            />
          </div>
        </el-form-item>

        <el-form-item label="同步选项">
          <el-checkbox v-model="formData.truncateBeforeSync">
            同步前清空目标表数据
          </el-checkbox>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 日志查看对话框 -->
    <el-dialog
      v-model="logDialogVisible"
      title="任务执行日志"
      width="800px"
    >
      <div class="log-container">
        <el-scrollbar height="400px">
          <pre class="log-content">{{ taskLogs.join('\n') }}</pre>
        </el-scrollbar>
      </div>
      <template #footer>
        <el-button @click="logDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="refreshLogs">刷新日志</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import {
  type SyncTask,
  type DbConfig,
  getSyncTasksApi,
  createSyncTaskApi,
  updateSyncTaskApi,
  deleteSyncTaskApi,
  executeSyncTaskApi,
  stopSyncTaskApi,
  getSyncTaskProgressApi,
  getSyncTaskLogsApi,
  getDbConnectionsApi,
  getDbTablesApi
} from "@/api/database";

defineOptions({
  name: "DatabaseSync"
});

// 响应式数据
const loading = ref(false);
const dialogVisible = ref(false);
const logDialogVisible = ref(false);
const isEdit = ref(false);
const submitLoading = ref(false);
const loadingTables = ref(false);
const taskList = ref<SyncTask[]>([]);
const connectionList = ref<DbConfig[]>([]);
const sourceTableList = ref<{key: string, label: string}[]>([]);
const taskLogs = ref<string[]>([]);
const currentLogTaskId = ref<string>("");
const formRef = ref<FormInstance>();
const progressTimer = ref<NodeJS.Timeout>();

// 表单数据
const formData = reactive<SyncTask>({
  name: "",
  sourceConnectionId: "",
  targetConnectionId: "",
  sourceSchemaName: "",
  targetSchemaName: "",
  tables: [],
  truncateBeforeSync: false,
  status: 'PENDING'
});

// 表单验证规则
const formRules: FormRules = {
  name: [{ required: true, message: "请输入任务名称", trigger: "blur" }],
  sourceConnectionId: [{ required: true, message: "请选择源数据库", trigger: "change" }],
  targetConnectionId: [{ required: true, message: "请选择目标数据库", trigger: "change" }],
  tables: [{ required: true, message: "请选择要同步的表", trigger: "change" }]
};

// 获取状态标签样式
const getStatusTagType = (status: string) => {
  const statusMap = {
    'PENDING': '',
    'RUNNING': 'warning',
    'COMPLETED_SUCCESS': 'success',
    'COMPLETED_WITH_ERRORS': 'warning',
    'FAILED': 'danger'
  };
  return statusMap[status] || '';
};

// 获取状态文本
const getStatusText = (status: string) => {
  const statusMap = {
    'PENDING': '待执行',
    'RUNNING': '执行中',
    'COMPLETED_SUCCESS': '成功',
    'COMPLETED_WITH_ERRORS': '部分成功',
    'FAILED': '失败'
  };
  return statusMap[status] || status;
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

// 获取任务列表
const fetchTasks = async () => {
  loading.value = true;
  try {
    // 暂时使用模拟数据
    taskList.value = [
      {
        id: "1",
        name: "用户数据同步",
        sourceConnectionId: "1",
        targetConnectionId: "2",
        sourceSchemaName: "public",
        targetSchemaName: "backup",
        tables: ["users", "user_profiles"],
        truncateBeforeSync: true,
        status: 'COMPLETED_SUCCESS',
        progress: 100,
        totalTables: 2,
        completedTables: 2,
        lastRunAt: new Date().toISOString()
      }
    ];
  } catch (error) {
    ElMessage.error("获取任务列表失败");
  } finally {
    loading.value = false;
  }
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
        name: "备份PostgreSQL",
        dbType: "postgresql",
        host: "backup-server",
        port: 5432,
        database: "backup_db",
        username: "postgres",
        password: "******"
      }
    ];
  } catch (error) {
    ElMessage.error("获取连接列表失败");
  }
};

// 加载源表列表
const loadSourceTables = async () => {
  if (!formData.sourceConnectionId) {
    ElMessage.warning("请先选择源数据库");
    return;
  }
  
  loadingTables.value = true;
  try {
    // 暂时使用模拟数据
    const tables = ["users", "user_profiles", "orders", "products", "categories"];
    sourceTableList.value = tables.map(table => ({
      key: table,
      label: table
    }));
    ElMessage.success("表列表加载成功");
  } catch (error) {
    ElMessage.error("加载表列表失败");
  } finally {
    loadingTables.value = false;
  }
};

// 源数据库改变时清空表选择
const handleSourceChange = () => {
  formData.tables = [];
  sourceTableList.value = [];
};

// 新增任务
const handleAdd = () => {
  isEdit.value = false;
  resetForm();
  dialogVisible.value = true;
};

// 编辑任务
const handleEdit = (row: SyncTask) => {
  isEdit.value = true;
  Object.assign(formData, row);
  dialogVisible.value = true;
};

// 删除任务
const handleDelete = async (row: SyncTask) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除任务 "${row.name}" 吗？`,
      "确认删除",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }
    );
    
    ElMessage.success("删除成功");
    fetchTasks();
  } catch (error) {
    // 用户取消删除
  }
};

// 执行任务
const handleExecute = async (row: SyncTask) => {
  try {
    await ElMessageBox.confirm(
      `确定要执行任务 "${row.name}" 吗？`,
      "确认执行",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "info"
      }
    );
    
    ElMessage.success("任务已开始执行");
    // 更新任务状态为执行中
    row.status = 'RUNNING';
    row.progress = 0;
    
    // 开始轮询进度
    startProgressPolling();
  } catch (error) {
    // 用户取消执行
  }
};

// 停止任务
const handleStop = async (row: SyncTask) => {
  try {
    await ElMessageBox.confirm(
      `确定要停止任务 "${row.name}" 吗？`,
      "确认停止",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }
    );
    
    ElMessage.success("任务已停止");
    row.status = 'FAILED';
    stopProgressPolling();
  } catch (error) {
    // 用户取消停止
  }
};

// 查看日志
const handleViewLogs = async (row: SyncTask) => {
  currentLogTaskId.value = row.id!;
  await refreshLogs();
  logDialogVisible.value = true;
};

// 刷新日志
const refreshLogs = async () => {
  try {
    // 暂时使用模拟数据
    taskLogs.value = [
      `[${new Date().toLocaleString()}] 任务开始执行`,
      `[${new Date().toLocaleString()}] 连接源数据库成功`,
      `[${new Date().toLocaleString()}] 连接目标数据库成功`,
      `[${new Date().toLocaleString()}] 开始同步表: users`,
      `[${new Date().toLocaleString()}] 表 users 同步完成，共 1000 条记录`,
      `[${new Date().toLocaleString()}] 开始同步表: user_profiles`,
      `[${new Date().toLocaleString()}] 表 user_profiles 同步完成，共 1000 条记录`,
      `[${new Date().toLocaleString()}] 任务执行完成`
    ];
  } catch (error) {
    ElMessage.error("获取日志失败");
  }
};

// 开始进度轮询
const startProgressPolling = () => {
  progressTimer.value = setInterval(async () => {
    // 模拟进度更新
    const runningTasks = taskList.value.filter(t => t.status === 'RUNNING');
    runningTasks.forEach(task => {
      if (task.progress! < 100) {
        task.progress = Math.min((task.progress || 0) + 10, 100);
        task.completedTables = Math.floor((task.progress / 100) * (task.totalTables || 1));
        
        if (task.progress === 100) {
          task.status = 'COMPLETED_SUCCESS';
        }
      }
    });
    
    if (runningTasks.length === 0) {
      stopProgressPolling();
    }
  }, 2000);
};

// 停止进度轮询
const stopProgressPolling = () => {
  if (progressTimer.value) {
    clearInterval(progressTimer.value);
    progressTimer.value = undefined;
  }
};

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return;
  
  try {
    await formRef.value.validate();
    submitLoading.value = true;
    
    if (isEdit.value) {
      ElMessage.success("更新成功");
    } else {
      ElMessage.success("创建成功");
    }
    
    dialogVisible.value = false;
    fetchTasks();
  } catch (error) {
    ElMessage.error("保存失败");
  } finally {
    submitLoading.value = false;
  }
};

// 重置表单
const resetForm = () => {
  Object.assign(formData, {
    name: "",
    sourceConnectionId: "",
    targetConnectionId: "",
    sourceSchemaName: "",
    targetSchemaName: "",
    tables: [],
    truncateBeforeSync: false,
    status: 'PENDING'
  });
  sourceTableList.value = [];
  formRef.value?.clearValidate();
};

// 对话框关闭处理
const handleDialogClose = () => {
  resetForm();
};

// 组件挂载时获取数据
onMounted(() => {
  fetchTasks();
  fetchConnections();
  startProgressPolling();
});

// 组件卸载时清理定时器
onUnmounted(() => {
  stopProgressPolling();
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

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.log-container {
  background-color: #f5f5f5;
  border-radius: 4px;
  padding: 10px;
}

.log-content {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.4;
  color: #333;
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>
