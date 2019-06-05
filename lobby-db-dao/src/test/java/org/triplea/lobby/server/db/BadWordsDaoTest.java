package org.triplea.lobby.server.db;

import static org.hamcrest.core.Is.is;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.test.common.Integration;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;

@ExtendWith(DBUnitExtension.class)
@Integration
class BadWordsDaoTest {
  private static final BadWordsDao BAD_WORDS_DAO =
      JdbiDatabase.newConnection().onDemand(BadWordsDao.class);


  private static final List<String> expectedBadWords = Arrays.asList(
      "aaa", "one", "two", "zzz");

  @Test
  @DataSet("bad_words/select.yml")
  void getBadWords() {
    MatcherAssert.assertThat(
        BAD_WORDS_DAO.getBadWords(),
        is(expectedBadWords));
  }

  @Test
  @DataSet("bad_words/pre-insert.yml")
  @ExpectedDataSet(value = "bad_words/post-insert.yml", orderBy = {"word"})
  void addBadWord() {
    MatcherAssert.assertThat(
        BAD_WORDS_DAO.addBadWord("bad-word"),
        is(1));
  }

  @Test
  @DataSet("bad_words/pre-remove.yml")
  @ExpectedDataSet("bad_words/post-remove.yml")
  void removeBadWord() {
    MatcherAssert.assertThat(
        BAD_WORDS_DAO.removeBadWord("two"),
        is(1));

    MatcherAssert.assertThat(
        BAD_WORDS_DAO.removeBadWord("not-present"),
        is(0));
  }
}
