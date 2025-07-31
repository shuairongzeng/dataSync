import { ElMessage, ElNotification } from 'element-plus';

/**
 * 错误类型枚举
 */
export enum ErrorType {
  NETWORK = 'NETWORK',
  VALIDATION = 'VALIDATION',
  AUTHENTICATION = 'AUTHENTICATION',
  AUTHORIZATION = 'AUTHORIZATION',
  BUSINESS = 'BUSINESS',
  SYSTEM = 'SYSTEM'
}

/**
 * 错误信息接口
 */
export interface ErrorInfo {
  type: ErrorType;
  code?: string;
  message: string;
  details?: any;
  timestamp?: number;
}

/**
 * 错误处理器类
 */
export class ErrorHandler {
  
  /**
   * 处理HTTP错误
   */
  static handleHttpError(error: any): ErrorInfo {
    const timestamp = Date.now();
    
    // 网络错误
    if (!error.response) {
      const errorInfo: ErrorInfo = {
        type: ErrorType.NETWORK,
        message: '网络连接失败，请检查网络设置',
        timestamp
      };
      this.showError(errorInfo);
      return errorInfo;
    }
    
    const { status, data } = error.response;
    let errorInfo: ErrorInfo;
    
    switch (status) {
      case 400:
        errorInfo = {
          type: ErrorType.VALIDATION,
          code: data?.code || 'BAD_REQUEST',
          message: data?.message || '请求参数错误',
          details: data?.details,
          timestamp
        };
        break;
        
      case 401:
        errorInfo = {
          type: ErrorType.AUTHENTICATION,
          code: data?.code || 'UNAUTHORIZED',
          message: data?.message || '身份验证失败，请重新登录',
          timestamp
        };
        break;
        
      case 403:
        errorInfo = {
          type: ErrorType.AUTHORIZATION,
          code: data?.code || 'FORBIDDEN',
          message: data?.message || '权限不足，无法执行此操作',
          timestamp
        };
        break;
        
      case 404:
        errorInfo = {
          type: ErrorType.BUSINESS,
          code: data?.code || 'NOT_FOUND',
          message: data?.message || '请求的资源不存在',
          timestamp
        };
        break;
        
      case 500:
        errorInfo = {
          type: ErrorType.SYSTEM,
          code: data?.code || 'INTERNAL_ERROR',
          message: data?.message || '服务器内部错误，请稍后重试',
          details: data?.details,
          timestamp
        };
        break;
        
      default:
        errorInfo = {
          type: ErrorType.SYSTEM,
          code: data?.code || 'UNKNOWN_ERROR',
          message: data?.message || `请求失败 (${status})`,
          timestamp
        };
    }
    
    this.showError(errorInfo);
    return errorInfo;
  }
  
  /**
   * 处理业务错误
   */
  static handleBusinessError(message: string, code?: string, details?: any): ErrorInfo {
    const errorInfo: ErrorInfo = {
      type: ErrorType.BUSINESS,
      code,
      message,
      details,
      timestamp: Date.now()
    };
    
    this.showError(errorInfo);
    return errorInfo;
  }
  
  /**
   * 处理验证错误
   */
  static handleValidationError(message: string, details?: any): ErrorInfo {
    const errorInfo: ErrorInfo = {
      type: ErrorType.VALIDATION,
      code: 'VALIDATION_ERROR',
      message,
      details,
      timestamp: Date.now()
    };
    
    this.showError(errorInfo);
    return errorInfo;
  }
  
