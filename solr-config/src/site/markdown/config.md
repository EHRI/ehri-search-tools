Solr Configuration
==================

This document is a work-in-progress (or a "living" document if you want to be pretention.)

Here are some basic information about how we run Solr:

 - the current version we run is 4.4.0
 - it's hosted by Tomcat 6
 - we run a multi-core install with (currently) just one core, called "portal"
 - the app configuration (`solr.home`) is in `/opt/webapps/solr4/ehri`
 - the .war file is `/opt/webapps/solr4/ehri/solr-4.4.0.war` and *symlinked* to `/opt/webapps/solr4/ehri/solr.war`
 - the lib directory is `/opt/webapps/solr4/ehri/lib-4.4.0` and *symlinked* to `/opt/webapps/solr4/ehri/lib`
 - Tomcat 6 is at `/usr/share/tomcat6`
 - the log file to see if things are going wrong is `/usr/share/tomcat6/logs/catalina.out`
 - the webapp config XML is at `/usr/share/tomcat6/conf/Catalina/localhost/ehri.xml`. It currently looks like this:

```xml
<?xml version="1.0" encoding="utf-8"?>
<Context docBase="/opt/webapps/solr4/ehri/solr.war" debug="0" crossContext="true">
  <Environment name="solr/home" type="java.lang.String" value="/opt/webapps/solr4/ehri" override="true"/>
</Context>
```

In theory this means that upgrading to higher Solr versions should be a case of:

 - obtaining updated libs TODO: use Maven to make an assembly containing these
 - obtain an updated war (from the dist?, or can we do it with Maven too?)
 - change the `lib` and `solr.war` symlinks to point to the new ones

More work needed, obviously...
