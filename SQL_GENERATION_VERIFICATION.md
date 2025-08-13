# SQL 生成验证 - 多数据库兼容性测试

## 测试目的
验证 Oracle 修复后，其他数据库的 SQL 生成是否正常工作

## 各数据库预期 SQL 语法

### MySQL
```sql
SELECT * FROM test_table LIMIT 100;
INSERT INTO test_table (id, name) VALUES (0, 'value');
UPDATE test_table SET name = 'new_value' WHERE id = 'value';
DELETE FROM test_table WHERE id = 'value';
DESC test_table;
```

### PostgreSQL
```sql
SELECT * FROM test_table LIMIT 100;
INSERT INTO test_table (id, name) VALUES (0, 'value');
UPDATE test_table SET name = 'new_value' WHERE id = 'value';
DELETE FROM test_table WHERE id = 'value';
SELECT column_name, data_type, is_nullable, column_default, character_maximum_length
FROM information_schema.columns 
WHERE table_name = 'test_table'
ORDER BY ordinal_position;
```

### SQL Server
```sql
SELECT TOP 100 * FROM test_table;
INSERT INTO test_table (id, name) VALUES (0, 'value');
UPDATE test_table SET name = 'new_value' WHERE id = 'value';
DELETE FROM test_table WHERE id = 'value';
SELECT c.column_name, c.data_type, c.is_nullable, c.column_default, c.character_maximum_length
FROM information_schema.columns c
WHERE c.table_name = 'test_table'
ORDER BY c.ordinal_position;
```

### 达梦数据库
```sql
SELECT * FROM test_table LIMIT 100;
INSERT INTO test_table (id, name) VALUES (0, 'value');
UPDATE test_table SET name = 'new_value' WHERE id = 'value';
DELETE FROM test_table WHERE id = 'value';
DESC test_table;
```

### Oracle（修复后）
```sql
SELECT * FROM (
  SELECT * FROM test_table
) WHERE ROWNUM <= 100
INSERT INTO test_table (id, name) VALUES (0, 'value')
UPDATE test_table SET name = 'new_value' WHERE id = 'value'
DELETE FROM test_table WHERE id = 'value'
SELECT column_name, data_type, nullable, data_default, data_length
FROM user_tab_columns 
WHERE table_name = UPPER('test_table')
ORDER BY column_id
```

### 海量数据库 (Vastbase)
```sql
SELECT * FROM test_table LIMIT 100;
INSERT INTO test_table (id, name) VALUES (0, 'value');
UPDATE test_table SET name = 'new_value' WHERE id = 'value';
DELETE FROM test_table WHERE id = 'value';
SELECT column_name, data_type, is_nullable, column_default, character_maximum_length
FROM information_schema.columns 
WHERE table_name = 'test_table'
ORDER BY ordinal_position;
```

## 关键差异点

1. **分页语法**：
   - MySQL/达梦: `LIMIT n`
   - PostgreSQL/Vastbase: `LIMIT n`
   - SQL Server: `TOP n`
   - Oracle: `ROWNUM <= n` (嵌套查询)

2. **分号处理**：
   - Oracle: 无分号结尾
   - 其他数据库: 有分号结尾

3. **表结构查询**：
   - MySQL/达梦: `DESC table`
   - PostgreSQL/SQL Server: `information_schema`
   - Oracle: `user_tab_columns` 或 `all_tab_columns`

## 修复前后对比

### 修复前（错误）
Oracle: `SELECT * FROM OMS_ORDER_BATCH LIMIT 100;`

### 修复后（正确）
Oracle: 
```sql
SELECT * FROM (
  SELECT * FROM OMS_ORDER_BATCH
) WHERE ROWNUM <= 100
```

## 验证检查清单

- [ ] MySQL: LIMIT 语法正常，有分号
- [ ] PostgreSQL: LIMIT OFFSET 语法正常，有分号  
- [ ] SQL Server: TOP 语法正常，有分号
- [ ] Oracle: ROWNUM 语法正常，无分号
- [ ] 达梦: LIMIT 语法正常，有分号
- [ ] Vastbase: LIMIT OFFSET 语法正常，有分号
- [ ] 所有数据库的 INSERT/UPDATE/DELETE 语法正确
- [ ] 所有数据库的 DESCRIBE 查询语法正确