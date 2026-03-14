# lib

Shared utility libraries used across the TripleA project.

## Modules

### feign-common
Shared configuration for building Feign HTTP clients. Provides base client factory, error handling, and common request/response configuration. Other modules use this to create typed HTTP clients with consistent behavior.

### java-extras
General-purpose Java utility classes. Includes helpers for collections, concurrency, file I/O, and other common operations. Published as a Maven artifact.

### swing-lib
Swing UI utilities and reusable components. Provides helper classes for building Swing interfaces — dialog builders, layout helpers, and common UI patterns. Used by `game-core` and `game-headed`.

### swing-lib-test-support
Test utilities for Swing components. Helpers for testing UI code.

### test-common
Shared test utilities used across modules. Custom Hamcrest matchers (`CustomMatcher`, `IsInstant`, `CollectionMatchers`), test data file loading, JSON serialization helpers, RSA key pair loading for security tests, and Swing test utilities (headless detection, component lookup). See [test-common/AGENTS.md](test-common/AGENTS.md) for details.

### websocket-client
WebSocket client library built on Java 11+ `java.net.http.WebSocket` (stdlib). Provides `GenericWebSocketClient` for connecting to WebSocket servers, with JSON message serialization via Gson and type-safe listener dispatch. See [websocket-client/AGENTS.md](websocket-client/AGENTS.md) for details.

### websocket-server
WebSocket server library. Provides `StandaloneWebsocketServer` (extends `org.java_websocket.server.WebSocketServer`) with message routing via `WebSocketMessagingBus`, session management, and broadcast support.

### xml-reader
Custom XML parsing library for reading TripleA game XML files. Provides annotation-based XML-to-POJO mapping (`@Tag`, `@Attribute`, `@TagList`) used by the `map-data` module.
