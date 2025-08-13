/**
 * SQL语句生成器 - 支持多种数据库的分页查询
 */

export interface PaginationConfig {
  limit: number;
  offset?: number;
}

/**
 * 根据数据库类型添加适当的 SQL 结尾
 * @param sql SQL语句
 * @param dbType 数据库类型
 * @returns 处理后的SQL语句
 */
function addSqlEnding(sql: string, dbType: string): string {
  const normalizedDbType = dbType.toLowerCase().trim();
  
  // Oracle 通常不使用分号作为查询结尾（在客户端工具中）
  if (normalizedDbType === 'oracle') {
    return sql;
  }
  
  // 其他数据库使用分号
  return sql.endsWith(';') ? sql : sql + ';';
}

/**
 * 根据数据库类型生成分页查询语句
 * @param dbType 数据库类型
 * @param tableName 表名
 * @param columns 列名数组，如果为空则使用 *
 * @param config 分页配置
 * @param schema 模式名（可选）
 * @returns 分页查询SQL语句
 */
export function generatePaginationQuery(
  dbType: string,
  tableName: string,
  columns: string[] = [],
  config: PaginationConfig = { limit: 100 },
  schema?: string
): string {
  const normalizedDbType = dbType.toLowerCase().trim();
  const columnList = columns.length > 0 ? columns.join(', ') : '*';
  const fullTableName = schema ? `${schema}.${tableName}` : tableName;
  
  // 调试输出（Oracle 问题排查）
  console.log(`[SQL Debug] dbType: "${dbType}", normalized: "${normalizedDbType}", table: "${fullTableName}"`);
  
  switch (normalizedDbType) {
    // MySQL、PostgreSQL、SQLite - 支持 LIMIT OFFSET
    case 'mysql':
    case 'postgresql':
    case 'sqlite':
    case 'vastbase':  // 海量数据库（基于PostgreSQL）
      if (config.offset && config.offset > 0) {
        return `SELECT ${columnList} FROM ${fullTableName} LIMIT ${config.limit} OFFSET ${config.offset};`;
      }
      return `SELECT ${columnList} FROM ${fullTableName} LIMIT ${config.limit};`;

    // Oracle - 使用 ROWNUM（兼容 Oracle 11g+）
    case 'oracle':
      console.log(`[SQL Debug] Oracle branch, offset: ${config.offset}, limit: ${config.limit}`);
      if (config.offset && config.offset > 0) {
        // 使用 ROWNUM 进行分页（兼容老版本Oracle）
        return `SELECT * FROM (
  SELECT ROWNUM rnum, t.* FROM (
    SELECT ${columnList} FROM ${fullTableName}
  ) t WHERE ROWNUM <= ${config.offset + config.limit}
) WHERE rnum > ${config.offset}`;
      }
      // Oracle 11g+ 兼容的简单查询（不使用分号）
      return `SELECT * FROM (
  SELECT ${columnList} FROM ${fullTableName}
) WHERE ROWNUM <= ${config.limit}`;

    // SQL Server - 使用 TOP 或 OFFSET FETCH (2012+)
    case 'sqlserver':
    case 'mssql':
      if (config.offset && config.offset > 0) {
        // SQL Server 2012+ 支持 OFFSET FETCH
        return `SELECT ${columnList} FROM ${fullTableName} 
ORDER BY (SELECT NULL) 
OFFSET ${config.offset} ROWS 
FETCH NEXT ${config.limit} ROWS ONLY;`;
      }
      return `SELECT TOP ${config.limit} ${columnList} FROM ${fullTableName};`;

    // 达梦数据库 - 支持 LIMIT 语法（类似MySQL）
    case 'dameng':
    case 'dm':
      if (config.offset && config.offset > 0) {
        return `SELECT ${columnList} FROM ${fullTableName} LIMIT ${config.offset}, ${config.limit};`;
      }
      return `SELECT ${columnList} FROM ${fullTableName} LIMIT ${config.limit};`;

    // GBase数据库 - 根据具体版本支持不同语法
    case 'gbase':
    case 'gbase8s':
      // GBase 8s（基于Informix）使用 FIRST/SKIP
      if (config.offset && config.offset > 0) {
        return `SELECT FIRST ${config.limit} SKIP ${config.offset} ${columnList} FROM ${fullTableName};`;
      }
      return `SELECT FIRST ${config.limit} ${columnList} FROM ${fullTableName};`;

    case 'gbase8a':
      // GBase 8a（基于MySQL）使用 LIMIT
      if (config.offset && config.offset > 0) {
        return `SELECT ${columnList} FROM ${fullTableName} LIMIT ${config.offset}, ${config.limit};`;
      }
      return `SELECT ${columnList} FROM ${fullTableName} LIMIT ${config.limit};`;

    // 人大金仓数据库（KingbaseES）- 基于PostgreSQL
    case 'kingbase':
    case 'kingbasees':
      if (config.offset && config.offset > 0) {
        return `SELECT ${columnList} FROM ${fullTableName} LIMIT ${config.limit} OFFSET ${config.offset};`;
      }
      return `SELECT ${columnList} FROM ${fullTableName} LIMIT ${config.limit};`;

    // 南大通用数据库（GBase UP）
    case 'gbaseup':
      // 类似PostgreSQL语法
      if (config.offset && config.offset > 0) {
        return `SELECT ${columnList} FROM ${fullTableName} LIMIT ${config.limit} OFFSET ${config.offset};`;
      }
      return `SELECT ${columnList} FROM ${fullTableName} LIMIT ${config.limit};`;

    // 瀚高数据库（HighGo）- 基于PostgreSQL
    case 'highgo':
      if (config.offset && config.offset > 0) {
        return `SELECT ${columnList} FROM ${fullTableName} LIMIT ${config.limit} OFFSET ${config.offset};`;
      }
      return `SELECT ${columnList} FROM ${fullTableName} LIMIT ${config.limit};`;

    // 神舟通用数据库（Oscar）- 基于PostgreSQL
    case 'oscar':
      if (config.offset && config.offset > 0) {
        return `SELECT ${columnList} FROM ${fullTableName} LIMIT ${config.limit} OFFSET ${config.offset};`;
      }
      return `SELECT ${columnList} FROM ${fullTableName} LIMIT ${config.limit};`;

    // TiDB - 兼容MySQL语法
    case 'tidb':
      if (config.offset && config.offset > 0) {
        return `SELECT ${columnList} FROM ${fullTableName} LIMIT ${config.offset}, ${config.limit};`;
      }
      return `SELECT ${columnList} FROM ${fullTableName} LIMIT ${config.limit};`;

    // OceanBase - 兼容MySQL语法
    case 'oceanbase':
      if (config.offset && config.offset > 0) {
        return `SELECT ${columnList} FROM ${fullTableName} LIMIT ${config.offset}, ${config.limit};`;
      }
      return `SELECT ${columnList} FROM ${fullTableName} LIMIT ${config.limit};`;

    // 默认情况：尝试使用 LIMIT（大多数现代数据库都支持）
    default:
      console.warn(`未知的数据库类型: ${dbType}，使用默认的LIMIT语法`);
      if (config.offset && config.offset > 0) {
        return `SELECT ${columnList} FROM ${fullTableName} LIMIT ${config.limit} OFFSET ${config.offset};`;
      }
      return `SELECT ${columnList} FROM ${fullTableName} LIMIT ${config.limit};`;
  }
}

