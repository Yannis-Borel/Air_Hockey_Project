#!/bin/bash

cd "$(dirname "$0")"

echo "=== Build en cours... ==="
mvn package -q 2>/dev/null

if [ $? -ne 0 ]; then
    echo "=== BUILD ECHOUE — relance sans -q pour voir les erreurs ==="
    mvn package 2>&1 | grep -E "ERROR|error:"
    exit 1
fi

echo "=== Lancement du jeu ==="
java -XstartOnFirstThread -jar target/air-hockey-1.0-SNAPSHOT.jar
