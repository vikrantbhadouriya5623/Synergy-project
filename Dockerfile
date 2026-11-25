
FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

RUN mkdir -p out && \
    javac --add-modules jdk.httpserver \
    -d out $(find src -name "*.java")

EXPOSE 10000

CMD ["sh", "-c", "java --add-modules jdk.httpserver -cp out com.autosorter.Main"]
