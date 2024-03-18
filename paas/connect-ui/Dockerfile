FROM debezium/debezium-ui:latest
USER 0
RUN chmod 755 /deployments/run-java.sh
USER 1001
ENTRYPOINT ["/deployments/run-java.sh"]