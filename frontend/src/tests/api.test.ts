import { describe, it, expect, vi, beforeEach } from 'vitest';
import { loginApi, registerApi, getCurrentUserApi } from '@/api/user';
import { getDbConnectionsApi, testDbConnectionApi } from '@/api/database';
import { getSystemHealthApi } from '@/api/system';

// Mock HTTP client
vi.mock('@/utils/http', () => ({
  http: {
    request: vi.fn()
  }
}));

describe('API Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('User API', () => {
    it('should login successfully', async () => {
      const mockResponse = {
        token: 'mock-jwt-token',
        username: 'testuser',
        email: 'test@example.com',
        role: 'USER'
      };

      const { http } = await import('@/utils/http');
      vi.mocked(http.request).mockResolvedValue(mockResponse);

      const result = await loginApi({
        username: 'testuser',
        password: 'password123'
      });

      expect(http.request).toHaveBeenCalledWith(
        'post',
        '/api/auth/signin',
        { data: { username: 'testuser', password: 'password123' } }
      );
      expect(result).toEqual(mockResponse);
    });

    it('should register user successfully', async () => {
      const mockResponse = {
        message: '用户注册成功',
        username: 'newuser'
      };

      const { http } = await import('@/utils/http');
      vi.mocked(http.request).mockResolvedValue(mockResponse);

      const result = await registerApi({
        username: 'newuser',
        password: 'password123',
        email: 'newuser@example.com'
      });

      expect(http.request).toHaveBeenCalledWith(
        'post',
        '/api/auth/signup',
        { 
          data: { 
            username: 'newuser', 
            password: 'password123',
            email: 'newuser@example.com'
          } 
        }
      );
      expect(result).toEqual(mockResponse);
    });

    it('should get current user info', async () => {
      const mockResponse = {
        id: 1,
        username: 'testuser',
        email: 'test@example.com',
        role: 'USER',
        enabled: true,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z'
      };

      const { http } = await import('@/utils/http');
      vi.mocked(http.request).mockResolvedValue(mockResponse);

      const result = await getCurrentUserApi();

      expect(http.request).toHaveBeenCalledWith('get', '/api/auth/me');
      expect(result).toEqual(mockResponse);
    });
  });

  describe('Database API', () => {
    it('should get database connections', async () => {
      const mockConnections = [
        {
          id: '1',
          name: 'Test MySQL',
          dbType: 'mysql',
          host: 'localhost',
          port: 3306,
          database: 'testdb',
          username: 'root',
          password: '******'
        }
      ];

      const { http } = await import('@/utils/http');
      vi.mocked(http.request).mockResolvedValue(mockConnections);

      const result = await getDbConnectionsApi();

      expect(http.request).toHaveBeenCalledWith('get', '/api/database/connections');
      expect(result).toEqual(mockConnections);
    });

    it('should test database connection', async () => {
      const mockTestResult = {
        success: true,
        message: '连接测试成功',
        connectionTime: 125
      };

      const testData = {
        dbType: 'mysql',
        host: 'localhost',
        port: 3306,
        database: 'testdb',
        username: 'root',
        password: 'password'
      };

      const { http } = await import('@/utils/http');
      vi.mocked(http.request).mockResolvedValue(mockTestResult);

      const result = await testDbConnectionApi(testData);

      expect(http.request).toHaveBeenCalledWith(
        'post',
        '/api/database/test-connection',
        { data: testData }
      );
      expect(result).toEqual(mockTestResult);
    });
  });

  describe('System API', () => {
    it('should get system health status', async () => {
      const mockHealth = {
        status: 'UP',
        timestamp: '2024-01-01T00:00:00Z',
        components: {
          database: { status: 'UP' },
          memory: { status: 'UP' },
          disk: { status: 'UP' },
          cpu: { status: 'UP' }
        }
      };

      const { http } = await import('@/utils/http');
      vi.mocked(http.request).mockResolvedValue(mockHealth);

      const result = await getSystemHealthApi();

      expect(http.request).toHaveBeenCalledWith('get', '/api/system/health');
      expect(result).toEqual(mockHealth);
    });
  });
});
