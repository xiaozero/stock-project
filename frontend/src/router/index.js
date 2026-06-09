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
  history: createWebHistory('/stock'),
  routes
})

export default router