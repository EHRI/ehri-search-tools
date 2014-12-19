# Solr Test harness

The test harness (under development) is based on the `SolrTestCaseJ4` class and uses the configuration in the
`solr/conf` directory. This directory should be kept up-to-date with the production Solr configuration and the
`fabfile.py` deployment script to be able to deploy it properly to the EHRI servers.

Note: There should be no static Jar files in this project. All the library dependencies (such as those for
the Solr language detection etc) should be properly set as versioned Maven deps with a **test scope** (this is
important otherwise the indexer tool will be unnecessarily bloated with Jars.)

**IMPORTANT**: At present the test data is not under SCM in this project (for various reasons.) For the tests to run
the test data should be in Solr JSON format in `src/test/resources/searchdata.json`.

