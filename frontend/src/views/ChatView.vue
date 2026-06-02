<template>
  <div class="chat-view">
    <div class="chat-container">
      <div class="session-panel">
        <div class="panel-header">
          <span>会话列表</span>
          <el-button type="primary" size="small" @click="createSession">新建</el-button>
        </div>
        <div class="session-list">
          <div
            v-for="session in sessions"
            :key="session.sessionId"
            :class="['session-item', { active: currentSessionId === session.sessionId }]"
            @click="selectSession(session.sessionId)"
          >
            <span class="session-title">{{ session.title }}</span>
            <el-icon class="delete-btn" @click.stop="deleteSession(session.sessionId)"><Delete /></el-icon>
          </div>
        </div>
      </div>

      <div class="chat-main">
        <div class="chat-header">
          <h2>AI 对话</h2>
          <div class="chat-controls">
            <el-select v-model="currentModel" placeholder="选择模型" size="small" @change="onModelChange">
              <el-option v-for="m in models" :key="m" :label="m" :value="m" />
            </el-select>
            <el-checkbox v-model="enableThink">深度思考</el-checkbox>
            <el-checkbox v-model="enableWebSearch">联网搜索</el-checkbox>
            <el-button v-if="sending" type="danger" size="small" @click="cancelStream">停止</el-button>
          </div>
        </div>

        <div class="message-list" ref="messageListRef">
          <div v-for="msg in messages" :key="msg.id" :class="['message', msg.role]">
            <div class="message-content">
              <div v-if="msg.role === 'user'" class="message-text">{{ msg.content }}</div>
              <template v-else>
                <div v-if="msg.thinkContent" class="think-content">
                  <div class="think-header">💭 思考过程</div>
                  <div class="think-text">{{ msg.thinkContent }}</div>
                </div>
                <div class="message-text">{{ msg.content }}</div>
                <div class="message-meta">
                  <span v-if="msg.durationMs">耗时: {{ (msg.durationMs / 1000).toFixed(1) }}s</span>
                </div>
              </template>
            </div>
          </div>

          <!-- 正在流式输出的消息 -->
          <div v-if="streamingMessage || streamingThink" class="message assistant">
            <div class="message-content">
              <div v-if="streamingThink" class="think-content">
                <div class="think-header">💭 思考中...</div>
                <div class="think-text">{{ streamingThink }}</div>
              </div>
              <div class="message-text">{{ streamingMessage }}</div>
              <div class="streaming-indicator">
                <span class="dot"></span>
                <span class="dot"></span>
                <span class="dot"></span>
              </div>
            </div>
          </div>
        </div>

        <div class="input-area">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="2"
            placeholder="输入消息，Enter发送，Shift+Enter换行"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <el-button type="primary" @click="sendMessage" :loading="sending">发送</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import axios from 'axios'

const models = ref([])
const sessions = ref([])
const currentSessionId = ref(null)
const currentModel = ref('')
const enableThink = ref(false)
const enableWebSearch = ref(false)

const messages = ref([])
const inputText = ref('')
const sending = ref(false)
const streamingMessage = ref('')
const streamingThink = ref('')
const messageListRef = ref(null)

const request = axios.create({
  baseURL: '/stock',
  timeout: 300000
})

const loadModels = async () => {
  try {
    const res = await request.get('/api/chat/models')
    console.log('models response:', res, res.data)
    models.value = Array.isArray(res.data) ? res.data : (res.data?.result || [])
    if (models.value.length > 0 && !currentModel.value) {
      currentModel.value = models.value[0]
    }
  } catch (e) {
    console.error('加载模型列表失败', e)
  }
}

const loadSessions = async () => {
  try {
    const res = await request.get('/api/chat/sessions')
    sessions.value = res.data?.result || res.data || []
  } catch (e) {
    console.error('加载会话列表失败', e)
  }
}

const createSession = async () => {
  try {
    const res = await request.post('/api/chat/session')
    currentSessionId.value = res.data?.result || res.data
    messages.value = []
    await loadSessions()
  } catch (e) {
    console.error('创建会话失败', e)
  }
}

const selectSession = async (sessionId) => {
  currentSessionId.value = sessionId
  try {
    const res = await request.get(`/api/chat/sessions/${sessionId}/messages`)
    messages.value = res.data?.result || res.data || []
    scrollToBottom()
  } catch (e) {
    console.error('加载消息失败', e)
  }
}

const deleteSession = async (sessionId) => {
  try {
    await request.delete(`/api/chat/session/${sessionId}`)
    if (currentSessionId.value === sessionId) {
      currentSessionId.value = null
      messages.value = []
    }
    await loadSessions()
  } catch (e) {
    console.error('删除会话失败', e)
  }
}

