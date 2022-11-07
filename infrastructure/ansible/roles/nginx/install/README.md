Installs nginx with configuration for port forwarding and https certificate.
We'll have nginx listen on secure port, forward received requests to application
server on non-secure port, and then respond back to the client via secure (https)
connection.
