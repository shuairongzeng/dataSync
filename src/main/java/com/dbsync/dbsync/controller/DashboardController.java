package com.dbsync.dbsync.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 仪表板控制器 - 提供首页数据
 */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DashboardController {

    /**
     * 获取图表数据 - 需求人数、提问数量、解决数量、用户满意度
     */
    @GetMapping("/chart-data")
    public ResponseEntity<?> getChartData() {
        List<Map<String, Object>> chartData = new ArrayList<>();
        
        // 需求人数
        Map<String, Object> requirementData = new HashMap<>();
        requirementData.put("name", "需求人数");
        requirementData.put("value", 36000);
        requirementData.put("percent", "+88%");
        requirementData.put("color", "#41b6ff");
        requirementData.put("bgColor", "#effaff");
        requirementData.put("duration", 2200);
        requirementData.put("data", Arrays.asList(2101, 5288, 4239, 4962, 6752, 5208, 7450));
        chartData.add(requirementData);
        
        // 提问数量
        Map<String, Object> questionData = new HashMap<>();
        questionData.put("name", "提问数量");
        questionData.put("value", 16580);
        questionData.put("percent", "+70%");
        questionData.put("color", "#e85f33");
        questionData.put("bgColor", "#fff5f4");
        questionData.put("duration", 1600);
        questionData.put("data", Arrays.asList(2216, 1148, 1255, 788, 4821, 1973, 4379));
        chartData.add(questionData);
        
        // 解决数量
        Map<String, Object> resolveData = new HashMap<>();
        resolveData.put("name", "解决数量");
        resolveData.put("value", 16499);
        resolveData.put("percent", "+99%");
        resolveData.put("color", "#26ce83");
        resolveData.put("bgColor", "#eff8f4");
        resolveData.put("duration", 1500);
        resolveData.put("data", Arrays.asList(861, 1002, 3195, 1715, 3666, 2415, 3645));
        chartData.add(resolveData);
        
        // 用户满意度
        Map<String, Object> satisfactionData = new HashMap<>();
        satisfactionData.put("name", "用户满意度");
        satisfactionData.put("value", 100);
        satisfactionData.put("percent", "+100%");
        satisfactionData.put("color", "#7846e5");
        satisfactionData.put("bgColor", "#f6f4fe");
        satisfactionData.put("duration", 100);
        satisfactionData.put("data", Arrays.asList(100));
        chartData.add(satisfactionData);
        
        return ResponseEntity.ok(chartData);
    }

    /**
     * 获取柱状图数据 - 分析概览
     */
    @GetMapping("/bar-chart-data")
    public ResponseEntity<?> getBarChartData() {
        List<Map<String, Object>> barChartData = new ArrayList<>();
        
        // 上周数据
        Map<String, Object> lastWeek = new HashMap<>();
        lastWeek.put("requireData", Arrays.asList(2101, 5288, 4239, 4962, 6752, 5208, 7450));
        lastWeek.put("questionData", Arrays.asList(2216, 1148, 1255, 1788, 4821, 1973, 4379));
        barChartData.add(lastWeek);
        
        // 本周数据
        Map<String, Object> thisWeek = new HashMap<>();
        thisWeek.put("requireData", Arrays.asList(2101, 3280, 4400, 4962, 5752, 6889, 7600));
        thisWeek.put("questionData", Arrays.asList(2116, 3148, 3255, 3788, 4821, 4970, 5390));
        barChartData.add(thisWeek);
        
        return ResponseEntity.ok(barChartData);
    }

    /**
     * 获取进度数据 - 解决概率
     */
    @GetMapping("/progress-data")
    public ResponseEntity<?> getProgressData() {
        List<Map<String, Object>> progressData = new ArrayList<>();
        
        String[] weeks = {"周日", "周六", "周五", "周四", "周三", "周二", "周一"};
        int[] percentages = {100, 96, 94, 89, 88, 86, 85};
        int[] durations = {80, 85, 90, 95, 100, 105, 110};
        String[] colors = {"#26ce83", "#26ce83", "#26ce83", "#41b6ff", "#41b6ff", "#41b6ff", "#41b6ff"};
        
        for (int i = 0; i < weeks.length; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("week", weeks[i]);
            item.put("percentage", percentages[i]);
            item.put("duration", durations[i]);
            item.put("color", colors[i]);
            progressData.add(item);
        }
        
        return ResponseEntity.ok(progressData);
    }

    /**
     * 获取表格数据 - 数据统计
     */
    @GetMapping("/table-data")
    public ResponseEntity<?> getTableData() {
        List<Map<String, Object>> tableData = new ArrayList<>();
        Random random = new Random();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 0; i < 30; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", i + 1);
            item.put("requiredNumber", getRandomIntBetween(13500, 19999, random));
            item.put("questionNumber", getRandomIntBetween(12600, 16999, random));
            item.put("resolveNumber", getRandomIntBetween(13500, 17999, random));
            item.put("satisfaction", getRandomIntBetween(95, 100, random));
            item.put("date", LocalDate.now().minusDays(i).format(formatter));
            tableData.add(item);
        }

        return ResponseEntity.ok(tableData);
    }

    /**
     * 获取最新动态数据
     */
    @GetMapping("/latest-news")
    public ResponseEntity<?> getLatestNews() {
        List<Map<String, Object>> latestNews = new ArrayList<>();
        Random random = new Random();
        String[] weekDays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 0; i < 14; i++) {
            Map<String, Object> item = new HashMap<>();
            LocalDate date = LocalDate.now().minusDays(i);
            int dayOfWeek = date.getDayOfWeek().getValue() % 7; // 转换为0-6的索引

            item.put("id", i + 1);
            item.put("requiredNumber", getRandomIntBetween(13500, 19999, random));
            item.put("questionNumber", getRandomIntBetween(12600, 16999, random));
            item.put("resolveNumber", getRandomIntBetween(13500, 17999, random));
            item.put("satisfaction", getRandomIntBetween(95, 100, random));
            item.put("date", date.format(formatter) + " " + weekDays[dayOfWeek]);
            latestNews.add(item);
        }

        return ResponseEntity.ok(latestNews);
    }

    /**
     * 获取系统概览数据
     */
    @GetMapping("/overview")
    public ResponseEntity<?> getSystemOverview() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalUsers", 36000);
        overview.put("totalQuestions", 16580);
        overview.put("resolvedQuestions", 16499);
        overview.put("satisfaction", 100);
        overview.put("systemStatus", "正常");
        overview.put("lastUpdateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return ResponseEntity.ok(overview);
    }

    /**
     * 生成指定范围内的随机整数
     */
    private int getRandomIntBetween(int min, int max, Random random) {
        return random.nextInt(max - min + 1) + min;
    }
}
