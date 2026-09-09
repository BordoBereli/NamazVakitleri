#!/bin/bash
# send_fcm_cli.sh — send a data-only FCM message using the Firebase CLI's stored credentials
#
# Usage: ./send_fcm_cli.sh <topic> <title> <body> [project_id]
set -euo pipefail

TOPIC="$1"
TITLE="$2"
BODY="$3"
PROJECT_ID="${4:-namaz-vakitleri}"

ACCESS_TOKEN=$(python3 - <<'EOF'
import json, urllib.request, urllib.parse
cfg = json.load(open('/Users/a195143/.config/configstore/firebase-tools.json'))
rt = cfg['tokens']['refresh_token']
data = urllib.parse.urlencode({
    'grant_type': 'refresh_token',
    'client_id': '563584335869-fgrhgmd47bqnekij5i8b5pr03ho849e6.apps.googleusercontent.com',
    'client_secret': 'j9iVZfS8kkCEFUPaAeJV0sAi',
    'refresh_token': rt,
}).encode()
req = urllib.request.Request('https://oauth2.googleapis.com/token', data=data, method='POST')
print(json.load(urllib.request.urlopen(req))['access_token'])
EOF
)

echo "Sending data-only FCM message to topic '$TOPIC'..."
curl -s -X POST "https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"message\": {
      \"topic\": \"$TOPIC\",
      \"data\": {
        \"title\": \"$TITLE\",
        \"body\": \"$BODY\"
      }
    }
  }"
echo
