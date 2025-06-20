import { dayjs, cloneDeep, getRandomIntBetween } from "./utils";
import GroupLine from "~icons/ri/group-line";
import Question from "~icons/ri/question-answer-line";
import CheckLine from "~icons/ri/chat-check-line";
import Smile from "~icons/ri/star-smile-line";
import {
  getChartData,
  getBarChartData,
  getProgressData,
  getTableData,
  getLatestNews
} from "@/api/dashboard";

const days = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"];

// 图标映射
const iconMap = {
  "需求人数": GroupLine,
  "提问数量": Question,
  "解决数量": CheckLine,
  "用户满意度": Smile
};

// API数据获取函数
export const fetchChartData = async () => {
  try {
    const data = await getChartData();
    return data.map(item => ({
      ...item,
      icon: iconMap[item.name] || GroupLine
    }));
  } catch (error) {
    console.error("获取图表数据失败:", error);
    return chartData; // 返回默认数据
  }
};

export const fetchBarChartData = async () => {
  try {
    return await getBarChartData();
  } catch (error) {
    console.error("获取柱状图数据失败:", error);
    return barChartData; // 返回默认数据
  }
};

export const fetchProgressData = async () => {
  try {
    return await getProgressData();
  } catch (error) {
    console.error("获取进度数据失败:", error);
    return progressData; // 返回默认数据
  }
};

export const fetchTableData = async () => {
  try {
    return await getTableData();
  } catch (error) {
    console.error("获取表格数据失败:", error);
    return tableData; // 返回默认数据
  }
};

export const fetchLatestNews = async () => {
  try {
    return await getLatestNews();
  } catch (error) {
    console.error("获取最新动态失败:", error);
    return latestNewsData; // 返回默认数据
  }
};

/** 需求人数、提问数量、解决数量、用户满意度 */
const chartData = [
  {
    icon: GroupLine,
    bgColor: "#effaff",
    color: "#41b6ff",
    duration: 2200,
    name: "需求人数",
    value: 36000,
    percent: "+88%",
    data: [2101, 5288, 4239, 4962, 6752, 5208, 7450] // 平滑折线图数据
  },
  {
    icon: Question,
    bgColor: "#fff5f4",
    color: "#e85f33",
    duration: 1600,
    name: "提问数量",
    value: 16580,
    percent: "+70%",
    data: [2216, 1148, 1255, 788, 4821, 1973, 4379]
  },
  {
    icon: CheckLine,
    bgColor: "#eff8f4",
    color: "#26ce83",
    duration: 1500,
    name: "解决数量",
    value: 16499,
    percent: "+99%",
    data: [861, 1002, 3195, 1715, 3666, 2415, 3645]
  },
  {
    icon: Smile,
    bgColor: "#f6f4fe",
    color: "#7846e5",
    duration: 100,
    name: "用户满意度",
    value: 100,
    percent: "+100%",
    data: [100]
  }
];

/** 分析概览 */
const barChartData = [
  {
    requireData: [2101, 5288, 4239, 4962, 6752, 5208, 7450],
    questionData: [2216, 1148, 1255, 1788, 4821, 1973, 4379]
  },
  {
    requireData: [2101, 3280, 4400, 4962, 5752, 6889, 7600],
    questionData: [2116, 3148, 3255, 3788, 4821, 4970, 5390]
  }
];

/** 解决概率 */
const progressData = [
  {
    week: "周一",
    percentage: 85,
    duration: 110,
    color: "#41b6ff"
  },
  {
    week: "周二",
    percentage: 86,
    duration: 105,
    color: "#41b6ff"
  },
  {
    week: "周三",
    percentage: 88,
    duration: 100,
    color: "#41b6ff"
  },
  {
    week: "周四",
    percentage: 89,
    duration: 95,
    color: "#41b6ff"
  },
  {
    week: "周五",
    percentage: 94,
    duration: 90,
    color: "#26ce83"
  },
  {
    week: "周六",
    percentage: 96,
    duration: 85,
    color: "#26ce83"
  },
  {
    week: "周日",
    percentage: 100,
    duration: 80,
    color: "#26ce83"
  }
].reverse();

/** 数据统计 */
const tableData = Array.from({ length: 30 }).map((_, index) => {
  return {
    id: index + 1,
    requiredNumber: getRandomIntBetween(13500, 19999),
    questionNumber: getRandomIntBetween(12600, 16999),
    resolveNumber: getRandomIntBetween(13500, 17999),
    satisfaction: getRandomIntBetween(95, 100),
    date: dayjs().subtract(index, "day").format("YYYY-MM-DD")
  };
});

/** 最新动态 */
const latestNewsData = cloneDeep(tableData)
  .slice(0, 14)
  .map((item, index) => {
    return Object.assign(item, {
      date: `${dayjs().subtract(index, "day").format("YYYY-MM-DD")} ${
        days[dayjs().subtract(index, "day").day()]
      }`
    });
  });

export { chartData, barChartData, progressData, tableData, latestNewsData };
