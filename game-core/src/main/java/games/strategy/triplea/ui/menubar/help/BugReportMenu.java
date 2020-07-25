package games.strategy.triplea.ui.menubar.help;

import games.strategy.triplea.UrlConstants;
import javax.swing.Action;
import lombok.experimental.UtilityClass;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;

@UtilityClass
class BugReportMenu {
  Action buildMenu() {
    return SwingAction.of(
        "Send Bug Report",
        e -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.GITHUB_ISSUES));
  }
}
