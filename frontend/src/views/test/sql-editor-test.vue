<template>
  <div class="sql-editor-test">
    <h2>SQL编辑器测试页面</h2>
    
    <div class="test-section">
      <h3>简化版SQL编辑器（测试基本输入）</h3>
      <SimpleSqlEditor
        v-model="sqlTextSimple"
        :height="'300px'"
        placeholder="请输入SQL查询语句..."
        @change="handleSqlChangeSimple"
      />

      <div class="output-section">
        <h4>当前SQL内容：</h4>
        <pre>{{ sqlTextSimple }}</pre>
      </div>
    </div>

    <div class="test-section">
      <h3>完整版SQL编辑器</h3>
      <SqlEditor
        v-model="sqlText"
        :height="'300px'"
        :enable-completion="true"
        placeholder="请输入SQL查询语句..."
        @change="handleSqlChange"
      />

      <div class="output-section">
        <h4>当前SQL内容：</h4>
        <pre>{{ sqlText }}</pre>
      </div>
    </div>

    <div class="test-section">
      <h3>带表名和字段的SQL编辑器</h3>
      <SqlEditor
        v-model="sqlText2"
        :height="'300px'"
        :tables="testTables"
        :table-columns="testTableColumns"
        :enable-completion="true"
        placeholder="请输入SQL查询语句（支持表名和字段补全）..."
        @change="handleSqlChange2"
      />
      
      <div class="output-section">
        <h4>当前SQL内容：</h4>
        <pre>{{ sqlText2 }}</pre>
      </div>
    </div>

    <div class="test-info">
      <h3>测试说明</h3>
      <ul>
        <li>输入 "SELECT" 然后按 Tab 键或 Ctrl+Space 测试关键字补全</li>
        <li>输入 "FROM " 然后输入表名前几个字符测试表名补全</li>
        <li>输入 "SELECT * FROM users WHERE " 然后输入字段名测试字段补全</li>
        <li>支持大小写不敏感匹配</li>
        <li>支持优先级排序</li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import SqlEditor from '@/components/SqlEditor/SqlEditor.vue'
import SimpleSqlEditor from '@/components/SqlEditor/SimpleSqlEditor.vue'

// 测试数据
const sqlTextSimple = ref('SELECT * FROM users WHERE ')
const sqlText = ref('SELECT * FROM users WHERE ')
const sqlText2 = ref('SELECT u.id, u.name FROM users u JOIN orders o ON ')

// 测试表名
const testTables = ref([
  'users',
  'orders', 
  'products',
  'categories',
  'user_profiles',
  'order_items'
])

// 测试表字段
const testTableColumns = ref(new Map([
  ['users', ['id', 'name', 'email', 'created_at', 'updated_at']],
  ['orders', ['id', 'user_id', 'total_amount', 'status', 'created_at']],
  ['products', ['id', 'name', 'price', 'category_id', 'description']],
  ['categories', ['id', 'name', 'parent_id']],
  ['user_profiles', ['id', 'user_id', 'avatar', 'bio', 'phone']],
  ['order_items', ['id', 'order_id', 'product_id', 'quantity', 'price']]
]))

// 事件处理
const handleSqlChangeSimple = (value: string) => {
  console.log('Simple SQL changed:', value)
}

const handleSqlChange = (value: string) => {
  console.log('SQL changed:', value)
}

const handleSqlChange2 = (value: string) => {
  console.log('SQL2 changed:', value)
}
</script>

<style scoped>
.sql-editor-test {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.test-section {
  margin-bottom: 40px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 20px;
}

.output-section {
  margin-top: 20px;
  padding: 15px;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.output-section pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.test-info {
  background-color: #e3f2fd;
  padding: 20px;
  border-radius: 8px;
  border-left: 4px solid #2196f3;
}

.test-info ul {
  margin: 10px 0;
  padding-left: 20px;
}

.test-info li {
  margin-bottom: 8px;
}

h2 {
  color: #333;
  margin-bottom: 30px;
}

h3 {
  color: #555;
  margin-bottom: 15px;
}

h4 {
  color: #666;
  margin-bottom: 10px;
}
</style>
