# Ubuntu-based Solr container
FROM dockerfile/java:oracle-java8

ENV SOLR_VERSION 4.10.4
ENV SOLR solr-$SOLR_VERSION
ENV SOLR_MIRROR http://archive.apache.org/dist/lucene/solr
ENV TOOL_VERSION 1.1.1-SNAPSHOT
ENV PORT 8983

RUN export DEBIAN_FRONTEND=noninteractive && \
  apt-get update && \
  apt-get -y install lsof curl procps && \
  mkdir -p /opt && \
  wget -nv --output-document=/opt/$SOLR.tgz $SOLR_MIRROR/$SOLR_VERSION/$SOLR.tgz && \
  tar -C /opt --extract --file /opt/$SOLR.tgz && \
  rm /opt/$SOLR.tgz && \
  ln -s /opt/$SOLR /opt/solr

# NB: Solr config should be mounted at /opt/solr-config/ehri
COPY solr-config/target/solr-config-${TOOL_VERSION}-solr-core.tar.gz /tmp/
RUN mkdir /opt/solr-config && \
  tar -C /opt/solr-config --extract --file /tmp/solr-config-${TOOL_VERSION}-solr-core.tar.gz && \
  ln -s /opt/solr-config/ehri/lib-$SOLR_VERSION /opt/solr-config/ehri/lib

EXPOSE $PORT

# Run the example Solr Jetty launcher with our config
WORKDIR /opt/solr/example
CMD ["/usr/bin/java", "-jar", "/opt/solr/example/start.jar", "-Dsolr.solr.home=/opt/solr-config/ehri"]
