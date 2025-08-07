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
        <el-table-column label="源数据库" width="220">
          <template #default="{ row }">
            {{ getConnectionName(row.sourceConnectionId) }}
          </template>
        </el-table-column>
        <el-table-column label="目标数据库" width="220">
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
        <el-table-column label="操作" width="450" fixed="right">
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
                {{ loadingTables ? '加载中...' : '加载源表列表' }}
              </el-button>
              <span class="ml-2 text-sm text-gray-500">
                已选择 {{ formData.tables.length }} 个表
                <span v-if="sourceTableList.length > 0">
                  / 共 {{ sourceTableList.length }} 个可用表
                </span>
              </span>
            </div>

            <!-- 加载状态提示 -->
            <div v-if="loadingTables" class="mb-2 p-2 bg-blue-50 border border-blue-200 rounded text-sm text-blue-600">
              <i class="el-icon-loading mr-1"></i>
              正在加载表列表，请稍候...（大型数据库可能需要较长时间）
            </div>

            <!-- 表列表为空时的提示 -->
            <div v-if="!loadingTables && sourceTableList.length === 0" class="mb-2 p-2 bg-yellow-50 border border-yellow-200 rounded text-sm text-yellow-600">
              <i class="el-icon-warning mr-1"></i>
              暂无可用表，请点击"加载源表列表"按钮获取表信息
            </div>

            <el-transfer
              v-model="formData.tables"
              :data="sourceTableList"
              :titles="['可用表', '同步表']"
              filterable
              filter-placeholder="搜索表名"
              :disabled="loadingTables"
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
  getDbTablesApi,
  checkConnectionHealthApi
} from "@/api/database";
import { dbMetadataCache, CacheKeys } from '@/utils/cache';
import { frontendCacheManager } from '@/api/cache';

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
    taskList.value = await getSyncTasksApi();
  } catch (error) {
    ElMessage.error("获取任务列表失败");
  } finally {
    loading.value = false;
  }
};

// 获取连接列表
const fetchConnections = async () => {
  try {
    connectionList.value = await getDbConnectionsApi();
  } catch (error) {
    ElMessage.error("获取连接列表失败");
  }
};

// 表列表缓存配置
const CACHE_DURATION = 10 * 60 * 1000; // 10分钟缓存

// 加载源表列表（支持缓存和重试）
const loadSourceTables = async (retryCount = 0) => {
  if (!formData.sourceConnectionId) {
    ElMessage.warning("请先选择源数据库");
    return;
  }

  const cacheKey = CacheKeys.tables(formData.sourceConnectionId, formData.sourceSchemaName);

  // 检查缓存是否有效
  const cached = dbMetadataCache.get(cacheKey);
  if (cached) {
    sourceTableList.value = cached;
    ElMessage.success("表列表加载成功（来自缓存）");
    frontendCacheManager.recordHit();
    return;
  }

  loadingTables.value = true;
  try {
    // 先进行连接健康检查
    try {
      const healthResult = await checkConnectionHealthApi(formData.sourceConnectionId.toString());
      if (!healthResult.healthy) {
        throw new Error(`数据库连接异常: ${healthResult.message}`);
      }
    } catch (healthError: any) {
      throw new Error(`连接健康检查失败: ${healthError.message || '无法连接到数据库'}`);
    }

    const tables = await getDbTablesApi(formData.sourceConnectionId.toString(), formData.sourceSchemaName);
    const tableOptions = tables.map(table => ({
      key: table,
      label: table
    }));

    sourceTableList.value = tableOptions;

    // 更新缓存
    dbMetadataCache.set(cacheKey, tableOptions, CACHE_DURATION);
    frontendCacheManager.recordMiss();

    ElMessage.success(`表列表加载成功（共${tables.length}个表）`);
  } catch (error: any) {
    console.error("加载表列表失败:", error);

    // 如果是超时错误且重试次数少于2次，则自动重试
    if (error.message?.includes('timeout') && retryCount < 2) {
      ElMessage.warning(`加载超时，正在重试... (${retryCount + 1}/2)`);
      setTimeout(() => {
        loadSourceTables(retryCount + 1);
      }, 2000);
      return;
    }

    ElMessage.error(`加载表列表失败: ${error.message || '未知错误'}`);
  } finally {
    loadingTables.value = false;
  }
};

// 源数据库改变时清空表选择和缓存
const handleSourceChange = () => {
  formData.tables = [];
  sourceTableList.value = [];

  // 清除相关缓存
  if (formData.sourceConnectionId) {
    const cacheKey = CacheKeys.tables(formData.sourceConnectionId, formData.sourceSchemaName);
    dbMetadataCache.delete(cacheKey);
  }
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
    
    await deleteSyncTaskApi(row.id!.toString());
    ElMessage.success("删除成功");
    fetchTasks();
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.error || "删除失败");
    }
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
    
    await executeSyncTaskApi(row.id!.toString());
    ElMessage.success("任务已开始执行");
    
    // 更新任务状态为执行中
    row.status = 'RUNNING';
    row.progress = 0;
    
    // 开始轮询进度
    startProgressPolling();
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.error || "执行失败");
    }
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
    
    await stopSyncTaskApi(row.id!.toString());
    ElMessage.success("任务已停止");
    row.status = 'FAILED';
    stopProgressPolling();
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.error || "停止失败");
    }
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
    taskLogs.value = await getSyncTaskLogsApi(currentLogTaskId.value);
  } catch (error) {
    ElMessage.error("获取日志失败");
  }
};

// 开始进度轮询
const startProgressPolling = () => {
  progressTimer.value = setInterval(async () => {
    const runningTasks = taskList.value.filter(t => t.status === 'RUNNING');
    
    if (runningTasks.length === 0) {
      stopProgressPolling();
      return;
    }
    
    // 获取每个运行中任务的真实进度
    for (const task of runningTasks) {
      try {
        const progress = await getSyncTaskProgressApi(task.id!.toString());
        // 更新任务进度
        task.progress = progress.progress;
        task.completedTables = progress.completedTables;
        task.totalTables = progress.totalTables;
        task.status = progress.status;
        
        // 如果任务已完成或失败，停止轮询
        if (task.status === 'COMPLETED_SUCCESS' || task.status === 'FAILED') {
          stopProgressPolling();
          break;
        }
      } catch (error) {
        console.error('获取进度失败：', error);
      }
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
      await updateSyncTaskApi(formData.id!.toString(), formData);
      ElMessage.success("更新成功");
    } else {
      await createSyncTaskApi(formData);
      ElMessage.success("创建成功");
    }
    
    dialogVisible.value = false;
    fetchTasks();
  } catch (error: any) {
    ElMessage.error(error.response?.data?.error || "保存失败");
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
