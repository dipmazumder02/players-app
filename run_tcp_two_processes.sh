#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
PORT="${PORT:-8082}"
HOST="${HOST:-127.0.0.1}"
mvn -q -DskipTests package
# start responder
java -cp target/classes com.dip.players.tcp.TcpResponderMain "$PORT" &
RESP_PID=$!
echo "Responder PID: $RESP_PID"
sleep 1
# start initiator (blocks until done)
java -cp target/classes com.dip.players.tcp.TcpInitiatorMain "$HOST" "$PORT"
wait "$RESP_PID"
