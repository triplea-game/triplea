package games.strategy.engine.lobby.moderator.toolbox.tabs.bad.words;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.lobby.moderator.toolbox.tabs.ToolboxTabModelTestUtil;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.lobby.moderator.toolbox.words.ToolboxBadWordsClient;

@ExtendWith(MockitoExtension.class)
class BadWordsTabModelTest {

  private static final String BAD_WORD = "Die loudly like a rough kraken.";
  private static final List<String> badWords =
      List.of(
          "O, gar.",
          "How dead. You rob like a sail.",
          "When the freebooter stutters for jamaica, all waves view warm, mighty swabbies.");

  @Mock private ToolboxBadWordsClient toolboxBadWordsClient;

  @InjectMocks private BadWordsTabModel badWordsTabModel;

  @Test
  void fetchData() {
    when(toolboxBadWordsClient.getBadWords()).thenReturn(badWords);

    final List<List<String>> tableData = badWordsTabModel.fetchTableData();

    ToolboxTabModelTestUtil.verifyTableDimensions(tableData, BadWordsTabModel.fetchTableHeaders());

    for (int i = 0; i < badWords.size(); i++) {
      ToolboxTabModelTestUtil.verifyTableDataAtRow(
          tableData, i, badWords.get(i), BadWordsTabActions.REMOVE_BUTTON_TEXT);
    }
  }

  @Test
  void removeBadWord() {
    badWordsTabModel.removeBadWord(BAD_WORD);

    verify(toolboxBadWordsClient).removeBadWord(BAD_WORD);
  }

  @Test
  void addBadWord() {
    badWordsTabModel.addBadWord(BAD_WORD);

    verify(toolboxBadWordsClient).addBadWord(BAD_WORD);
  }
}
