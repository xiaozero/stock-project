<template>
  <div class="nav-header">
    <div class="nav-content">
      <div class="nav-left">
<!--        <span class="logo">股票项目</span>-->
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
