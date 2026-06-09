# Stock Project 前端导航重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为股票项目添加顶部导航和页面拆分，实现：首页、自选股票、AI分析、模型对话四个页面

**Architecture:** 使用 vue-router 配置路由，页面组件置于 `src/views/`，通用组件置于 `src/components/`，NavHeader.vue 作为顶部导航栏

**Tech Stack:** Vue 3 + vue-router + Element Plus

---

## 文件结构

| 操作 | 文件路径 | 职责 |
|------|----------|------|
| Create | `src/router/index.js` | 路由配置 |
| Create | `src/views/HomeView.vue` | 首页 |
| Create | `src/views/StocksView.vue` | 自选股票列表 |
| Create | `src/views/AiView.vue` | AI数据分析（预留） |
| Create | `src/views/ChatView.vue` | 本地模型对话（预留） |
| Create | `src/components/NavHeader.vue` | 顶部导航组件 |
| Create | `src/components/QuickAction.vue` | 快捷功能卡片 |
| Modify | `src/App.vue` | 路由出口 + 导航栏 |
| Modify | `src/main.js` | 挂载 router |
| Modify | `vite.config.js` | 添加 proxy 路由 `/chat` |

---

### Task 1: 安装 vue-router 并修改 main.js

**Files:**
- Modify: `src/main.js`
- Modify: `package.json` (已包含 vue-router ^4.2.0)

- [ ] **Step 1: 修改 main.js 添加 router**

```js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from './router'
import App from './App.vue'

const app = createApp(App)
app.use(createPinia())
app.use(ElementPlus)
app.use(router)
app.mount('#app')
```

---

### Task 2: 创建路由配置 src/router/index.js

**Files:**
- Create: `src/router/index.js`

- [ ] **Step 1: 创建路由配置文件**

```js
import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import StocksView from '@/views/StocksView.vue'
import AiView from '@/views/AiView.vue'
import ChatView from '@/views/ChatView.vue'

const routes = [
  { path: '/', component: HomeView },
  { path: '/stocks', component: StocksView },
  { path: '/ai-analysis', component: AiView },
  { path: '/chat', component: ChatView }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
```

---

### Task 3: 创建 NavHeader.vue 顶部导航组件

**Files:**
- Create: `src/components/NavHeader.vue`

- [ ] **Step 1: 创建导航组件**

```vue
<template>
  <div class="nav-header">
    <div class="nav-content">
      <div class="nav-left">
        <span class="logo">股票项目</span>
      </div>
      <div class="nav-center">
        <router-link to="/" class="nav-item">首页</router-link>
        <router-link to="/stocks" class="nav-item">自选股票</router-link>
        <router-link to="/ai-analysis" class="nav-item">AI分析</router-link>
        <router-link to="/chat" class="nav-item">模型对话</router-link>
      </div>
      <div class="nav-right"></div>
    </div>
  </div>
</template>

<style scoped>
.nav-header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}
.nav-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  height: 60px;
}
.nav-left {
  flex: 1;
}
.logo {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}
.nav-center {
  display: flex;
  gap: 40px;
}
.nav-item {
  text-decoration: none;
  color: #666;
  font-size: 15px;
  padding: 8px 0;
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
}
.nav-item:hover,
.nav-item.router-link-active {
  color: #409eff;
  border-bottom-color: #409eff;
}
.nav-right {
  flex: 1;
}
</style>
```

---

### Task 4: 创建 QuickAction.vue 快捷功能卡片组件

**Files:**
- Create: `src/components/QuickAction.vue`

- [ ] **Step 1: 创建快捷功能卡片组件**

```vue
<template>
  <div class="quick-actions">
    <router-link to="/stocks" class="action-card">
      <div class="icon">📈</div>
      <div class="text">自选股票</div>
    </router-link>
    <router-link to="/ai-analysis" class="action-card">
      <div class="icon">🤖</div>
      <div class="text">AI分析</div>
    </router-link>
    <router-link to="/chat" class="action-card">
      <div class="icon">💬</div>
      <div class="text">模型对话</div>
    </router-link>
    <div class="action-card" @click="$emit('refresh')">
      <div class="icon">🔄</div>
      <div class="text">刷新数据</div>
    </div>
  </div>
</template>

<style scoped>
.quick-actions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  padding: 20px 0;
}
.action-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  cursor: pointer;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s, box-shadow 0.2s;
  text-decoration: none;
  color: inherit;
}
.action-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}
.icon {
  font-size: 32px;
  margin-bottom: 12px;
}
.text {
  font-size: 14px;
  color: #333;
}
</style>
```

---

### Task 5: 创建 HomeView.vue 首页

**Files:**
- Create: `src/views/HomeView.vue`

- [ ] **Step 1: 创建首页组件**

```vue
<template>
  <div class="home-view">
    <div class="overview">
      <h2>市场概览</h2>
      <div class="market-info">
        <div class="market-item">
          <span class="label">自选股数量</span>
          <span class="value">{{ stockCount }}</span>
        </div>
        <div class="market-item">
          <span class="label">今日涨跌</span>
          <span class="value" :class="upDown >= 0 ? 'up' : 'down'">
            {{ upDown >= 0 ? '+' : '' }}{{ upDown.toFixed(2) }}%
          </span>
        </div>
      </div>
    </div>
    <QuickAction @refresh="onRefresh" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import QuickAction from '@/components/QuickAction.vue'
import { stockApi } from '@/api/stock'

const stockCount = ref(0)
const upDown = ref(0)

const loadData = async () => {
  try {
    const res = await stockApi.list()
    if (res.code === 200) {
      stockCount.value = res.result?.length || 0
      const totalRate = res.result?.reduce((sum, s) => sum + (s.floatRate || 0), 0) || 0
      upDown.value = stockCount.value > 0 ? (totalRate / stockCount.value * 100) : 0
    }
  } catch (e) {
    console.error(e)
  }
}

const onRefresh = () => {
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.home-view {
  padding: 20px;
}
.overview {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.overview h2 {
  margin: 0 0 16px;
  font-size: 18px;
  color: #333;
}
.market-info {
  display: flex;
  gap: 40px;
}
.market-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.label {
  font-size: 14px;
  color: #999;
}
.value {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}
.up {
  color: #f56c6c;
}
.down {
  color: #67c23a;
}
</style>
```

