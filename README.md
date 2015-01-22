[![Build Status](https://travis-ci.org/EHRI/ehri-search-tools.svg?branch=master)](https://travis-ci.org/EHRI/ehri-search-tools)

# EHRI Solr Configuration & Tools

This project contains:

* [A test harness for EHRI's Solr search engine configuration](solr-config/README.md)
* [A tool for converting data from the EHRI rest backend to Solr format](index-data-converter/README.md)

The test harness also provides for building a tar file containing the additional libraries Solr requires
given our configuration (language detection, Polish stemming, etc). Currently these can not be automatically
deployed.

## Building:

To build both a standalone jar for the indexer tool, and the set of auxiliary Solr libraries, run:

```
mvn package
```

To build one or other of the modules, use `-pl <module-name>`, i.e:

```
mvn package -pl index-data-converter # will generate the jar indexer/target/index-data-converter-1.0.2-jar-with-dependencies.jar
```

or

```
mvn package -pl solr-config # will generate the tar solr-config/target/solr-config-1.0.2-solr-core.tar.gz
```

The `fabfile.py` handles some deployment tasks, viewable by running `fab --list`. These include:

```
    clean_deploy    Build a clean version and deploy.
    copy_solr_core  Copy the Solr lib, plus core config files to the server
    copy_to_server  Upload the indexer tool to its target directory.
    deploy          Deploy the indexer tool, copy the Solr config, set the permissions
    reload          Reload Solr config files by restarting the portal core.
```
