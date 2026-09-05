package games.strategy.engine.posted.game.pbf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.triplea.test.common.TestDataFileReader;

class NodeBbForumPosterTest {

  /** sample response from https://forums.triplea-game.org/ */
  @Test
  void uploadResponseParsing_tripleaForums() {
    String sampleResponse =
        TestDataFileReader.readContents("forums/upload_response_triplea_forums.json");
    String saveGameUrl = NodeBbForumPoster.parseSaveGameUrlFromJsonResponse(sampleResponse);
    assertThat(saveGameUrl).isEqualTo("/assets/uploads/files/1665366598238-triplea_3321_1car.tsvg");
  }

  /** sample response from https://www.axisandallies.org/ */
  @Test
  void uploadResponseParsing_axisAndAlliesOrg() {
    String sampleResponse =
        TestDataFileReader.readContents("forums/upload_response_axis_and_allies_org.json");
    String saveGameUrl = NodeBbForumPoster.parseSaveGameUrlFromJsonResponse(sampleResponse);
    assertThat(saveGameUrl)
        .isEqualTo("/forums/assets/uploads/files/1665368873282-triplea_20781_1car.tsvg");
  }
}
