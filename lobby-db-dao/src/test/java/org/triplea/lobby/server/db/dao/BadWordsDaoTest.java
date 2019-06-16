package org.triplea.lobby.server.db.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.lobby.server.db.JdbiDatabase;
import org.triplea.test.common.Integration;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;

@ExtendWith(DBUnitExtension.class)
@Integration
@DataSet("bad_words/select.yml")
class BadWordsDaoTest {
  private static final BadWordsDao badWordsDao =
      JdbiDatabase.newConnection().onDemand(BadWordsDao.class);

  private static final List<String> expectedBadWords = Arrays.asList(
      "aaa", "one", "two", "zzz");

  @Test
  void getBadWords() {
    assertThat(
        badWordsDao.getBadWords(),
        is(expectedBadWords));
  }

  @Test
  @ExpectedDataSet(value = "bad_words/select_post_insert.yml", orderBy = {"word"})
  void addBadWord() {
    assertThat(
        badWordsDao.addBadWord("new-bad-word"),
        is(1));
  }

  @Test
  @ExpectedDataSet("bad_words/select_post_remove.yml")
  void removeBadWord() {
    assertThat(badWordsDao.removeBadWord("not-present"), is(0));

    expectedBadWords.forEach(badWord -> assertThat(badWordsDao.removeBadWord(badWord), is(1)));
  }
}
