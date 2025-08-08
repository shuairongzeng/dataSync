<template>
  <div class="sql-editor-wrapper" ref="editorContainer"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { EditorView, keymap, lineNumbers, highlightActiveLine } from '@codemirror/view'
import { EditorState } from '@codemirror/state'
import { sql } from '@codemirror/lang-sql'
import { oneDark } from '@codemirror/theme-one-dark'
import { autocompletion } from '@codemirror/autocomplete'
import { defaultKeymap, history } from '@codemirror/commands'
import { createSqlCompletion, SqlCompletionProvider } from './SqlCompletion'

// Props定义
interface Props {
  modelValue?: string
  placeholder?: string
  readonly?: boolean
  theme?: 'light' | 'dark'
  height?: string
  fontSize?: number
  tables?: string[]
  tableColumns?: Map<string, string[]>
  enableCompletion?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  placeholder: '请输入SQL查询语句...',
  readonly: false,
  theme: 'light',
  height: '300px',
  fontSize: 14,
  tables: () => [],
  tableColumns: () => new Map(),
  enableCompletion: true
})

// Emits定义
interface Emits {
  (e: 'update:modelValue', value: string): void
  (e: 'change', value: string): void
  (e: 'focus'): void
  (e: 'blur'): void
}

const emit = defineEmits<Emits>()

// 响应式数据
const editorContainer = ref<HTMLElement>()
let editorView: EditorView | null = null
let completionProvider: SqlCompletionProvider | null = null

// 创建编辑器状态
const createEditorState = (content: string) => {
  const extensions = [
    // 基础功能
    lineNumbers(),
    highlightActiveLine(),
    history(),
    keymap.of(defaultKeymap),

    // SQL 语言支持
    sql(),

    // 更新监听器
    EditorView.updateListener.of((update) => {
      if (update.docChanged) {
        const newValue = update.state.doc.toString()
        emit('update:modelValue', newValue)
        emit('change', newValue)
      }
    }),

    // 事件处理
    EditorView.domEventHandlers({
      focus: () => emit('focus'),
      blur: () => emit('blur')
    }),

    // 主题样式
    EditorView.theme({
      '&': {
        fontSize: `${props.fontSize}px`,
        height: props.height
      },
      '.cm-content': {
        padding: '12px',
        minHeight: props.height,
        fontFamily: '"Consolas", "Monaco", "Courier New", monospace'
      },
      '.cm-focused': {
        outline: 'none'
      },
      '.cm-editor': {
        borderRadius: '6px',
        border: '1px solid #dcdfe6'
      },
      '.cm-editor.cm-focused': {
        borderColor: '#409eff',
        boxShadow: '0 0 0 2px rgba(64, 158, 255, 0.2)'
      }
    })
  ]

  // 添加只读状态
  if (props.readonly) {
    extensions.push(EditorState.readOnly.of(true))
  }

  // 添加自动补全
  if (props.enableCompletion) {
    // 创建SQL补全提供器
    const sqlCompletion = createSqlCompletion(props.tables, props.tableColumns)
    completionProvider = sqlCompletion.provider
    
    console.log('Creating SQL completion with tables:', props.tables)
    
    extensions.push(
      autocompletion({
        override: [sqlCompletion.extension],
        activateOnTyping: true,
        maxRenderedOptions: 15,
        defaultKeymap: true,
        closeOnBlur: true,
        selectOnOpen: false,
        icons: false
      })
    )
  }

  // 添加暗色主题
  if (props.theme === 'dark') {
    extensions.push(oneDark)
  }

  return EditorState.create({
    doc: content,
    extensions
  })
}

// 初始化编辑器
const initEditor = async () => {
  await nextTick()

  if (!editorContainer.value) return

  const state = createEditorState(props.modelValue)

  editorView = new EditorView({
    state,
    parent: editorContainer.value
  })
}

