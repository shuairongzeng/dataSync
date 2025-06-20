# API配置和跨域解决方案

## 当前配置概述

### 环境信息
- **前端地址**: http://localhost:3000
- **后端地址**: http://localhost:8080
- **跨域问题**: 不同端口会产生跨域请求

### 解决方案
我们采用 **Vite代理 + 环境变量** 的方式来解决跨域问题和API地址配置。

## 配置文件说明

### 1. 环境变量配置 (`.env.development`)

```env
# 后端API基础地址 (开发环境使用代理，设置为空字符串)
VITE_BASE_API = ""

# 开发环境代理目标地址
VITE_PROXY_DOMAIN = "http://localhost:8080"
```

**说明**：
- `VITE_BASE_API = ""`: 开发环境下设置为空，让请求通过Vite代理
- `VITE_PROXY_DOMAIN`: 代理的目标地址，指向后端服务

### 2. Vite代理配置 (`vite.config.ts`)

```typescript
server: {
  port: 3000,
  host: "0.0.0.0",
  proxy: {
    "/api": {
      target: "http://localhost:8080",  // 后端地址
      changeOrigin: true,               // 改变请求头中的origin
      rewrite: (path: string) => path.replace(/^\/api/, "/api")
    }
  }
}
```

**工作原理**：
- 前端发起请求: `http://localhost:3000/api/test/health`
- Vite代理转发到: `http://localhost:8080/api/test/health`
- 避免了跨域问题

### 3. HTTP客户端配置 (`src/utils/http/index.ts`)

```typescript
const { VITE_BASE_API } = import.meta.env;

const defaultConfig: AxiosRequestConfig = {
  baseURL: VITE_BASE_API || "",  // 开发环境为空，生产环境为完整URL
  timeout: 10000,
  // ...其他配置
};
```

## 不同环境的配置

### 开发环境 (Development)
```env
VITE_BASE_API = ""
VITE_PROXY_DOMAIN = "http://localhost:8080"
```
- 使用Vite代理解决跨域
- 前端请求相对路径 `/api/*`
- 代理转发到后端服务

### 生产环境 (Production)
```env
VITE_BASE_API = "https://api.yourdomain.com"
VITE_PROXY_DOMAIN = ""
```
- 直接请求完整的API地址
- 不使用代理
- 需要后端配置CORS或使用Nginx反向代理

## API接口列表

### 认证相关
- `POST /api/auth/signin` - 用户登录
- `POST /api/auth/signup` - 用户注册
- `GET /api/auth/me` - 获取当前用户信息

### 系统相关
- `GET /api/test/health` - 健康检查
- `GET /api/test/public` - 公共测试接口

### 数据库相关
- `GET /api/database/connections` - 获取数据库连接列表
- `POST /api/database/connections` - 创建数据库连接
- `POST /api/database/test-connection` - 测试数据库连接

### 同步任务相关
- `GET /api/sync/tasks` - 获取同步任务列表
- `POST /api/sync/tasks` - 创建同步任务
- `POST /api/sync/tasks/{id}/execute` - 执行同步任务

### 自定义查询相关
- `POST /api/custom-query/execute-and-save` - 执行自定义查询并保存

## 测试API连接

### 方法1: 使用内置测试页面
访问 `http://localhost:3000/#/database/api-test` 查看API连接测试页面。

### 方法2: 手动测试
```bash
# 测试健康检查接口
curl http://localhost:3000/api/test/health

# 或直接访问后端
curl http://localhost:8080/api/test/health
```

## 常见问题和解决方案

### 1. 代理不生效
**症状**: 请求仍然出现跨域错误
**解决方案**:
- 确认Vite开发服务器已重启
- 检查代理配置是否正确
- 确认请求路径以 `/api` 开头

### 2. 后端连接失败
**症状**: 请求超时或连接被拒绝
**解决方案**:
- 确认后端服务运行在8080端口
- 检查防火墙设置
- 确认后端健康检查接口可访问

### 3. CORS错误（生产环境）
**症状**: 生产环境出现跨域错误
**解决方案**:
- 后端配置CORS允许前端域名
- 使用Nginx反向代理
- 确保API地址配置正确

### 4. 环境变量不生效
**症状**: 配置修改后没有效果
**解决方案**:
- 重启Vite开发服务器
- 检查环境变量文件名是否正确
- 确认变量名以 `VITE_` 开头

## 后端CORS配置建议

如果后端需要配置CORS，可以参考以下配置：

### Spring Boot配置
```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

## 部署注意事项

### 开发环境
- 确保后端服务运行在8080端口
- 前端使用 `pnpm dev` 启动开发服务器
- 代理会自动处理跨域问题

### 生产环境
- 修改 `.env.production` 中的 `VITE_BASE_API`
- 确保后端配置了正确的CORS策略
- 或使用Nginx反向代理统一处理

## 验证配置是否正确

1. 启动后端服务 (端口8080)
2. 启动前端服务 (`pnpm dev`)
3. 访问测试页面: `http://localhost:3000/#/database/api-test`
4. 查看所有测试是否通过

如果测试通过，说明API配置正确，可以正常开发和使用。
