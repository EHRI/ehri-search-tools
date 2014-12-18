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
env.tool_name = 'index-helper'
env.service_name = 'tomcat6'
env.tool_jar_path = '/opt/webapps/docview/bin/indexer.jar'
env.config_path = '/opt/webapps/solr4/ehri/portal/conf'
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
    Deploy the latest version of the site to the servers, install any
    required third party modules, and then restart the webserver
    """
    copy_to_server()
    copy_config()
    set_permissions()
    reload()

def reload():
    """
    Reload Solr config files by restarting the portal core.
    """
    run("curl \"%(solr_admin_url)s/cores?action=RELOAD&core=%(solr_core_name)s\"" % env)


def clean_deploy():
    """Build a clean version and deploy."""
    local('mvn clean compile assembly:single')
    deploy()

def get_tool_jar_file():
    version = get_artifact_version()
    tool_file = "%s-%s-jar-with-dependencies.jar" % (env.tool_name, version)
    return os.path.join("target", tool_file)

def copy_to_server():
    "Upload the app to a versioned path."
    # Ensure the deployment directory is there...
    local_file = get_tool_jar_file()
    if not os.path.exists(local_file):
        abort("Jar not found: " + local_file)
    put(local_file, env.tool_jar_path)

def copy_config():
    with lcd("solr/conf"):
        for f in env.config_files:
            put(f, os.path.join(env.config_path, os.path.dirname(f)))

def get_artifact_version():
    return local(
            "mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate" +
            " -Dexpression=project.version|grep -Ev '(^\[|Download\w+:)'",
            capture=True).strip()

def set_permissions():
    """todo"""
    for f in env.config_files:
        run("chgrp webadm " + os.path.join(env.config_path, f))

def start():
    "Start Tomcat"
    # NB: This doesn't use sudo() directly because it insists on asking
    # for a password, even though we should have NOPASSWD in visudo.
    run('sudo service %(service_name)s start' % env, pty=False, shell=False)

def stop():
    "Stop Tomcat"
    # NB: This doesn't use sudo() directly because it insists on asking
    # for a password, even though we should have NOPASSWD in visudo.
    run('sudo service %(service_name)s stop' % env, pty=False, shell=False)

def restart():
    "Restart Tomcat"
    # NB: This doesn't use sudo() directly because it insists on asking
    # for a password, even though we should have NOPASSWD in visudo.
    run('sudo service %(service_name)s restart' % env, pty=False, shell=False)

