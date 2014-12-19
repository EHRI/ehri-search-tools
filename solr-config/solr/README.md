# Test Solr Core

This directory is used as the Solr core for the search test classes. The contents of the `conf` directory
are also used directly for deployment via the `fabfile.py`.

NB: We do not need a `lib` directory (or anything else, e.g. `data`) because the additional libraries needed
for the Solr configuration (e.g. those used for language detection) are test-scoped dependencies in the `pom.xml`.

TODO: Work out how to create an assembly to gather the various dependency jars for deployment.
