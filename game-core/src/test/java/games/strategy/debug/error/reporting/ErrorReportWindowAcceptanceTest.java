package games.strategy.debug.error.reporting;

import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.awt.GraphicsEnvironment;
import java.util.function.BiConsumer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.test.common.swing.SwingComponentWrapper;

import swinglib.DialogBuilder;


/**
 * Set up a window, enter in data for the user input text fields, click the submit button and verify
 * we have that data sent to the {@code reportHandler}.
 */
@ExtendWith(MockitoExtension.class)
class ErrorReportWindowAcceptanceTest {

  private static final String TITLE = "addis informacio";

  private static final String DESCRIPTION = "errus descriptus";

  @BeforeAll
  static void disableSwingPopups() {
    DialogBuilder.suppressDialog();
  }

  @BeforeAll
  static void skipIfHeadless() {
    assumeFalse(GraphicsEnvironment.isHeadless());
  }

  @Mock
  private BiConsumer<JFrame, UserErrorReport> reportHandler;

  @Test
  void fillInErrorDetailsAndSendThem() {
    final JFrame frame = new ErrorReportWindow(reportHandler).buildWindow();

    SwingComponentWrapper.of(frame)
        .findChildByName(ErrorReportComponents.Names.TITLE.toString(), JTextField.class)
        .setText(TITLE);

    SwingComponentWrapper.of(frame)
        .findChildByName(ErrorReportComponents.Names.DESCRIPTION.toString(), JTextArea.class)
        .setText(DESCRIPTION);

    SwingComponentWrapper.of(frame)
        .findChildByName(ErrorReportComponents.Names.UPLOAD_BUTTON.toString(), JButton.class)
        .doClick();

    verify(reportHandler, times(1))
        .accept(any(), eq(UserErrorReport.builder()
            .title(TITLE)
            .description(DESCRIPTION)
            .build()));
  }
}
