# LumiLivre API — task runner (https://just.systems)
# Cross-platform (Linux, macOS, Windows). Instale com:
#   - macOS:    brew install just
#   - Linux:    cargo install just  (ou pacote da distro)
#   - Windows:  scoop install just  ou  winget install Casey.Just
#
# Lista de targets: `just`  ou  `just --list`

set windows-shell := ["powershell.exe", "-NoLogo", "-NoProfile", "-Command"]

mvnw := if os() == "windows" { ".\\mvnw.cmd" } else { "./mvnw" }

flyway_url   := "jdbc:postgresql://localhost:5432/lumilivre"
flyway_user  := "lumilivre"
flyway_pass  := "lumilivre"
flyway_locs  := "classpath:db/migration,classpath:db/seed,classpath:db/vendor/postgresql"

# Mostra a lista de targets ordenada.
default:
    @just --list --unsorted

# Sobe Postgres + Redis + Mailhog em background.
up:
    docker compose up -d

# Derruba todos os containers do stack local.
down:
    docker compose down

# Derruba e apaga volumes (banco zerado).
clean:
    docker compose down -v
    {{mvnw}} clean

# Aplica migrations + seed demo no Postgres local.
migrate:
    docker compose up -d postgres
    {{mvnw}} flyway:migrate -Dflyway.url={{flyway_url}} -Dflyway.user={{flyway_user}} -Dflyway.password={{flyway_pass}} -Dflyway.locations={{flyway_locs}}

# Re-aplica somente o seed (R__ repeatable já roda em todo migrate).
seed: migrate

# Setup completo do zero: containers + migrations + seed.
[unix]
setup:
    docker compose up -d
    @echo "Aguardando Postgres ficar saudavel..."
    @sleep 6
    just migrate
    @echo ""
    @echo "API pronta para 'just api'. Mailhog em http://localhost:8025"

[windows]
setup:
    docker compose up -d
    @Write-Host "Aguardando Postgres ficar saudavel..."
    @Start-Sleep -Seconds 6
    just migrate
    @Write-Host ""
    @Write-Host "API pronta para 'just api'. Mailhog em http://localhost:8025"

# Roda a API no profile local.
api:
    {{mvnw}} spring-boot:run -Dspring-boot.run.profiles=local

# Build do jar.
build:
    {{mvnw}} clean package -DskipTests

# Roda toda a suite de testes.
test:
    {{mvnw}} test

# Verify completo (testes + jacoco report).
verify:
    {{mvnw}} verify

# Verify com gate jacoco forcado (mesma flag do CI quando F8 fechar).
verify-strict:
    {{mvnw}} verify -Djacoco.enforce=true

# Abre psql no container Postgres.
psql:
    docker exec -it lumilivre-postgres psql -U lumilivre -d lumilivre

# Mostra status dos containers.
status:
    docker compose ps

# Tail nos logs dos containers.
logs:
    docker compose logs -f --tail=100
