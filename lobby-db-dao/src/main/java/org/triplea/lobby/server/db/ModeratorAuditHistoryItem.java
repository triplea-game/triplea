package org.triplea.lobby.server.db;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class ModeratorAuditHistoryItem {
  private Instant dateCreated;
  private String username;
  private String actionName;
  private String actionTarget;
}
