import axios from 'axios'

const request = axios.create({
  baseURL: '/stock',
  timeout: 30000
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200 || res.code === 0) {
      return res
    }
    throw new Error(res.message || '请求失败')
  },
  error => Promise.reject(error)
)

export const stockApi = {
  list: () => request.get('/api/stock/list'),
  getPrice: () => request.post('/api/stock/getStockRealPrice'),
  aiAnalyze: (stockCode, stockName) => request.post('/api/stock/ai-analyze', { stockCode, stockName })
}