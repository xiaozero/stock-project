import axios from 'axios'

const request = axios.create({
  baseURL: '/stock',
  timeout: 120000
})

export const chatApi = {
  getModels: () => request.get('/api/chat/models'),

  getSessions: () => request.get('/api/chat/sessions'),

  getMessages: (sessionId) => request.get(`/api/chat/sessions/${sessionId}/messages`),

  createSession: () => request.post('/api/chat/session'),

  deleteSession: (sessionId) => request.delete(`/api/chat/session/${sessionId}`),

  chat: (data) => request.post('/api/chat/chat', data)
}