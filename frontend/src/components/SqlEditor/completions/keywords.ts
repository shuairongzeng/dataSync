/**
 * SQL关键字定义
 * 按使用频率和重要性排序
 */

export interface SqlKeyword {
  label: string
  type: 'keyword' | 'function' | 'operator'
  detail?: string
  documentation?: string
  insertText?: string
  priority: number // 优先级，数字越大优先级越高
}

// 核心SQL关键字（高频使用）
export const CORE_KEYWORDS: SqlKeyword[] = [
  {
    label: 'SELECT',
    type: 'keyword',
    detail: 'Query data from tables',
    documentation: 'SELECT statement is used to select data from a database.',
    insertText: 'SELECT ',
    priority: 100
  },
  {
    label: 'FROM',
    type: 'keyword',
    detail: 'Specify source table',
    documentation: 'FROM clause specifies the table to select data from.',
    insertText: 'FROM ',
    priority: 95
  },
  {
    label: 'WHERE',
    type: 'keyword',
    detail: 'Filter conditions',
    documentation: 'WHERE clause is used to filter records.',
    insertText: 'WHERE ',
    priority: 90
  },
  {
    label: 'INSERT',
    type: 'keyword',
    detail: 'Insert new records',
    documentation: 'INSERT INTO statement is used to insert new records in a table.',
    insertText: 'INSERT INTO ',
    priority: 85
  },
  {
    label: 'UPDATE',
    type: 'keyword',
    detail: 'Update existing records',
    documentation: 'UPDATE statement is used to modify existing records in a table.',
    insertText: 'UPDATE ',
    priority: 80
  },
  {
    label: 'DELETE',
    type: 'keyword',
    detail: 'Delete records',
    documentation: 'DELETE statement is used to delete existing records in a table.',
    insertText: 'DELETE FROM ',
    priority: 75
  },
  {
    label: 'JOIN',
    type: 'keyword',
    detail: 'Join tables',
    documentation: 'JOIN clause is used to combine rows from two or more tables.',
    insertText: 'JOIN ',
    priority: 70
  },
  {
    label: 'INNER JOIN',
    type: 'keyword',
    detail: 'Inner join tables',
    documentation: 'INNER JOIN returns records that have matching values in both tables.',
    insertText: 'INNER JOIN ',
    priority: 68
  },
  {
    label: 'LEFT JOIN',
    type: 'keyword',
    detail: 'Left outer join',
    documentation: 'LEFT JOIN returns all records from the left table, and matched records from the right table.',
    insertText: 'LEFT JOIN ',
    priority: 66
  },
  {
    label: 'RIGHT JOIN',
    type: 'keyword',
    detail: 'Right outer join',
    documentation: 'RIGHT JOIN returns all records from the right table, and matched records from the left table.',
    insertText: 'RIGHT JOIN ',
    priority: 64
  },
  {
    label: 'ORDER BY',
    type: 'keyword',
    detail: 'Sort results',
    documentation: 'ORDER BY keyword is used to sort the result-set in ascending or descending order.',
    insertText: 'ORDER BY ',
    priority: 65
  },
  {
    label: 'GROUP BY',
    type: 'keyword',
    detail: 'Group results',
    documentation: 'GROUP BY statement groups rows that have the same values into summary rows.',
    insertText: 'GROUP BY ',
    priority: 60
  },
  {
    label: 'HAVING',
    type: 'keyword',
    detail: 'Filter grouped results',
    documentation: 'HAVING clause was added to SQL because the WHERE keyword cannot be used with aggregate functions.',
    insertText: 'HAVING ',
    priority: 55
  }
]

// 常用SQL关键字
export const COMMON_KEYWORDS: SqlKeyword[] = [
  {
    label: 'AND',
    type: 'operator',
    detail: 'Logical AND',
    documentation: 'AND operator displays a record if all the conditions separated by AND are TRUE.',
    insertText: 'AND ',
    priority: 50
  },
  {
    label: 'OR',
    type: 'operator',
    detail: 'Logical OR',
    documentation: 'OR operator displays a record if any of the conditions separated by OR is TRUE.',
    insertText: 'OR ',
    priority: 48
  },
  {
    label: 'NOT',
    type: 'operator',
    detail: 'Logical NOT',
    documentation: 'NOT operator displays a record if the condition(s) is NOT TRUE.',
    insertText: 'NOT ',
    priority: 46
  },
  {
    label: 'IN',
    type: 'operator',
    detail: 'Match any value in list',
    documentation: 'IN operator allows you to specify multiple values in a WHERE clause.',
    insertText: 'IN (',
    priority: 45
  },
  {
    label: 'LIKE',
    type: 'operator',
    detail: 'Pattern matching',
    documentation: 'LIKE operator is used in a WHERE clause to search for a specified pattern in a column.',
    insertText: 'LIKE ',
    priority: 44
  },
  {
    label: 'BETWEEN',
    type: 'operator',
    detail: 'Range condition',
    documentation: 'BETWEEN operator selects values within a given range.',
    insertText: 'BETWEEN ',
    priority: 42
  },
  {
    label: 'IS NULL',
    type: 'operator',
    detail: 'Check for null values',
    documentation: 'IS NULL operator is used to test for empty values (NULL values).',
    insertText: 'IS NULL',
    priority: 40
  },
  {
    label: 'IS NOT NULL',
    type: 'operator',
    detail: 'Check for non-null values',
    documentation: 'IS NOT NULL operator is used to test for non-empty values (NOT NULL values).',
    insertText: 'IS NOT NULL',
    priority: 38
  },
  {
    label: 'DISTINCT',
    type: 'keyword',
    detail: 'Return unique values',
    documentation: 'DISTINCT keyword is used to return only distinct (different) values.',
    insertText: 'DISTINCT ',
    priority: 36
  },
  {
    label: 'LIMIT',
    type: 'keyword',
    detail: 'Limit result count',
    documentation: 'LIMIT clause is used to specify the number of records to return.',
    insertText: 'LIMIT ',
    priority: 35
  },
  {
    label: 'OFFSET',
    type: 'keyword',
    detail: 'Skip records',
    documentation: 'OFFSET clause is used to skip a specified number of records.',
    insertText: 'OFFSET ',
    priority: 33
  }
]

