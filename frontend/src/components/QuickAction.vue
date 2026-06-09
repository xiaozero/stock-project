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
