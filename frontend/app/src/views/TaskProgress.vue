<template>
  <div class="task-progress-container">
    <h1>Task Progress</h1>

    <div v-if="loading" class="loading">Loading task progress...</div>
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="!loading && !error && tasksProgress.length === 0" class="no-tasks">
      No task progress information available.
    </div>

    <div v-else-if="!loading && !error" class="progress-list">
      <div v-for="task in tasksProgress" :key="task.id" class="task-item">
        <h3>{{ task.name }} (ID: {{ task.id }})</h3>
        <p><strong>Status:</strong> <span :class="statusClass(task.status)">{{ task.status }}</span></p>
        <div v-if="task.status === 'Running' || task.status === 'Processing'">
          <p><strong>Progress:</strong> {{ task.progressPercentage }}%</p>
          <div class="progress-bar-container">
            <div class="progress-bar" :style="{ width: task.progressPercentage + '%' }"></div>
          </div>
        </div>
        <p v-if="task.startTime"><strong>Start Time:</strong> {{ new Date(task.startTime).toLocaleString() }}</p>
        <p v-if="task.endTime"><strong>End Time:</strong> {{ new Date(task.endTime).toLocaleString() }}</p>
        <p v-if="task.message"><strong>Details:</strong> {{ task.message }}</p>
        <button @click="refreshTaskProgress(task.id)" :disabled="task.isLoadingDetails">
          {{ task.isLoadingDetails ? 'Refreshing...' : 'Refresh Details' }}
        </button>
      </div>
    </div>
    <button @click="fetchAllTaskProgress" :disabled="loadingAll">
      {{ loadingAll ? 'Refreshing All...' : 'Refresh All Progress' }}
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import taskService from '@/services/taskService';

const tasksProgress = ref([]);
const loading = ref(true);
const loadingAll = ref(false);
const error = ref('');
let pollingInterval = null;

const statusClass = (status) => {
  if (!status) return '';
  return `status-${status.toLowerCase().replace(' ', '-')}`;
};

const fetchAllTaskProgress = async () => {
  loadingAll.value = true;
  error.value = '';
  try {
    const progressData = await taskService.getAllTasksProgress();
    tasksProgress.value = progressData.map(p => ({ ...p, isLoadingDetails: false }));
  } catch (err) {
    console.error('Error fetching all task progress:', err);
    error.value = 'Failed to load task progress.';
  } finally {
    loading.value = false;
    loadingAll.value = false;
  }
};

const refreshTaskProgress = async (taskId) => {
  const task = tasksProgress.value.find(t => t.id === taskId);
  if (task) {
    task.isLoadingDetails = true;
  }
  try {
    const singleProgress = await taskService.getTaskProgress(taskId);
    const index = tasksProgress.value.findIndex(t => t.id === taskId);
    if (index !== -1) {
      tasksProgress.value[index] = { ...singleProgress, isLoadingDetails: false };
    } else {
      tasksProgress.value.push({ ...singleProgress, isLoadingDetails: false });
    }
  } catch (err) {
    console.error(`Error refreshing progress for task ${taskId}:`, err);
  } finally {
    if (task) {
      task.isLoadingDetails = false;
    }
  }
};

onMounted(() => {
  fetchAllTaskProgress();
  // pollingInterval = setInterval(fetchAllTaskProgress, 30000);
});

onUnmounted(() => {
  if (pollingInterval) {
    clearInterval(pollingInterval);
  }
});

</script>

<style scoped>
.task-progress-container {
  padding: 20px;
}
.loading, .error, .no-tasks {
  text-align: center;
  padding: 20px;
  font-size: 1.2em;
}
.error {
  color: red;
}
.progress-list {
  margin-top: 20px;
}
.task-item {
  border: 1px solid #eee;
  border-radius: 5px;
  padding: 15px;
  margin-bottom: 15px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}
.task-item h3 {
  margin-top: 0;
}
.status-running, .status-processing { color: #007bff; font-weight: bold; }
.status-completed { color: #28a745; font-weight: bold; }
.status-failed { color: #dc3545; font-weight: bold; }
.status-pending { color: #ffc107; font-weight: bold; }

.progress-bar-container {
  width: 100%;
  background-color: #e9ecef;
  border-radius: .25rem;
  margin-top: 5px;
  margin-bottom: 10px;
}
.progress-bar {
  height: 20px;
  background-color: #007bff;
  border-radius: .25rem;
  text-align: center;
  color: white;
  line-height: 20px;
}
button {
  margin-top: 5px;
  margin-right: 10px;
}
</style>
