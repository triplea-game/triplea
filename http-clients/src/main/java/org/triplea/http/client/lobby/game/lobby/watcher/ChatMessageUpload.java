package org.triplea.http.client.lobby.game.lobby.watcher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Data transfer object between client and server representing a single chat message upload. */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class ChatMessageUpload {
  private String fromPlayer;
  private String chatMessage;
  private String gameId;
  private String apiKey;
}
