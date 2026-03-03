FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY gigachat_cert/russian_trusted_root_ca.cer /usr/local/share/ca-certificates/root.crt
COPY gigachat_cert/russian_trusted_root_ca_gost_2025.cer /usr/local/share/ca-certificates/root_gost.crt
COPY gigachat_cert/russian_trusted_sub_ca.cer /usr/local/share/ca-certificates/sub.crt
COPY gigachat_cert/russian_trusted_sub_ca_2024.cer /usr/local/share/ca-certificates/sub_2024.crt
COPY gigachat_cert/russian_trusted_sub_ca_gost_2025.cer /usr/local/share/ca-certificates/sub_gost.crt

RUN keytool -importcert -trustcacerts -file /usr/local/share/ca-certificates/root.crt -alias russian-root -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt && \
    keytool -importcert -trustcacerts -file /usr/local/share/ca-certificates/root_gost.crt -alias russian-root-gost -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt && \
    keytool -importcert -trustcacerts -file /usr/local/share/ca-certificates/sub.crt -alias russian-sub -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt && \
    keytool -importcert -trustcacerts -file /usr/local/share/ca-certificates/sub_2024.crt -alias russian-sub-2024 -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt && \
    keytool -importcert -trustcacerts -file /usr/local/share/ca-certificates/sub_gost.crt -alias russian-sub-gost -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt

# 3. Проверочная команда: выведет список установленных сертификатов в логи сборки
RUN keytool -list -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit | grep russian

COPY target/*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]