# SQL脚本执行功能使用指南

## 🚀 功能概述

现在DBSync系统完全支持复杂SQL脚本的执行，特别针对Oracle数据库进行了优化，支持：

- ✅ **多语句脚本执行**：支持一次性执行包含多个SQL语句的脚本
- ✅ **Oracle特定语法**：完美支持触发器、序列、存储过程等复杂语法
- ✅ **事务控制**：支持事务模式和非事务模式执行
- ✅ **详细执行报告**：提供每个语句的执行结果和错误信息
- ✅ **安全执行**：包含错误处理和回滚机制

## 🛠️ API接口

### 1. 执行SQL脚本
```
POST /api/database/connections/{id}/script/execute
```

**请求体：**
```json
{
  "script": "完整的SQL脚本内容",
  "schema": "可选的schema名称",
  "executeInTransaction": true
}
```

**响应示例：**
```json
{
  "success": true,
  "message": "脚本执行完成，成功: 8, 失败: 0, 总耗时: 1250 ms",
  "totalCount": 8,
  "successCount": 8,
  "failedCount": 0,
  "totalTime": 1250,
  "statementResults": [
    {
      "statement": {
        "sql": "CREATE SEQUENCE SEQ_AUTH_DETAIL_ID START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE",
        "type": "DDL",
        "category": "SEQUENCE",
        "startLine": 5,
        "endLine": 8
      },
      "success": true,
      "message": "SEQUENCE 执行成功",
      "affectedRows": 0,
      "executionTime": 156,
      "lineNumber": 5,
      "statementPreview": "CREATE SEQUENCE SEQ_AUTH_DETAIL_ID..."
    }
  ],
  "timestamp": 1691924671234
}
```

### 2. 解析SQL脚本（预览）
```
POST /api/database/script/parse
```

**请求体：**
```json
{
  "script": "SQL脚本内容",
  "dbType": "oracle"
}
```

## 📝 支持的SQL语句类型

### Oracle数据库特别支持

1. **序列创建**
```sql
CREATE SEQUENCE SEQ_AUTH_DETAIL_ID
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;
```

2. **表创建（包含复杂约束）**
```sql
CREATE TABLE AUTHORIZATION_DETAIL (
    ID NUMBER PRIMARY KEY,
    AUTHORIZER_CODE VARCHAR2(50) NOT NULL,
    CREATE_TIME DATE DEFAULT SYSDATE NOT NULL
);
```

3. **触发器创建**
```sql
CREATE OR REPLACE TRIGGER TRG_AUTH_DETAIL_ID
    BEFORE INSERT ON AUTHORIZATION_DETAIL
    FOR EACH ROW
BEGIN
    IF :NEW.ID IS NULL THEN
        SELECT SEQ_AUTH_DETAIL_ID.NEXTVAL INTO :NEW.ID FROM DUAL;
    END IF;
END;
/
```

4. **表和字段注释**
```sql
COMMENT ON TABLE AUTHORIZATION_DETAIL IS '授权明细表';
COMMENT ON COLUMN AUTHORIZATION_DETAIL.ID IS '主键，自增ID';
```

5. **索引创建**
```sql
CREATE INDEX IDX_AUTH_DETAIL_AUTHORIZER ON AUTHORIZATION_DETAIL(AUTHORIZER_CODE);
```

## ⚙️ 执行模式

### 事务模式 (推荐)
- `executeInTransaction: true`
- 所有语句在一个事务中执行
- 任何语句失败将导致整个脚本回滚
- 适用于需要原子性的脚本执行

### 非事务模式
- `executeInTransaction: false`
- 每个语句独立执行和提交
- 单个语句失败不影响其他语句
- 适用于独立的DDL操作

## 🔧 前端使用示例

```typescript
import { executeScriptApi, ScriptExecutionRequest } from "@/api/database";

// 执行脚本
const executeScript = async () => {
  const request: ScriptExecutionRequest = {
    script: `
      -- 创建序列
      CREATE SEQUENCE SEQ_TEST_ID START WITH 1 INCREMENT BY 1;
      
      -- 创建表
      CREATE TABLE TEST_TABLE (
        ID NUMBER PRIMARY KEY,
        NAME VARCHAR2(100)
      );
      
      -- 创建触发器
      CREATE OR REPLACE TRIGGER TRG_TEST_ID
        BEFORE INSERT ON TEST_TABLE
        FOR EACH ROW
      BEGIN
        :NEW.ID := SEQ_TEST_ID.NEXTVAL;
      END;
      /
    `,
    schema: "PT1_ECI_CQDM",
    executeInTransaction: true
  };
  
  try {
    const result = await executeScriptApi("2", request);
    
    if (result.success) {
      console.log(`脚本执行成功！成功: ${result.successCount}, 失败: ${result.failedCount}`);
      
      // 显示详细执行结果
      result.statementResults.forEach((stmt, index) => {
        console.log(`语句 ${index + 1}: ${stmt.success ? '成功' : '失败'}`);
        console.log(`  类型: ${stmt.statement.type}/${stmt.statement.category}`);
        console.log(`  耗时: ${stmt.executionTime}ms`);
        if (!stmt.success) {
          console.error(`  错误: ${stmt.message}`);
        }
      });
    } else {
      console.error("脚本执行失败:", result.message);
    }
  } catch (error) {
    console.error("API调用失败:", error);
  }
};
```

## 🛡️ 安全特性

1. **连接验证**：验证数据库连接是否存在且已启用
2. **超时控制**：语句执行超时时间5分钟，连接超时30秒
3. **事务回滚**：事务模式下任何错误都会触发回滚
4. **错误隔离**：非事务模式下错误不会影响其他语句
5. **详细日志**：完整记录执行过程和错误信息

## 📊 执行结果解读

### 成功执行
- `success: true`
- `failedCount: 0`
- 所有语句的`success`字段都为`true`

### 部分失败
- `success: false`
- `failedCount > 0`
- 检查`statementResults`中失败语句的`message`

### 完全失败
- `success: false`
- `successCount: 0`
- 通常是连接问题或脚本解析错误

## 🎯 最佳实践

1. **使用事务模式**：对于需要原子性的操作（如建表+触发器+索引）
2. **合理分组**：将相关的DDL语句组织在一起
3. **添加注释**：使用SQL注释说明每个部分的作用
4. **测试优先**：先在开发环境测试复杂脚本
5. **监控执行时间**：对于大型脚本，关注执行时间和资源占用

## 🚨 注意事项

1. **Oracle语法**：触发器等块语句必须以`/`结尾
2. **权限要求**：确保数据库用户有足够权限执行DDL语句
3. **Schema指定**：Oracle中通过用户权限控制Schema访问
4. **超时设置**：复杂DDL操作可能需要较长时间
5. **回滚策略**：事务模式下失败会回滚所有已执行的语句

现在您可以在SQL编辑器中直接粘贴和执行复杂的Oracle脚本了！