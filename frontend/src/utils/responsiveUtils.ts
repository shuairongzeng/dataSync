import { ref, computed, onMounted, onUnmounted } from "vue";

// 断点定义
export const breakpoints = {
  xs: 0,
  sm: 576,
  md: 768,
  lg: 992,
  xl: 1200,
  xxl: 1600
} as const;

// 屏幕尺寸类型
export type BreakpointKey = keyof typeof breakpoints;

// 响应式状态
const windowWidth = ref(0);
const windowHeight = ref(0);

// 更新窗口尺寸
const updateWindowSize = () => {
  windowWidth.value = window.innerWidth;
  windowHeight.value = window.innerHeight;
};

// 初始化
let isInitialized = false;

const initializeResponsive = () => {
  if (isInitialized) return;
  
  updateWindowSize();
  window.addEventListener('resize', updateWindowSize);
  isInitialized = true;
};

// 清理
const cleanupResponsive = () => {
  window.removeEventListener('resize', updateWindowSize);
  isInitialized = false;
};

/**
 * 响应式工具 Hook
 */
export function useResponsive() {
  onMounted(() => {
    initializeResponsive();
  });

  onUnmounted(() => {
    cleanupResponsive();
  });

  // 当前断点
  const currentBreakpoint = computed<BreakpointKey>(() => {
    const width = windowWidth.value;
    
    if (width >= breakpoints.xxl) return 'xxl';
    if (width >= breakpoints.xl) return 'xl';
    if (width >= breakpoints.lg) return 'lg';
    if (width >= breakpoints.md) return 'md';
    if (width >= breakpoints.sm) return 'sm';
    return 'xs';
  });

  // 是否为移动设备
  const isMobile = computed(() => windowWidth.value < breakpoints.md);
  
  // 是否为平板设备
  const isTablet = computed(() => 
    windowWidth.value >= breakpoints.md && windowWidth.value < breakpoints.lg
  );
  
  // 是否为桌面设备
  const isDesktop = computed(() => windowWidth.value >= breakpoints.lg);

  // 检查是否匹配指定断点
  const matches = (breakpoint: BreakpointKey) => {
    return computed(() => windowWidth.value >= breakpoints[breakpoint]);
  };

  // 检查是否在指定断点范围内
  const between = (min: BreakpointKey, max: BreakpointKey) => {
    return computed(() => 
      windowWidth.value >= breakpoints[min] && windowWidth.value < breakpoints[max]
    );
  };

  // 根据断点返回不同的值
  const breakpointValue = <T>(values: Partial<Record<BreakpointKey, T>>, defaultValue: T): T => {
    const current = currentBreakpoint.value;
    
    // 按优先级查找匹配的值
    const priorities: BreakpointKey[] = ['xxl', 'xl', 'lg', 'md', 'sm', 'xs'];
    const currentIndex = priorities.indexOf(current);
    
    // 从当前断点开始向下查找
    for (let i = currentIndex; i < priorities.length; i++) {
      const key = priorities[i];
      if (values[key] !== undefined) {
        return values[key] as T;
      }
    }
    
    return defaultValue;
  };

  return {
    windowWidth: computed(() => windowWidth.value),
    windowHeight: computed(() => windowHeight.value),
    currentBreakpoint,
    isMobile,
    isTablet,
    isDesktop,
    matches,
    between,
    breakpointValue
  };
}

/**
 * 获取响应式列数
 */
export function useResponsiveColumns(
  columns: Partial<Record<BreakpointKey, number>> = {}
) {
  const { breakpointValue } = useResponsive();
  
  const defaultColumns = {
    xs: 1,
    sm: 2,
    md: 3,
    lg: 4,
    xl: 5,
    xxl: 6
  };
  
  const mergedColumns = { ...defaultColumns, ...columns };
  
  return computed(() => breakpointValue(mergedColumns, 1));
}

/**
 * 获取响应式间距
 */
export function useResponsiveSpacing(
  spacing: Partial<Record<BreakpointKey, number>> = {}
) {
  const { breakpointValue } = useResponsive();
  
  const defaultSpacing = {
    xs: 8,
    sm: 12,
    md: 16,
    lg: 20,
    xl: 24,
    xxl: 32
  };
  
  const mergedSpacing = { ...defaultSpacing, ...spacing };
  
  return computed(() => breakpointValue(mergedSpacing, 16));
}

/**
 * 响应式表格配置
 */
export function useResponsiveTable() {
  const { isMobile, isTablet } = useResponsive();
  
  const tableSize = computed(() => {
    if (isMobile.value) return 'small';
    if (isTablet.value) return 'default';
    return 'default';
  });
  
  const showPagination = computed(() => !isMobile.value);
  
  const paginationLayout = computed(() => {
    if (isMobile.value) return 'prev, pager, next';
    if (isTablet.value) return 'total, prev, pager, next';
    return 'total, sizes, prev, pager, next, jumper';
  });
  
  const pageSize = computed(() => {
    if (isMobile.value) return 10;
    if (isTablet.value) return 15;
    return 20;
  });
  
  return {
    tableSize,
    showPagination,
    paginationLayout,
    pageSize
  };
}

/**
 * 响应式表单配置
 */
export function useResponsiveForm() {
  const { isMobile, isTablet } = useResponsive();
  
  const labelPosition = computed(() => {
    if (isMobile.value) return 'top';
    return 'right';
  });
  
  const labelWidth = computed(() => {
    if (isMobile.value) return 'auto';
    if (isTablet.value) return '100px';
    return '120px';
  });
  
  const formSize = computed(() => {
    if (isMobile.value) return 'default';
    return 'default';
  });
  
  const inline = computed(() => !isMobile.value);
  
  return {
    labelPosition,
    labelWidth,
    formSize,
    inline
  };
}

/**
 * 响应式对话框配置
 */
export function useResponsiveDialog() {
  const { isMobile, windowWidth } = useResponsive();
  
  const width = computed(() => {
    if (isMobile.value) return '95%';
    
    const w = windowWidth.value;
    if (w < 1200) return '80%';
    if (w < 1600) return '60%';
    return '50%';
  });
  
  const fullscreen = computed(() => isMobile.value);
  
  return {
    width,
    fullscreen
  };
}

// 导出断点常量
export { breakpoints };
