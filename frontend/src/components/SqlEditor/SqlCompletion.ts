import { CompletionContext, CompletionResult, Completion } from '@codemirror/autocomplete'

// SQL关键字列表
const SQL_KEYWORDS = [
  'SELECT', 'FROM', 'WHERE', 'JOIN', 'INNER JOIN', 'LEFT JOIN', 'RIGHT JOIN', 'FULL JOIN',
  'ON', 'GROUP BY', 'HAVING', 'ORDER BY', 'ASC', 'DESC', 'LIMIT', 'OFFSET',
  'INSERT', 'INTO', 'VALUES', 'UPDATE', 'SET', 'DELETE', 'CREATE', 'TABLE',
  'ALTER', 'DROP', 'INDEX', 'VIEW', 'DATABASE', 'SCHEMA',
  'AND', 'OR', 'NOT', 'IN', 'EXISTS', 'BETWEEN', 'LIKE', 'IS', 'NULL',
  'DISTINCT', 'ALL', 'AS', 'CASE', 'WHEN', 'THEN', 'ELSE', 'END',
  'COUNT', 'SUM', 'AVG', 'MIN', 'MAX', 'COALESCE', 'ISNULL'
]

// SQL操作符
const SQL_OPERATORS = ['=', '!=', '<>', '<', '>', '<=', '>=', '+', '-', '*', '/', '%']

// 补全项类型
export enum CompletionType {
  KEYWORD = 'keyword',
  TABLE = 'table',
  COLUMN = 'column',
  FUNCTION = 'function',
  OPERATOR = 'operator'
}

// 补全项接口
export interface SqlCompletionItem {
  label: string
  type: CompletionType
  detail?: string
  info?: string
  boost?: number
}

// SQL上下文分析器
export class SqlContextAnalyzer {
  private text: string
  private pos: number

  constructor(text: string, pos: number) {
    this.text = text.toLowerCase()
    this.pos = pos
  }

  // 获取当前光标位置的上下文
  getContext(): SqlContext {
    const beforeCursor = this.text.substring(0, this.pos)
    const afterCursor = this.text.substring(this.pos)
    
    // 获取当前单词
    const currentWord = this.getCurrentWord(beforeCursor)
    
    // 分析SQL语句结构
    const context: SqlContext = {
      currentWord,
      beforeCursor,
      afterCursor,
      expectsTableName: this.expectsTableName(beforeCursor),
      expectsColumnName: this.expectsColumnName(beforeCursor),
      expectsKeyword: this.expectsKeyword(beforeCursor),
      currentTable: this.getCurrentTable(beforeCursor),
      availableTables: this.getAvailableTables(beforeCursor)
    }

    return context
  }

  private getCurrentWord(text: string): string {
    const match = text.match(/(\w+)$/)
    return match ? match[1] : ''
  }

  private expectsTableName(text: string): boolean {
    // 检查是否在FROM、JOIN等关键字后
    const patterns = [
      /\bfrom\s+$/i,
      /\bjoin\s+$/i,
      /\binner\s+join\s+$/i,
      /\bleft\s+join\s+$/i,
      /\bright\s+join\s+$/i,
      /\bfull\s+join\s+$/i,
      /\binto\s+$/i,
      /\bupdate\s+$/i
    ]
    
    const result = patterns.some(pattern => pattern.test(text))
    console.log('expectsTableName check:', { text: text.slice(-20), result })
    return result
  }

  private expectsColumnName(text: string): boolean {
    // 检查是否在SELECT、WHERE等位置
    const patterns = [
      /\bselect\s+$/i,
      /\bselect\s+.*,\s*$/i,
      /\bwhere\s+$/i,
      /\bwhere\s+.*\s+(and|or)\s+$/i,
      /\bgroup\s+by\s+$/i,
      /\border\s+by\s+$/i,
      /\bhaving\s+$/i,
      /\bon\s+$/i,
      /\bset\s+$/i
    ]
    
    return patterns.some(pattern => pattern.test(text))
  }

  private expectsKeyword(text: string): boolean {
    // 检查是否期望SQL关键字
    const trimmed = text.trim()
    if (!trimmed) return true
    
    // 在语句开始或某些关键字后期望新的关键字
    const patterns = [
      /^\s*$/,
      /\bfrom\s+\w+\s*$/i,
      /\bwhere\s+.*$/i,
      /\bgroup\s+by\s+\w+\s*$/i,
      /\border\s+by\s+\w+(\s+(asc|desc))?\s*$/i
    ]
    
    return patterns.some(pattern => pattern.test(text))
  }

  private getCurrentTable(text: string): string | null {
    // 尝试找到当前操作的表名
    const fromMatch = text.match(/\bfrom\s+(\w+)/i)
    if (fromMatch) return fromMatch[1]
    
    const updateMatch = text.match(/\bupdate\s+(\w+)/i)
    if (updateMatch) return updateMatch[1]
    
    return null
  }

  private getAvailableTables(text: string): string[] {
    const tables: string[] = []
    
    // 从FROM子句中提取表名
    const fromMatches = text.match(/\bfrom\s+(\w+)(?:\s+(?:as\s+)?(\w+))?/gi)
    if (fromMatches) {
      fromMatches.forEach(match => {
        const parts = match.split(/\s+/)
        if (parts.length >= 2) {
          tables.push(parts[1])
        }
      })
    }
    
    // 从JOIN子句中提取表名
    const joinMatches = text.match(/\bjoin\s+(\w+)(?:\s+(?:as\s+)?(\w+))?/gi)
    if (joinMatches) {
      joinMatches.forEach(match => {
        const parts = match.split(/\s+/)
        if (parts.length >= 2) {
          tables.push(parts[parts.length - 1])
        }
      })
    }
    
    return [...new Set(tables)] // 去重
  }
}

