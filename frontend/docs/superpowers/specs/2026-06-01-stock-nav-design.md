# Stock Project 前端导航重构设计方案

## 概述

为股票项目前端添加导航结构，将代码拆分为独立页面和组件，提升可维护性和可扩展性。

## 设计决策

### 导航布局
- **方案**：顶部大导航（选项C）
- **样式**：带背景色顶部横栏，Logo居中，导航项均匀分布

### 配色风格
- **方案**：简洁白（选项B）
- **样式**：白色或浅灰背景，深色文字，干净极简

### 页面结构

| 页面 | 路由 | 说明 |
|------|------|------|
| 首页 | `/` | 股票概览 + 快捷功能卡片 |
| 自选股票列表 | `/stocks` | 现有股票列表功能 |
| AI数据分析 | `/ai-analysis` | AI数据图表分析 |
| 本地模型对话 | `/chat` | 与本地模型对话 |

## 组件设计

### NavHeader.vue
- 顶部导航栏组件
- Logo居中，左侧空白/右侧快捷操作
- 导航项：首页、自选股票、AI分析、模型对话
- 选中状态高亮

### QuickAction.vue
- 快捷功能卡片组件
- 4个入口：自选股、AI分析、模型对话、刷新
- 可点击跳转对应页面

### 页面组件

| 组件 | 职责 |
|------|------|
| HomeView.vue | 首页：股票概览 + 快捷功能卡片 |
| StocksView.vue | 自选股票列表（迁移现有App.vue内容） |
| AiView.vue | AI数据分析页面（预留） |
| ChatView.vue | 本地模型对话页面（预留） |

## 技术实现

- 使用 vue-router 配置路由
- 页面组件放在 `src/views/` 目录
- 通用组件放在 `src/components/` 目录
- 导航使用 Element Plus 的 el-menu 组件
- 样式采用简洁白风格

## 路由配置

```js
routes: [
  { path: '/', component: HomeView },
  { path: '/stocks', component: StocksView },
  { path: '/ai-analysis', component: AiView },
  { path: '/chat', component: ChatView }
]
```