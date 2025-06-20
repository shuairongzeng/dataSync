<template>
  <div class="main">
    <!-- 系统状态概览 -->
    <el-row :gutter="20" class="mb-4">
      <el-col :span="6">
        <el-card class="status-card">
          <div class="status-item">
            <div class="status-icon success">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="status-content">
              <div class="status-title">系统状态</div>
              <div class="status-value">{{ systemStatus.status }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="status-card">
          <div class="status-item">
            <div class="status-icon info">
              <el-icon><Connection /></el-icon>
            </div>
            <div class="status-content">
              <div class="status-title">活跃连接</div>
              <div class="status-value">{{ systemStatus.activeConnections }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="status-card">
          <div class="status-item">
            <div class="status-icon warning">
              <el-icon><Timer /></el-icon>
            </div>
            <div class="status-content">
              <div class="status-title">运行中任务</div>
              <div class="status-value">{{ systemStatus.runningTasks }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="status-card">
          <div class="status-item">
            <div class="status-icon primary">
              <el-icon><DataAnalysis /></el-icon>
            </div>
            <div class="status-content">
              <div class="status-title">今日同步量</div>
              <div class="status-value">{{ formatNumber(systemStatus.todaySync) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <!-- 左侧：实时任务监控 -->
      <el-col :span="16">
        <el-card class="monitor-card">
          <template #header>
            <div class="card-header">
              <span>实时任务监控</span>
              <div class="header-actions">
                <el-switch
                  v-model="autoRefresh"
                  active-text="自动刷新"
                  @change="toggleAutoRefresh"
                />
                <el-button size="small" @click="refreshTasks" :loading="loading">
                  <el-icon><Refresh /></el-icon>
                  刷新
                </el-button>
              </div>
            </div>
          </template>

          <!-- 任务列表 -->
          <div class="task-list">
            <div 
              v-for="task in runningTasks" 
              :key="task.id"
              class="task-item"
            >
              <div class="task-header">
                <div class="task-info">
                  <h4 class="task-name">{{ task.name }}</h4>
                  <el-tag :type="getStatusTagType(task.status)" size="small">
                    {{ getStatusText(task.status) }}
                  </el-tag>
                </div>
                <div class="task-time">
                  开始时间: {{ formatTime(task.startTime) }}
                </div>
              </div>
              
              <div class="task-progress">
                <div class="progress-info">
                  <span>总体进度: {{ task.completedTables }}/{{ task.totalTables }} 表</span>
                  <span>{{ task.progress }}%</span>
                </div>
                <el-progress 
                  :percentage="task.progress" 
                  :status="task.progress === 100 ? 'success' : ''"
                  :stroke-width="8"
                />
              </div>

              <div class="task-details" v-if="task.currentTable">
                <div class="current-table">
                  <span class="label">当前表:</span>
                  <span class="value">{{ task.currentTable }}</span>
                </div>
                <div class="table-progress" v-if="task.tableProgress">
                  <span class="label">表进度:</span>
                  <el-progress 
                    :percentage="task.tableProgress" 
                    :show-text="false"
                    :stroke-width="4"
                    class="inline-progress"
                  />
                  <span class="value">{{ task.processedRecords }}/{{ task.totalRecords }} 条</span>
                </div>
              </div>

              <div class="task-actions">
                <el-button size="small" @click="viewTaskLogs(task)">查看日志</el-button>
                <el-button 
                  size="small" 
                  type="warning" 
                  @click="stopTask(task)"
                  v-if="task.status === 'RUNNING'"
                >
                  停止任务
                </el-button>
              </div>
            </div>

            <el-empty 
              v-if="runningTasks.length === 0" 
              description="暂无运行中的任务"
              :image-size="100"
            />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：系统监控 -->
      <el-col :span="8">
        <el-card class="system-monitor-card">
          <template #header>
            <span>系统监控</span>
          </template>

          <!-- 性能指标 -->
          <div class="performance-metrics">
            <div class="metric-item">
              <div class="metric-label">CPU使用率</div>
              <el-progress 
                :percentage="systemMetrics.cpuUsage" 
                :color="getProgressColor(systemMetrics.cpuUsage)"
              />
            </div>
            
            <div class="metric-item">
              <div class="metric-label">内存使用率</div>
              <el-progress 
                :percentage="systemMetrics.memoryUsage" 
                :color="getProgressColor(systemMetrics.memoryUsage)"
              />
            </div>
            
            <div class="metric-item">
              <div class="metric-label">磁盘使用率</div>
              <el-progress 
                :percentage="systemMetrics.diskUsage" 
                :color="getProgressColor(systemMetrics.diskUsage)"
              />
            </div>
          </div>

          <!-- 连接状态 -->
          <div class="connection-status mt-4">
            <h4>数据库连接状态</h4>
            <div class="connection-list">
              <div 
                v-for="conn in connectionStatus" 
                :key="conn.id"
                class="connection-item"
              >
                <div class="connection-info">
                  <span class="connection-name">{{ conn.name }}</span>
                  <el-tag 
                    :type="conn.status === 'connected' ? 'success' : 'danger'" 
                    size="small"
                  >
                    {{ conn.status === 'connected' ? '已连接' : '断开' }}
                  </el-tag>
                </div>
                <div class="connection-details">
                  <span class="text-sm text-gray-500">
                    {{ conn.dbType }} - {{ conn.host }}:{{ conn.port }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 最近日志 -->
        <el-card class="log-card mt-4">
          <template #header>
            <span>最近日志</span>
          </template>
          
          <div class="log-list">
            <div 
              v-for="log in recentLogs" 
              :key="log.id"
              class="log-item"
              :class="log.level"
            >
              <div class="log-time">{{ formatTime(log.timestamp) }}</div>
              <div class="log-message">{{ log.message }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 任务日志对话框 -->
    <el-dialog
      v-model="logDialogVisible"
      title="任务执行日志"
      width="800px"
    >
      <div class="log-container">
        <el-scrollbar height="400px">
          <pre class="log-content">{{ currentTaskLogs.join('\n') }}</pre>
        </el-scrollbar>
      </div>
      <template #footer>
        <el-button @click="logDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="refreshCurrentTaskLogs">刷新</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  CircleCheck,
  Connection,
  Timer,
  DataAnalysis,
  Refresh
} from "@element-plus/icons-vue";

defineOptions({
  name: "DatabaseMonitor"
});

// 响应式数据
const loading = ref(false);
const autoRefresh = ref(true);
const logDialogVisible = ref(false);
const currentTaskId = ref("");
const currentTaskLogs = ref<string[]>([]);
const refreshTimer = ref<NodeJS.Timeout>();

// 系统状态
const systemStatus = reactive({
  status: "正常",
  activeConnections: 5,
  runningTasks: 2,
  todaySync: 1250000
});

// 系统性能指标
const systemMetrics = reactive({
  cpuUsage: 35,
  memoryUsage: 68,
  diskUsage: 45
});

// 运行中的任务
const runningTasks = ref([
  {
    id: "1",
    name: "用户数据同步",
    status: "RUNNING",
    progress: 65,
    totalTables: 5,
    completedTables: 3,
    currentTable: "user_profiles",
    tableProgress: 40,
    processedRecords: 4000,
    totalRecords: 10000,
    startTime: new Date(Date.now() - 300000).toISOString()
  },
  {
    id: "2",
    name: "订单数据备份",
    status: "RUNNING",
    progress: 25,
    totalTables: 8,
    completedTables: 2,
    currentTable: "orders",
    tableProgress: 15,
    processedRecords: 1500,
    totalRecords: 10000,
    startTime: new Date(Date.now() - 180000).toISOString()
  }
]);

// 连接状态
const connectionStatus = ref([
  {
    id: "1",
    name: "本地MySQL",
    dbType: "MySQL",
    host: "localhost",
    port: 3306,
    status: "connected"
  },
  {
    id: "2",
    name: "生产PostgreSQL",
    dbType: "PostgreSQL",
    host: "prod-server",
    port: 5432,
    status: "connected"
  },
  {
    id: "3",
    name: "备份Oracle",
    dbType: "Oracle",
    host: "backup-server",
    port: 1521,
    status: "disconnected"
  }
]);

// 最近日志
const recentLogs = ref([
  {
    id: "1",
    timestamp: new Date().toISOString(),
    level: "info",
    message: "任务 '用户数据同步' 开始执行表 user_profiles"
  },
  {
    id: "2",
    timestamp: new Date(Date.now() - 60000).toISOString(),
    level: "success",
    message: "表 users 同步完成，共处理 10000 条记录"
  },
  {
    id: "3",
    timestamp: new Date(Date.now() - 120000).toISOString(),
    level: "warning",
    message: "连接 '备份Oracle' 出现超时，正在重试"
  },
  {
    id: "4",
    timestamp: new Date(Date.now() - 180000).toISOString(),
    level: "info",
    message: "任务 '订单数据备份' 开始执行"
  }
]);

// 获取状态标签样式
const getStatusTagType = (status: string) => {
  const statusMap = {
    'RUNNING': 'warning',
    'COMPLETED_SUCCESS': 'success',
    'FAILED': 'danger',
    'PENDING': ''
  };
  return statusMap[status] || '';
};

// 获取状态文本
const getStatusText = (status: string) => {
  const statusMap = {
    'RUNNING': '执行中',
    'COMPLETED_SUCCESS': '已完成',
    'FAILED': '失败',
    'PENDING': '等待中'
  };
  return statusMap[status] || status;
};

// 获取进度条颜色
const getProgressColor = (percentage: number) => {
  if (percentage < 50) return '#67c23a';
  if (percentage < 80) return '#e6a23c';
  return '#f56c6c';
};

// 格式化数字
const formatNumber = (num: number) => {
  if (num >= 1000000) {
    return (num / 1000000).toFixed(1) + 'M';
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'K';
  }
  return num.toString();
};

// 格式化时间
const formatTime = (timeStr: string) => {
  return new Date(timeStr).toLocaleString();
};

// 刷新任务数据
const refreshTasks = async () => {
  loading.value = true;
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 500));
    
    // 模拟进度更新
    runningTasks.value.forEach(task => {
      if (task.status === 'RUNNING' && task.progress < 100) {
        task.progress = Math.min(task.progress + Math.random() * 10, 100);
        task.completedTables = Math.floor((task.progress / 100) * task.totalTables);
        
        if (task.tableProgress !== undefined) {
          task.tableProgress = Math.min(task.tableProgress + Math.random() * 15, 100);
          task.processedRecords = Math.floor((task.tableProgress / 100) * task.totalRecords);
        }
        
        if (task.progress >= 100) {
          task.status = 'COMPLETED_SUCCESS';
        }
      }
    });
    
    // 更新系统指标
    systemMetrics.cpuUsage = Math.max(10, Math.min(90, systemMetrics.cpuUsage + (Math.random() - 0.5) * 10));
    systemMetrics.memoryUsage = Math.max(20, Math.min(95, systemMetrics.memoryUsage + (Math.random() - 0.5) * 5));
    
  } catch (error) {
    ElMessage.error("刷新数据失败");
  } finally {
    loading.value = false;
  }
};

// 切换自动刷新
const toggleAutoRefresh = (enabled: boolean) => {
  if (enabled) {
    startAutoRefresh();
  } else {
    stopAutoRefresh();
  }
};

// 开始自动刷新
const startAutoRefresh = () => {
  refreshTimer.value = setInterval(() => {
    refreshTasks();
  }, 3000);
};

// 停止自动刷新
const stopAutoRefresh = () => {
  if (refreshTimer.value) {
    clearInterval(refreshTimer.value);
    refreshTimer.value = undefined;
  }
};

// 查看任务日志
const viewTaskLogs = (task: any) => {
  currentTaskId.value = task.id;
  refreshCurrentTaskLogs();
  logDialogVisible.value = true;
};

// 刷新当前任务日志
const refreshCurrentTaskLogs = () => {
  // 模拟获取任务日志
  currentTaskLogs.value = [
    `[${new Date().toLocaleString()}] 任务开始执行`,
    `[${new Date().toLocaleString()}] 连接源数据库成功`,
    `[${new Date().toLocaleString()}] 连接目标数据库成功`,
    `[${new Date().toLocaleString()}] 开始同步表: users`,
    `[${new Date().toLocaleString()}] 表 users 同步完成，共 10000 条记录`,
    `[${new Date().toLocaleString()}] 开始同步表: user_profiles`,
    `[${new Date().toLocaleString()}] 正在处理表 user_profiles，进度 40%`
  ];
};

// 停止任务
const stopTask = async (task: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要停止任务 "${task.name}" 吗？`,
      "确认停止",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }
    );
    
    task.status = 'FAILED';
    ElMessage.success("任务已停止");
  } catch (error) {
    // 用户取消
  }
};

// 组件挂载时开始监控
onMounted(() => {
  refreshTasks();
  if (autoRefresh.value) {
    startAutoRefresh();
  }
});

// 组件卸载时清理定时器
onUnmounted(() => {
  stopAutoRefresh();
});
</script>

<style scoped>
.main {
  padding: 20px;
}

.status-card {
  height: 100px;
}

.status-item {
  display: flex;
  align-items: center;
  height: 100%;
}

.status-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  font-size: 24px;
  color: white;
}

.status-icon.success {
  background-color: #67c23a;
}

.status-icon.info {
  background-color: #409eff;
}

.status-icon.warning {
  background-color: #e6a23c;
}

.status-icon.primary {
  background-color: #606266;
}

.status-content {
  flex: 1;
}

.status-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 5px;
}

.status-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.monitor-card {
  min-height: 600px;
}

.task-list {
  max-height: 500px;
  overflow-y: auto;
}

.task-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 15px;
  background-color: #fafafa;
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.task-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.task-name {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.task-time {
  font-size: 12px;
  color: #909399;
}

.task-progress {
  margin-bottom: 15px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
}

.task-details {
  margin-bottom: 15px;
  font-size: 14px;
}

.current-table {
  margin-bottom: 8px;
}

.table-progress {
  display: flex;
  align-items: center;
  gap: 10px;
}

.label {
  color: #909399;
  min-width: 60px;
}

.value {
  color: #303133;
  font-weight: 500;
}

.inline-progress {
  flex: 1;
  max-width: 200px;
}

.task-actions {
  display: flex;
  gap: 10px;
}

.system-monitor-card {
  height: 600px;
}

.performance-metrics {
  margin-bottom: 20px;
}

.metric-item {
  margin-bottom: 20px;
}

.metric-label {
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
}

.connection-status h4 {
  margin-bottom: 15px;
  font-size: 16px;
  color: #303133;
}

.connection-list {
  max-height: 200px;
  overflow-y: auto;
}

.connection-item {
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
}

.connection-item:last-child {
  border-bottom: none;
}

.connection-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
}

.connection-name {
  font-weight: 500;
  color: #303133;
}

.connection-details {
  font-size: 12px;
  color: #909399;
}

.log-card {
  height: 300px;
}

.log-list {
  max-height: 200px;
  overflow-y: auto;
}

.log-item {
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 12px;
}

.log-item:last-child {
  border-bottom: none;
}

.log-item.info {
  color: #409eff;
}

.log-item.success {
  color: #67c23a;
}

.log-item.warning {
  color: #e6a23c;
}

.log-item.error {
  color: #f56c6c;
}

.log-time {
  color: #909399;
  margin-bottom: 2px;
}

.log-message {
  color: #303133;
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
