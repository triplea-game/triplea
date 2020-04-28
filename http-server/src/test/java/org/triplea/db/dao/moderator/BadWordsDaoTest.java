package org.triplea.db.dao.moderator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.triplea.db.dao.DaoTest;

@DataSet(cleanBefore = true, value = "bad_words/select.yml")
class BadWordsDaoTest extends DaoTest {
  private static final List<String> expectedBadWords = List.of("aaa", "one", "two", "zzz");

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

  @SuppressWarnings("unused")
  private static List<String> badWordContains() {
    final List<String> badWords = new ArrayList<>(List.of("zzZz", "_two_"));
    badWords.addAll(expectedBadWords);
    return badWords;
  }

  @ParameterizedTest
  @MethodSource
  void badWordContains(final String badWord) {
    assertThat(badWordsDao.containsBadWord(badWord), is(true));
  }

  @ParameterizedTest
  @ValueSource(strings = {"zz", "", "some string not containing any bad words"})
  void notBadWordContains(final String notInBadWords) {
    assertThat(badWordsDao.containsBadWord(notInBadWords), is(false));
  }
}
