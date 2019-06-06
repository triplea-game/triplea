package org.triplea.server.moderator.toolbox.audit.history;

import java.util.List;
import java.util.stream.Collectors;

import org.triplea.http.client.moderator.toolbox.ModeratorEvent;
import org.triplea.lobby.server.db.ModeratorAuditHistoryDao;

import lombok.AllArgsConstructor;

@AllArgsConstructor
// TODO: test-me
class ModeratorAuditHistoryService {
  private final ModeratorAuditHistoryDao moderatorAuditHistoryDao;

  List<ModeratorEvent> lookupHistory(final int rowNumber, final int rowCount) {
    return moderatorAuditHistoryDao.lookupHistoryItems(rowNumber, rowCount)
        .stream()
        .map(item -> ModeratorEvent.builder()
            .date(item.getDateCreated())
            .moderatorName(item.getUsername())
            .moderatorAction(item.getActionName())
            .actionTarget(item.getActionTarget())
            .build())
        .collect(Collectors.toList());
  }
}
