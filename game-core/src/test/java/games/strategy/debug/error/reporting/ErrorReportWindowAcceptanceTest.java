package games.strategy.debug.error.reporting;

import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.awt.GraphicsEnvironment;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.test.common.swing.SwingComponentWrapper;

import swinglib.ConfirmationDialogBuilder;


/**
 * Set up a window, enter in data for the user input text fields, click the submit button and verify
 * we have that data sent to the {@code reportHandler}.
 */
@ExtendWith(MockitoExtension.class)
class ErrorReportWindowAcceptanceTest {

  private static final String ADDITIONAL_INFO = "addis informacio";

  private static final String ERROR_DESCRIPTION = "errus descriptus";

  @BeforeAll
  static void disableSwingPopups() {
    ConfirmationDialogBuilder.suppressDialog();
  }

  @BeforeAll
  static void skipIfHeadless() {
    assumeFalse(GraphicsEnvironment.isHeadless());
  }
  
  @Mock
  private Consumer<UserErrorReport> reportHandler;

  @Test
  void fillInErrorDetailsAndSendThem() {
    final JFrame frame = new ErrorReportWindow(reportHandler).buildWindow();

    SwingComponentWrapper.of(frame)
        .findChildByName(ErrorReportComponents.Names.ERROR_DESCRIPTION.toString(), JTextArea.class)
        .setText(ERROR_DESCRIPTION);

    SwingComponentWrapper.of(frame)
        .findChildByName(ErrorReportComponents.Names.ADDITIONAL_INFO_NAME.toString(), JTextArea.class)
        .setText(ADDITIONAL_INFO);

    SwingComponentWrapper.of(frame)
        .findChildByName(ErrorReportComponents.Names.UPLOAD_BUTTON.toString(), JButton.class)
        .doClick();

    verify(reportHandler, times(1))
        .accept(UserErrorReport.builder()
            .description(ERROR_DESCRIPTION)
            .additionalInfo(ADDITIONAL_INFO)
            .build());
  }
}
