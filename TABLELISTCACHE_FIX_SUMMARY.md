# tableListCache 未定义错误修复总结

## 问题描述
在运行时出现错误：
```
chunk-CPAMCG7S.js?v=09aee17a:2536 Uncaught ReferenceError: tableListCache is not defined
    at handleSourceChange (sync.vue:418:3)
```

## 问题原因
在之前的缓存重构过程中，我们将旧的 `tableListCache` (Map对象) 替换为新的 `dbMetadataCache` (CacheManager实例)，但在 `handleSourceChange` 函数中仍然有对旧变量的引用。

## 修复方案

### 修复前的代码 ❌
```typescript
// 源数据库改变时清空表选择和缓存
const handleSourceChange = () => {
  formData.tables = [];
  sourceTableList.value = [];

  // 清除相关缓存
  const oldCacheKey = `${formData.sourceConnectionId}_${formData.sourceSchemaName || 'default'}`;
  tableListCache.delete(oldCacheKey); // ❌ tableListCache 未定义
};
```

### 修复后的代码 ✅
```typescript
// 源数据库改变时清空表选择和缓存
const handleSourceChange = () => {
  formData.tables = [];
  sourceTableList.value = [];

  // 清除相关缓存
  if (formData.sourceConnectionId) {
    const cacheKey = CacheKeys.tables(formData.sourceConnectionId, formData.sourceSchemaName);
    dbMetadataCache.delete(cacheKey); // ✅ 使用新的缓存系统
  }
};
```

## 修复的改进点

### 1. **统一缓存键生成** ✅
- **修复前**：手动拼接缓存键 `${connectionId}_${schema || 'default'}`
- **修复后**：使用 `CacheKeys.tables()` 统一生成，确保一致性

### 2. **类型安全** ✅
- **修复前**：直接操作 Map 对象，没有类型检查
- **修复后**：使用 CacheManager 的类型安全方法

### 3. **空值检查** ✅
- **修复前**：没有检查 connectionId 是否存在
- **修复后**：添加了 `if (formData.sourceConnectionId)` 检查

### 4. **缓存管理一致性** ✅
现在所有缓存操作都使用统一的系统：
```typescript
// 获取缓存
const cached = dbMetadataCache.get(cacheKey);

// 设置缓存
dbMetadataCache.set(cacheKey, data, CACHE_DURATION);

// 删除缓存
dbMetadataCache.delete(cacheKey);
```

## 完整的缓存流程

### 1. **加载表列表时**
```typescript
const loadSourceTables = async () => {
  const cacheKey = CacheKeys.tables(connectionId, schema);
  
  // 检查缓存
  const cached = dbMetadataCache.get(cacheKey);
  if (cached) {
    frontendCacheManager.recordHit();
    return cached;
  }
  
  // 从API获取数据
  const tables = await getDbTablesApi(connectionId, schema);
  
  // 更新缓存
  dbMetadataCache.set(cacheKey, tables, CACHE_DURATION);
  frontendCacheManager.recordMiss();
  
  return tables;
};
```

### 2. **数据库连接改变时**
```typescript
const handleSourceChange = () => {
  // 清空当前数据
  formData.tables = [];
  sourceTableList.value = [];
  
  // 清除相关缓存
  if (formData.sourceConnectionId) {
    const cacheKey = CacheKeys.tables(formData.sourceConnectionId, formData.sourceSchemaName);
    dbMetadataCache.delete(cacheKey);
  }
};
```

## 验证方法

1. **启动前端服务**：
   ```bash
   cd frontend
   pnpm dev
   ```

2. **测试缓存功能**：
   - 打开数据同步页面
   - 选择源数据库连接
   - 加载表列表
   - 切换到其他数据库连接
   - 再切换回原来的连接
   - 检查控制台是否还有错误

3. **验证缓存清除**：
   - 选择数据库A，加载表列表
   - 切换到数据库B（应该清除A的缓存）
   - 再切换回数据库A（应该重新从API加载）

## 技术要点

### CacheManager vs Map
- **Map**: 简单的键值存储，没有TTL和统计功能
- **CacheManager**: 
  - ✅ 支持TTL（生存时间）
  - ✅ 自动清理过期数据
  - ✅ 缓存统计（命中率等）
  - ✅ 大小限制和LRU清理

### 缓存键一致性
使用 `CacheKeys` 工具确保：
- 相同参数生成相同的键
- 键格式统一和可预测
- 支持不同类型的缓存（表、列、Schema等）

## 预期效果

修复后的功能：
1. ✅ 不再出现 `tableListCache is not defined` 错误
2. ✅ 数据库连接切换时正确清除缓存
3. ✅ 缓存键生成统一和一致
4. ✅ 支持完整的缓存生命周期管理
5. ✅ 提供更好的用户体验和性能

这个修复确保了缓存系统的完整性和一致性，用户现在可以正常使用数据同步功能而不会遇到JavaScript错误。
