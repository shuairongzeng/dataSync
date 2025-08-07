import { $t } from "@/plugins/i18n";
import { codemirror } from "@/router/enums";

// 临时注释掉 CodeMirror 路由，避免导入错误
// export default {
//   path: "/codemirror",
//   redirect: "/codemirror/index",
//   meta: {
//     icon: "ri/code-box-line",
//     title: $t("menus.pureCodeMirror"),
//     rank: codemirror
//   },
//   children: [
//     {
//       path: "/codemirror/index",
//       name: "CodeMirror",
//       component: () => import("@/views/codemirror/index.vue"),
//       meta: {
//         title: $t("menus.pureCodeMirror"),
//         extraIcon: "IF-pure-iconfont-new svg"
//       }
//     }
//   ]
// } satisfies RouteConfigsTable;

// 临时禁用此路由模块
export default null;