/**
 * 生成简单的SELECT查询（用于查看表数据）
 * @param dbType 数据库类型
 * @param tableName 表名
 * @param columns 列名数组
 * @param schema 模式名
 * @returns SELECT查询语句
 */
export function generateSelectQuery(
  dbType: string,
  tableName: string,
  columns: string[] = [],
  schema?: string
): string {
  return generatePaginationQuery(dbType, tableName, columns, { limit: 100 }, schema);
}

/**
 * 生成INSERT语句模板
 * @param dbType 数据库类型
 * @param tableName 表名
 * @param columns 列信息数组
 * @param schema 模式名
 * @returns INSERT语句模板
 */
export function generateInsertQuery(
  dbType: string,
  tableName: string,
  columns: Array<{ columnName: string; dataType: string; nullable: boolean }>,
  schema?: string
): string {
  const fullTableName = schema ? `${schema}.${tableName}` : tableName;
  const columnNames = columns.map(col => col.columnName).join(', ');
  const valuePlaceholders = columns.map(col => {
    // 根据数据类型生成合适的占位符
    const dataType = col.dataType.toLowerCase();
    if (dataType.includes('char') || dataType.includes('text') || dataType.includes('varchar')) {
      return "'value'";
    } else if (dataType.includes('date') || dataType.includes('time')) {
      return "'2024-01-01'";
    } else if (dataType.includes('int') || dataType.includes('number') || dataType.includes('decimal')) {
      return '0';
    } else {
      return "'value'";
    }
  }).join(', ');

  return addSqlEnding(`INSERT INTO ${fullTableName} (${columnNames}) VALUES (${valuePlaceholders})`, dbType);
}

