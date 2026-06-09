#!/bin/bash
SESSION=$(curl -s "http://localhost:18888/stock/api/chat/session" -X POST)
echo "Session: $SESSION"

cat > /tmp/test-weather.json << JSONEOF
{"sessionId":"SESSION_PLACEHOLDER","model":"qwen3.5:2b","message":"北京天气怎么样","enableThink":false,"enableWebSearch":true}
JSONEOF

sed -i "s/SESSION_PLACEHOLDER/$SESSION/" /tmp/test-weather.json

curl -X POST "http://localhost:18888/stock/api/chat/chat/stream" \
  -H "Content-Type: application/json; charset=utf-8" \
  -d @/tmp/test-weather.json \
  --max-time 60 2>&1 | head -50
