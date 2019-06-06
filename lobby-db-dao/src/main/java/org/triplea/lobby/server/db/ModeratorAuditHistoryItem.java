package org.triplea.lobby.server.db;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ModeratorAuditHistoryItem {
  private Instant dateCreated;
  private String username;
  private String actionName;
  private String actionTarget;
}
