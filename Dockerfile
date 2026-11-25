# Ubuntu-based Solr container
FROM docker.io/solr:10.1.0

ENV SOLR_HOME=/opt/solr/solr-config/ehri

# Load the Solr modules providing the extra analysers (ICU, Morfologik,
# Stempel, etc.) and language detection, rather than bundling the jars.
ENV SOLR_MODULES=analysis-extras,langid

ARG PROJECT_VERSION

RUN echo "Using project version: $PROJECT_VERSION"

# NB: Solr config should be mounted at solr-config/ehri
COPY solr-config/target/solr-config-${PROJECT_VERSION}-solr-core.tar.gz /tmp/

# The base image runs as the (non-root) solr user, which cannot write
# to /opt/solr, so we need to switch to root to set things up.
USER root

# The portal search makes queries with large numbers of boolean clauses, and
# since Solr 9 the core's maxBooleanClauses setting is capped by the global
# limit in solr.xml (which defaults to 1024.)
RUN mkdir -p /opt/solr/solr-config/ehri/portal && \
  printf '<?xml version="1.0" encoding="UTF-8" ?>\n\n<solr>\n  <int name="maxBooleanClauses">${solr.max.booleanClauses:3072}</int>\n</solr>\n' \
    > /opt/solr/solr-config/ehri/solr.xml && \
  tar -C /opt/solr/solr-config/ehri/portal --extract --file /tmp/solr-config-${PROJECT_VERSION}-solr-core.tar.gz \
    --exclude 'lib-*' && \
  rm /tmp/solr-config-${PROJECT_VERSION}-solr-core.tar.gz && \
  chown -R solr:solr /opt/solr/solr-config

USER solr

# Solr 10 starts in SolrCloud mode by default: we use a standalone core.
CMD ["solr-foreground", "--user-managed"]
