package org.triplea.modules.moderation.access.log;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.access.log.AccessLogDao;
import org.triplea.http.client.lobby.moderator.toolbox.log.AccessLogData;
import org.triplea.http.client.lobby.moderator.toolbox.log.AccessLogRequest;

@Builder
class AccessLogService {
  @Nonnull private final AccessLogDao accessLogDao;

  public static AccessLogService build(final Jdbi jdbi) {
    return AccessLogService.builder() //
        .accessLogDao(jdbi.onDemand(AccessLogDao.class))
        .build();
  }

  List<AccessLogData> fetchAccessLog(final AccessLogRequest accessLogRequest) {
    return accessLogDao
        .fetchAccessLogRows(
            accessLogRequest.getPagingParams().getRowNumber(),
            accessLogRequest.getPagingParams().getPageSize(),
            accessLogRequest.getAccessLogSearchRequest().getUsername(),
            accessLogRequest.getAccessLogSearchRequest().getIp(),
            accessLogRequest.getAccessLogSearchRequest().getSystemId())
        .stream()
        .map(
            daoData ->
                AccessLogData.builder()
                    .accessDate(daoData.getAccessTime())
                    .username(daoData.getUsername())
                    .ip(daoData.getIp())
                    .systemId(daoData.getSystemId())
                    .registered(daoData.isRegistered())
                    .build())
        .collect(Collectors.toList());
  }
}
