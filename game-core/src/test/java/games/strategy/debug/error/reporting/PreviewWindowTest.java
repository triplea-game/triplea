package games.strategy.debug.error.reporting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;

import javax.swing.JFrame;
import javax.swing.JTextArea;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.test.common.swing.SwingComponentWrapper;

@ExtendWith(MockitoExtension.class)
class PreviewWindowTest {

  private static final String ADDITIONAL_INFO = "Lixas velum in antenna!";
  private static final String DESCRIPTION = "Barbatus mons superbe talems gluten est.";

  private UserErrorReport userErrorReport = UserErrorReport.builder()
      .additionalInfo(ADDITIONAL_INFO)
      .description(DESCRIPTION)
      .build();

  /**
   * Just check that we have rendered at least the user supplied info to the window in a text
   * area, and that the text area is read-only.
   */
  @Test
  void previewWindowContainsReportingData() {
    final PreviewWindow window = new PreviewWindow();
    final JFrame frame = window.build(null, userErrorReport);

    final JTextArea textArea = SwingComponentWrapper.of(frame)
        .findChildByName(PreviewWindow.ComponentNames.PREVIEW_AREA.toString(), JTextArea.class);

    assertThat(textArea.isEditable(), is(false));
    assertThat(textArea.getText(), containsString(DESCRIPTION));
    assertThat(textArea.getText(), containsString(ADDITIONAL_INFO));
  }
}