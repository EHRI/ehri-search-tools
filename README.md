Indexer
=======

**This is a work in progress!**

Notes: To build a jar, use `mvn clean compile assembly:single`. The `compile` phase must be present. See:
http://stackoverflow.com/a/574650/285374.

Current options:

```
usage: indexer
 -e,--ehri <arg>   Base URL for EHRI REST service.
 -f,--file <arg>   Read input from a file instead of the REST service. Use
                   '-' for stdin.
 -h,--help         Print this message.
 -n,--noconvert    Don't convert data to index format. Implies --noindex.
 -p,--pretty       Pretty print out JSON given by --print.
 -s,--solr <arg>   Base URL for Solr service (minus the action segment).
 -v,--verbose      Print index stats.
```

TODO:
* Add proper logging
* Add proper error handling
* Ensure all resources are properly cleaned up
* Add option to index all child items of an item
* Add some tests!
