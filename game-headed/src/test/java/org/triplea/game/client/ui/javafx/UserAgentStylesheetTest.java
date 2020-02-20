package org.triplea.game.client.ui.javafx;

import java.lang.reflect.Field;
import javafx.application.Application;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class UserAgentStylesheetTest {

  @BeforeEach
  void setup() throws Exception {
    setUserAgentStylesheet("");
  }

  /**
   * This is essentially just a hack to avoid {@code java.lang.IllegalStateException: Toolkit not
   * initialized} which occurs when mocking any subclass of {@link javafx.scene.control.Control}
   * because of its static initializer. We could alternatively setup a "real" environment, but as
   * long as this works there isn't really a need for it.
   */
  private void setUserAgentStylesheet(final @Nullable String stylesheet) throws Exception {
    final Field userAgentStyleSheetField =
        Application.class.getDeclaredField("userAgentStylesheet");
    userAgentStyleSheetField.setAccessible(true);
    userAgentStyleSheetField.set(null, stylesheet);
  }

  @AfterEach
  void cleanup() throws Exception {
    setUserAgentStylesheet(null);
  }
}
