// Mock implementation for taskService.js

let mockTasks = [
  { id: '1', name: 'Sync Customer Data', sourceSystem: 'CRM', destinationSystem: 'ERP', description: 'Syncs new customers daily', schedule: '0 0 * * *', enabled: true },
  { id: '2', name: 'Product Catalog Update', sourceSystem: 'PIM', destinationSystem: 'Webshop', description: 'Updates product info every hour', schedule: '0 * * * *', enabled: false },
];
let nextId = 3;

class TaskService {
  async getTasks() {
    console.log('TaskService: Fetching tasks (mock)');
    return new Promise(resolve => {
      setTimeout(() => resolve([...mockTasks]), 300);
    });
  }

  async getTask(id) {
    console.log(`TaskService: Fetching task ${id} (mock)`);
    return new Promise(resolve => {
      const task = mockTasks.find(t => t.id === id);
      setTimeout(() => resolve(task ? {...task} : undefined), 100);
    });
  }

  async createTask(taskData) {
    console.log('TaskService: Creating task (mock)', taskData);
    return new Promise(resolve => {
      const newTask = { ...taskData, id: String(nextId++) };
      mockTasks.push(newTask);
      setTimeout(() => resolve({...newTask}), 100);
    });
  }

  async updateTask(id, taskData) {
    console.log(`TaskService: Updating task ${id} (mock)`, taskData);
    return new Promise((resolve, reject) => {
      const index = mockTasks.findIndex(t => t.id === id);
      if (index !== -1) {
        mockTasks[index] = { ...mockTasks[index], ...taskData };
        setTimeout(() => resolve({...mockTasks[index]}), 100);
      } else {
        setTimeout(() => reject(new Error('Task not found')), 100);
      }
    });
  }

  async deleteTask(id) {
    console.log(`TaskService: Deleting task ${id} (mock)`);
    return new Promise(resolve => {
      mockTasks = mockTasks.filter(t => t.id !== id);
      setTimeout(() => resolve(), 100);
    });
  }
}


  // Mock data for task progress
  getMockProgressData() {
    // Correlate with mockTasks if possible, or create separate
    return [
      { id: '1', name: 'Sync Customer Data', status: 'Completed', progressPercentage: 100, startTime: new Date(Date.now() - 3600000).toISOString(), endTime: new Date(Date.now() - 3000000).toISOString(), message: 'Processed 1050 records.' },
      { id: '2', name: 'Product Catalog Update', status: 'Running', progressPercentage: 65, startTime: new Date(Date.now() - 600000).toISOString(), message: 'Currently processing batch 7 of 10.' },
      { id: '3', name: 'Inventory Sync', status: 'Failed', progressPercentage: 30, startTime: new Date(Date.now() - 7200000).toISOString(), endTime: new Date(Date.now() - 7000000).toISOString(), message: 'Error connecting to source API.' },
      { id: '4', name: 'Order Fulfillment Update', status: 'Pending', progressPercentage: 0, message: 'Scheduled to run in 10 minutes.' },
    ];
  }

  async getAllTasksProgress() {
    console.log('TaskService: Fetching all tasks progress (mock)');
    return new Promise(resolve => {
      setTimeout(() => resolve(this.getMockProgressData()), 500);
    });
  }

  async getTaskProgress(taskId) {
    console.log(`TaskService: Fetching progress for task ${taskId} (mock)`);
    return new Promise(resolve => {
      const allProgress = this.getMockProgressData();
      const progress = allProgress.find(p => p.id === taskId);
      setTimeout(() => resolve(progress || { id: taskId, name: 'Unknown Task', status: 'Error', message: 'Progress not found.' }), 300);
    });
  }
}

export default new TaskService();
