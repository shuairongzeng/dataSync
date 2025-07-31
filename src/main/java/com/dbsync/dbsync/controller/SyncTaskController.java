package com.dbsync.dbsync.controller;

import com.dbsync.dbsync.model.SyncTask;
import com.dbsync.dbsync.model.SyncTaskRequest;
import com.dbsync.dbsync.service.SyncTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 同步任务管理控制器
 */
@RestController
@RequestMapping("/api/sync")
@CrossOrigin(origins = "*")
public class SyncTaskController {

    @Autowired
    private SyncTaskService syncTaskService;

    /**
     * 获取所有同步任务
     */
    @GetMapping("/tasks")
    public ResponseEntity<List<SyncTask>> getAllTasks() {
        try {
            List<SyncTask> tasks = syncTaskService.getAllTasks();
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 根据ID获取同步任务
     */
    @GetMapping("/tasks/{id}")
    public ResponseEntity<?> getTaskById(@PathVariable Long id) {
        try {
            SyncTask task = syncTaskService.getTaskById(id);
            if (task != null) {
                return ResponseEntity.ok(task);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "同步任务不存在");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 创建同步任务
     */
    @PostMapping("/tasks")
    public ResponseEntity<?> createTask(@RequestBody SyncTaskRequest taskRequest) {
        try {
            // 将请求对象转换为实体类
            SyncTask task = taskRequest.toSyncTask();
            SyncTask createdTask = syncTaskService.createTask(task);
            return ResponseEntity.ok(createdTask);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 更新同步任务
     */
    @PutMapping("/tasks/{id}")
    public ResponseEntity<?> updateTask(@PathVariable Long id, @RequestBody SyncTaskRequest taskRequest) {
        try {
            // 将请求对象转换为实体类
            SyncTask task = taskRequest.toSyncTask();
            SyncTask updatedTask = syncTaskService.updateTask(id, task);
            return ResponseEntity.ok(updatedTask);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除同步任务
     */
    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id) {
        try {
            boolean success = syncTaskService.deleteTask(id);
            if (success) {
                Map<String, String> result = new HashMap<>();
                result.put("message", "删除成功");
                return ResponseEntity.ok(result);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "删除失败");
                return ResponseEntity.badRequest().body(error);
            }
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 执行同步任务
     */
    @PostMapping("/tasks/{id}/execute")
    public ResponseEntity<?> executeTask(@PathVariable Long id) {
        try {
            syncTaskService.executeTask(id);
            Map<String, String> result = new HashMap<>();
            result.put("message", "任务已开始执行");
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 停止同步任务
     */
    @PostMapping("/tasks/{id}/stop")
    public ResponseEntity<?> stopTask(@PathVariable Long id) {
        try {
            syncTaskService.stopTask(id);
            Map<String, String> result = new HashMap<>();
            result.put("message", "任务已停止");
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取任务进度
     */
    @GetMapping("/tasks/{id}/progress")
    public ResponseEntity<?> getTaskProgress(@PathVariable Long id) {
        try {
            Map<String, Object> progress = syncTaskService.getTaskProgress(id);
            return ResponseEntity.ok(progress);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取任务日志
     */
    @GetMapping("/tasks/{id}/logs")
    public ResponseEntity<?> getTaskLogs(@PathVariable Long id) {
        try {
            List<String> logs = syncTaskService.getTaskLogs(id);
            return ResponseEntity.ok(logs);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取源数据库的表列表
     */
    @GetMapping("/connections/{connectionId}/tables")
    public ResponseEntity<?> getTables(@PathVariable Long connectionId, 
                                     @RequestParam(required = false) String schema) {
        try {
            List<String> tables = syncTaskService.getSourceTables(connectionId, schema);
            return ResponseEntity.ok(tables);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取运行中的任务
     */
    @GetMapping("/tasks/running")
    public ResponseEntity<?> getRunningTasks() {
        try {
            List<SyncTask> runningTasks = syncTaskService.getAllTasks().stream()
                    .filter(task -> "RUNNING".equals(task.getStatus()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(runningTasks);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取任务统计信息
     */
    @GetMapping("/tasks/statistics")
    public ResponseEntity<?> getTaskStatistics() {
        try {
            List<SyncTask> allTasks = syncTaskService.getAllTasks();
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("total", allTasks.size());
            statistics.put("pending", allTasks.stream().filter(task -> "PENDING".equals(task.getStatus())).count());
            statistics.put("running", allTasks.stream().filter(task -> "RUNNING".equals(task.getStatus())).count());
            statistics.put("completed", allTasks.stream().filter(task -> "COMPLETED_SUCCESS".equals(task.getStatus())).count());
            statistics.put("failed", allTasks.stream().filter(task -> "FAILED".equals(task.getStatus())).count());
            
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}