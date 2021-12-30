# Lobby Server

Installs the lobby server. We run lobby versions parallel to one another on different port numbers.
This allows us to run multiple lobbies on the same host. Further, we have magic config in nginx
to redirect to the correct server instance based on the `triplea-version` header value.
