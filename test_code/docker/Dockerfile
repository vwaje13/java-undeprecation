#FROM blccdeda01-registry.cecc.t-mobile.lab/cicd_poc:1.0.0-f893352
FROM docker.artifactory.rosetta.ericssondevops.com/sd-mana-tmo-bss-docker/cecc-eda2-cloud-native/cicd_poc:1.0.0-f893352
EXPOSE 8080
RUN pwd
RUN mkdir -p /app/kafka/config
WORKDIR /app/kafka
COPY /target/*.jar /app/kafka/
COPY /bin/*.sh /app/kafka/
RUN chmod -R +x /app/kafka/
ENTRYPOINT ["/bin/bash","-c"]
CMD ["/app/kafka/startup.sh"]

