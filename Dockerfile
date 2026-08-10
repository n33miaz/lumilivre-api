# =============================================================================
#  LumiLivre API — multi-stage Docker build
# -----------------------------------------------------------------------------
#  Build:  docker build -t lumilivre-api:dev .
#  Run:    docker run -p 8080:8080 \
#            -e LUMILIVRE_DB_URL='jdbc:postgresql://host:5432/lumilivre?sslmode=disable' \
#            -e LUMILIVRE_DB_USER=... -e LUMILIVRE_DB_PASSWORD=... \
#            -e LUMILIVRE_JWT_SECRET=... \
#            -e LUMILIVRE_SUPABASE_URL=... -e LUMILIVRE_SUPABASE_KEY=... \
#            -e LUMILIVRE_SUPABASE_SERVICE_ROLE_KEY=... \
#            -e LUMILIVRE_MAIL_USERNAME=... -e LUMILIVRE_MAIL_PASSWORD=... \
#            lumilivre-api:dev
#
#  Toda a configuração vem de variáveis de ambiente (application.properties usa
#  ${LUMILIVRE_*}). NUNCA copie .env nem asse segredos na imagem.
#  Opcionais úteis em dev: LUMILIVRE_STORAGE_PROVIDER=local,
#  LUMILIVRE_FLYWAY_LOCATIONS=classpath:db/migration,classpath:db/seed
# =============================================================================

# ---- Stage 1: build (Maven + JDK 17) ----------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Camada de dependências: só o pom.xml primeiro, para o cache do Docker
# sobreviver a mudanças em src/. (Usamos o mvn da imagem em vez do ./mvnw para
# evitar problemas de CRLF em checkouts Windows.)
COPY pom.xml ./
RUN mvn -q -B dependency:go-offline

# Código-fonte por último — só esta camada rebuilda quando o código muda.
COPY src ./src
RUN mvn -q -B -DskipTests package

# ---- Stage 2: runtime (JRE 17 slim) ------------------------------------------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Usuário não-root; /app/storage é o destino do LUMILIVRE_STORAGE_PROVIDER=local
# (lumilivre.storage.local.base-dir default = ./storage, relativo ao WORKDIR).
RUN addgroup -S lumilivre && adduser -S -G lumilivre lumilivre \
    && mkdir -p /app/storage \
    && chown -R lumilivre:lumilivre /app

COPY --from=build /build/target/*.jar /app/app.jar

# JVM enxuta para containers pequenos (Render free = 512 MB):
# MaxRAMPercentage respeita o limite de memória do container; SerialGC tem o
# menor footprint. Sobrescreva com -e JAVA_OPTS="..." se tiver mais memória.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseSerialGC"

# Sem isto o Spring cai no perfil `default`, e o logback-spring.xml poe
# `br.com.lumilivre` em DEBUG — o message source registra cada busca de chave
# i18n, dezenas por requisicao. Onde o stdout e limitado (Render), cada linha
# passa a custar centenas de ms e trava a thread: a aplicacao sobe e mesmo
# assim o deploy expira. `prod` tambem liga o log JSON estruturado.
ENV SPRING_PROFILES_ACTIVE=prod

# A porta e do ambiente (server.port=${PORT:8080}); o Render injeta a dele.
# EXPOSE e HEALTHCHECK precisam seguir a MESMA variavel, senao a checagem bate
# numa porta onde ninguem escuta.
ENV PORT=8080

USER lumilivre
EXPOSE ${PORT}

# /actuator/health é permitAll no SecurityConfig; wget é o da busybox (alpine).
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -qO- "http://localhost:${PORT}/actuator/health" || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
