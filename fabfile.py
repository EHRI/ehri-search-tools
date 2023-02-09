"""
Fabric deployment script for EHRI backend webapp.
"""

import os
import sys
from datetime import datetime

from fabric import task
from invoke import run as local
from patchwork import files

tool_name = 'index-data-converter'
service_name = 'solr'
tool_jar_path = '/opt/docview/bin/indexer.jar'
solr_core_name = 'portal'
remote_dir = '/opt/docview/solrconf'
config_path = os.path.join(remote_dir, "conf")
lib_path = os.path.join(remote_dir, "lib")
data_path = os.path.join(remote_dir, "data")
config_files = ["schema.xml", "solrconfig.xml", "*.txt", "lang/*"]
solr_admin_url = "http://localhost:8983/solr/admin"


@task
def deploy(ctx, clean=False):
    """Build (optionally with clean) and deploy the distribution"""

    if clean:
        local("mvn clean package -DskipTests")
    else:
        local("mvn package -DskipTests")

    jar = get_tool_jar_file(ctx)
    if not os.path.exists(jar):
        raise Exception(f"Jar not found: {jar}")
    ctx.put(jar, tool_jar_path)

    copy_solr_core(ctx)

    # Set correct permissions on config files...
    ctx.run(f"chown -RH $USER.webadm {config_path}")
    ctx.run(f"chmod -R g+w {config_path}")

    reload(ctx)


@task
def reload(ctx):
    """Reload Solr config files by restarting the portal core."""
    ctx.run(f"curl -s '{solr_admin_url}/cores?action=RELOAD&wt=json&core={solr_core_name}'")


@task
def status(ctx):
    """Get Solr core status"""
    ctx.run(f"curl -s '{solr_admin_url}/cores?action=STATUS&wt=json&core={solr_core_name}'")


@task
def get_artifact_version(ctx):
    """Get the current artifact version from Maven"""
    return local(
        "mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate" +
        " -Dexpression=project.version|grep -Ev '(^\[|Download\w+:)'").stdout.strip()


@task
def get_tool_jar_file(ctx):
    version = get_artifact_version(ctx)
    tool_file = f"{tool_name}-{version}-jar-with-dependencies.jar"
    return os.path.join(tool_name, "target", tool_file)


@task
def copy_solr_core(ctx):
    version = get_artifact_version(ctx)
    core_tgz = f"solr-config/target/solr-config-{version}-solr-core.tar.gz"
    temp_name = get_temp_name(suffix = ".tar.gz")
    remote_name = os.path.join("/var/tmp", os.path.basename(temp_name))
    ctx.put(core_tgz, remote_name)
    ctx.run(f"tar zxvf {remote_name} -C {remote_dir}")
    ctx.run(f"rm {remote_name}")


@task
def restart(ctx):
    """Restart the Neo4j process"""
    if input("Restart Solr (y/n)\n").lower() == "y":
        ctx.run(f"sudo service {service_name} restart")


@task
def get_version_stamp(ctx):
    """Get the tag for a version, consisting of the current time and git revision"""
    res = local("git rev-parse --short HEAD").stdout.strip()
    timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
    return f"{timestamp}_{res}"


def get_temp_name(suffix):
    """Get a temporary file name"""
    import tempfile
    tf = tempfile.NamedTemporaryFile(suffix=suffix)
    name = tf.name
    tf.close()
    return name


def get_timestamp():
    return datetime.now().strftime("%Y%m%d%H%M%S")


