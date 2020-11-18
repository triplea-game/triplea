# Maps Server Design

Describes the design behind how maps are uploaded and downloaded.


## Map Download System

![map-download-design](https://user-images.githubusercontent.com/12397753/99204329-2377c300-276a-11eb-841f-88332f9973f8.png)


Requests are sent to the maps server, it does a lookup in database to return
a listing of the latest version of maps, identified by a public-id.

The client requests from the maps server to download a map by public-id.
The server looks up where in storage the map file lives and then
returns 302 redirect to the actual location of the file to be downloaded.

The 302 is a redirect to a 'storage server' that is hosting the file
on NGINX.


## Maps Server

Maps server is a command-and-control component that interacts with database,
accepts map uploads, and can give a listing of maps to be downloaded.

When a map is uploaded, the maps server will send the uploaded file bytes
to the final storage location and update database as appropriate.

When a map is downloaded, the maps server will use database to know which
storage server has the map and give a link to it.

## Storage Server

The storage server runs both nginx for static file storage and dropwizard
for an intelligent http server. 

### File Uploads from Maps Server

The storage server http server is responsible largely for accepting file
uploads. When a file is uploaded to the maps server, the maps server needs
to distribute the uploaded contents to storage servers. The storage server
provides an API that can be used by the maps server to (forward) upload
map file byte information.

### Secure communication from Maps Server to Storage Server

The communication should be done via https and each server will have a fixed
and secret secret key that will be transmitted as a bearer token in http
headers.


### Server Registration (Service Discovery)

When a storage server boots up, it'll write to database to indicate
the server is available and add itself as a storage server. When the maps
server is looking for storage servers, it'll query database to know about
the available storage servers.


