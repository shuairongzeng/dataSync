<template>
  <div class="task-config-container">
    <h1>Task Configuration</h1>

    <!-- Form to Add/Edit Task -->
    <div class="task-form">
      <h3>{{ isEditing ? 'Edit Task' : 'Add New Task' }}</h3>
      <form @submit.prevent="handleSubmit">
        <div class="form-group">
          <label for="taskName">Task Name:</label>
          <input type="text" id="taskName" v-model="currentTask.name" required />
        </div>
        <div class="form-group">
          <label for="sourceSystem">Source System:</label>
          <input type="text" id="sourceSystem" v-model="currentTask.sourceSystem" />
        </div>
        <div class="form-group">
          <label for="destinationSystem">Destination System:</label>
          <input type="text" id="destinationSystem" v-model="currentTask.destinationSystem" />
        </div>
        <div class="form-group">
          <label for="description">Description:</label>
          <textarea id="description" v-model="currentTask.description"></textarea>
        </div>
        <div class="form-group">
          <label for="schedule">Schedule (e.g., Cron):</label>
          <input type="text" id="schedule" v-model="currentTask.schedule" />
        </div>
        <div class="form-group">
          <label>
            <input type="checkbox" v-model="currentTask.enabled" /> Enabled
          </label>
        </div>
        <button type="submit">{{ isEditing ? 'Update Task' : 'Add Task' }}</button>
        <button type="button" v-if="isEditing" @click="cancelEdit">Cancel Edit</button>
      </form>
    </div>

    <hr />

    <!-- List of Existing Tasks -->
    <div class="task-list">
      <h3>Existing Tasks</h3>
      <ul v-if="tasks.length > 0">
        <li v-for="task in tasks" :key="task.id">
          <strong>{{ task.name }}</strong> ({{ task.enabled ? 'Enabled' : 'Disabled' }})
          <p><small>Source: {{ task.sourceSystem }} | Dest: {{ task.destinationSystem }}</small></p>
          <p><small>Schedule: {{ task.schedule }}</small></p>
          <button @click="editTask(task)">Edit</button>
          <button @click="deleteTask(task.id)">Delete</button>
        </li>
      </ul>
      <p v-else>No tasks configured yet.</p>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import taskService from '@/services/taskService'; // We'll create this next

const tasks = ref([]);
const isEditing = ref(false);
const currentTask = ref({
  id: null,
  name: '',
  sourceSystem: '',
  destinationSystem: '',
  description: '',
  schedule: '',
  enabled: true,
});

const resetCurrentTask = () => {
  isEditing.value = false;
  currentTask.value = {
    id: null,
    name: '',
    sourceSystem: '',
    destinationSystem: '',
    description: '',
    schedule: '',
    enabled: true,
  };
};

const fetchTasks = async () => {
  try {
    tasks.value = await taskService.getTasks();
  } catch (error) {
    console.error('Error fetching tasks:', error);
  }
};

const handleSubmit = async () => {
  if (isEditing.value) {
    try {
      const updatedTask = await taskService.updateTask(currentTask.value.id, currentTask.value);
      const index = tasks.value.findIndex(t => t.id === updatedTask.id);
      if (index !== -1) {
        tasks.value[index] = updatedTask;
      }
    } catch (error) {
      console.error('Error updating task:', error);
    }
  } else {
    try {
      const newTask = await taskService.createTask(currentTask.value);
      tasks.value.push(newTask);
    } catch (error) {
      console.error('Error creating task:', error);
    }
  }
  resetCurrentTask();
};

const editTask = (task) => {
  isEditing.value = true;
  currentTask.value = { ...task };
};

const cancelEdit = () => {
  resetCurrentTask();
};

const deleteTask = async (taskId) => {
  if (confirm('Are you sure you want to delete this task?')) {
    try {
      await taskService.deleteTask(taskId);
      tasks.value = tasks.value.filter(t => t.id !== taskId);
    } catch (error) {
      console.error('Error deleting task:', error);
    }
  }
};

onMounted(() => {
  fetchTasks();
});
</script>

<style scoped>
.task-config-container {
  padding: 20px;
}
.task-form, .task-list {
  margin-bottom: 30px;
  padding: 15px;
  border: 1px solid #eee;
  border-radius: 4px;
}
.form-group {
  margin-bottom: 10px;
}
label {
  display: block;
  margin-bottom: 5px;
}
input[type="text"],
textarea {
  width: 100%;
  padding: 8px;
  box-sizing: border-box;
}
textarea {
  min-height: 80px;
}
button {
  margin-right: 10px;
}
ul {
  list-style-type: none;
  padding: 0;
}
li {
  padding: 10px;
  border-bottom: 1px solid #f0f0f0;
}
li:last-child {
  border-bottom: none;
}
</style>
