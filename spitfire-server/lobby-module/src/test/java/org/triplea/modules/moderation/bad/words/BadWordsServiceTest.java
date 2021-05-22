package org.triplea.modules.moderation.bad.words;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.moderator.BadWordsDao;
import org.triplea.db.dao.moderator.ModeratorAuditHistoryDao;

@ExtendWith(MockitoExtension.class)
class BadWordsServiceTest {

  private static final String TEST_VALUE = "some-value";
  private static final ImmutableList<String> BAD_WORDS_SAMPLE =
      ImmutableList.of("one", "two", "three");

  private static final int MODERATOR_ID = 100;

  @Mock private BadWordsDao badWordsDao;
  @Mock private ModeratorAuditHistoryDao moderatorAuditHistoryDao;

  @InjectMocks private BadWordsService badWordsService;

  @Test
  void removeBadWordSuccessCase() {
    when(badWordsDao.removeBadWord(TEST_VALUE)).thenReturn(1);

    assertThat(badWordsService.removeBadWord(MODERATOR_ID, TEST_VALUE), is(true));

    verify(moderatorAuditHistoryDao)
        .addAuditRecord(
            ModeratorAuditHistoryDao.AuditArgs.builder()
                .moderatorUserId(MODERATOR_ID)
                .actionName(ModeratorAuditHistoryDao.AuditAction.REMOVE_BAD_WORD)
                .actionTarget(TEST_VALUE)
                .build());
  }

  @Test
  void removeBadWordFailureCase() {
    when(badWordsDao.removeBadWord(TEST_VALUE)).thenReturn(0);

    assertThat(badWordsService.removeBadWord(MODERATOR_ID, TEST_VALUE), is(false));

    verify(moderatorAuditHistoryDao, never()).addAuditRecord(any());
  }

  @Test
  void addBadWordSuccessCase() {
    when(badWordsDao.addBadWord(TEST_VALUE)).thenReturn(1);

    assertThat(badWordsService.addBadWord(MODERATOR_ID, TEST_VALUE), is(true));

    verify(moderatorAuditHistoryDao)
        .addAuditRecord(
            ModeratorAuditHistoryDao.AuditArgs.builder()
                .moderatorUserId(MODERATOR_ID)
                .actionName(ModeratorAuditHistoryDao.AuditAction.ADD_BAD_WORD)
                .actionTarget(TEST_VALUE)
                .build());
  }

  @Test
  void addBadWordFailureCase() {
    when(badWordsDao.addBadWord(TEST_VALUE)).thenReturn(0);

    assertThat(badWordsService.addBadWord(MODERATOR_ID, TEST_VALUE), is(false));
    verify(moderatorAuditHistoryDao, never()).addAuditRecord(any());
  }

  @Test
  void getBadWords() {
    when(badWordsDao.getBadWords()).thenReturn(BAD_WORDS_SAMPLE);

    assertThat(badWordsService.getBadWords(), is(BAD_WORDS_SAMPLE));
  }
}
