# lobby-client

Feign-based HTTP client library for the TripleA lobby server API. Published as a Maven artifact (`maven-publish` plugin) consumed by the game client and lobby server.

## Architecture

### Layers

1. **Configuration** — `LobbyHttpClientConfig` (static singleton set once at startup), `ClientIdentifiers`
2. **Authentication** — `AuthenticationHeaders` wraps `ApiKey` into `Authorization: Bearer <key>` + version/system-id headers
3. **HTTP Clients** — Feign interfaces with `@RequestLine` annotations; each provides a static `newClient(URI, ...)` factory
4. **WebSocket Messages** — message envelope types implementing `WebSocketMessage` with static `MessageType<T>` constants
5. **Connection Facades** — `PlayerToLobbyConnection` and `GameToLobbyConnection` combine HTTP clients + WebSocket into unified APIs

### Client Construction Pattern

Every Feign client interface follows this pattern:

```java
public interface SomeClient {
  static SomeClient newClient(URI uri, ApiKey apiKey) {
    return HttpClient.newClient(SomeClient.class, uri,
        new AuthenticationHeaders(apiKey).createHeaders());
  }

  @RequestLine("POST /lobby/some-endpoint")
  SomeResponse doSomething(SomeRequest request);

  // Convenience default methods wrapping builder logic
  default void doSomethingSimple(String id, long value) {
    doSomething(SomeRequest.builder().id(id).value(value).build());
  }
}
```

Clients without authentication (login, error reporting, maps, forgot-password) use `AuthenticationHeaders.systemIdHeaders()` instead.

### HTTP Headers

| Header | Source | When Sent |
|---|---|---|
| `Authorization` | `Bearer <ApiKey>` | Authenticated endpoints |
| `Triplea-Version` | `LobbyHttpClientConfig.clientVersion` | All requests |
| `System-Id-Header` | `LobbyHttpClientConfig.systemId` | All requests |

### Aggregator Classes

- **`HttpLobbyClient`** — holds `ModeratorToolboxClient`, `ModeratorLobbyClient`, `UserAccountClient`, `RemoteActionsClient`, `PlayerLobbyActionsClient`; factory method `newGameListingClient()`
- **`ModeratorToolboxClient`** — holds all `Toolbox*Client` instances (access logs, event logs, bans, bad words, moderator management)

## API Endpoints

### Authentication & Accounts
| Endpoint | Client | Method |
|---|---|---|
| `POST /lobby/user-login/authenticate` | `LobbyLoginClient` | `login` |
| `POST /lobby/user-login/create-account` | `LobbyLoginClient` | `createAccount` |
| `GET /user-account/fetch-email` | `UserAccountClient` | `fetchEmail` |
| `POST /user-account/change-email` | `UserAccountClient` | `changeEmail` |
| `POST /user-account/change-password` | `UserAccountClient` | `changePassword` |
| `POST /lobby/forgot-password` | `ForgotPasswordClient` | `sendForgotPasswordRequest` |

### Game Hosting & Listing
| Endpoint | Client | Method |
|---|---|---|
| `POST /lobby/game-hosting-request` | `GameHostingClient` | `sendGameHostingRequest` |
| `GET /lobby/games/fetch-games` | `GameListingClient` | `fetchGameListing` |
| `POST /lobby/games/boot-game` | `GameListingClient` | `bootGame` |
| `POST /lobby/games/post-game` | `LobbyWatcherClient` | `postGame` |
| `POST /lobby/games/update-game` | `LobbyWatcherClient` | `updateGame` |
| `POST /lobby/games/remove-game` | `LobbyWatcherClient` | `removeGame` |
| `POST /lobby/games/keep-alive` | `LobbyWatcherClient` | `sendKeepAlive` |
| `POST /lobby/games/player-joined` | `LobbyWatcherClient` | `playerJoined` |
| `POST /lobby/games/player-left` | `LobbyWatcherClient` | `playerLeft` |

### Moderation
| Endpoint | Client | Method |
|---|---|---|
| `POST /lobby/moderator/ban-player` | `ModeratorLobbyClient` | `banPlayer` |
| `POST /lobby/moderator/disconnect-player` | `ModeratorLobbyClient` | `disconnectPlayer` |
| `POST /lobby/moderator/mute-player` | `ModeratorLobbyClient` | `muteUser` |

