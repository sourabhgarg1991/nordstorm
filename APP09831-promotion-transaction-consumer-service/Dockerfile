ARG CIC_RELEASE
FROM artifactory.nordstrom.com/docker/cic/java_jre_runtime_v21:${CIC_RELEASE}
USER root

ARG APP_VERSION

# Add Maintainer Info
LABEL maintainer="TECH_FIN_DATA_INTEGRATION_SUPPORT@nordstrom.com"

# Add a volume pointing to /tmp
VOLUME /tmp

# Make port 8080 available to the world outside this container
EXPOSE 8080

# GCP credential
ARG GCP_ENV
COPY gcp-credentials-${GCP_ENV}.json /opt/gcp-credentials-${GCP_ENV}.json

ENV JAR_FILE=promotion-transaction-consumer-service-${APP_VERSION}.jar
ARG JAR_PATH="build/libs/${JAR_FILE}"
ENV NEWRELIC_CONFIG_LABELS configure-new-relic-config-labels

COPY ${JAR_PATH} ${JAR_FILE}

USER nonroot

ENTRYPOINT exec java \
    -Dnewrelic.config.distributed_tracing.enabled=true \
    -Dnewrelic.config.error_collector.enabled=true \
    -Dnewrelic.config.log_level=off \
    -Dnewrelic.config.labels="$NEWRELIC_CONFIG_LABELS" \
    -javaagent:/opt/newrelic/newrelic.jar \
    -jar ${JAR_FILE}