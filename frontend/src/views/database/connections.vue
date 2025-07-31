<template>
  <div class="main">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>数据库连接管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增连接
          </el-button>
        </div>
      </template>

      <!-- 连接列表 -->
      <el-table :data="connectionList" style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="连接名称" width="150" />
        <el-table-column prop="dbType" label="数据库类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getDbTypeTagType(row.dbType)">
              {{ getDbTypeLabel(row.dbType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="host" label="主机地址" width="150" />
        <el-table-column prop="port" label="端口" width="80" />
        <el-table-column prop="database" label="数据库名" width="120" />
        <el-table-column prop="username" label="用户名" width="100" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleTest(row)">测试连接</el-button>
            <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑连接' : '新增连接'"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="连接名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入连接名称" />
        </el-form-item>
        
        <el-form-item label="数据库类型" prop="dbType">
          <el-select 
            v-model="formData.dbType" 
            placeholder="请选择数据库类型"
            @change="handleDbTypeChange"
          >
            <el-option
              v-for="item in DB_TYPES"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="主机地址" prop="host">
              <el-input v-model="formData.host" placeholder="请输入主机地址" />
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="端口" prop="port">
              <el-input-number 
                v-model="formData.port" 
                :min="1" 
                :max="65535" 
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="数据库名" prop="database">
          <el-input v-model="formData.database" placeholder="请输入数据库名" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="formData.username" placeholder="请输入用户名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码" prop="password">
              <el-input 
                v-model="formData.password" 
                type="password" 
                placeholder="请输入密码"
                show-password
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Schema" prop="schema">
          <el-input v-model="formData.schema" placeholder="可选，默认为空" />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input 
            v-model="formData.description" 
            type="textarea" 
            :rows="3"
            placeholder="请输入连接描述"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleTestConnection" :loading="testLoading">
            测试连接
          </el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import {
  type DbConfig,
  type DbTestRequest,
  DB_TYPES,
  getDbConnectionsApi,
  createDbConnectionApi,
  updateDbConnectionApi,
  deleteDbConnectionApi,
  testDbConnectionApi
} from "@/api/database";

defineOptions({
  name: "DatabaseConnections"
});

// 响应式数据
const loading = ref(false);
const dialogVisible = ref(false);
const isEdit = ref(false);
const testLoading = ref(false);
const submitLoading = ref(false);
const connectionList = ref<DbConfig[]>([]);
const formRef = ref<FormInstance>();

// 表单数据
const formData = reactive<DbConfig>({
  name: "",
  dbType: "",
  host: "",
  port: 3306,
  database: "",
  username: "",
  password: "",
  schema: "",
  description: ""
});

// 表单验证规则
const formRules: FormRules = {
  name: [{ required: true, message: "请输入连接名称", trigger: "blur" }],
  dbType: [{ required: true, message: "请选择数据库类型", trigger: "change" }],
  host: [{ required: true, message: "请输入主机地址", trigger: "blur" }],
  port: [{ required: true, message: "请输入端口号", trigger: "blur" }],
  database: [{ required: true, message: "请输入数据库名", trigger: "blur" }],
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }]
};

// 获取数据库类型标签样式
const getDbTypeTagType = (dbType: string) => {
  const typeMap = {
    mysql: "success",
    postgresql: "primary",
    oracle: "warning",
    sqlserver: "info",
    dameng: "danger",
    vastbase: ""
  };
  return typeMap[dbType] || "";
};

// 获取数据库类型标签文本
const getDbTypeLabel = (dbType: string) => {
  const type = DB_TYPES.find(t => t.value === dbType);
  return type ? type.label : dbType;
};

// 数据库类型改变时自动设置默认端口
const handleDbTypeChange = (dbType: string) => {
  const type = DB_TYPES.find(t => t.value === dbType);
  if (type) {
    formData.port = type.port;
  }
};

// 获取连接列表
const fetchConnections = async () => {
  loading.value = true;
  try {
    connectionList.value = await getDbConnectionsApi();
  } catch (error) {
    ElMessage.error("获取连接列表失败");
  } finally {
    loading.value = false;
  }
};

// 新增连接
const handleAdd = () => {
  isEdit.value = false;
  resetForm();
  dialogVisible.value = true;
};

// 编辑连接
const handleEdit = (row: DbConfig) => {
  isEdit.value = true;
  Object.assign(formData, row);
  dialogVisible.value = true;
};

// 删除连接
const handleDelete = async (row: DbConfig) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除连接 "${row.name}" 吗？`,
      "确认删除",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }
    );
    
    // 调用删除 API
    await deleteDbConnectionApi(row.id!.toString());
    ElMessage.success("删除成功");
    fetchConnections();
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.error || "删除失败");
    }
  }
};

// 测试连接
const handleTest = async (row: DbConfig) => {
  testLoading.value = true;
  try {
    const testData: DbTestRequest = {
      dbType: row.dbType,
      host: row.host,
      port: row.port,
      database: row.database,
      username: row.username,
      password: row.password,
      schema: row.schema
    };
    
    const result = await testDbConnectionApi(testData);
    if (result.success) {
      ElMessage.success(`连接测试成功 (${result.connectionTime}ms)`);
    } else {
      ElMessage.error(result.message);
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || "连接测试失败");
  } finally {
    testLoading.value = false;
  }
};

// 测试当前表单的连接
const handleTestConnection = async () => {
  if (!formRef.value) return;
  
  try {
    await formRef.value.validate();
    testLoading.value = true;
    
    const testData: DbTestRequest = {
      dbType: formData.dbType,
      host: formData.host,
      port: formData.port,
      database: formData.database,
      username: formData.username,
      password: formData.password,
      schema: formData.schema
    };
    
    const result = await testDbConnectionApi(testData);
    if (result.success) {
      ElMessage.success(`连接测试成功 (${result.connectionTime}ms)`);
    } else {
      ElMessage.error(result.message);
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || "请先完善连接信息");
  } finally {
    testLoading.value = false;
  }
};

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return;
  
  try {
    await formRef.value.validate();
    submitLoading.value = true;
    
    if (isEdit.value) {
      // 更新连接
      await updateDbConnectionApi(formData.id!.toString(), formData);
      ElMessage.success("更新成功");
    } else {
      // 创建连接
      await createDbConnectionApi(formData);
      ElMessage.success("创建成功");
    }
    
    dialogVisible.value = false;
    fetchConnections();
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
    dbType: "",
    host: "",
    port: 3306,
    database: "",
    username: "",
    password: "",
    schema: "",
    description: ""
  });
  formRef.value?.clearValidate();
};

// 对话框关闭处理
const handleDialogClose = () => {
  resetForm();
};

// 组件挂载时获取数据
onMounted(() => {
  fetchConnections();
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
</style>