/**
 * 生成UPDATE语句模板
 * @param dbType 数据库类型
 * @param tableName 表名
 * @param columns 列信息数组
 * @param schema 模式名
 * @returns UPDATE语句模板
 */
export function generateUpdateQuery(
  dbType: string,
  tableName: string,
  columns: Array<{ columnName: string; dataType: string; isPrimaryKey: boolean }>,
  schema?: string
): string {
  const fullTableName = schema ? `${schema}.${tableName}` : tableName;
  
  // 分离主键和非主键字段
  const primaryKeys = columns.filter(col => col.isPrimaryKey);
  const nonPrimaryKeys = columns.filter(col => !col.isPrimaryKey);
  
  if (nonPrimaryKeys.length === 0) {
    return `-- 表 ${fullTableName} 没有非主键字段可以更新`;
  }
  
  // 生成SET子句
  const setClause = nonPrimaryKeys.map(col => {
    const dataType = col.dataType.toLowerCase();
    if (dataType.includes('char') || dataType.includes('text') || dataType.includes('varchar')) {
      return `${col.columnName} = 'new_value'`;
    } else if (dataType.includes('date') || dataType.includes('time')) {
      return `${col.columnName} = '2024-01-01'`;
    } else if (dataType.includes('int') || dataType.includes('number') || dataType.includes('decimal')) {
      return `${col.columnName} = 0`;
    } else {
      return `${col.columnName} = 'new_value'`;
    }
  }).join(', ');
  
  // 生成WHERE子句
  let whereClause = '';
  if (primaryKeys.length > 0) {
    whereClause = ' WHERE ' + primaryKeys.map(col => `${col.columnName} = 'value'`).join(' AND ');
  } else {
    whereClause = ' WHERE condition = value';
  }
  
  return addSqlEnding(`UPDATE ${fullTableName} SET ${setClause}${whereClause}`, dbType);
}

/**
 * 生成DELETE语句模板
 * @param dbType 数据库类型
 * @param tableName 表名
 * @param primaryKeys 主键字段数组
 * @param schema 模式名
 * @returns DELETE语句模板
 */
export function generateDeleteQuery(
  dbType: string,
  tableName: string,
  primaryKeys: Array<{ columnName: string; dataType: string }> = [],
  schema?: string
): string {
  const fullTableName = schema ? `${schema}.${tableName}` : tableName;
  
  let whereClause = '';
  if (primaryKeys.length > 0) {
    whereClause = ' WHERE ' + primaryKeys.map(col => `${col.columnName} = 'value'`).join(' AND ');
  } else {
    whereClause = ' WHERE condition = value';
  }
  
  return addSqlEnding(`DELETE FROM ${fullTableName}${whereClause}`, dbType);
}

/**
 * 生成描述表结构的查询语句
 * @param dbType 数据库类型
 * @param tableName 表名
 * @param schema 模式名
 * @returns 描述表结构的SQL语句
 */
