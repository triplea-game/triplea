package org.triplea.modules.moderation.audit.history;

import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.triplea.db.dao.ModeratorAuditHistoryDao;
import org.triplea.http.client.lobby.moderator.toolbox.log.ModeratorEvent;

@AllArgsConstructor
class ModeratorAuditHistoryService {
  private final ModeratorAuditHistoryDao moderatorAuditHistoryDao;

  List<ModeratorEvent> lookupHistory(final int rowNumber, final int rowCount) {
    return moderatorAuditHistoryDao.lookupHistoryItems(rowNumber, rowCount).stream()
        .map(
            item ->
                ModeratorEvent.builder()
                    .date(item.getDateCreated())
                    .moderatorName(item.getUsername())
                    .moderatorAction(item.getActionName())
                    .actionTarget(item.getActionTarget())
                    .build())
        .collect(Collectors.toList());
  }
}
