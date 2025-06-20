import { $t } from "@/plugins/i18n";
const { VITE_HIDE_HOME } = import.meta.env;
const Layout = () => import("@/layout/index.vue");

export default {
  path: "/database",
  name: "Database",
  component: Layout,
  redirect: "/database/connections",
  meta: {
    icon: "ri:database-2-line",
    title: $t("menus.database"),
    rank: 2
  },
  children: [
    {
      path: "/database/connections",
      name: "DatabaseConnections",
      component: () => import("@/views/database/connections.vue"),
      meta: {
        title: $t("menus.databaseConnections"),
        showParent: true
      }
    },
    {
      path: "/database/sync",
      name: "DatabaseSync",
      component: () => import("@/views/database/sync.vue"),
      meta: {
        title: $t("menus.databaseSync"),
        showParent: true
      }
    },
    {
      path: "/database/query",
      name: "DatabaseQuery",
      component: () => import("@/views/database/query.vue"),
      meta: {
        title: $t("menus.databaseQuery"),
        showParent: true
      }
    },
    {
      path: "/database/monitor",
      name: "DatabaseMonitor",
      component: () => import("@/views/database/monitor.vue"),
      meta: {
        title: $t("menus.databaseMonitor"),
        showParent: true
      }
    },
    {
      path: "/database/api-test",
      name: "ApiTest",
      component: () => import("@/views/test/api-test.vue"),
      meta: {
        title: "API连接测试",
        showParent: true
      }
    }
  ]
} satisfies RouteConfigsTable;
