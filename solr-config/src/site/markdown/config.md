# Solr Configuration

This document is a work-in-progress (or a "living" document if you want to be pretention.)

Here are some basic information about how we run Solr:

 - the current version we run is 4.6.1
 - it's hosted by Tomcat 6
 - we run a multi-core install with (currently) just one core, called "portal"
 - the app configuration (`solr.home`) is in `/opt/webapps/solr4/ehri`
 - the .war file is `/opt/webapps/solr4/ehri/solr-4.6.1.war` and *symlinked* to `/opt/webapps/solr4/ehri/solr.war`
 - the lib directory is `/opt/webapps/solr4/ehri/lib-4.6.1` and *symlinked* to `/opt/webapps/solr4/ehri/lib`
 - Tomcat 6 is at `/usr/share/tomcat6`
 - the log file to see if things are going wrong is `/usr/share/tomcat6/logs/catalina.out`
 - the webapp config XML is at `/usr/share/tomcat6/conf/Catalina/localhost/ehri.xml`. It currently looks like this:

```xml
<?xml version="1.0" encoding="utf-8"?>
<Context docBase="/opt/webapps/solr4/ehri/solr.war" debug="0" crossContext="true" reloadable="false">
  <Environment name="solr/home" type="java.lang.String" value="/opt/webapps/solr4/ehri" override="true"/>
</Context>
```

NB: Note the `reloadable="false"` bit: this prevents Tomcat from deleting the context XML when the
war is changed (e.g. when updating Solr).

In theory this means that upgrading to higher Solr versions should be a case of:

 - obtaining updated libs TODO: use Maven to make an assembly containing these
 - obtain an updated war (from the dist?, or can we do it with Maven too?)
 - change the `lib` and `solr.war` symlinks to point to the new ones

## Upgrading Solr

This is the rough guide:

 - ensure Solr version X can run on our crusty old Java 6 (**sadface**)
 - take a backup of the `/usr/share/tomcat6/conf/Catalina/localhost/ehri.xml` in case Tomcat wants to delete it
 - cd into `/opt/webapps/solr4/ehri` on the server
 - obtain the `solr-4.X.X.war` file from the official distro place it here
 - make a new lib directory for `lib-4.X.X` on the server
 - run `mvn package -pl solr-config` to build a lib distro
 - copy `solr-config/target/solr-config-1.0.2-libs.tar.gz` to the server and extract them into `lib-4.X.X`
 - update the symlinks from `solr.war` and `lib` respectively to point to the new `4.X.X` lib and war
 - restart Solr

(NB: There is **definitely** a better way of doing it than this.)