---

### Task 6: 创建 StocksView.vue 自选股票页面

**Files:**
- Create: `src/views/StocksView.vue`

- [ ] **Step 1: 创建自选股票页面（从 App.vue 迁移）**

```vue
<template>
  <div class="stocks-view">
    <div class="header">
      <h2>自选股票</h2>
      <el-button type="primary" @click="fetchList" :loading="loading">刷新</el-button>
    </div>

    <el-table :data="stocks" style="width: 100%; margin-top: 20px" v-if="stocks.length">
      <el-table-column prop="stockName" label="股票名称" width="120" />
      <el-table-column prop="stockCode" label="股票代码" width="120" />
      <el-table-column prop="buyPrice" label="买入价格" width="100" />
      <el-table-column prop="currentPrice" label="当前价格" width="100" />
      <el-table-column prop="floatPrice" label="浮动金额" width="100">
        <template #default="{ row }">
          <span :class="row.floatPrice >= 0 ? 'up' : 'down'">
            {{ row.floatPrice !== null ? row.floatPrice.toFixed(2) : '-' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="floatRate" label="浮动比例" width="100">
        <template #default="{ row }">
          <span :class="row.floatRate >= 0 ? 'up' : 'down'">
            {{ row.floatRate !== null ? (row.floatRate * 100).toFixed(2) + '%' : '-' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="targetPrice" label="目标价格" width="100" />
      <el-table-column prop="isReminder" label="提醒" width="80">
        <template #default="{ row }">
          {{ row.isReminder === 1 ? '是' : '否' }}
        </template>
      </el-table-column>
    </el-table>

    <p v-else-if="!loading" style="margin-top: 20px; color: #999;">暂无自选股票</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { stockApi } from '@/api/stock'

const stocks = ref([])
const loading = ref(false)

const fetchList = async () => {
  loading.value = true
  try {
    const res = await stockApi.list()
    if (res.code === 200) {
      stocks.value = res.result || []
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.stocks-view {
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header h2 {
  margin: 0;
  font-size: 18px;
  color: #333;
}
.up {
  color: #f56c6c;
}
.down {
  color: #67c23a;
}
</style>
```

---

### Task 7: 创建 AiView.vue 和 ChatView.vue 预留页面

**Files:**
- Create: `src/views/AiView.vue`
- Create: `src/views/ChatView.vue`

- [ ] **Step 1: 创建 AiView.vue（预留页面）**

```vue
<template>
  <div class="ai-view">
    <h2>AI数据分析</h2>
    <p style="color: #999; margin-top: 20px;">功能开发中...</p>
  </div>
</template>

<style scoped>
.ai-view {
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.ai-view h2 {
  margin: 0;
  font-size: 18px;
  color: #333;
}
</style>
```

- [ ] **Step 2: 创建 ChatView.vue（预留页面）**

```vue
<template>
  <div class="chat-view">
    <h2>本地模型对话</h2>
    <p style="color: #999; margin-top: 20px;">功能开发中...</p>
  </div>
</template>

<style scoped>
.chat-view {
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.chat-view h2 {
  margin: 0;
  font-size: 18px;
  color: #333;
}
</style>
```

---

### Task 8: 修改 App.vue 为路由出口 + 导航栏

**Files:**
- Modify: `src/App.vue`

- [ ] **Step 1: 替换 App.vue 为路由结构**

```vue
<template>
  <div id="app">
    <NavHeader />
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import NavHeader from '@/components/NavHeader.vue'
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
body {
  background: #f5f7fa;
}
#app {
  min-height: 100vh;
}
.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}
</style>
```

---

### Task 9: 更新 vite.config.js 代理配置

**Files:**
- Modify: `vite.config.js`

- [ ] **Step 1: 添加 /chat 代理（用于后续模型对话）**

```js
proxy: {
  '/stock': {
    target: 'http://localhost:18888',
    changeOrigin: true,
    rewrite: (path) => path.replace(/^\/stock/, '')
  },
  '/chat': {
    target: 'http://localhost:5001',
    changeOrigin: true
  }
}
```

---

### Task 10: 验证与测试

- [ ] **Step 1: 启动开发服务器**

```bash
npm run dev
```

- [ ] **Step 2: 浏览器验证**
- 打开 http://localhost:5173 查看首页
- 点击导航项验证页面切换
- 检查"自选股票"页面数据加载

---

## 自检清单

- [ ] spec 覆盖：4个页面、导航组件、快捷卡片、路由配置
- [ ] 占位符检查：无 TBD/TODO/实现后续
- [ ] 类型一致性：所有组件使用一致的样式和命名

---

**计划完成。两种执行方式：**

**1. Subagent-Driven (推荐)** - 每个 Task 由独立 subagent 执行，Task 间有检查点

**2. Inline Execution** - 当前 session 内顺序执行，有检查点

选择哪种方式？