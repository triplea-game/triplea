# domain-data

Shared domain value objects used across client and server. Published as a Maven artifact for consumption by the lobby server (which lives in a separate repository).

## Classes

All classes are in `org.triplea.domain.data`:

| Class | Purpose |
|-------|---------|
| `UserName` | Player display name (3-40 chars, alphanumeric + hyphens/underscores, starts with letter) |
| `PlayerChatId` | UUID-based session identifier for chat |
| `ApiKey` | OAuth authorization token (UUID-based, max 36 chars) |
| `SystemId` | System-level player identifier, persisted locally via Java Preferences |
| `ChatParticipant` | User in a chat channel (username, chat ID, moderator flag, status) — `Serializable` |
| `LobbyGame` | Game state in the lobby (host, map name, player count, round, status) |
| `LobbyConstants` | Validation constants (username length, email length, password length) |
| `SystemIdLoader` | Loads/generates `SystemId` from Java Preferences |
| `PlayerEmailValidation` | RFC-compliant email validation |

## Dependencies

No internal module dependencies — this is a leaf module. Uses only Lombok and standard library.

## Publishing

Published to GitHub Packages as a Maven artifact (version from `JAR_VERSION` env var).