  /**
   * 显示错误信息
   */
  static showError(errorInfo: ErrorInfo) {
    switch (errorInfo.type) {
      case ErrorType.NETWORK:
        ElNotification({
          title: '网络错误',
          message: errorInfo.message,
          type: 'error',
          duration: 5000
        });
        break;
        
      case ErrorType.AUTHENTICATION:
        ElNotification({
          title: '认证失败',
          message: errorInfo.message,
          type: 'warning',
          duration: 5000
        });
        // 可以在这里添加跳转到登录页的逻辑
        break;
        
      case ErrorType.AUTHORIZATION:
        ElMessage({
          message: errorInfo.message,
          type: 'warning',
          duration: 3000
        });
        break;
        
      case ErrorType.VALIDATION:
        ElMessage({
          message: errorInfo.message,
          type: 'warning',
          duration: 3000
        });
        break;
        
      case ErrorType.BUSINESS:
        ElMessage({
          message: errorInfo.message,
          type: 'error',
          duration: 4000
        });
        break;
        
      case ErrorType.SYSTEM:
        ElNotification({
          title: '系统错误',
          message: errorInfo.message,
          type: 'error',
          duration: 6000
        });
        break;
        
      default:
        ElMessage({
          message: errorInfo.message,
          type: 'error',
          duration: 3000
        });
    }
  }
  
  /**
   * 格式化错误消息
   */
  static formatErrorMessage(error: any): string {
    if (typeof error === 'string') {
      return error;
    }
    
    if (error?.message) {
      return error.message;
    }
    
    if (error?.response?.data?.message) {
      return error.response.data.message;
    }
    
    return '操作失败，请稍后重试';
  }
  
  /**
   * 记录错误日志
   */
  static logError(error: any, context?: string) {
    const errorLog = {
      timestamp: new Date().toISOString(),
      context: context || 'Unknown',
      error: {
        message: error?.message || 'Unknown error',
        stack: error?.stack,
        response: error?.response?.data
      }
    };
    
    console.error('Error Log:', errorLog);
    
    // 在生产环境中，可以将错误日志发送到服务器
    if (process.env.NODE_ENV === 'production') {
      // 发送错误日志到服务器的逻辑
    }
  }
}

/**
 * 创建带有错误处理的异步函数包装器
 */
export function withErrorHandling<T extends (...args: any[]) => Promise<any>>(
  fn: T,
  context?: string
): T {
  return (async (...args: any[]) => {
    try {
      return await fn(...args);
    } catch (error) {
      ErrorHandler.logError(error, context);
      ErrorHandler.handleHttpError(error);
      throw error;
    }
  }) as T;
}

/**
 * 数据库相关错误的特殊处理
 */
export class DatabaseErrorHandler extends ErrorHandler {
  
  static handleConnectionError(error: any): ErrorInfo {
    const message = this.formatConnectionErrorMessage(error);
    return this.handleBusinessError(message, 'CONNECTION_ERROR');
  }
  
  static handleQueryError(error: any): ErrorInfo {
    const message = this.formatQueryErrorMessage(error);
    return this.handleBusinessError(message, 'QUERY_ERROR');
  }
  
  private static formatConnectionErrorMessage(error: any): string {
    const errorMsg = this.formatErrorMessage(error).toLowerCase();
    
    if (errorMsg.includes('connection refused')) {
      return '无法连接到数据库，请检查数据库服务是否正常运行';
    } else if (errorMsg.includes('access denied')) {
      return '数据库访问被拒绝，请检查用户名和密码';
    } else if (errorMsg.includes('timeout')) {
      return '数据库连接超时，请检查网络连接';
    } else if (errorMsg.includes('unknown database')) {
      return '指定的数据库不存在';
    } else {
      return '数据库连接失败: ' + this.formatErrorMessage(error);
    }
  }
  
  private static formatQueryErrorMessage(error: any): string {
    const errorMsg = this.formatErrorMessage(error).toLowerCase();
    
    if (errorMsg.includes('syntax error')) {
      return 'SQL语法错误，请检查SQL语句';
    } else if (errorMsg.includes('table') && errorMsg.includes('doesn\'t exist')) {
      return '指定的表不存在';
    } else if (errorMsg.includes('column') && errorMsg.includes('unknown')) {
      return '指定的列不存在';
    } else if (errorMsg.includes('duplicate entry')) {
      return '数据重复，违反唯一性约束';
    } else if (errorMsg.includes('data too long')) {
      return '数据长度超出字段限制';
    } else {
      return 'SQL执行失败: ' + this.formatErrorMessage(error);
    }
  }
}
