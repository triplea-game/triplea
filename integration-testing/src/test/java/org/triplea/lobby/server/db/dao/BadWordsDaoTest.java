package org.triplea.lobby.server.db.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

@DataSet(cleanBefore = true, value = "bad_words/select.yml")
class BadWordsDaoTest extends DaoTest {
  private static final List<String> expectedBadWords = Arrays.asList("aaa", "one", "two", "zzz");

  private final BadWordsDao badWordsDao = DaoTest.newDao(BadWordsDao.class);

  @Test
  void getBadWords() {
    assertThat(badWordsDao.getBadWords(), is(expectedBadWords));
  }

  @Test
  @ExpectedDataSet(
      value = "bad_words/select_post_insert.yml",
      orderBy = {"word"})
  void addBadWord() {
    assertThat(badWordsDao.addBadWord("new-bad-word"), is(1));
  }

  @Test
  @ExpectedDataSet("bad_words/select_post_remove.yml")
  void removeBadWord() {
    assertThat(badWordsDao.removeBadWord("not-present"), is(0));

    expectedBadWords.forEach(badWord -> assertThat(badWordsDao.removeBadWord(badWord), is(1)));
  }
}