// SQL上下文接口
export interface SqlContext {
  currentWord: string
  beforeCursor: string
  afterCursor: string
  expectsTableName: boolean
  expectsColumnName: boolean
  expectsKeyword: boolean
  currentTable: string | null
  availableTables: string[]
}

// SQL补全提供器
export class SqlCompletionProvider {
  private tables: string[]
  private tableColumns: Map<string, string[]>

  constructor(tables: string[] = [], tableColumns: Map<string, string[]> = new Map()) {
    this.tables = tables
    this.tableColumns = tableColumns
  }

  // 更新表和字段数据
  updateData(tables: string[], tableColumns: Map<string, string[]>) {
    this.tables = tables
    this.tableColumns = tableColumns
  }

  // 获取补全建议
  getCompletions(context: CompletionContext): CompletionResult | null {
    const text = context.state.doc.toString()
    const pos = context.pos
    
    console.log('SQL Completion triggered:', { text, pos, tables: this.tables.length })
    
    const analyzer = new SqlContextAnalyzer(text, pos)
    const sqlContext = analyzer.getContext()
    
    console.log('SQL Context:', sqlContext)
    
    const completions: Completion[] = []
    
    // 根据上下文添加不同类型的补全
    if (sqlContext.expectsTableName) {
      console.log('Adding table completions')
      completions.push(...this.getTableCompletions(sqlContext.currentWord))
    }
    
    if (sqlContext.expectsColumnName) {
      console.log('Adding column completions')
      completions.push(...this.getColumnCompletions(sqlContext.currentWord, sqlContext.currentTable))
    }
    
    if (sqlContext.expectsKeyword || !sqlContext.currentWord) {
      console.log('Adding keyword completions')
      completions.push(...this.getKeywordCompletions(sqlContext.currentWord))
    }
    
    // 如果没有特定上下文，提供所有类型的补全
    if (!sqlContext.expectsTableName && !sqlContext.expectsColumnName) {
      console.log('Adding all completions')
      completions.push(...this.getAllCompletions(sqlContext.currentWord))
    }
    
    // 过滤和排序
    const filteredCompletions = this.filterAndSort(completions, sqlContext.currentWord)
    
    console.log('Final completions:', filteredCompletions.length, filteredCompletions)
    
    if (filteredCompletions.length === 0) {
      return null
    }
    
    return {
      from: pos - sqlContext.currentWord.length,
      options: filteredCompletions
    }
  }

  private getTableCompletions(prefix: string): Completion[] {
    return this.tables
      .filter(table => this.matchesPrefix(table, prefix))
      .map(table => ({
        label: table,
        type: 'table',
        detail: 'table',
        boost: 10
      }))
  }

  private getColumnCompletions(prefix: string, currentTable: string | null): Completion[] {
    const completions: Completion[] = []
    
    if (currentTable && this.tableColumns.has(currentTable.toLowerCase())) {
      // 当前表的字段
      const columns = this.tableColumns.get(currentTable.toLowerCase()) || []
      completions.push(...columns
        .filter(column => this.matchesPrefix(column, prefix))
        .map(column => ({
          label: column,
          type: 'column',
          detail: `column from ${currentTable}`,
          boost: 15
        }))
      )
    } else {
      // 所有表的字段
      this.tableColumns.forEach((columns, tableName) => {
        completions.push(...columns
          .filter(column => this.matchesPrefix(column, prefix))
          .map(column => ({
            label: column,
            type: 'column',
            detail: `column from ${tableName}`,
            boost: 5
          }))
        )
      })
    }
    
    return completions
  }

  private getKeywordCompletions(prefix: string): Completion[] {
    return SQL_KEYWORDS
      .filter(keyword => this.matchesPrefix(keyword, prefix))
      .map(keyword => ({
        label: keyword,
        type: 'keyword',
        detail: 'SQL keyword',
        boost: 8
      }))
  }

  private getAllCompletions(prefix: string): Completion[] {
    const completions: Completion[] = []
    
    // 添加表名
    completions.push(...this.getTableCompletions(prefix))
    
    // 添加字段名
    completions.push(...this.getColumnCompletions(prefix, null))
    
    // 添加关键字
    completions.push(...this.getKeywordCompletions(prefix))
    
    return completions
  }

  private matchesPrefix(text: string, prefix: string): boolean {
    if (!prefix) return true
    return text.toLowerCase().startsWith(prefix.toLowerCase())
  }

  private filterAndSort(completions: Completion[], prefix: string): Completion[] {
    // 去重
    const uniqueCompletions = completions.filter((completion, index, self) => 
      index === self.findIndex(c => c.label === completion.label && c.type === completion.type)
    )
    
    // 排序：boost值高的在前，然后按字母顺序
    return uniqueCompletions.sort((a, b) => {
      const boostDiff = (b.boost || 0) - (a.boost || 0)
      if (boostDiff !== 0) return boostDiff
      return a.label.localeCompare(b.label)
    })
  }
}

// 创建补全扩展
export function createSqlCompletion(tables: string[] = [], tableColumns: Map<string, string[]> = new Map()) {
  const provider = new SqlCompletionProvider(tables, tableColumns)
  
  return {
    provider,
    extension: (context: CompletionContext) => provider.getCompletions(context)
  }
}