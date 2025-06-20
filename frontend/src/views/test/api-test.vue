<template>
  <div class="api-test-container">
    <el-card class="test-card">
      <template #header>
        <div class="card-header">
          <span>API连接测试</span>
          <el-button type="primary" @click="runAllTests">运行所有测试</el-button>
        </div>
      </template>

      <div class="test-section">
        <h3>环境信息</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="前端地址">
            {{ frontendUrl }}
          </el-descriptions-item>
          <el-descriptions-item label="后端API地址">
            {{ backendUrl }}
          </el-descriptions-item>
          <el-descriptions-item label="代理配置">
            {{ proxyDomain }}
          </el-descriptions-item>
          <el-descriptions-item label="当前环境">
            {{ currentEnv }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="test-section">
        <h3>API测试结果</h3>
        <el-table :data="testResults" style="width: 100%">
          <el-table-column prop="name" label="测试项" width="200" />
          <el-table-column prop="url" label="请求URL" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag 
                :type="row.status === 'success' ? 'success' : row.status === 'error' ? 'danger' : 'info'"
              >
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="responseTime" label="响应时间" width="100">
            <template #default="{ row }">
              {{ row.responseTime ? row.responseTime + 'ms' : '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button size="small" @click="runSingleTest(row)">
                {{ row.status === 'testing' ? '测试中...' : '重新测试' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="test-section" v-if="errorDetails">
        <h3>错误详情</h3>
        <el-alert
          :title="errorDetails.title"
          type="error"
          :description="errorDetails.message"
          show-icon
          :closable="false"
        />
        <el-collapse class="mt-4">
          <el-collapse-item title="详细错误信息" name="error">
            <pre class="error-detail">{{ errorDetails.detail }}</pre>
          </el-collapse-item>
        </el-collapse>
      </div>

      <div class="test-section">
        <h3>解决方案建议</h3>
        <el-alert
          title="如果测试失败，请检查以下几点："
          type="info"
          :closable="false"
        >
          <ul class="solution-list">
            <li>确保后端服务运行在 http://localhost:8080</li>
            <li>检查后端是否配置了CORS跨域支持</li>
            <li>确认前端代理配置是否正确</li>
            <li>检查防火墙是否阻止了端口访问</li>
            <li>尝试直接访问后端健康检查接口：
              <el-link type="primary" :href="healthCheckUrl" target="_blank">
                {{ healthCheckUrl }}
              </el-link>
            </li>
          </ul>
        </el-alert>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { healthCheckApi } from "@/api/user";

defineOptions({
  name: "ApiTest"
});

// 环境信息
const frontendUrl = ref(window.location.origin);
const backendUrl = ref(import.meta.env.VITE_BASE_API || "");
const proxyDomain = ref(import.meta.env.VITE_PROXY_DOMAIN || "");
const currentEnv = ref(import.meta.env.MODE);
const healthCheckUrl = ref(`${import.meta.env.VITE_PROXY_DOMAIN || 'http://localhost:8080'}/api/test/health`);

// 测试结果
const testResults = ref([
  {
    id: 1,
    name: "健康检查",
    url: "/api/test/health",
    status: "pending",
    responseTime: null,
    testFn: testHealthCheck
  },
  {
    id: 2,
    name: "公共接口",
    url: "/api/test/public",
    status: "pending", 
    responseTime: null,
    testFn: testPublicApi
  },
  {
    id: 3,
    name: "CORS预检",
    url: "/api/test/health",
    status: "pending",
    responseTime: null,
    testFn: testCorsPreflightRequest
  }
]);

// 错误详情
const errorDetails = ref(null);

// 获取状态文本
const getStatusText = (status: string) => {
  const statusMap = {
    pending: "待测试",
    testing: "测试中",
    success: "成功",
    error: "失败"
  };
  return statusMap[status] || status;
};

// 测试健康检查接口
async function testHealthCheck() {
  const startTime = Date.now();
  try {
    const response = await healthCheckApi();
    const responseTime = Date.now() - startTime;
    
    return {
      status: "success",
      responseTime,
      data: response
    };
  } catch (error) {
    const responseTime = Date.now() - startTime;
    throw {
      status: "error",
      responseTime,
      error
    };
  }
}

// 测试公共接口
async function testPublicApi() {
  const startTime = Date.now();
  try {
    const response = await fetch("/api/test/public");
    const responseTime = Date.now() - startTime;
    
    if (response.ok) {
      const data = await response.text();
      return {
        status: "success",
        responseTime,
        data
      };
    } else {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
  } catch (error) {
    const responseTime = Date.now() - startTime;
    throw {
      status: "error",
      responseTime,
      error
    };
  }
}

// 测试CORS预检请求
async function testCorsPreflightRequest() {
  const startTime = Date.now();
  try {
    const response = await fetch("/api/test/health", {
      method: "OPTIONS",
      headers: {
        "Access-Control-Request-Method": "GET",
        "Access-Control-Request-Headers": "Content-Type"
      }
    });
    const responseTime = Date.now() - startTime;
    
    return {
      status: "success",
      responseTime,
      data: {
        status: response.status,
        headers: Object.fromEntries(response.headers.entries())
      }
    };
  } catch (error) {
    const responseTime = Date.now() - startTime;
    throw {
      status: "error",
      responseTime,
      error
    };
  }
}

// 运行单个测试
const runSingleTest = async (testItem: any) => {
  testItem.status = "testing";
  testItem.responseTime = null;
  errorDetails.value = null;

  try {
    const result = await testItem.testFn();
    testItem.status = result.status;
    testItem.responseTime = result.responseTime;
    
    ElMessage.success(`${testItem.name} 测试成功`);
  } catch (error) {
    testItem.status = "error";
    testItem.responseTime = error.responseTime;
    
    // 设置错误详情
    errorDetails.value = {
      title: `${testItem.name} 测试失败`,
      message: error.error?.message || "未知错误",
      detail: JSON.stringify(error, null, 2)
    };
    
    ElMessage.error(`${testItem.name} 测试失败`);
  }
};

// 运行所有测试
const runAllTests = async () => {
  ElMessage.info("开始运行所有测试...");
  
  for (const testItem of testResults.value) {
    await runSingleTest(testItem);
    // 添加延迟避免请求过于频繁
    await new Promise(resolve => setTimeout(resolve, 500));
  }
  
  const successCount = testResults.value.filter(t => t.status === "success").length;
  const totalCount = testResults.value.length;
  
  if (successCount === totalCount) {
    ElMessage.success(`所有测试通过 (${successCount}/${totalCount})`);
  } else {
    ElMessage.warning(`部分测试失败 (${successCount}/${totalCount})`);
  }
};

// 组件挂载时自动运行测试
onMounted(() => {
  // 延迟1秒后自动运行测试
  setTimeout(() => {
    runAllTests();
  }, 1000);
});
</script>

<style scoped>
.api-test-container {
  padding: 20px;
}

.test-card {
  max-width: 1200px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.test-section {
  margin-bottom: 30px;
}

.test-section h3 {
  margin-bottom: 15px;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.solution-list {
  margin: 10px 0;
  padding-left: 20px;
}

.solution-list li {
  margin-bottom: 8px;
  line-height: 1.5;
}

.error-detail {
  background-color: #f5f5f5;
  padding: 15px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.4;
  overflow-x: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>
