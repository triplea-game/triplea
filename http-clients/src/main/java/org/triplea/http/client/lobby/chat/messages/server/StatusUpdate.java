package org.triplea.http.client.lobby.chat.messages.server;

import lombok.Value;
import org.triplea.domain.data.UserName;

@Value
public class StatusUpdate {
  private final UserName userName;
  private final String status;
}
