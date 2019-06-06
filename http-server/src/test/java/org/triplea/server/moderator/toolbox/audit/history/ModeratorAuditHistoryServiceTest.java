package org.triplea.server.moderator.toolbox.audit.history;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.moderator.toolbox.ModeratorEvent;
import org.triplea.lobby.server.db.ModeratorAuditHistoryDao;
import org.triplea.lobby.server.db.ModeratorAuditHistoryItem;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
class ModeratorAuditHistoryServiceTest {

  private static final int ROW_OFFSET = 30;
  private static final int ROW_COUNT = 70;

  private static final ModeratorAuditHistoryItem ITEM_1 = ModeratorAuditHistoryItem.builder()
      .actionName("Jolly courages lead to love.")
      .actionTarget("All peglegs hoist evil, small rums.")
      .dateCreated(Instant.now())
      .username("Ahoy, yer not lootting me without a yellow fever!")
      .build();

  private static final ModeratorAuditHistoryItem ITEM_2 = ModeratorAuditHistoryItem.builder()
      .actionName("Ahoy, endure me kraken, ye old wind!")
      .actionTarget("All gulls love salty, swashbuckling pins.")
      .dateCreated(Instant.now().minusSeconds(5000))
      .username("Waves travel with urchin at the sunny singapore!")
      .build();

  @Mock
  private ModeratorAuditHistoryDao moderatorAuditHistoryDao;

  @InjectMocks
  private ModeratorAuditHistoryService moderatorAuditHistoryService;


  @Test
  void lookupHistory() {
    when(moderatorAuditHistoryDao.lookupHistoryItems(ROW_OFFSET, ROW_COUNT))
        .thenReturn(ImmutableList.of(ITEM_1, ITEM_2));

    final List<ModeratorEvent> results = moderatorAuditHistoryService.lookupHistory(ROW_OFFSET, ROW_COUNT);

    assertThat(results, hasSize(2));
    assertThat(results.get(0).getDate(), is(ITEM_1.getDateCreated()));
    assertThat(results.get(0).getActionTarget(), is(ITEM_1.getActionTarget()));
    assertThat(results.get(0).getModeratorAction(), is(ITEM_1.getActionName()));
    assertThat(results.get(0).getModeratorName(), is(ITEM_1.getUsername()));

    assertThat(results.get(1).getDate(), is(ITEM_2.getDateCreated()));
    assertThat(results.get(1).getActionTarget(), is(ITEM_2.getActionTarget()));
    assertThat(results.get(1).getModeratorAction(), is(ITEM_2.getActionName()));
    assertThat(results.get(1).getModeratorName(), is(ITEM_2.getUsername()));
  }
}
