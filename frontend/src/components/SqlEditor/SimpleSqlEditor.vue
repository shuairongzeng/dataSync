<template>
  <div class="simple-sql-editor" ref="editorContainer"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { EditorView } from '@codemirror/view'
import { EditorState } from '@codemirror/state'
import { sql } from '@codemirror/lang-sql'
import { minimalSetup } from 'codemirror'

// Props定义
interface Props {
  modelValue?: string
  placeholder?: string
  height?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  placeholder: '请输入SQL查询语句...',
  height: '300px'
})

// Emits定义
interface Emits {
  (e: 'update:modelValue', value: string): void
  (e: 'change', value: string): void
}

const emit = defineEmits<Emits>()

// 响应式数据
const editorContainer = ref<HTMLElement>()
let editorView: EditorView | null = null

// 创建编辑器状态
const createEditorState = (content: string) => {
  return EditorState.create({
    doc: content,
    extensions: [
      minimalSetup,
      sql(),
      EditorView.updateListener.of((update) => {
        if (update.docChanged) {
          const newValue = update.state.doc.toString()
          emit('update:modelValue', newValue)
          emit('change', newValue)
        }
      }),
      EditorView.theme({
        '&': {
          height: props.height
        },
        '.cm-content': {
          padding: '12px',
          minHeight: props.height,
          fontFamily: '"Consolas", "Monaco", "Courier New", monospace'
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

// 监听props变化
watch(() => props.modelValue, (newValue) => {
  updateContent(newValue || '')
})

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
.simple-sql-editor {
  width: 100%;
  border-radius: 6px;
  overflow: hidden;
}

.simple-sql-editor :deep(.cm-editor) {
  width: 100%;
}

.simple-sql-editor :deep(.cm-scroller) {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}
</style>
