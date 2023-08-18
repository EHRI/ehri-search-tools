# Ubuntu-based Solr container
FROM solr:8.11

ENV SOLR_HOME=/opt/solr/solr-config/ehri

# NB: Solr config should be mounted at solr-config/ehri
COPY solr-config/target/solr-config-*-solr-core.tar.gz /tmp/
USER root
RUN mkdir -p /opt/solr/solr-config/ehri/portal && \
  echo "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n\n<solr></solr>" > /opt/solr/solr-config/ehri/solr.xml && \
  tar -C /opt/solr/solr-config/ehri/portal --extract --file /tmp/solr-config-*-solr-core.tar.gz && \
  ln -s /opt/solr/solr-config/ehri/portal/lib-* /opt/solr/solr-config/ehri/portal/lib && \
  chown solr.solr --recursive /opt/solr/solr-config
USER solr
