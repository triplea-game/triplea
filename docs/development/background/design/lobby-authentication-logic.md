### Authentication
- done over https
- client will hash their password, send to lobby
- lobby will bcrypt the password, compare to value stored in database
- if valid, lobby generates an Api-key and a player-chat-id and stores in `lobby_api_key`

### Api-key
- after authentication, client will send api-key to server, this allows further interactions
  with the lobby server
- server will check `lobby_api_key` table that the key exists for that user

### Player-Chat-Id
- this is a publicly known key
- this value is attached to chat participants, when connecting to chat, the new chat participant
  will receive a list of all chatter names and their chat id.
- chat-id is not used for authentication, it is in a sense "one-way", the server will inform each
  chat participant individually of the other chatter-ids.
- Because the server controls chat-id, it cannot be spoofed by client. If you join as "Chatter A",
  the server will assign a chat-id to you say "id-A". Any other user joining, will be informed
  that "Chatter A" with "id-a" as a participant. Because the server controls the mapping of "chat-id"
  to chat participants, it can be used to identify chat users.

### Overall Flow

The below diagram describes the login process and the identifiers and keys that are created:

![api-key-and-chat-id-flow](https://user-images.githubusercontent.com/12397753/68798585-868cc200-060b-11ea-8568-e555c5e69589.png)