const cancelStream = async () => {
  if (!currentSessionId.value) return
  try {
    await request.post(`/api/chat/chat/cancel/${currentSessionId.value}`)
    sending.value = false
    streamingMessage.value = ''
    streamingThink.value = ''
  } catch (e) {
    console.error('取消失败', e)
  }
}

const onModelChange = () => {
  console.log('模型切换为:', currentModel.value)
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

const sendMessage = async () => {
  if (!inputText.value.trim()) return

  if (!currentSessionId.value) {
    ElMessage.warning('请先创建会话')
    return
  }
  if (!currentModel.value) {
    ElMessage.warning('请先选择一个模型')
    return
  }

  sending.value = true
  const userMessage = inputText.value
  inputText.value = ''

  // 添加用户消息
  messages.value.push({
    id: Date.now(),
    role: 'user',
    content: userMessage
  })
  scrollToBottom()

  // 清空流式输出状态
  streamingMessage.value = ''
  streamingThink.value = ''

  try {
    const response = await fetch('/stock/api/chat/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        sessionId: currentSessionId.value,
        model: currentModel.value,
        message: userMessage,
        enableThink: enableThink.value,
        enableWebSearch: enableWebSearch.value
      })
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (!line.trim() || !line.startsWith('data:')) continue

        const data = line.slice(5).trim()
        try {
          const json = JSON.parse(data)

          if (json.type === 'token') {
            streamingMessage.value += json.content
          } else if (json.type === 'think') {
            streamingThink.value += json.content
          } else if (json.type === 'done') {
            console.log('完成:', json)
          } else if (json.type === 'search') {
            console.log('搜索结果:', json.query, json.results)
          }
          scrollToBottom()
        } catch (e) {
          // 忽略解析错误
        }
      }
    }

    // 完成后重新加载消息获取完整内容
    await loadMessages()
  } catch (e) {
    console.error('SSE连接失败', e)
    ElMessage.error('连接失败: ' + e.message)
  } finally {
    sending.value = false
    streamingMessage.value = ''
    streamingThink.value = ''
  }
}

const loadMessages = async () => {
  if (!currentSessionId.value) return
  try {
    const res = await request.get(`/api/chat/sessions/${currentSessionId.value}/messages`)
    messages.value = res.data?.result || res.data || []
    scrollToBottom()
  } catch (e) {
    console.error('加载消息失败', e)
  }
}

onMounted(() => {
  loadModels()
  loadSessions()
})
</script>

<style scoped>
.chat-view {
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.chat-container {
  display: flex;
  height: calc(100vh - 200px);
  min-height: 500px;
}

.session-panel {
  width: 240px;
  background: #f5f7fa;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e4e7ed;
}

.panel-header span {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.session-list {
  flex: 1;
  overflow-y: auto;
}

.session-item {
  padding: 12px 16px;
  cursor: pointer;
  color: #666;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e4e7ed;
  font-size: 14px;
}

.session-item:hover {
  background: #e4e7ed;
}

.session-item.active {
  background: #fff;
  color: #409eff;
  font-weight: 500;
}

.delete-btn {
  opacity: 0;
  transition: opacity 0.2s;
  color: #909399;
}

.session-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  color: #f56c6c;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 16px 20px;
  background: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e4e7ed;
}

.chat-header h2 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.chat-controls {
  display: flex;
  gap: 16px;
  align-items: center;
}

.chat-controls .el-select {
  width: 160px;
}

.chat-controls .el-checkbox {
  color: #666;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #fff;
}

.message {
  margin-bottom: 16px;
  display: flex;
}

.message.user {
  justify-content: flex-end;
}

.message.assistant {
  justify-content: flex-start;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  font-size: 14px;
}

.message.user .message-content {
  background: #409eff;
  color: #fff;
}

.message.assistant .message-content {
  background: #f5f7fa;
  color: #333;
  border: 1px solid #e4e7ed;
}

.message-text {
  word-break: break-word;
  white-space: pre-wrap;
}

.think-content {
  background: #fff8e6;
  border: 1px solid #ffeaa7;
  border-radius: 8px;
  padding: 8px 12px;
  margin-bottom: 8px;
  color: #d63031;
}

.think-header {
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 4px;
}

.think-text {
  font-style: italic;
  font-size: 13px;
  white-space: pre-wrap;
}

.message-meta {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.streaming-indicator {
  display: flex;
  gap: 4px;
  margin-top: 8px;
}

.streaming-indicator .dot {
  width: 6px;
  height: 6px;
  background: #409eff;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out;
}

.streaming-indicator .dot:nth-child(1) { animation-delay: -0.32s; }
.streaming-indicator .dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.input-area {
  padding: 16px 20px;
  background: #fff;
  display: flex;
  gap: 12px;
  align-items: flex-end;
  border-top: 1px solid #e4e7ed;
}

.input-area .el-textarea {
  flex: 1;
}
</style>