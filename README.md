# Indexer

This is a convenience tool for indexing the EHRI database. The basic idea is to read some JSON from a web service 
(EHRI REST), convert it to another format (Solr Doc), and POST it to another web service (Solr). The traditional
way to do this would be something like:

```
curl <WS-URL> | convert-json | curl -X POST "Content-type: application/json" <SOLR-UPDATE-URL> --data @-
```

Here, we just bundle the downloading and uploading bits as well, with some shortcut syntax. There are ways to
accomplish the shell pipeline approach using certain options detailed below.

Notes: To build a jar, use `mvn clean compile assembly:single`. The `compile` phase must be present. See:
http://stackoverflow.com/a/574650/285374. There should then be a Jar called `ehri-indexer-1
.0-SNAPSHOT-jar-with-dependencies.jar` inside the `target` folder, which you can execute with the usual `java -jar
<file.jar> [OPTIONS] ... [ARGS]`.

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
 -i,--index              Index the data. This is NOT the default for safety
                         reasons.
 -n,--noconvert          Don't convert data to index format.
 -P,--pretty             Pretty print out JSON given by --print.
 -p,--print              Print converted JSON to stdout. Also implied by
                         --noindex.
 -r,--rest <arg>         Base URL for EHRI REST service.
 -s,--solr <arg>         Base URL for Solr service (minus the action
                         segment).
 -v,--verbose            Print index stats.
 -V,--veryverbose        Print individual item ids

Each <spec> should consist of:
* an item type (all items of that type)
* an item id prefixed with '@' (individual items)
* a type|id (bar separated - all children of an item)
The default URIs for Solr and the REST service are:
* http://localhost:7474/ehri
* http://localhost:8983/solr/portal
```

## Examples:

Index documentary unit and repository types from default service endpoints:

```
java -jar indexer.jar --index documentaryUnit repository
```

Index individual item `us-005578`:

```
java -jar indexer.jar --index @us-005578
```

Pretty print (to stdout) the converted JSON output for all documentary units, but don't index:

```
java -jar indexer.jar --pretty documentaryUnit
```

Pretty print (to stdout) the raw REST service output:

```
java -jar indexer.jar --pretty --noconvert documentaryUnit
```

Clear the entire index:

```
java -jar indexer.jar --clear-all
```

Index data read from a JSON file instead of the REST service, outputting some stats:

```
java -jar indexer.jar --index -f data.json -v
```

Same as above, but piping the data through stdin (use '-' as the file name):

```
cat data.json | java -jar indexer.jar --index -f - -v
```

Read data from stdin, convert it, and pipe it to a Curl upload for indexing:

```
cat orig.json | java -jar indexer.jar -f - | curl -X POST -H "Content-type: application/json"
"http://localhost:8983/solr/ehri/update?commit=true" --data @-
```


## TODO:

* Add proper logging
* Add proper error handling
* Ensure all resources are properly cleaned up
* Add more tests!
