#!/bin/bash
SESSION=$(curl -s "http://localhost:18888/stock/api/chat/session" -X POST)
echo "Session: $SESSION"

# Create JSON file with proper encoding
cat > /tmp/test-weather.json << 'JSONEOF'
{"sessionId":"SESSION_PLACEHOLDER","model":"qwen3.5:2b","message":"beijing weather","enableThink":false,"enableWebSearch":true}
JSONEOF

sed -i "s/SESSION_PLACEHOLDER/$SESSION/" /tmp/test-weather.json

curl -X POST "http://localhost:18888/stock/api/chat/chat/stream" \
  -H "Content-Type: application/json; charset=utf-8" \
  -d @/tmp/test-weather.json \
  --max-time 90 2>&1 | head -60