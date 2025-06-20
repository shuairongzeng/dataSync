import { http } from "@/utils/http";

// 登录请求参数
export type LoginRequest = {
  username: string;
  password: string;
};

// 注册请求参数
export type RegisterRequest = {
  username: string;
  password: string;
  email: string;
};

// 后端JWT响应格式
export type JwtResponse = {
  token: string;
  username: string;
  email: string;
  role: string;
};

// 用户信息响应格式
export type UserResponse = {
  id: number;
  username: string;
  email: string;
  role: string;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
};

// 适配前端的用户结果格式
export type UserResult = {
  success: boolean;
  data: {
    /** 头像 */
    avatar: string;
    /** 用户名 */
    username: string;
    /** 昵称 */
    nickname: string;
    /** 当前登录用户的角色 */
    roles: Array<string>;
    /** 按钮级别权限 */
    permissions: Array<string>;
    /** `token` */
    accessToken: string;
    /** 用于调用刷新`accessToken`的接口时所需的`token` */
    refreshToken: string;
    /** `accessToken`的过期时间（格式'xxxx/xx/xx xx:xx:xx'） */
    expires: Date;
  };
};

export type RefreshTokenResult = {
  success: boolean;
  data: {
    /** `token` */
    accessToken: string;
    /** 用于调用刷新`accessToken`的接口时所需的`token` */
    refreshToken: string;
    /** `accessToken`的过期时间（格式'xxxx/xx/xx xx:xx:xx'） */
    expires: Date;
  };
};

export type UserInfo = {
  /** 头像 */
  avatar: string;
  /** 用户名 */
  username: string;
  /** 昵称 */
  nickname: string;
  /** 邮箱 */
  email: string;
  /** 联系电话 */
  phone: string;
  /** 简介 */
  description: string;
};

export type UserInfoResult = {
  success: boolean;
  data: UserInfo;
};

type ResultTable = {
  success: boolean;
  data?: {
    /** 列表数据 */
    list: Array<any>;
    /** 总条目数 */
    total?: number;
    /** 每页显示条目个数 */
    pageSize?: number;
    /** 当前页数 */
    currentPage?: number;
  };
};

/** 登录接口 - 对接后端 /api/auth/signin */
export const loginApi = (data: LoginRequest) => {
  return http.request<JwtResponse>("post", "/api/auth/signin", { data });
};

/** 注册接口 - 对接后端 /api/auth/signup */
export const registerApi = (data: RegisterRequest) => {
  return http.request<any>("post", "/api/auth/signup", { data });
};

/** 获取当前用户信息 - 对接后端 /api/auth/me */
export const getCurrentUserApi = () => {
  return http.request<UserResponse>("get", "/api/auth/me");
};

/** 系统健康检查 - 对接后端 /api/test/health */
export const healthCheckApi = () => {
  return http.request<any>("get", "/api/test/health");
};

/** 兼容原有登录接口 */
export const getLogin = (data: LoginRequest) => {
  return loginApi(data).then(response => {
    // 转换后端响应格式为前端期望格式
    const jwtResponse = response as JwtResponse;
    const userResult: UserResult = {
      success: true,
      data: {
        avatar: "/img/avatar.jpg", // 默认头像
        username: jwtResponse.username,
        nickname: jwtResponse.username,
        roles: [jwtResponse.role],
        permissions: jwtResponse.role === "ADMIN" ? ["*:*:*"] : ["user:*:*"],
        accessToken: jwtResponse.token,
        refreshToken: jwtResponse.token, // 暂时使用同一个token
        expires: new Date(Date.now() + 24 * 60 * 60 * 1000) // 24小时后过期
      }
    };
    return userResult;
  });
};

/** 刷新`token` */
export const refreshTokenApi = (data?: object) => {
  return http.request<RefreshTokenResult>("post", "/refresh-token", { data });
};

/** 账户设置-个人信息 */
export const getMine = (data?: object) => {
  return getCurrentUserApi().then(response => {
    const userResponse = response as UserResponse;
    const userInfo: UserInfoResult = {
      success: true,
      data: {
        avatar: "/img/avatar.jpg",
        username: userResponse.username,
        nickname: userResponse.username,
        email: userResponse.email,
        phone: "",
        description: `${userResponse.role} 用户`
      }
    };
    return userInfo;
  });
};

/** 账户设置-个人安全日志 */
export const getMineLogs = (data?: object) => {
  return http.request<ResultTable>("get", "/mine-logs", { data });
};
