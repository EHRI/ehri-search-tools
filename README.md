Indexer
=======

**This is a work in progress!**

Notes: To build a jar, use `mvn clean compile assembly:single`. The `compile` phase must be present. See:
http://stackoverflow.com/a/574650/285374.

Current options:

```
usage: indexer  [OPTIONS] <spec> ... <specN>
 -c,--clear-id <arg>     Clear an individual id. Can be used multiple
                         times.
 -C,--clear-type <arg>   Clear an item type. Can be used multiple times.
 -D,--clear-all          Clear entire index first (use with caution.)
 -f,--file <arg>         Read input from a file instead of the REST
                         service. Use '-' for stdin.
 -h,--help               Print this message.
 -n,--noconvert          Don't convert data to index format. Implies
                         --noindex.
 -P,--pretty             Pretty print out JSON given by --print.
 -p,--print              Print converted JSON to stdout. Also implied by
                         --noindex.
 -r,--rest <arg>         Base URL for EHRI REST service.
 -s,--solr <arg>         Base URL for Solr service (minus the action
                         segment).
 -v,--verbose            Print index stats.
 -V,--veryverbose        Print individual item ids

Each <spec> should consist of:
- an item type (all items of that type)
- an item id prefixed with '@' (individual items)
- a type|id (bar separated - all children of an item)
```

TODO:
* Add proper logging
* Add proper error handling
* Ensure all resources are properly cleaned up
* Add more tests!
