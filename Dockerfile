# Ubuntu-based Solr container
FROM docker.io/solr:6.6.6

ENV SOLR_HOME=/opt/solr/solr-config/ehri
ENV SOLR_VERSION=6.6.6

ARG PROJECT_VERSION

RUN echo "Using project version: $PROJECT_VERSION"

# NB: Solr config should be mounted at solr-config/ehri
COPY solr-config/target/solr-config-${PROJECT_VERSION}-solr-core.tar.gz /tmp/
RUN mkdir -p /opt/solr/solr-config/ehri/portal && \
  echo "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n\n<solr></solr>" > /opt/solr/solr-config/ehri/solr.xml && \
  tar -C /opt/solr/solr-config/ehri/portal --extract --file /tmp/solr-config-${PROJECT_VERSION}-solr-core.tar.gz && \
  ln -s /opt/solr/solr-config/ehri/portal/lib-${SOLR_VERSION} /opt/solr/solr-config/ehri/portal/lib
