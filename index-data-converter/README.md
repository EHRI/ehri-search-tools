## EHRI Index Tool

This is a convenience tool used by the EHRI frontend to synchronise the search engine with the EHRI backend
(and for doing the same easily from the command-line.) The basic idea is to read some JSON from a web service
(EHRI REST), convert it to another format (Solr Doc), and POST it to another web service (Solr).

The traditional way to do this would be something like:

```
curl <WS-URL> | convert-json | curl -X POST "Content-type: application/json" <SOLR-UPDATE-URL> --data @-
```

Here, we just bundle the downloading and uploading bits with some shortcut syntax. There are ways to
accomplish the shell pipeline approach using certain options detailed below.

### Current options:

```
usage: index-data-converter [OPTIONS] <spec> ... <specN>
 -c,--clear-id <arg>          Clear an individual id. Can be used multiple
                              times.
 -C,--clear-type <arg>        Clear an item type. Can be used multiple
                              times.
 -D,--clear-all               Clear entire index first (use with caution.)
 -f,--file <arg>              Read input from a file instead of the REST
                              service. Use '-' for stdin.
 -H <header=value>            Set a header for the REST service.
 -h,--help                    Print this message.
 -i,--index                   Index the data. This is NOT the default for
                              safety reasons.
 -k,--clear-key-value <arg>   Clear items with a given key=value pair. Can
                              be used multiple times.
 -n,--noconvert               Don't convert data to index format.
 -P,--pretty                  Pretty print out JSON given by --print
                              (implies --print).
 -p,--print                   Print converted JSON to stdout. The default
                              action in the omission of --index.
 -r,--rest <arg>              Base URL for EHRI REST service.
 -s,--solr <arg>              Base URL for Solr service (minus the action
                              segment.)
 -S,--stats                   Print indexing stats.
 -v,--verbose                 Print individual item ids to show progress.
 -version                     Print the version number and exit.

Each <spec> should consist of:
* an item type (all items of that type)
* an item id prefixed with '@' (individual items)
* a type|id (bar separated - all children of an item)
The default URIs for Solr and the REST service are:
* http://localhost:7474/ehri
* http://localhost:8983/solr/portal
```

### Examples:

Index documentary unit and repository types from default service endpoints:

```
java -jar index-data-converter.jar --index DocumentaryUnit Repository
```

Index individual item `us-005578`:

```
java -jar index-data-converter.jar --index @us-005578
```

Pretty print (to stdout) the converted JSON output for all documentary units, but don't index:

```
java -jar index-data-converter.jar --pretty DocumentaryUnit
```

Pretty print (to stdout) the raw REST service output:

```
java -jar index-data-converter.jar --pretty --noconvert DocumentaryUnit
```

Clear the entire index:

```
java -jar index-data-converter.jar --clear-all
```

Clear items with holderId 'us-005248':

```
java -jar index-data-converter.jar --clear-key-value holderId=us-005248
```

Index data read from a JSON file instead of the REST service, outputting some stats:

```
java -jar index-data-converter.jar --index -f data.json -v
```

Same as above, but piping the data through stdin (use '-' as the file name):

```
cat data.json | java -jar index-data-converter.jar --index -f - -v
```

Read data from stdin, convert it, and pipe it to a Curl upload for indexing:

```
cat orig.json | java -jar index-data-converter.jar -f - | curl -X POST -H "Content-type: application/json"
"http://localhost:8983/solr/ehri/update?commit=true" --data @-
```


### TODO:

* Add proper logging
* Add proper error handling
* Ensure all resources are properly cleaned up
* Add more tests!