export function generateDescribeQuery(
  dbType: string,
  tableName: string,
  schema?: string
): string {
  const normalizedDbType = dbType.toLowerCase().trim();
  const fullTableName = schema ? `${schema}.${tableName}` : tableName;
  
  switch (normalizedDbType) {
    // MySQL、TiDB、OceanBase - 使用 DESC 或 DESCRIBE
    case 'mysql':
    case 'tidb':
    case 'oceanbase':
      return addSqlEnding(`DESC ${fullTableName}`, dbType);
    
    // PostgreSQL系列 - 使用 \d 命令（但在SQL中需要查询系统表）
    case 'postgresql':
    case 'vastbase':
    case 'kingbase':
    case 'gbaseup':
    case 'highgo':
    case 'oscar':
      return addSqlEnding(`SELECT 
  column_name, 
  data_type, 
  is_nullable, 
  column_default,
  character_maximum_length
FROM information_schema.columns 
WHERE table_name = '${tableName}'${schema ? ` AND table_schema = '${schema}'` : ''}
ORDER BY ordinal_position`, dbType);

    // Oracle - 查询用户表或全部表的列信息
    case 'oracle':
      if (schema) {
        return addSqlEnding(`SELECT 
  column_name, 
  data_type, 
  nullable, 
  data_default,
  data_length
FROM all_tab_columns 
WHERE table_name = UPPER('${tableName}') 
  AND owner = UPPER('${schema}')
ORDER BY column_id`, dbType);
      } else {
        return addSqlEnding(`SELECT 
  column_name, 
  data_type, 
  nullable, 
  data_default,
  data_length
FROM user_tab_columns 
WHERE table_name = UPPER('${tableName}')
ORDER BY column_id`, dbType);
      }

    // SQL Server - 使用系统视图
    case 'sqlserver':
    case 'mssql':
      return addSqlEnding(`SELECT 
  c.column_name,
  c.data_type,
  c.is_nullable,
  c.column_default,
  c.character_maximum_length
FROM information_schema.columns c
WHERE c.table_name = '${tableName}'${schema ? ` AND c.table_schema = '${schema}'` : ''}
ORDER BY c.ordinal_position`, dbType);

    // 达梦数据库 - 类似Oracle
    case 'dameng':
    case 'dm':
      return addSqlEnding(`DESC ${fullTableName}`, dbType);

    // GBase 8s - 类似Informix，使用系统表
    case 'gbase':
    case 'gbase8s':
      return addSqlEnding(`SELECT 
  colname as column_name,
  coltype as data_type,
  CASE WHEN coltype LIKE '%NOT NULL%' THEN 'NO' ELSE 'YES' END as is_nullable
FROM syscolumns 
WHERE tabid = (SELECT tabid FROM systables WHERE tabname = '${tableName}')
ORDER BY colno`, dbType);

    // GBase 8a - 兼容MySQL
    case 'gbase8a':
      return addSqlEnding(`DESC ${fullTableName}`, dbType);

    // SQLite - 使用 PRAGMA
    case 'sqlite':
      return addSqlEnding(`PRAGMA table_info(${tableName})`, dbType);

    // 默认情况：尝试使用 information_schema（SQL标准）
    default:
      console.warn(`未知的数据库类型: ${dbType}，使用标准information_schema查询`);
      return addSqlEnding(`SELECT 
  column_name, 
  data_type, 
  is_nullable, 
  column_default
FROM information_schema.columns 
WHERE table_name = '${tableName}'${schema ? ` AND table_schema = '${schema}'` : ''}
ORDER BY ordinal_position`, dbType);
  }
}

/**
 * 获取数据库支持的分页语法类型
 * @param dbType 数据库类型
 * @returns 分页语法类型描述
 */
export function getPaginationSyntaxInfo(dbType: string): string {
  const normalizedDbType = dbType.toLowerCase().trim();
  
  switch (normalizedDbType) {
    case 'mysql':
    case 'dameng':
    case 'gbase8a':
    case 'tidb':
    case 'oceanbase':
      return 'LIMIT offset, count';
    
    case 'postgresql':
    case 'sqlite':
    case 'vastbase':
    case 'kingbase':
    case 'gbaseup':
    case 'highgo':
    case 'oscar':
      return 'LIMIT count OFFSET offset';
    
    case 'oracle':
      return 'ROWNUM 分页（兼容性最佳）';
    
    case 'sqlserver':
    case 'mssql':
      return 'TOP n 或 OFFSET FETCH';
    
    case 'gbase':
    case 'gbase8s':
      return 'FIRST count SKIP offset';
    
    default:
      return '使用标准LIMIT语法';
  }
}