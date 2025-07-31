import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { ElMessage } from 'element-plus';
import DatabaseQuery from '../query.vue';
import * as databaseApi from '@/api/database';

// Mock Element Plus
vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn()
  },
  ElMessageBox: {
    confirm: vi.fn()
  }
}));

// Mock API functions
vi.mock('@/api/database', () => ({
  getDbConnectionsApi: vi.fn(),
  getTablesApi: vi.fn(),
  getTableColumnsApi: vi.fn(),
  executeSqlQueryApi: vi.fn(),
  executeCustomQueryApi: vi.fn(),
  getQueryHistoryApi: vi.fn(),
  saveQueryHistoryApi: vi.fn(),
  deleteQueryHistoryApi: vi.fn()
}));

// Mock CodeMirror
vi.mock('codemirror-editor-vue3', () => ({
  default: {
    name: 'CodemirrorEditorVue3',
    template: '<div class="mock-codemirror"></div>',
    props: ['modelValue', 'options'],
    emits: ['update:modelValue']
  }
}));

describe('DatabaseQuery', () => {
  let wrapper: any;
  
  const mockConnections = [
    { id: 1, name: 'Test DB 1', dbType: 'mysql' },
    { id: 2, name: 'Test DB 2', dbType: 'postgresql' }
  ];
  
  const mockTables = ['users', 'orders', 'products'];
  
  const mockColumns = [
    {
      columnName: 'id',
      dataType: 'bigint',
      nullable: false,
      isPrimaryKey: true,
      isAutoIncrement: true,
      remarks: 'Primary key'
    },
    {
      columnName: 'name',
      dataType: 'varchar(255)',
      nullable: false,
      isPrimaryKey: false,
      isAutoIncrement: false,
      remarks: 'User name'
    }
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    
    // Setup API mocks
    vi.mocked(databaseApi.getDbConnectionsApi).mockResolvedValue(mockConnections);
    vi.mocked(databaseApi.getTablesApi).mockResolvedValue(mockTables);
    vi.mocked(databaseApi.getTableColumnsApi).mockResolvedValue(mockColumns);
    
    wrapper = mount(DatabaseQuery, {
      global: {
        stubs: {
          'el-card': true,
          'el-row': true,
          'el-col': true,
          'el-select': true,
          'el-option': true,
          'el-button': true,
          'el-tree': true,
          'el-input': true,
          'el-table': true,
          'el-table-column': true,
          'el-dialog': true,
          'el-form': true,
          'el-form-item': true,
          'el-empty': true,
          'el-skeleton': true,
          'el-progress': true,
          'el-icon': true,
          'codemirror-editor-vue3': true
        }
      }
    });
  });

  describe('Table Structure Loading', () => {
    it('should load connections on mount', async () => {
      await wrapper.vm.$nextTick();
      expect(databaseApi.getDbConnectionsApi).toHaveBeenCalled();
    });

    it('should load tables when connection is selected', async () => {
      // Set selected connection
      await wrapper.setData({ selectedConnectionId: '1' });
      
      // Trigger the watch effect
      await wrapper.vm.$nextTick();
      
      expect(databaseApi.getTablesApi).toHaveBeenCalledWith('1', undefined);
    });

    it('should load table columns for each table', async () => {
      await wrapper.setData({ selectedConnectionId: '1' });
      
      // Call loadTables directly
      await wrapper.vm.loadTables();
      
      expect(databaseApi.getTableColumnsApi).toHaveBeenCalledTimes(mockTables.length);
      mockTables.forEach(tableName => {
        expect(databaseApi.getTableColumnsApi).toHaveBeenCalledWith('1', tableName, undefined);
      });
    });

    it('should handle table loading errors gracefully', async () => {
      vi.mocked(databaseApi.getTablesApi).mockRejectedValue(new Error('Connection failed'));
      
      await wrapper.setData({ selectedConnectionId: '1' });
      await wrapper.vm.loadTables();
      
      expect(ElMessage.error).toHaveBeenCalledWith(expect.stringContaining('加载表结构失败'));
    });

    it('should handle column loading errors gracefully', async () => {
      vi.mocked(databaseApi.getTableColumnsApi).mockRejectedValue(new Error('Column fetch failed'));
      
      await wrapper.setData({ selectedConnectionId: '1' });
      await wrapper.vm.loadTables();
      
      // Should still show tables even if column loading fails
      expect(wrapper.vm.tableTreeData).toHaveLength(mockTables.length);
    });
  });

  describe('Table Search', () => {
    beforeEach(async () => {
      await wrapper.setData({ 
        selectedConnectionId: '1',
        tableTreeData: [
          { id: 'users', label: 'users (2 列)', type: 'table', children: [] },
          { id: 'orders', label: 'orders (3 列)', type: 'table', children: [] },
          { id: 'products', label: 'products (4 列)', type: 'table', children: [] }
        ]
      });
    });

    it('should filter tables based on search text', async () => {
      await wrapper.setData({ tableSearchText: 'user' });
      wrapper.vm.filterTables();
      
      expect(wrapper.vm.filteredTableTreeData).toHaveLength(1);
      expect(wrapper.vm.filteredTableTreeData[0].id).toBe('users');
    });

    it('should show all tables when search text is empty', async () => {
      await wrapper.setData({ tableSearchText: '' });
      wrapper.vm.filterTables();
      
      expect(wrapper.vm.filteredTableTreeData).toHaveLength(3);
    });

    it('should handle case-insensitive search', async () => {
      await wrapper.setData({ tableSearchText: 'USER' });
      wrapper.vm.filterTables();
      
      expect(wrapper.vm.filteredTableTreeData).toHaveLength(1);
      expect(wrapper.vm.filteredTableTreeData[0].id).toBe('users');
    });
  });

  describe('SQL Generation', () => {
    const mockTableData = {
      id: 'users',
      label: 'users (2 列)',
      type: 'table',
      children: [
        { id: 'users_id', label: 'id (bigint) NOT NULL PK', type: 'column' },
        { id: 'users_name', label: 'name (varchar) NOT NULL', type: 'column' }
      ]
    };

    it('should generate SELECT SQL when table is clicked', async () => {
      await wrapper.vm.handleTableClick(mockTableData);
      
      expect(wrapper.vm.sqlContent).toContain('SELECT');
      expect(wrapper.vm.sqlContent).toContain('FROM users');
      expect(wrapper.vm.sqlContent).toContain('id, name');
    });

    it('should generate different types of SQL via right-click menu', async () => {
      // Test SELECT SQL generation
      wrapper.vm.generateSelectSql('users', mockTableData);
      expect(wrapper.vm.sqlContent).toContain('SELECT');
      
      // Test COUNT SQL generation
      wrapper.vm.generateCountSql('users');
      expect(wrapper.vm.sqlContent).toContain('COUNT(*)');
      
      // Test INSERT SQL generation
      wrapper.vm.generateInsertSql('users', mockTableData);
      expect(wrapper.vm.sqlContent).toContain('INSERT INTO users');
      
      // Test UPDATE SQL generation
      wrapper.vm.generateUpdateSql('users', mockTableData);
      expect(wrapper.vm.sqlContent).toContain('UPDATE users');
      
      // Test DELETE SQL generation
      wrapper.vm.generateDeleteSql('users');
      expect(wrapper.vm.sqlContent).toContain('DELETE FROM users');
    });

    it('should handle column clicks', async () => {
      const mockColumnData = {
        id: 'users_name',
        label: 'name (varchar) NOT NULL',
        type: 'column'
      };
      
      await wrapper.vm.handleTableClick(mockColumnData);
      expect(wrapper.vm.sqlContent).toContain('name');
    });
  });

  describe('Retry Mechanism', () => {
    it('should retry loading table columns when retry button is clicked', async () => {
      vi.mocked(databaseApi.getTableColumnsApi).mockRejectedValueOnce(new Error('First attempt failed'))
        .mockResolvedValueOnce(mockColumns);
      
      await wrapper.setData({ selectedConnectionId: '1' });
      await wrapper.vm.retryLoadTable('users');
      
      expect(databaseApi.getTableColumnsApi).toHaveBeenCalledWith('1', 'users', undefined);
      expect(ElMessage.success).toHaveBeenCalledWith(expect.stringContaining('成功加载表 users'));
    });

    it('should handle retry failures', async () => {
      vi.mocked(databaseApi.getTableColumnsApi).mockRejectedValue(new Error('Retry failed'));
      
      await wrapper.setData({ selectedConnectionId: '1' });
      await wrapper.vm.retryLoadTable('users');
      
      expect(ElMessage.error).toHaveBeenCalledWith(expect.stringContaining('重试加载表 users 失败'));
    });
  });

  describe('Loading States', () => {
    it('should show loading state during table loading', async () => {
      await wrapper.setData({ loadingTables: true });
      
      expect(wrapper.vm.loadingTables).toBe(true);
    });

    it('should update loading progress during table loading', async () => {
      await wrapper.setData({ selectedConnectionId: '1' });
      
      // Mock a slow loading process
      let resolvePromise: (value: any) => void;
      const slowPromise = new Promise(resolve => {
        resolvePromise = resolve;
      });
      
      vi.mocked(databaseApi.getTablesApi).mockReturnValue(slowPromise);
      
      const loadPromise = wrapper.vm.loadTables();
      
      // Check initial loading state
      expect(wrapper.vm.loadingTables).toBe(true);
      expect(wrapper.vm.loadingProgress).toBe(0);
      
      // Resolve the promise
      resolvePromise!(mockTables);
      await loadPromise;
      
      expect(wrapper.vm.loadingTables).toBe(false);
    });
  });
});
