<template>
  <div class="main">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>系统日志管理</span>
          <div class="header-actions">
            <el-button size="small" @click="exportLogs">
              <el-icon><Download /></el-icon>
              导出日志
            </el-button>
            <el-button size="small" type="warning" @click="showClearDialog = true">
              <el-icon><Delete /></el-icon>
              清理日志
            </el-button>
            <el-button size="small" @click="refreshLogs" :loading="loading">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜索条件 -->
      <div class="search-form">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="日志级别:">
            <el-select v-model="searchForm.level" placeholder="全部" clearable>
              <el-option
                v-for="level in LOG_LEVELS"
                :key="level.value"
                :label="level.label"
                :value="level.value"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="模块:">
            <el-select v-model="searchForm.module" placeholder="全部" clearable>
              <el-option
                v-for="module in SYSTEM_MODULES"
                :key="module.value"
                :label="module.label"
                :value="module.value"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="时间范围:">
            <el-date-picker
              v-model="timeRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              @change="handleTimeRangeChange"
            />
          </el-form-item>
          
          <el-form-item label="关键词:">
            <el-input
              v-model="searchForm.keyword"
              placeholder="搜索日志内容"
              style="width: 200px"
              @keyup.enter="searchLogs"
            />
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="searchLogs">搜索</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 日志列表 -->
      <el-table :data="logList" style="width: 100%" v-loading="loading">
        <el-table-column prop="timestamp" label="时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.timestamp) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="level" label="级别" width="80">
          <template #default="{ row }">
            <el-tag :color="getLevelColor(row.level)" size="small">
              {{ row.level }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="module" label="模块" width="100">
          <template #default="{ row }">
            {{ getModuleLabel(row.module) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="message" label="消息" show-overflow-tooltip />
        
        <el-table-column prop="username" label="用户" width="100" />
        
        <el-table-column prop="ip" label="IP地址" width="120" />
        
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewLogDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 日志详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="日志详情"
      width="800px"
    >
      <div v-if="currentLog" class="log-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="时间">
            {{ formatTime(currentLog.timestamp) }}
          </el-descriptions-item>
          <el-descriptions-item label="级别">
            <el-tag :color="getLevelColor(currentLog.level)">
              {{ currentLog.level }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="模块">
            {{ getModuleLabel(currentLog.module) }}
          </el-descriptions-item>
          <el-descriptions-item label="用户">
            {{ currentLog.username || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="IP地址">
            {{ currentLog.ip || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="用户代理">
            {{ currentLog.userAgent || '-' }}
          </el-descriptions-item>
        </el-descriptions>
        
        <div class="mt-4">
          <h4>消息内容</h4>
          <el-input
            v-model="currentLog.message"
            type="textarea"
            :rows="3"
            readonly
          />
        </div>
        
        <div class="mt-4" v-if="currentLog.details">
          <h4>详细信息</h4>
          <el-input
            v-model="currentLog.details"
            type="textarea"
            :rows="6"
            readonly
          />
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 清理日志对话框 -->
    <el-dialog
      v-model="showClearDialog"
      title="清理日志"
      width="400px"
    >
      <div class="clear-form">
        <p>选择要清理的日志时间范围：</p>
        <el-form :model="clearForm" label-width="100px">
          <el-form-item label="清理策略:">
            <el-radio-group v-model="clearForm.strategy">
              <el-radio value="days">保留最近天数</el-radio>
              <el-radio value="date">清理指定日期前</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item v-if="clearForm.strategy === 'days'" label="保留天数:">
            <el-input-number 
              v-model="clearForm.days" 
              :min="1" 
              :max="365"
              style="width: 100%"
            />
          </el-form-item>
          
          <el-form-item v-if="clearForm.strategy === 'date'" label="截止日期:">
            <el-date-picker
              v-model="clearForm.beforeDate"
              type="date"
              placeholder="选择日期"
              style="width: 100%"
            />
          </el-form-item>
        </el-form>
        
        <el-alert
          title="警告：清理操作不可恢复，请谨慎操作！"
          type="warning"
          :closable="false"
        />
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showClearDialog = false">取消</el-button>
          <el-button type="danger" @click="confirmClearLogs" :loading="clearing">
            确认清理
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Download, Delete, Refresh } from "@element-plus/icons-vue";
import {
  type SystemLog,
  type LogQueryParams,
  LOG_LEVELS,
  SYSTEM_MODULES
} from "@/api/system";

defineOptions({
  name: "SystemLogs"
});

// 响应式数据
const loading = ref(false);
const clearing = ref(false);
const detailDialogVisible = ref(false);
const showClearDialog = ref(false);
const logList = ref<SystemLog[]>([]);
const currentLog = ref<SystemLog | null>(null);
const timeRange = ref<[string, string] | null>(null);

// 搜索表单
const searchForm = reactive<LogQueryParams>({
  level: "",
  module: "",
  keyword: ""
});

// 分页信息
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
});

// 清理表单
const clearForm = reactive({
  strategy: "days",
  days: 30,
  beforeDate: null
});

// 获取日志级别颜色
const getLevelColor = (level: string) => {
  const levelConfig = LOG_LEVELS.find(l => l.value === level);
  return levelConfig ? levelConfig.color : '#909399';
};

// 获取模块标签
const getModuleLabel = (module: string) => {
  const moduleConfig = SYSTEM_MODULES.find(m => m.value === module);
  return moduleConfig ? moduleConfig.label : module;
};

// 格式化时间
const formatTime = (timeStr: string) => {
  return new Date(timeStr).toLocaleString();
};

// 时间范围变化处理
const handleTimeRangeChange = (value: [string, string] | null) => {
  if (value) {
    searchForm.startTime = value[0];
    searchForm.endTime = value[1];
  } else {
    searchForm.startTime = undefined;
    searchForm.endTime = undefined;
  }
};

// 获取日志列表
const fetchLogs = async () => {
  loading.value = true;
  try {
    // 暂时使用模拟数据
    const mockLogs: SystemLog[] = [
      {
        id: "1",
        timestamp: new Date().toISOString(),
        level: "INFO",
        module: "AUTH",
        message: "用户登录成功",
        username: "admin",
        ip: "192.168.1.100",
        userAgent: "Mozilla/5.0..."
      },
      {
        id: "2",
        timestamp: new Date(Date.now() - 60000).toISOString(),
        level: "ERROR",
        module: "DATABASE",
        message: "数据库连接失败",
        details: "Connection timeout after 30 seconds",
        ip: "192.168.1.100"
      },
      {
        id: "3",
        timestamp: new Date(Date.now() - 120000).toISOString(),
        level: "WARN",
        module: "SYNC",
        message: "同步任务执行缓慢",
        username: "system",
        ip: "127.0.0.1"
      }
    ];
    
    logList.value = mockLogs;
    pagination.total = mockLogs.length;
  } catch (error) {
    ElMessage.error("获取日志列表失败");
  } finally {
    loading.value = false;
  }
};

// 搜索日志
const searchLogs = () => {
  pagination.page = 1;
  fetchLogs();
};

// 重置搜索
const resetSearch = () => {
  Object.assign(searchForm, {
    level: "",
    module: "",
    keyword: "",
    startTime: undefined,
    endTime: undefined
  });
  timeRange.value = null;
  searchLogs();
};

// 刷新日志
const refreshLogs = () => {
  fetchLogs();
};

// 查看日志详情
const viewLogDetail = (log: SystemLog) => {
  currentLog.value = log;
  detailDialogVisible.value = true;
};

// 导出日志
const exportLogs = async () => {
  try {
    // 模拟导出
    ElMessage.success("日志导出成功");
  } catch (error) {
    ElMessage.error("导出失败");
  }
};

// 确认清理日志
const confirmClearLogs = async () => {
  try {
    await ElMessageBox.confirm(
      "确定要清理日志吗？此操作不可恢复！",
      "确认清理",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }
    );
    
    clearing.value = true;
    
    // 模拟清理操作
    await new Promise(resolve => setTimeout(resolve, 2000));
    
    ElMessage.success("日志清理成功");
    showClearDialog.value = false;
    fetchLogs();
  } catch (error) {
    // 用户取消
  } finally {
    clearing.value = false;
  }
};

// 分页大小变化
const handleSizeChange = (size: number) => {
  pagination.size = size;
  fetchLogs();
};

// 当前页变化
const handleCurrentChange = (page: number) => {
  pagination.page = page;
  fetchLogs();
};

// 组件挂载时获取数据
onMounted(() => {
  fetchLogs();
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
  gap: 10px;
}

.search-form {
  background-color: #f5f7fa;
  padding: 20px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.log-detail h4 {
  margin-bottom: 10px;
  color: #303133;
}

.clear-form p {
  margin-bottom: 20px;
  color: #606266;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
