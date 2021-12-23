## Http Server

### Background

Http-Server is a 'new' server to host lobby and other functionalities. Historically this was powered by a pure java stack that used java sockets (NIO). The java server was written very early in the project, mid-2000s, the 'http-server' allows for a modern (2019) server to be used. The modern server has integration with JDBI, annotation based rate limiting, authentication and affords an opportunity to rewrite the lobby server in a simpler and more modular fashion.

### Authentication

Connecting to the server, on success, will issue an API key back to the client. Subsequent interactions from the client with the server will send the API key to server for further authorization. Keep in mind all endpoints are publicly available.

### Communication Directions - Http to Server & Websocket to Client

Communication to server is done via standard Http endpoints. Server will process these messages triggering event listeners that will communicate back to clients via websocket.

### Keep-Alive

This concept is to avoid 'ghosts' when we fail to process a disconnect. Players and connected games will need to send HTTP requests to a keep-alive endpoint to explicitly 'register' their liveness. When these messages are not received after a cut-off period, then the game or player are removed.
