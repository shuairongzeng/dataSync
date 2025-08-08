# Frontend Integration Guide for Enhanced Caching

## Overview
This guide explains how to integrate the new backend caching system with the frontend query interface.

## API Changes

### New Enhanced Query Endpoint
Replace the old query execution with the new enhanced endpoint:

**Old**: `POST /api/query/execute`
**New**: `POST /api/enhanced-query/execute`

### Request Format
```javascript
{
  "connectionId": 1,
  "sql": "SELECT * FROM users",
  "schema": "public",
  "useCache": true,        // Enable/disable caching
  "saveHistory": true      // Save to query history
}
```

### Response Format
Same as before, but with potential cache indicators:
```javascript
{
  "columns": ["id", "name", "email"],
  "rows": [[1, "John", "john@example.com"]],
  "rowCount": 1,
  "executionTime": 150,
  "message": "Query executed successfully"
}
```

## Frontend Updates Needed

### 1. Update Query Service
```javascript
// In your query service file
const executeQuery = async (connectionId, sql, schema, options = {}) => {
  const response = await fetch('/api/enhanced-query/execute', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      connectionId,
      sql,
      schema,
      useCache: options.useCache !== false, // Default to true
      saveHistory: options.saveHistory !== false
    })
  });
  
  return response.json();
};
```

### 2. Add Cache Controls to UI
Add cache control options to the query interface:

```vue
<template>
  <div class="query-controls">
    <!-- Existing controls -->
    
    <!-- New cache controls -->
    <div class="cache-controls">
      <label>
        <input 
          type="checkbox" 
          v-model="useCaching" 
          @change="onCacheSettingChange"
        />
        Enable Caching
      </label>
      
      <button 
        @click="clearConnectionCache" 
        :disabled="!currentConnectionId"
        class="btn-secondary"
      >
        Clear Cache
      </button>
      
      <button 
        @click="warmupCache" 
        :disabled="!currentConnectionId"
        class="btn-secondary"
      >
        Warmup Cache
      </button>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      useCaching: true,
      // ... existing data
    }
  },
  
  methods: {
    async executeQuery() {
      try {
        this.loading = true;
        const result = await this.queryService.executeQuery(
          this.currentConnectionId,
          this.sqlContent,
          this.selectedSchema,
          {
            useCache: this.useCaching,
            saveHistory: true
          }
        );
        
        this.queryResult = result;
        // ... handle result
      } catch (error) {
        // ... handle error
      } finally {
        this.loading = false;
      }
    },
    
    async clearConnectionCache() {
      try {
        await fetch(`/api/enhanced-query/cache/${this.currentConnectionId}`, {
          method: 'DELETE',
          headers: { 'Authorization': `Bearer ${this.token}` }
        });
        
        this.$message.success('Cache cleared successfully');
      } catch (error) {
        this.$message.error('Failed to clear cache');
      }
    },
    
    async warmupCache() {
      try {
        await fetch(`/api/enhanced-query/cache/warmup/${this.currentConnectionId}?schema=${this.selectedSchema}`, {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${this.token}` }
        });
        
        this.$message.success('Cache warmup completed');
      } catch (error) {
        this.$message.error('Failed to warmup cache');
      }
    }
  }
}
</script>
```

### 3. Update Table/Schema Loading
Use cached endpoints for faster table and schema loading:

```javascript
// Load tables with caching
const loadTables = async (connectionId, schema) => {
  const response = await fetch(
    `/api/enhanced-query/tables/${connectionId}?schema=${schema}&useCache=true`
  );
  return response.json();
};

// Load table columns with caching
const loadTableColumns = async (connectionId, tableName, schema) => {
  const response = await fetch(
    `/api/enhanced-query/tables/${connectionId}/${tableName}/columns?schema=${schema}&useCache=true`
  );
  return response.json();
};
```

### 4. Add Cache Status Indicators
Show cache status in the UI:

```vue
<template>
  <div class="query-status">
    <span v-if="lastQueryFromCache" class="cache-indicator">
      📦 From Cache
    </span>
    <span v-else class="cache-indicator">
      🔄 Fresh Query
    </span>
  </div>
</template>
```

## Migration Steps

1. **Update API calls**: Replace old endpoints with enhanced ones
2. **Add cache controls**: Implement UI controls for cache management
3. **Test functionality**: Verify caching works correctly
4. **Update documentation**: Document new cache features for users
5. **Monitor performance**: Check cache hit rates and performance improvements

## Configuration

Add cache settings to your frontend config:

```javascript
// config.js
export const cacheConfig = {
  defaultEnabled: true,
  showCacheStatus: true,
  allowCacheControl: true,
  warmupOnConnect: false
};
```

## Benefits for Users

1. **Faster Queries**: Repeated queries return instantly from cache
2. **Better Performance**: Reduced server load and faster UI responses
3. **Offline Capability**: Some data available even when connection is slow
4. **Smart Caching**: Automatic cache invalidation and cleanup
5. **User Control**: Users can enable/disable caching as needed