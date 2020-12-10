## Nginx Docker for Maps Server

Use the start and stop scripts in this folder to start and stop
a nginx server.

Map files available for download are hosted on a nginx server.
We define a folder mount that is made available to the nginx
server from where it can serve static reosurces (map zips).

To simulate the entire stack, you'll also need to start a docker
database and the map upload server.

