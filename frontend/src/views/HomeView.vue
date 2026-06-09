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