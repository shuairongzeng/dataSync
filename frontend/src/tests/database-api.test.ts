import { describe, it, expect, beforeEach, vi } from 'vitest';
import { getDbConnectionsApi } from '@/api/database';
import { http } from '@/utils/http';

// Mock the http utility
vi.mock('@/utils/http', () => ({
  http: {
    request: vi.fn()
  }
}));

describe('Database API Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getDbConnectionsApi', () => {
    it('should call the correct endpoint', async () => {
      const mockConnections = [
        {
          id: 1,
          name: '测试连接',
          dbType: 'mysql',
          host: 'localhost',
          port: 3306,
          database: 'test',
          username: 'root',
          password: '******',
          enabled: true,
          createdAt: '2024-01-01 10:00:00',
          updatedAt: '2024-01-01 10:00:00'
        }
      ];

      (http.request as any).mockResolvedValue(mockConnections);

      const result = await getDbConnectionsApi();

      expect(http.request).toHaveBeenCalledWith('get', '/api/database/connections');
      expect(result).toEqual(mockConnections);
    });

    it('should handle empty response', async () => {
      (http.request as any).mockResolvedValue([]);

      const result = await getDbConnectionsApi();

      expect(result).toEqual([]);
    });

    it('should handle API errors', async () => {
      const mockError = new Error('Network error');
      (http.request as any).mockRejectedValue(mockError);

      await expect(getDbConnectionsApi()).rejects.toThrow('Network error');
    });

    it('should handle server error response', async () => {
      const mockError = {
        response: {
          status: 500,
          data: {
            error: '服务器内部错误',
            message: '数据库连接失败'
          }
        }
      };
      (http.request as any).mockRejectedValue(mockError);

      await expect(getDbConnectionsApi()).rejects.toEqual(mockError);
    });

    it('should handle authentication error', async () => {
      const mockError = {
        response: {
          status: 401,
          data: {
            error: '认证失败',
            message: '请重新登录'
          }
        }
      };
      (http.request as any).mockRejectedValue(mockError);

      await expect(getDbConnectionsApi()).rejects.toEqual(mockError);
    });
  });
});

// Integration test helper function
export const testDatabaseConnection = async () => {
  try {
    console.log('Testing database connection API...');
    const connections = await getDbConnectionsApi();
    console.log('✅ API call successful');
    console.log('📊 Connections received:', connections.length);
    
    if (connections.length > 0) {
      console.log('📋 First connection:', {
        id: connections[0].id,
        name: connections[0].name,
        dbType: connections[0].dbType,
        host: connections[0].host,
        port: connections[0].port
      });
    }
    
    return true;
  } catch (error: any) {
    console.error('❌ API call failed:', error.message);
    if (error.response) {
      console.error('📄 Response status:', error.response.status);
      console.error('📄 Response data:', error.response.data);
    }
    return false;
  }
};
