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
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="handleAiAnalyze(row)">
            AI分析
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <p v-else-if="!loading" style="margin-top: 20px; color: #999;">暂无自选股票</p>

    <!-- AI分析结果弹窗 -->
    <el-dialog v-model="aiDialogVisible" title="AI分析结果" width="600px">
      <div v-if="aiAnalysisResult">
        <p><strong>股票：</strong>{{ aiAnalysisResult.stockName }} ({{ aiAnalysisResult.stockCode }})</p>
        <p><strong>分析时间：</strong>{{ aiAnalysisResult.analysisDate }}</p>
        <p><strong>使用模型：</strong>{{ aiAnalysisResult.modelName }}</p>
        <el-divider />
        <div class="analysis-content">{{ aiAnalysisResult.analysisContent }}</div>
      </div>
      <div v-else-if="aiAnalyzing" class="loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>AI 正在分析中，请稍候...</span>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { stockApi } from '@/api/stock'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'

const stocks = ref([])
const loading = ref(false)
const aiDialogVisible = ref(false)
const aiAnalyzing = ref(false)
const aiAnalysisResult = ref(null)

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

const handleAiAnalyze = async (row) => {
  aiDialogVisible.value = true
  aiAnalyzing.value = true
  aiAnalysisResult.value = null

  try {
    const res = await stockApi.aiAnalyze(row.stockCode, row.stockName)
    if (res.code === 200) {
      aiAnalysisResult.value = res.result
    } else {
      ElMessage.error(res.msg || '分析失败')
    }
  } catch (e) {
    ElMessage.error('分析失败: ' + e.message)
  } finally {
    aiAnalyzing.value = false
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
.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #666;
}
.analysis-content {
  white-space: pre-wrap;
  line-height: 1.6;
}
</style>