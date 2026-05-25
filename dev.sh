#!/bin/bash
# comando de permissão: chmod +x dev.sh

# verifica se o docker está rodando
echo "[DEV] Verifying dependencies..."
if ! docker info > /dev/null 2>&1; then
  echo "[ERROR] Docker is not responding. Please make sure Docker is running and try again."
  exit 1
fi

# inicia o containers
echo "[DEV] Starting docker containers..."
docker compose up -d

# lê o arquivo .env e injeta as variáveis de ambiente 
echo "[DEV] Loading variables from '.env' file..."
while IFS='=' read -r key value; do
  if [[ "$key" != \#* ]]; then
    
    key=$(echo "$key" | tr -d ' \t\r\n')
    
    if [[ -n "$key" ]]; then
      value=$(echo "$value" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' | tr -d '\r')
      
      value="${value%\"}"
      value="${value#\"}"
      value="${value%\'}"
      value="${value#\'}"

      export "$key=$value"
    fi
  fi
done < .env

# inicia o projeto (ambiente dev: -Dspring-boot.run.profiles=local)
echo "[DEV] Starting Spring Boot..."
./mvnw spring-boot:run 