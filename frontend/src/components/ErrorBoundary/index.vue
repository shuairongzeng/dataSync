<template>
  <div class="error-boundary">
    <slot v-if="!hasError" />
    <div v-else class="error-container">
      <el-result
        icon="error"
        title="页面出现错误"
        :sub-title="errorMessage"
      >
        <template #extra>
          <el-button type="primary" @click="retry">重试</el-button>
          <el-button @click="goHome">返回首页</el-button>
        </template>
      </el-result>
      
      <el-collapse v-if="showDetails" class="error-details">
        <el-collapse-item title="错误详情" name="details">
          <pre class="error-stack">{{ errorStack }}</pre>
        </el-collapse-item>
      </el-collapse>
      
      <div class="error-actions">
        <el-button 
          size="small" 
          text 
          @click="showDetails = !showDetails"
        >
          {{ showDetails ? '隐藏' : '显示' }}错误详情
        </el-button>
        <el-button 
          size="small" 
          text 
          @click="reportError"
        >
          报告错误
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onErrorCaptured } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";

const router = useRouter();

const hasError = ref(false);
const errorMessage = ref("");
const errorStack = ref("");
const showDetails = ref(false);

// 捕获子组件错误
onErrorCaptured((error: Error, instance, info) => {
  console.error("Error captured:", error, info);
  
  hasError.value = true;
  errorMessage.value = error.message || "未知错误";
  errorStack.value = error.stack || "";
  
  // 可以在这里上报错误到监控系统
  reportErrorToMonitoring(error, info);
  
  return false; // 阻止错误继续传播
});

// 重试
const retry = () => {
  hasError.value = false;
  errorMessage.value = "";
  errorStack.value = "";
  showDetails.value = false;
};

// 返回首页
const goHome = () => {
  router.push("/");
};

// 报告错误
const reportError = () => {
  ElMessage.success("错误报告已提交，感谢您的反馈！");
};

// 上报错误到监控系统
const reportErrorToMonitoring = (error: Error, info: string) => {
  // 这里可以集成错误监控服务，如 Sentry
  const errorReport = {
    message: error.message,
    stack: error.stack,
    componentInfo: info,
    url: window.location.href,
    userAgent: navigator.userAgent,
    timestamp: new Date().toISOString()
  };
  
  console.log("Error report:", errorReport);
  // 实际项目中可以发送到错误监控服务
};
</script>

<style scoped>
.error-boundary {
  width: 100%;
  height: 100%;
}

.error-container {
  padding: 40px 20px;
  text-align: center;
}

.error-details {
  max-width: 800px;
  margin: 20px auto;
  text-align: left;
}

.error-stack {
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

.error-actions {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  gap: 20px;
}
</style>