// 聚合函数
export const AGGREGATE_FUNCTIONS: SqlKeyword[] = [
  {
    label: 'COUNT',
    type: 'function',
    detail: 'Count rows',
    documentation: 'COUNT() function returns the number of rows that matches a specified criterion.',
    insertText: 'COUNT(',
    priority: 30
  },
  {
    label: 'SUM',
    type: 'function',
    detail: 'Sum values',
    documentation: 'SUM() function returns the total sum of a numeric column.',
    insertText: 'SUM(',
    priority: 28
  },
  {
    label: 'AVG',
    type: 'function',
    detail: 'Average value',
    documentation: 'AVG() function returns the average value of a numeric column.',
    insertText: 'AVG(',
    priority: 26
  },
  {
    label: 'MAX',
    type: 'function',
    detail: 'Maximum value',
    documentation: 'MAX() function returns the largest value of the selected column.',
    insertText: 'MAX(',
    priority: 24
  },
  {
    label: 'MIN',
    type: 'function',
    detail: 'Minimum value',
    documentation: 'MIN() function returns the smallest value of the selected column.',
    insertText: 'MIN(',
    priority: 22
  }
]

// 字符串函数
export const STRING_FUNCTIONS: SqlKeyword[] = [
  {
    label: 'UPPER',
    type: 'function',
    detail: 'Convert to uppercase',
    documentation: 'UPPER() function converts a string to upper-case.',
    insertText: 'UPPER(',
    priority: 20
  },
  {
    label: 'LOWER',
    type: 'function',
    detail: 'Convert to lowercase',
    documentation: 'LOWER() function converts a string to lower-case.',
    insertText: 'LOWER(',
    priority: 18
  },
  {
    label: 'LENGTH',
    type: 'function',
    detail: 'String length',
    documentation: 'LENGTH() function returns the length of a string (in bytes).',
    insertText: 'LENGTH(',
    priority: 16
  },
  {
    label: 'SUBSTRING',
    type: 'function',
    detail: 'Extract substring',
    documentation: 'SUBSTRING() function extracts some characters from a string.',
    insertText: 'SUBSTRING(',
    priority: 14
  },
  {
    label: 'TRIM',
    type: 'function',
    detail: 'Remove spaces',
    documentation: 'TRIM() function removes leading and trailing spaces from a string.',
    insertText: 'TRIM(',
    priority: 12
  }
]

// 日期函数
export const DATE_FUNCTIONS: SqlKeyword[] = [
  {
    label: 'NOW',
    type: 'function',
    detail: 'Current timestamp',
    documentation: 'NOW() function returns the current date and time.',
    insertText: 'NOW()',
    priority: 10
  },
  {
    label: 'CURDATE',
    type: 'function',
    detail: 'Current date',
    documentation: 'CURDATE() function returns the current date.',
    insertText: 'CURDATE()',
    priority: 8
  },
  {
    label: 'DATE_FORMAT',
    type: 'function',
    detail: 'Format date',
    documentation: 'DATE_FORMAT() function formats a date as specified.',
    insertText: 'DATE_FORMAT(',
    priority: 6
  }
]

// 合并所有关键字
export const ALL_KEYWORDS: SqlKeyword[] = [
  ...CORE_KEYWORDS,
  ...COMMON_KEYWORDS,
  ...AGGREGATE_FUNCTIONS,
  ...STRING_FUNCTIONS,
  ...DATE_FUNCTIONS
]

// 按优先级排序
export const SORTED_KEYWORDS = ALL_KEYWORDS.sort((a, b) => b.priority - a.priority)

// 关键字映射（用于快速查找）
export const KEYWORD_MAP = new Map<string, SqlKeyword>()
ALL_KEYWORDS.forEach(keyword => {
  KEYWORD_MAP.set(keyword.label.toLowerCase(), keyword)
})

/**
 * 根据输入文本过滤关键字
 */
export function filterKeywords(input: string, maxResults: number = 10): SqlKeyword[] {
  if (!input) return SORTED_KEYWORDS.slice(0, maxResults)
  
  const lowerInput = input.toLowerCase()
  const results: SqlKeyword[] = []
  
  // 精确匹配优先
  for (const keyword of SORTED_KEYWORDS) {
    if (keyword.label.toLowerCase().startsWith(lowerInput)) {
      results.push(keyword)
      if (results.length >= maxResults) break
    }
  }
  
  // 如果精确匹配不够，添加包含匹配
  if (results.length < maxResults) {
    for (const keyword of SORTED_KEYWORDS) {
      if (!results.includes(keyword) && 
          keyword.label.toLowerCase().includes(lowerInput)) {
        results.push(keyword)
        if (results.length >= maxResults) break
      }
    }
  }
  
  return results
}