### Moderator Toolbox
| Endpoint | Client | Method |
|---|---|---|
| `POST /lobby/moderator-toolbox/access-log` | `ToolboxAccessLogClient` | `getAccessLog` |
| `POST /lobby/moderator-toolbox/audit-history/lookup` | `ToolboxEventLogClient` | `lookupModeratorEvents` |
| `GET /lobby/moderator-toolbox/get-user-bans` | `ToolboxUserBanClient` | `getUserBans` |
| `POST /lobby/moderator-toolbox/ban-user` | `ToolboxUserBanClient` | `banUser` |
| `POST /lobby/moderator-toolbox/remove-user-ban` | `ToolboxUserBanClient` | `removeUserBan` |
| `GET /lobby/moderator-toolbox/get-username-bans` | `ToolboxUsernameBanClient` | `getUsernameBans` |
| `POST /lobby/moderator-toolbox/add-username-ban` | `ToolboxUsernameBanClient` | `addUsernameBan` |
| `POST /lobby/moderator-toolbox/remove-username-ban` | `ToolboxUsernameBanClient` | `removeUsernameBan` |
| `GET /lobby/moderator-toolbox/bad-words/get` | `ToolboxBadWordsClient` | `getBadWords` |
| `POST /lobby/moderator-toolbox/bad-words/add` | `ToolboxBadWordsClient` | `addBadWord` |
| `POST /lobby/moderator-toolbox/bad-words/remove` | `ToolboxBadWordsClient` | `removeBadWord` |
| `GET /lobby/moderator-toolbox/fetch-moderators` | `ToolboxModeratorManagementClient` | `fetchModeratorList` |
| `GET /lobby/moderator-toolbox/is-admin` | `ToolboxModeratorManagementClient` | `isCurrentUserAdmin` |
| `POST /lobby/moderator-toolbox/admin/add-super-mod` | `ToolboxModeratorManagementClient` | `addAdmin` |
| `POST /lobby/moderator-toolbox/admin/remove-mod` | `ToolboxModeratorManagementClient` | `removeMod` |
| `POST /lobby/moderator-toolbox/admin/add-moderator` | `ToolboxModeratorManagementClient` | `addModerator` |
| `POST /lobby/moderator-toolbox/does-user-exist` | `ToolboxModeratorManagementClient` | `checkUserExists` |

### Player & Remote Actions
| Endpoint | Client | Method |
|---|---|---|
| `POST /lobby/fetch-player-info` | `PlayerLobbyActionsClient` | `fetchPlayerInformation` |
| `POST /lobby/fetch-players-in-game` | `PlayerLobbyActionsClient` | `fetchPlayersInGame` |
| `POST /lobby/remote/actions/is-player-banned` | `RemoteActionsClient` | `checkIfPlayerIsBanned` |
| `POST /lobby/remote/actions/send-shutdown` | `RemoteActionsClient` | `sendShutdownRequest` |

### Maps
| Endpoint | Client | Method |
|---|---|---|
| `GET /support/maps/listing` | `MapsClient` | `fetchMapListing` |
| `GET /support/maps/list-tags` | `MapTagAdminClient` | `fetchAllowedMapTagValues` |
| `POST /support/maps/update-tag` | `MapTagAdminClient` | `updateMapTag` |

### Error Reporting & Support
| Endpoint | Client | Method |
|---|---|---|
| `POST /support/error-report` | `ErrorReportClient` | `uploadErrorReport` |
| `POST /support/error-report-check` | `ErrorReportClient` | `canUploadErrorReport` |
| `GET /support/latest-version` | `LatestVersionClient` | `fetchLatestVersion` |

## WebSocket Connections

### PlayerToLobbyConnection

Facade combining `HttpLobbyClient` + WebSocket on `WebsocketPaths.PLAYER_CONNECTIONS`. Provides:
- Chat operations (send messages, slap players, status updates)
- Game listing (fetch, boot)
- Moderation pass-through (ban, disconnect, mute)
- Account management (email fetch/change)
- Lifecycle listeners (`addConnectionTerminatedListener`, `addConnectionClosedListener`)

### GameToLobbyConnection

Facade combining `HttpLobbyClient` + `LobbyWatcherClient` + WebSocket on `WebsocketPaths.GAME_CONNECTIONS`. Provides:
- Game lifecycle (post, update, keep-alive, remove)
- Player notifications (joined, left) — sent asynchronously via `AsyncRunner`
- Ban checking (`isPlayerBanned`)
- Lifecycle listeners

### WebSocket Message Types

**Chat**: `ChatSentMessage`, `ChatReceivedMessage` (max 240 chars), `ChatEventReceivedMessage`, `ConnectToChatMessage`, `PlayerSlapSentMessage`/`ReceivedMessage`, `PlayerStatusUpdateSentMessage`/`ReceivedMessage`, `PlayerJoinedMessage`, `PlayerLeftMessage`, `ChatterListingMessage`, `PlayerListingMessage`

**Game Listing**: `LobbyGameUpdatedMessage` (upsert), `LobbyGameRemovedMessage`

**Remote Actions**: `ShutdownServerMessage`, `PlayerBannedMessage`

## Tests

Tests use JUnit 5 with Hamcrest matchers. Key test classes:
- `BanDurationFormatterTest` — parameterized tests for duration formatting
- `ErrorReportRequestTest` — version string parsing regex validation
- `ChatReceivedMessageTest` — message truncation at 240-char boundary

## Dependencies

| Dependency | Purpose |
|---|---|
| `game-app:domain-data` | Value objects: `ApiKey`, `UserName`, `PlayerChatId`, `LobbyGame` |
| `lib:feign-common` | `HttpClient.newClient()` factory for Feign proxies |
| `lib:java-extras` | `IpAddressParser`, `StringUtils`, `AsyncRunner` |
| `lib:websocket-client` | `GenericWebSocketClient`, message envelope infrastructure |
| `lib:test-common` | Test utilities (test-only) |

## Build

```shell
./gradlew :http-clients:lobby-client:build
./gradlew :http-clients:lobby-client:test
```

Published artifact version is controlled by `JAR_VERSION` environment variable.
