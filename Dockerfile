# Etapa de build
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Copia os arquivos do projeto para dentro do contêiner
COPY . .

# Compila o projeto com Maven
RUN mvn clean package -DskipTests

# Etapa de execução
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copia o arquivo JAR gerado da etapa de build
COPY --from=build /app/target/*.jar app.jar

# Comando para rodar a aplicação
CMD ["java", "-jar", "app.jar"]
