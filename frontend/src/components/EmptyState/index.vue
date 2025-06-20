<template>
  <div class="empty-state">
    <div class="empty-image">
      <component :is="iconComponent" v-if="iconComponent" />
      <img v-else-if="image" :src="image" :alt="description" />
      <el-icon v-else size="80" color="#c0c4cc">
        <Box />
      </el-icon>
    </div>
    
    <div class="empty-content">
      <h3 class="empty-title">{{ title }}</h3>
      <p class="empty-description">{{ description }}</p>
    </div>
    
    <div v-if="showAction" class="empty-actions">
      <slot name="actions">
        <el-button type="primary" @click="handleAction">
          {{ actionText }}
        </el-button>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { Box } from "@element-plus/icons-vue";

interface Props {
  title?: string;
  description?: string;
  image?: string;
  icon?: string;
  actionText?: string;
  showAction?: boolean;
  type?: 'default' | 'no-data' | 'no-connection' | 'no-permission' | 'search-empty';
}

const props = withDefaults(defineProps<Props>(), {
  title: "暂无数据",
  description: "当前没有可显示的内容",
  actionText: "刷新",
  showAction: true,
  type: "default"
});

const emit = defineEmits<{
  action: [];
}>();

// 根据类型设置默认内容
const title = computed(() => {
  if (props.title !== "暂无数据") return props.title;
  
  const typeMap = {
    'no-data': '暂无数据',
    'no-connection': '连接失败',
    'no-permission': '无权限访问',
    'search-empty': '搜索无结果',
    'default': '暂无数据'
  };
  
  return typeMap[props.type] || typeMap.default;
});

const description = computed(() => {
  if (props.description !== "当前没有可显示的内容") return props.description;
  
  const typeMap = {
    'no-data': '当前没有可显示的数据，请稍后再试',
    'no-connection': '网络连接失败，请检查网络设置',
    'no-permission': '您没有权限访问此内容',
    'search-empty': '没有找到符合条件的结果，请尝试其他关键词',
    'default': '当前没有可显示的内容'
  };
  
  return typeMap[props.type] || typeMap.default;
});

const iconComponent = computed(() => {
  if (props.icon) return props.icon;
  return null;
});

const handleAction = () => {
  emit('action');
};
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  min-height: 300px;
}

.empty-image {
  margin-bottom: 24px;
}

.empty-image img {
  max-width: 200px;
  max-height: 150px;
  opacity: 0.8;
}

.empty-content {
  margin-bottom: 24px;
}

.empty-title {
  margin: 0 0 12px 0;
  font-size: 18px;
  font-weight: 500;
  color: #303133;
}

.empty-description {
  margin: 0;
  font-size: 14px;
  color: #909399;
  line-height: 1.5;
  max-width: 400px;
}

.empty-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .empty-state {
    padding: 40px 16px;
    min-height: 250px;
  }
  
  .empty-image img {
    max-width: 150px;
    max-height: 120px;
  }
  
  .empty-title {
    font-size: 16px;
  }
  
  .empty-description {
    font-size: 13px;
  }
}
</style>
