# 数据库同步管理系统 - 前端

基于 Vue3 + Element Plus + TypeScript 的数据库同步管理系统前端应用。

## 功能特性

- 🔐 **JWT认证** - 用户登录、注册、权限管理
- 🗄️ **数据库连接管理** - 支持多种数据库类型的连接配置
- 🔄 **数据同步任务** - 创建、执行、监控数据同步任务
- 📝 **自定义查询** - SQL编辑器，查询执行和结果保存
- 📊 **实时监控** - 任务进度、系统状态实时监控
- 📋 **系统日志** - 操作日志、错误日志管理
- 📱 **响应式设计** - 支持移动端和桌面端

## 技术栈

- **框架**: Vue 3.4+ (Composition API)
- **UI库**: Element Plus
- **语言**: TypeScript 5.0+
- **构建工具**: Vite 5.0+
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **HTTP客户端**: Axios
- **代码规范**: ESLint + Prettier

## 开发环境要求

- Node.js >= 18.0.0
- pnpm >= 8.0.0

## 精简版本（实际项目开发请用精简版本，提供 `非国际化` 、`国际化` 两个版本选择）

精简版本是基于 [vue-pure-admin](https://github.com/pure-admin/vue-pure-admin) 提炼出的架子，包含主体功能，更适合实际项目开发，打包后的大小在全局引入 [element-plus](https://element-plus.org) 的情况下仍然低于 `2.3MB`，并且会永久同步完整版的代码。开启 `brotli` 压缩和 `cdn` 替换本地库模式后，打包大小低于 `350kb`

[点我查看非国际化精简版本](https://github.com/pure-admin/pure-admin-thin)  
[点我查看国际化精简版本](https://github.com/pure-admin/pure-admin-thin/tree/i18n)

## 配套视频

[点我查看 UI 设计](https://www.bilibili.com/video/BV17g411T7rq)  
[点我查看快速开发教程](https://www.bilibili.com/video/BV1kg411v7QT)

## 配套保姆级文档

[点我查看 vue-pure-admin 文档](https://pure-admin.cn/)  
[点我查看 @pureadmin/utils 文档](https://pure-admin-utils.netlify.app)

## 高级服务

[点我查看详情](https://pure-admin.cn/pages/service/)

## `Tauri` 版本

[点我查看 Tauri 版本](https://github.com/pure-admin/tauri-pure-admin)

## `Electron` 版本

[点我查看 Electron 版本](https://github.com/pure-admin/electron-pure-admin)

## 预览

[点我查看预览](https://pure-admin.github.io/vue-pure-admin)

`PC` 端

<p align="center">
  <img alt="PureAdmin" src="https://xiaoxian521.github.io/hyperlink/img/vue-pure-admin/1.jpg">
  <br />
  <img alt="PureAdmin" src="https://xiaoxian521.github.io/hyperlink/img/vue-pure-admin/2.jpg">
</p>

暗色风格

<p align="center">
  <img alt="PureAdmin" src="https://xiaoxian521.github.io/hyperlink/img/vue-pure-admin/3.jpg">
  <br />
  <img alt="PureAdmin" src="https://xiaoxian521.github.io/hyperlink/img/vue-pure-admin/4.jpg">
</p>

移动端

<p align="center">
  <img alt="PureAdmin" src="https://xiaoxian521.github.io/hyperlink/img/vue-pure-admin/5.jpg">
</p>

### 使用 `Gitpod`

在 `Gitpod`（适用于 `GitHub` 的免费在线开发环境）中打开项目，并立即开始编码.

[![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/pure-admin/vue-pure-admin)

## 安装使用

### 拉取代码

#### 推荐使用 `@pureadmin/cli` 脚手架

<br/>
<img src="https://xiaoxian521.github.io/hyperlink/gif/pure-admin-cli.gif" alt="pure-admin-cli" />

1. 全局安装

```bash
npm install -g @pureadmin/cli
```

2. 交互式选择模板并创建项目

```bash
pure create
```

[点我查看 @pureadmin/cli 脚手架详细用法](https://github.com/pure-admin/pure-admin-cli#pureadmincli)

#### 从 `GitHub` 上拉取

```bash
git clone https://github.com/pure-admin/vue-pure-admin.git
```

#### 从 `Gitee` 上拉取

```bash
git clone https://gitee.com/yiming_chang/vue-pure-admin.git
```

### 安装依赖

```bash
cd vue-pure-admin

pnpm install
```

### 启动平台

```bash
pnpm dev
```

### 项目打包

```bash
pnpm build
```

## Docker 支持

1. 自定义镜像名为 `vue-pure-admin` 的镜像（请注意下面命令末尾有一个点 `.` 表示使用当前路径下的 `Dockerfile` 文件，可根据实际情况指定路径）

```bash
docker build -t vue-pure-admin .
```

2. 端口映射并启动 `docker` 容器（`8080:80`：表示在容器中使用 `80` 端口，并将该端口转发到主机的 `8080` 端口；`pure-admin`：表示自定义容器名；`vue-pure-admin`：表示自定义镜像名）

```bash
docker run -dp 8080:80  --name pure-admin vue-pure-admin
```

操作完上面两个命令后，在浏览器打开 `http://localhost:8080` 即可预览

当然也可以通过 [Docker Desktop](https://www.docker.com/products/docker-desktop/) 可视化界面去操作 `docker` 项目，如下图

<p align="center">
  <img alt="docker-desktop" width="100%" src="https://xiaoxian521.github.io/hyperlink/img/docker-desktop.jpg">
</p>

## 更新日志

[CHANGELOG](./CHANGELOG.zh_CN.md)

## 如何贡献

非常欢迎您的加入！[提一个 Issue](https://github.com/pure-admin/vue-pure-admin/issues/new/choose) 或者提交一个 `Pull Request`

**Pull Request:**

1. Fork 代码!
2. 创建自己的分支: `git checkout -b feat/xxxx`
3. 提交您的修改: `git commit -am 'feat(function): add xxxxx'`
4. 推送您的分支: `git push origin feat/xxxx`
5. 提交`pull request`

## 特别代码贡献

非常感谢你们能深入了解源码并对 `pure-admin` 组织作出优秀贡献 ❤️

|                   **贡献人**                    |                                   **具体代码**                                   |
| :---------------------------------------------: | :------------------------------------------------------------------------------: |
|       [hb0730](https://github.com/hb0730)       |    [代码](https://github.com/pure-admin/vue-pure-admin/commits?author=hb0730)    |
|         [o-cc](https://github.com/o-cc)         |     [代码](https://github.com/pure-admin/vue-pure-admin/commits?author=o-cc)     |
| [yj-liuzepeng](https://github.com/yj-liuzepeng) | [代码](https://github.com/pure-admin/vue-pure-admin/commits?author=yj-liuzepeng) |
|   [skyline523](https://github.com/skyline523)   |  [代码](https://github.com/pure-admin/vue-pure-admin/commits?author=skyline523)  |
| [shark-lajiao](https://github.com/shark-lajiao) | [代码](https://github.com/pure-admin/vue-pure-admin/commits?author=shark-lajiao) |
|      [WitMiao](https://github.com/WitMiao)      |   [代码](https://github.com/pure-admin/vue-pure-admin/commits?author=WitMiao)    |
|     [QFifteen](https://github.com/QFifteen)     |   [代码](https://github.com/pure-admin/vue-pure-admin/commits?author=QFifteen)   |
|      [edgexie](https://github.com/edgexie)      |   [代码](https://github.com/pure-admin/vue-pure-admin/commits?author=edgexie)    |
|       [way-jm](https://github.com/way-jm)       |    [代码](https://github.com/pure-admin/vue-pure-admin/commits?author=way-jm)    |
|   [simple-hui](https://github.com/simple-hui)   |  [代码](https://github.com/pure-admin/vue-pure-admin/commits?author=simple-hui)  |
|   [tinysimple](https://github.com/tinysimple)   |  [代码](https://github.com/pure-admin/vue-pure-admin/commits?author=tinysimple)  |

## `Git` 贡献提交规范

参考 [vue](https://github.com/vuejs/vue/blob/dev/.github/COMMIT_CONVENTION.md) 规范 ([Angular](https://github.com/conventional-changelog/conventional-changelog/tree/master/packages/conventional-changelog-angular))

- `feat` 增加新功能
- `fix` 修复问题/BUG
- `style` 代码风格相关无影响运行结果的
- `perf` 优化/性能提升
- `refactor` 重构
- `revert` 撤销修改
- `test` 测试相关
- `docs` 文档/注释
- `chore` 依赖更新/脚手架配置修改等
- `workflow` 工作流改进
- `ci` 持续集成
- `types` 类型定义文件更改
- `wip` 开发中

## 浏览器支持

本地开发推荐使用 `Chrome`、`Edge`、`Firefox` 浏览器，作者常用的是最新版 `Chrome` 浏览器  
实际使用中感觉 `Firefox` 在动画上要比别的浏览器更加丝滑，只是作者用 `Chrome` 已经习惯了，看个人爱好选择吧  
更详细的浏览器兼容性支持请看 [Vue 支持哪些浏览器？](https://cn.vuejs.org/about/faq.html#what-browsers-does-vue-support) 和 [Vite 浏览器兼容性](https://cn.vitejs.dev/guide/build#browser-compatibility)

| [<img src="https://raw.githubusercontent.com/alrra/browser-logos/master/src/edge/edge_48x48.png" alt=" Edge" width="24px" height="24px" />](http://godban.github.io/browsers-support-badges/)</br>IE | [<img src="https://raw.githubusercontent.com/alrra/browser-logos/master/src/edge/edge_48x48.png" alt=" Edge" width="24px" height="24px" />](http://godban.github.io/browsers-support-badges/)</br>Edge | [<img src="https://raw.githubusercontent.com/alrra/browser-logos/master/src/firefox/firefox_48x48.png" alt="Firefox" width="24px" height="24px" />](http://godban.github.io/browsers-support-badges/)</br>Firefox | [<img src="https://raw.githubusercontent.com/alrra/browser-logos/master/src/chrome/chrome_48x48.png" alt="Chrome" width="24px" height="24px" />](http://godban.github.io/browsers-support-badges/)</br>Chrome | [<img src="https://raw.githubusercontent.com/alrra/browser-logos/master/src/safari/safari_48x48.png" alt="Safari" width="24px" height="24px" />](http://godban.github.io/browsers-support-badges/)</br>Safari |
| :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------: | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------: | :---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------: | :-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------: | :-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------: |
|                                                                                                不支持                                                                                                |                                                                                              最后两个版本                                                                                              |                                                                                                   最后两个版本                                                                                                    |                                                                                                 最后两个版本                                                                                                  |                                                                                                 最后两个版本                                                                                                  |

## 维护者

[xiaoxian521](https://github.com/xiaoxian521)、[Ten-K](https://github.com/Ten-K)

## 许可证

完全免费开源

[MIT © 2020-present, pure-admin](./LICENSE)

## `Star`

非常感谢留下星星的好心人，感谢您的支持 :heart:

[![Stargazers repo roster for @pure-admin/vue-pure-admin](https://bytecrank.com/nastyox/reporoster/php/stargazersSVG.php?user=pure-admin&repo=vue-pure-admin)](https://github.com/pure-admin/vue-pure-admin/stargazers)

## `Fork`

瞧，那些 `小哥哥` 、`小姐姐` 认真 `学习` 的样子真滴是 `哎呦不错哦` :heart:

[![Forkers repo roster for @pure-admin/vue-pure-admin](https://bytecrank.com/nastyox/reporoster/php/forkersSVG.php?user=pure-admin&repo=vue-pure-admin)](https://github.com/pure-admin/vue-pure-admin/network/members)
