# TripleA Forums

The software that powers the TripleA Forums is [NodeBB](https://github.com/NodeBB/NodeBB). 
This document is instructions on how to build and run the Docker container of the forums using an 
[official Docker container of NodeBB](https://hub.docker.com/r/nodebb/docker) as the base image.

### Building

1. Make a directory on your machine for where the config file and upload folder will live. ```mkdir -p /opt/triple-forums```
1. Make a directory for uploaded files. Inside the upload directory, create folders category, emoji, files, profile, 
sounds, and system.
    ```
    mkdir -p /opt/triple-forums/uploads/category
    mkdir -p /opt/triple-forums/uploads/emoji
    mkdir -p /opt/triple-forums/uploads/files
    mkdir -p /opt/triple-forums/uploads/profile
    mkdir -p /opt/triple-forums/uploads/sounds
    mkdir -p /opt/triple-forums/uploads/system
    ```
1. Build the Docker image using the command ```docker build -t triplea-forums:latest .```

### Running

**First time run**

You will need a running database. The database can be either Redis, Mongo, or Postgres. Make sure you have one of these
installed on your machine or a Docker container running. 

1. Run the triplea-forums:latest image using the command 
```docker run -d --rm --name triplea-forums -v /opt/triple-forums/uploads:/usr/src/app/public/uploads -p 8080:4567 triplea-forums:latest```
1. Navigate to http://0.0.0.0:8080/. This will be the install and configuration screen for NodeBB.
1. Fill out the details on the page.
1. Once the configuration is complete, you will need to save the config.json file off the container to the folder you 
created earlier. Run ```docker container cp triplea-forums:/usr/src/app/config.json /opt/triplea-forums```

**Second and later runs**

1. If you did not save the config.json from before, you can make a copy of the example/config.json 
and update it per the instructions in [Configuration](#configuration) section. 
```cp example/config.json /opt/triplea-forums/config.json```
1. With the config.json run the triplea-forums:latest image using the command 
```docker run -d --rm --name triplea-forums -v /opt/triple-forums/config.json:/usr/src/app/config.json -v /opt/triple-forums/uploads:/usr/src/app/public/uploads -p 8080:4567 triplea-forums:latest```

### Stopping

To stop the running container, run the following: ```docker stop triplea-forums```

### NodeBB

The table below lists the version of NodeBB that TripleA Forums is currently running. This document 
is written to that version.


| TripleA Forums | NodeBB Version | Package Json |
|----------------|----------------|--------------|
| Current        | 1.13.3         | [package.json source](https://github.com/NodeBB/NodeBB/blob/v1.13.3/install/package.json)|
| Future         | 1.14.3         | [package.json source](https://github.com/NodeBB/NodeBB/blob/v1.14.3/install/package.json)|

#### Upgrading To A Newer NodeBB Version

Upgrading to a newer version uses the following process:

1. Change the version of the From image in the Dockerfile to the newer version of NodeBB.
1. Update the install/package.json to use packages from the newer versions package.json source.
1. Replace the nodebb-plugins in the install/package.json with the current list of plugins used for TripleA Forums.
1. Run Docker build to create the new image.

#### Plugins  

NodeBB customizations are plugins which are installed either through the admin interface or by adding them 
to package.json directly. For the TripleA Forums, they are installed through the package.json.

#### Installing/Upgrading A Plugin

Installing or grading a plugin use the following process:

1. Update the install/package.json to use the newer plugin.
1. Run Docker build to create the new image.

##### List of currently installed plugins. 

NOTE: *Refer to install/package.json for the most up to date list.*

```
"nodebb-plugin-2factor": "^2.7.2",
"nodebb-plugin-cards": "0.2.2",
"nodebb-plugin-composer-default": "6.3.25",
"nodebb-plugin-custom-pages": "^1.1.3",
"nodebb-plugin-dbsearch": "4.0.7",
"nodebb-plugin-desktop-notifications": "^0.3.3",
"nodebb-plugin-emoji": "^3.3.0",
"nodebb-plugin-emoji-android": "2.0.0",
"nodebb-plugin-emoji-one": "^2.0.0",
"nodebb-plugin-featured-threads": "0.2.1",
"nodebb-plugin-github-embed": "^0.7.5",
"nodebb-plugin-gravatar": "^2.2.4",
"nodebb-plugin-markdown": "8.11.2",
"nodebb-plugin-mentions": "2.7.4",
"nodebb-plugin-newsletter": "^0.8.1",
"nodebb-plugin-ns-embed": "^4.0.0",
"nodebb-plugin-ns-login": "^3.0.0",
"nodebb-plugin-poll": "^0.3.3",
"nodebb-plugin-shoutbox": "^0.3.4",
"nodebb-plugin-soundpack-default": "1.0.0",
"nodebb-plugin-spam-be-gone": "0.6.7",
"nodebb-plugin-sso-facebook": "^3.5.1",
"nodebb-plugin-sso-github2": "2.0.2",
"nodebb-plugin-sso-google": "^2.4.1",
"nodebb-plugin-sso-twitter": "^2.5.8"
```    

#### Configuration

The NodeBB configuration is a JSON file, ```config.json```, which is kept at the root of the NodeBB install.
An example of that file can be found at ```example/config.json```. That configuration file is setup to run Redis 
as the database. NodeBB can use Redis, MongoDB, or Postgres as a database. Information on all the configuration 
options for NodeBB can be found [here](https://docs.nodebb.org/configuring/config/).

Copy the example/config.json file to a location of your choice. Edit the file to update the parameters for the database. 

**Key points of the configuration to remember**:

1. The *url* parameter is referencing the address NodeBB is binding to inside the Docker container. This is different 
from the address of the host machine.
1. The *port* parameter is referencing the port NodeBB is listening on inside the Docker container. This does not 
have to be the same port on the host machine. Docker maps the host machine port to the container port.  
1. Database (Redis/Mongo/Postgres) parameters *host* and *port* are referring to the address and port of the machine 
the database is running on. 
