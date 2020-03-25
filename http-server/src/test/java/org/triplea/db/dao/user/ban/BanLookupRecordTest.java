package org.triplea.db.dao.user.ban;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.triplea.db.dao.user.ban.BanTableColumns.BAN_EXPIRY_COLUMN;
import static org.triplea.db.dao.user.ban.BanTableColumns.PUBLIC_ID_COLUMN;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Calendar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BanLookupRecordTest {
  private static final Instant NOW = Instant.now();
  private static final Timestamp timestamp = Timestamp.from(NOW);
  private static final String PUBLIC_ID = "public-id";

  @Mock private ResultSet resultSet;

  @Test
  void buildResultMapper() throws Exception {
    when(resultSet.getTimestamp(eq(BAN_EXPIRY_COLUMN), any(Calendar.class))).thenReturn(timestamp);
    when(resultSet.getString(PUBLIC_ID_COLUMN)).thenReturn(PUBLIC_ID);

    final BanLookupRecord result = BanLookupRecord.buildResultMapper().map(resultSet, null);

    assertThat(result.getBanExpiry(), is(NOW));
    assertThat(result.getPublicBanId(), is(PUBLIC_ID));
  }
}
