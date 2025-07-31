package com.dbsync.dbsync.service;

import com.dbsync.dbsync.entity.QueryHistory;
import com.dbsync.dbsync.mapper.QueryHistoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 查询历史服务类
 */
@Service
public class QueryHistoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryHistoryService.class);
    
    @Autowired
    private QueryHistoryMapper queryHistoryMapper;
    
    /**
     * 保存查询历史
     */
    @Transactional
    public QueryHistory saveQueryHistory(QueryHistory queryHistory) {
        try {
            // 设置执行时间
            if (queryHistory.getExecutedAt() == null) {
                queryHistory.setExecutedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            
            // 设置默认状态
            if (queryHistory.getStatus() == null) {
                queryHistory.setStatus("SUCCESS");
            }
            
            // 限制SQL长度，避免存储过长的SQL
            if (queryHistory.getSql() != null && queryHistory.getSql().length() > 10000) {
                queryHistory.setSql(queryHistory.getSql().substring(0, 10000) + "...[截断]");
            }
            
            int result = queryHistoryMapper.insertQueryHistory(queryHistory);
            if (result > 0) {
                logger.info("查询历史保存成功，ID: {}", queryHistory.getId());
                return queryHistory;
            } else {
                throw new RuntimeException("保存查询历史失败");
            }
        } catch (Exception e) {
            logger.error("保存查询历史异常: {}", e.getMessage(), e);
            throw new RuntimeException("保存查询历史失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有查询历史
     */
    public List<QueryHistory> getAllQueryHistory() {
        try {
            return queryHistoryMapper.findAllOrderByExecutedAtDesc();
        } catch (Exception e) {
            logger.error("获取查询历史异常: {}", e.getMessage(), e);
            throw new RuntimeException("获取查询历史失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据用户获取查询历史
     */
    public List<QueryHistory> getQueryHistoryByUser(String createdBy) {
        try {
            if (createdBy == null || createdBy.trim().isEmpty()) {
                return getAllQueryHistory();
            }
            return queryHistoryMapper.findByCreatedBy(createdBy);
        } catch (Exception e) {
            logger.error("根据用户获取查询历史异常: {}", e.getMessage(), e);
            throw new RuntimeException("获取查询历史失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取最近的查询历史
     */
    public List<QueryHistory> getRecentQueryHistory(Integer limit) {
        try {
            if (limit == null || limit <= 0) {
                limit = 50; // 默认获取最近50条
            }
            if (limit > 1000) {
                limit = 1000; // 最多1000条
            }
            return queryHistoryMapper.findRecentHistory(limit);
        } catch (Exception e) {
            logger.error("获取最近查询历史异常: {}", e.getMessage(), e);
            throw new RuntimeException("获取最近查询历史失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据用户获取最近的查询历史
     */
    public List<QueryHistory> getRecentQueryHistoryByUser(String createdBy, Integer limit) {
        try {
            if (limit == null || limit <= 0) {
                limit = 50; // 默认获取最近50条
            }
            if (limit > 1000) {
                limit = 1000; // 最多1000条
            }
            
            if (createdBy == null || createdBy.trim().isEmpty()) {
                return getRecentQueryHistory(limit);
            }
            
            return queryHistoryMapper.findRecentHistoryByUser(createdBy, limit);
        } catch (Exception e) {
            logger.error("根据用户获取最近查询历史异常: {}", e.getMessage(), e);
            throw new RuntimeException("获取最近查询历史失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据ID获取查询历史
     */
    public QueryHistory getQueryHistoryById(Long id) {
        try {
            return queryHistoryMapper.selectById(id);
        } catch (Exception e) {
            logger.error("根据ID获取查询历史异常: {}", e.getMessage(), e);
            throw new RuntimeException("获取查询历史失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除查询历史
     */
    @Transactional
    public boolean deleteQueryHistory(Long id) {
        try {
            int result = queryHistoryMapper.deleteById(id);
            if (result > 0) {
                logger.info("查询历史删除成功，ID: {}", id);
                return true;
            } else {
                logger.warn("查询历史删除失败，记录不存在，ID: {}", id);
                return false;
            }
        } catch (Exception e) {
            logger.error("删除查询历史异常: {}", e.getMessage(), e);
            throw new RuntimeException("删除查询历史失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据用户删除查询历史
     */
    @Transactional
    public int deleteQueryHistoryByUser(String createdBy) {
        try {
            int result = queryHistoryMapper.deleteByCreatedBy(createdBy);
            logger.info("用户 {} 的查询历史删除完成，删除 {} 条记录", createdBy, result);
            return result;
        } catch (Exception e) {
            logger.error("根据用户删除查询历史异常: {}", e.getMessage(), e);
            throw new RuntimeException("删除查询历史失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理过期的查询历史
     */
    @Transactional
    public int cleanupExpiredHistory(int daysToKeep) {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysToKeep);
            String cutoffTimeStr = cutoffTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            int result = queryHistoryMapper.deleteHistoryBeforeTime(cutoffTimeStr);
            logger.info("清理过期查询历史完成，删除 {} 条记录", result);
            return result;
        } catch (Exception e) {
            logger.error("清理过期查询历史异常: {}", e.getMessage(), e);
            throw new RuntimeException("清理过期查询历史失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取查询历史统计信息
     */
    public QueryHistoryMapper.QueryHistoryStats getQueryHistoryStats(String createdBy) {
        try {
            return queryHistoryMapper.getStatsByUser(createdBy);
        } catch (Exception e) {
            logger.error("获取查询历史统计信息异常: {}", e.getMessage(), e);
            throw new RuntimeException("获取查询历史统计信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据源连接获取查询历史
     */
    public List<QueryHistory> getQueryHistoryBySourceConnection(Long sourceConnectionId) {
        try {
            return queryHistoryMapper.findBySourceConnectionId(sourceConnectionId);
        } catch (Exception e) {
            logger.error("根据源连接获取查询历史异常: {}", e.getMessage(), e);
            throw new RuntimeException("获取查询历史失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据状态获取查询历史
     */
    public List<QueryHistory> getQueryHistoryByStatus(String status) {
        try {
            return queryHistoryMapper.findByStatus(status);
        } catch (Exception e) {
            logger.error("根据状态获取查询历史异常: {}", e.getMessage(), e);
            throw new RuntimeException("获取查询历史失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建查询历史记录
     */
    public QueryHistory createQueryHistory(String sql, Long sourceConnectionId, String sourceConnectionName,
                                         Long targetConnectionId, String targetConnectionName,
                                         String targetTableName, String targetSchemaName,
                                         Integer executionTime, String status, String errorMessage,
                                         Integer resultRows, String createdBy) {
        QueryHistory queryHistory = new QueryHistory();
        queryHistory.setSql(sql);
        queryHistory.setSourceConnectionId(sourceConnectionId);
        queryHistory.setSourceConnectionName(sourceConnectionName);
        queryHistory.setTargetConnectionId(targetConnectionId);
        queryHistory.setTargetConnectionName(targetConnectionName);
        queryHistory.setTargetTableName(targetTableName);
        queryHistory.setTargetSchemaName(targetSchemaName);
        queryHistory.setExecutionTime(executionTime);
        queryHistory.setStatus(status);
        queryHistory.setErrorMessage(errorMessage);
        queryHistory.setResultRows(resultRows);
        queryHistory.setCreatedBy(createdBy);
        
        return saveQueryHistory(queryHistory);
    }
}
