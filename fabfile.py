"""
Fabric deployment script for EHRI index helper and Solr config.
"""

from __future__ import with_statement

import os
import datetime
import subprocess
from datetime import datetime

from fabric.api import *
from fabric.contrib.console import confirm
from fabric.contrib.project import upload_project
from contextlib import contextmanager as _contextmanager

# globals
env.prod = False
env.use_ssh_config = True
env.tool_name = 'index-data-converter'
env.service_name = 'tomcat6'
env.tool_jar_path = '/opt/webapps/docview/bin/indexer.jar'
env.config_path = '/opt/webapps/solr4/ehri/portal/conf'
env.lib_path = '/opt/webapps/solr4/ehri/lib'
env.data_path = '/opt/webapps/solr4/ehri/portal/data'
env.user = os.getenv("USER")
env.config_files = ["schema.xml", "solrconfig.xml", "*.txt", "lang/*"]
env.solr_admin_url = "http://localhost:8080/ehri/admin"
env.solr_core_name = "portal"

TIMESTAMP_FORMAT = "%Y%m%d%H%M%S"

# environments
def test():
    "Use the remote testing server"
    env.hosts = ['ehritest']

def stage():
    "Use the remote staging server"
    env.hosts = ['ehristage']

def prod():
    "Use the remote virtual server"
    env.hosts = ['ehriprod']
    env.prod = True

def deploy():
    """
    Deploy the indexer tool, copy the Solr config, set the permissions
    correctly, and reload the Solr core.
    """
    copy_to_server()
    copy_config()
    _set_permissions()
    reload()

def reload():
    """
    Reload Solr config files by restarting the portal core.
    """
    run("curl \"%(solr_admin_url)s/cores?action=RELOAD&core=%(solr_core_name)s\"" % env)


def clean_deploy():
    """Do a clean build, deploy the indexer tool, copy the Solr config, set the permissions
    correctly, and reload the Solr core."""
    local('mvn clean compile assembly:single -pl ' + env.tool_name)
    deploy()

def copy_to_server():
    "Upload the indexer tool to its target directory"
    # Ensure the deployment directory is there...
    local_file = _get_tool_jar_file()
    if not os.path.exists(local_file):
        abort("Jar not found: " + local_file)
    put(local_file, env.tool_jar_path)

def copy_config():
    """Copy the Solr config files to the server"""
    with lcd("solr-config/solr/conf"):
        for f in env.config_files:
            put(f, os.path.join(env.config_path, os.path.dirname(f)))

def start():
    "Start Tomcat"
    _run_service_cmd("start")

def stop():
    "Stop Tomcat"
    _run_service_cmd("stop")

def restart():
    "Restart Tomcat"
    _run_service_cmd("restart")

def _set_permissions():
    """Set the currect permissions on the config files."""
    for f in env.config_files:
        run("chgrp webadm " + os.path.join(env.config_path, f))

def _get_tool_jar_file():
    version = _get_artifact_version()
    tool_file = "%s-%s-jar-with-dependencies.jar" % (env.tool_name, version)
    return os.path.join(env.tool_name, "target", tool_file)

def _run_service_cmd(name):
    # NB: This doesn't use sudo() directly because it insists on asking
    # for a password, even though we should have NOPASSWD in visudo.
    with settings(service_task=name):
        run('sudo service %(service_name)s %(service_task)s' % env, pty=False, shell=False)

def _get_artifact_version():
    """Get the current artifact version from Maven"""
    return local(
            "mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate" +
            " -Dexpression=project.version|grep -Ev '(^\[|Download\w+:)'",
            capture=True).strip()

