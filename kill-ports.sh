#!/bin/bash

# -----------------------------------------
# kill-ports.sh
# Kills all processes running on given ports
# Usage:
#   ./kill-ports.sh 8080 3000 5432
# -----------------------------------------

if [ "$#" -eq 0 ]; then
  echo "Usage: $0 <port1> <port2> ... <portN>"
  exit 1
fi

for PORT in "$@"
do
  echo "Checking port $PORT..."

  PIDS=$(lsof -ti tcp:$PORT)

  if [ -z "$PIDS" ]; then
    echo "No process running on port $PORT"
  else
    echo "Killing process(es) on port $PORT: $PIDS"
    kill -9 $PIDS
    echo "Port $PORT cleared"
  fi

  echo "----------------------------------"
done