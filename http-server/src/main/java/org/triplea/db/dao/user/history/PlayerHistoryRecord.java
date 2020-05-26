package org.triplea.db.dao.user.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;
import org.triplea.db.TimestampMapper;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlayerHistoryRecord {

  private long registrationDate;

  public static RowMapper<PlayerHistoryRecord> buildResultMapper() {
    return (rs, ctx) ->
        PlayerHistoryRecord.builder()
            .registrationDate(TimestampMapper.map(rs, "date_registered").toEpochMilli())
            .build();
  }
}
