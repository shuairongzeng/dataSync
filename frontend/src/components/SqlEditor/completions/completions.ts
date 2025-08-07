import { CompletionContext, CompletionResult, Completion } from '@codemirror/autocomplete'
import { filterKeywords, SqlKeyword } from './keywords'

/**
 * 补全项类型
 */
export interface CompletionItem {
  label: string
  type: 'keyword' | 'table' | 'column' | 'function' | 'operator'
  detail?: string
  info?: string
  apply?: string
  boost?: number
}

/**
 * 将SqlKeyword转换为CodeMirror Completion
 */
export function sqlKeywordToCompletion(keyword: SqlKeyword): Completion {
  return {
    label: keyword.label,
    type: keyword.type,
    detail: keyword.detail,
    info: keyword.documentation,
    apply: keyword.insertText || keyword.label,
    boost: keyword.priority
  }
}

/**
 * 获取当前光标位置的单词
 */
export function getCurrentWord(context: CompletionContext): { word: string; from: number; to: number } {
  const { state, pos } = context
  const line = state.doc.lineAt(pos)
  const lineText = line.text
  const linePos = pos - line.from
  
  // 查找单词边界
  let start = linePos
  let end = linePos
  
  // 向前查找单词开始位置
  while (start > 0 && /[a-zA-Z_]/.test(lineText[start - 1])) {
    start--
  }
  
  // 向后查找单词结束位置
  while (end < lineText.length && /[a-zA-Z_0-9]/.test(lineText[end])) {
    end++
  }
  
  return {
    word: lineText.slice(start, end),
    from: line.from + start,
    to: line.from + end
  }
}

/**
 * 分析SQL上下文
 */
export function analyzeSqlContext(context: CompletionContext): {
  isAfterSelect: boolean
  isAfterFrom: boolean
  isAfterWhere: boolean
  isAfterJoin: boolean
  currentTable?: string
  tables: string[]
} {
  const { state, pos } = context
  const textBefore = state.doc.sliceString(0, pos).toLowerCase()
  
  // 简单的SQL解析
  const selectMatch = textBefore.match(/\bselect\b/g)
  const fromMatch = textBefore.match(/\bfrom\s+(\w+)/g)
  const whereMatch = textBefore.match(/\bwhere\b/g)
  const joinMatch = textBefore.match(/\bjoin\b/g)
  
  // 提取表名
  const tables: string[] = []
  if (fromMatch) {
    fromMatch.forEach(match => {
      const tableName = match.replace(/\bfrom\s+/i, '').trim()
      if (tableName) tables.push(tableName)
    })
  }
  
  return {
    isAfterSelect: !!selectMatch,
    isAfterFrom: !!fromMatch,
    isAfterWhere: !!whereMatch,
    isAfterJoin: !!joinMatch,
    tables
  }
}

/**
 * SQL关键字补全函数
 */
export function sqlKeywordCompletion(context: CompletionContext): CompletionResult | null {
  const { word, from, to } = getCurrentWord(context)
  
  // 如果没有输入任何字符，不显示补全
  if (!word && !context.explicit) {
    return null
  }
  
  // 获取匹配的关键字
  const keywords = filterKeywords(word, 10)
  
  if (keywords.length === 0) {
    return null
  }
  
  // 转换为CodeMirror补全格式
  const options = keywords.map(sqlKeywordToCompletion)
  
  return {
    from,
    to,
    options,
    validFor: /^[a-zA-Z_]*$/
  }
}

/**
 * 创建表名补全函数
 */
export function createTableCompletion(tables: string[]) {
  return function tableCompletion(context: CompletionContext): CompletionResult | null {
    const { word, from, to } = getCurrentWord(context)
    const sqlContext = analyzeSqlContext(context)
    
    // 只在FROM或JOIN后面提供表名补全
    if (!sqlContext.isAfterFrom && !sqlContext.isAfterJoin) {
      return null
    }
    
    // 过滤匹配的表名
    const matchedTables = tables.filter(table => 
      table.toLowerCase().includes(word.toLowerCase())
    )
    
    if (matchedTables.length === 0) {
      return null
    }
    
    const options: Completion[] = matchedTables.map(table => ({
      label: table,
      type: 'table',
      detail: 'Table',
      apply: table,
      boost: 50
    }))
    
    return {
      from,
      to,
      options,
      validFor: /^[a-zA-Z_0-9]*$/
    }
  }
}

/**
 * 创建字段补全函数
 */
export function createColumnCompletion(tableColumns: Map<string, string[]>) {
  return function columnCompletion(context: CompletionContext): CompletionResult | null {
    const { word, from, to } = getCurrentWord(context)
    const sqlContext = analyzeSqlContext(context)
    
    // 只在SELECT后面或WHERE条件中提供字段补全
    if (!sqlContext.isAfterSelect && !sqlContext.isAfterWhere) {
      return null
    }
    
    // 收集所有可用的字段
    const allColumns: Completion[] = []
    
    sqlContext.tables.forEach(table => {
      const columns = tableColumns.get(table.toLowerCase())
      if (columns) {
        columns.forEach(column => {
          if (column.toLowerCase().includes(word.toLowerCase())) {
            allColumns.push({
              label: column,
              type: 'column',
              detail: `Column from ${table}`,
              apply: column,
              boost: 40
            })
          }
        })
      }
    })
    
    if (allColumns.length === 0) {
      return null
    }
    
    return {
      from,
      to,
      options: allColumns,
      validFor: /^[a-zA-Z_0-9]*$/
    }
  }
}

/**
 * 组合多个补全函数
 */
export function combineCompletions(...completionFns: Array<(context: CompletionContext) => CompletionResult | null>) {
  return function combinedCompletion(context: CompletionContext): CompletionResult | null {
    const results: CompletionResult[] = []
    
    for (const fn of completionFns) {
      const result = fn(context)
      if (result) {
        results.push(result)
      }
    }
    
    if (results.length === 0) {
      return null
    }
    
    // 合并所有补全结果
    const allOptions: Completion[] = []
    let from = results[0].from
    let to = results[0].to
    
    results.forEach(result => {
      allOptions.push(...result.options)
      from = Math.min(from, result.from)
      to = Math.max(to, result.to)
    })
    
    // 按boost排序
    allOptions.sort((a, b) => (b.boost || 0) - (a.boost || 0))
    
    return {
      from,
      to,
      options: allOptions.slice(0, 20), // 限制最多20个选项
      validFor: /^[a-zA-Z_0-9]*$/
    }
  }
}
