import { http } from "@/utils/http";

// 图表数据类型定义
export type ChartDataItem = {
  name: string;
  value: number;
  percent: string;
  color: string;
  bgColor: string;
  duration: number;
  data: number[];
};

// 柱状图数据类型定义
export type BarChartDataItem = {
  requireData: number[];
  questionData: number[];
};

// 进度数据类型定义
export type ProgressDataItem = {
  week: string;
  percentage: number;
  duration: number;
  color: string;
};

// 表格数据类型定义
export type TableDataItem = {
  id: number;
  requiredNumber: number;
  questionNumber: number;
  resolveNumber: number;
  satisfaction: number;
  date: string;
};

// 最新动态数据类型定义
export type LatestNewsItem = {
  id: number;
  requiredNumber: number;
  questionNumber: number;
  resolveNumber: number;
  satisfaction: number;
  date: string;
};

// 系统概览数据类型定义
export type SystemOverview = {
  totalUsers: number;
  totalQuestions: number;
  resolvedQuestions: number;
  satisfaction: number;
  systemStatus: string;
  lastUpdateTime: string;
};

/** 获取图表数据 */
export const getChartData = () => {
  return http.request<ChartDataItem[]>("get", "/api/dashboard/chart-data");
};

/** 获取柱状图数据 */
export const getBarChartData = () => {
  return http.request<BarChartDataItem[]>("get", "/api/dashboard/bar-chart-data");
};

/** 获取进度数据 */
export const getProgressData = () => {
  return http.request<ProgressDataItem[]>("get", "/api/dashboard/progress-data");
};

/** 获取表格数据 */
export const getTableData = () => {
  return http.request<TableDataItem[]>("get", "/api/dashboard/table-data");
};

/** 获取最新动态数据 */
export const getLatestNews = () => {
  return http.request<LatestNewsItem[]>("get", "/api/dashboard/latest-news");
};

/** 获取系统概览数据 */
export const getSystemOverview = () => {
  return http.request<SystemOverview>("get", "/api/dashboard/overview");
};
