<template>
  <teleport to="body">
    <transition name="loading-fade">
      <div v-if="visible" class="global-loading-overlay">
        <div class="loading-container">
          <div class="loading-spinner">
            <div class="spinner-ring"></div>
            <div class="spinner-ring"></div>
            <div class="spinner-ring"></div>
            <div class="spinner-ring"></div>
          </div>
          <div class="loading-text">{{ text }}</div>
          <div v-if="showProgress" class="loading-progress">
            <el-progress 
              :percentage="progress" 
              :show-text="false"
              :stroke-width="4"
            />
            <div class="progress-text">{{ progress }}%</div>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup lang="ts">
import { computed } from "vue";

interface Props {
  visible: boolean;
  text?: string;
  progress?: number;
  showProgress?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  text: "加载中...",
  progress: 0,
  showProgress: false
});

const visible = computed(() => props.visible);
const text = computed(() => props.text);
const progress = computed(() => props.progress);
const showProgress = computed(() => props.showProgress);
</script>

<style scoped>
.global-loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  backdrop-filter: blur(2px);
}

.loading-container {
  text-align: center;
  color: white;
}

.loading-spinner {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
}

.spinner-ring {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border: 4px solid transparent;
  border-top: 4px solid #409eff;
  border-radius: 50%;
  animation: spin 1.2s cubic-bezier(0.5, 0, 0.5, 1) infinite;
}

.spinner-ring:nth-child(1) {
  animation-delay: -0.45s;
}

.spinner-ring:nth-child(2) {
  animation-delay: -0.3s;
}

.spinner-ring:nth-child(3) {
  animation-delay: -0.15s;
}

.loading-text {
  font-size: 16px;
  margin-bottom: 20px;
  color: #ffffff;
}

.loading-progress {
  width: 200px;
  margin: 0 auto;
}

.progress-text {
  margin-top: 8px;
  font-size: 14px;
  color: #ffffff;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.loading-fade-enter-active,
.loading-fade-leave-active {
  transition: opacity 0.3s ease;
}

.loading-fade-enter-from,
.loading-fade-leave-to {
  opacity: 0;
}
</style>