// 更新编辑器内容
const updateContent = (newContent: string) => {
  if (!editorView) return
  
  const currentContent = editorView.state.doc.toString()
  if (currentContent !== newContent) {
    editorView.dispatch({
      changes: {
        from: 0,
        to: editorView.state.doc.length,
        insert: newContent
      }
    })
  }
}

// 获取编辑器内容
const getContent = (): string => {
  return editorView?.state.doc.toString() || ''
}

// 设置焦点
const focus = () => {
  editorView?.focus()
}

// 插入文本
const insertText = (text: string) => {
  if (!editorView) return
  
  const selection = editorView.state.selection.main
  editorView.dispatch({
    changes: {
      from: selection.from,
      to: selection.to,
      insert: text
    },
    selection: {
      anchor: selection.from + text.length
    }
  })
}

// 暴露方法给父组件
defineExpose({
  getContent,
  focus,
  insertText,
  editorView: () => editorView
})

// 监听props变化
watch(() => props.modelValue, (newValue) => {
  updateContent(newValue || '')
})

watch(() => props.theme, () => {
  // 主题变化时重新创建编辑器
  recreateEditor()
})

// 监听补全相关props变化
watch(() => [props.tables, props.tableColumns, props.enableCompletion], () => {
  console.log('Tables/columns changed:', props.tables, props.tableColumns)
  // 更新补全提供器数据
  if (completionProvider) {
    completionProvider.updateData(props.tables, props.tableColumns)
  }
  // 补全配置变化时重新创建编辑器
  recreateEditor()
}, { deep: true })

// 重新创建编辑器的通用方法
const recreateEditor = () => {
  if (editorView) {
    const content = editorView.state.doc.toString()
    editorView.destroy()
    editorView = null

    nextTick(() => {
      const state = createEditorState(content)
      editorView = new EditorView({
        state,
        parent: editorContainer.value!
      })
    })
  }
}

// 生命周期
onMounted(() => {
  initEditor()
})

onUnmounted(() => {
  if (editorView) {
    editorView.destroy()
    editorView = null
  }
})
</script>

<style scoped>
.sql-editor-wrapper {
  width: 100%;
  border-radius: 6px;
  overflow: hidden;
}

.sql-editor-wrapper :deep(.cm-editor) {
  width: 100%;
}

.sql-editor-wrapper :deep(.cm-scroller) {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}

.sql-editor-wrapper :deep(.cm-content) {
  white-space: pre-wrap;
  word-break: break-word;
}

.sql-editor-wrapper :deep(.cm-line) {
  line-height: 1.5;
}

/* 自动补全样式 */
.sql-editor-wrapper :deep(.cm-tooltip-autocomplete) {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  background: #fff;
  max-height: 200px;
  overflow-y: auto;
  z-index: 1000;
}

.sql-editor-wrapper :deep(.cm-completionLabel) {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
}

.sql-editor-wrapper :deep(.cm-completionDetail) {
  font-style: italic;
  color: #909399;
  font-size: 11px;
}

.sql-editor-wrapper :deep(.cm-completionIcon) {
  width: 16px;
  height: 16px;
  margin-right: 6px;
}

.sql-editor-wrapper :deep(.sql-completion-option) {
  padding: 4px 8px;
  border-radius: 3px;
  transition: background-color 0.2s;
}

.sql-editor-wrapper :deep(.sql-completion-option:hover) {
  background-color: #f5f7fa;
}

.sql-editor-wrapper :deep(.cm-completionIcon-keyword::before) {
  content: "K";
  color: #409eff;
  font-weight: bold;
  font-size: 10px;
}

.sql-editor-wrapper :deep(.cm-completionIcon-table::before) {
  content: "T";
  color: #67c23a;
  font-weight: bold;
  font-size: 10px;
}

.sql-editor-wrapper :deep(.cm-completionIcon-column::before) {
  content: "C";
  color: #e6a23c;
  font-weight: bold;
  font-size: 10px;
}
</style>
