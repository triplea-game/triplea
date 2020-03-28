package org.triplea.modules.moderation.audit.history;

import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.ModeratorAuditHistoryDao;
import org.triplea.http.client.lobby.moderator.toolbox.log.ModeratorEvent;

@AllArgsConstructor
class ModeratorAuditHistoryService {
  private final ModeratorAuditHistoryDao moderatorAuditHistoryDao;

  public static ModeratorAuditHistoryService build(final Jdbi jdbi) {
    return new ModeratorAuditHistoryService(jdbi.onDemand(ModeratorAuditHistoryDao.class));
  }

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
