package org.triplea.game.client.ui.javafx;

import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.hasChildren;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sonatype.goodies.prefs.memory.MemoryPreferences;
import org.testfx.framework.junit5.ApplicationTest;
import org.triplea.test.common.Integration;

import games.strategy.triplea.settings.ClientSetting;
import javafx.scene.Parent;
import javafx.stage.Stage;

@Integration
final class TripleAApplicationTest extends ApplicationTest {
  static {
    System.setProperty("testfx.robot", "glass");
    System.setProperty("testfx.headless", String.valueOf(true));
    System.setProperty("prism.order", "sw");
    System.setProperty("prism.text", "t2k");
    System.setProperty("testfx.setup.timeout", String.valueOf(30_000));
  }

  @Override
  public void init() {
    ClientSetting.setPreferences(new MemoryPreferences());
  }

  @Override
  public void start(final Stage stage) {
    new TripleA().start(stage);
  }

  @Nested
  final class DisplayTest {
    @Test
    void shouldInitiallyShowCertainElements() {
      verifyThat("#mainOptions", isVisible());
    }

    @Test
    void shouldInitiallyHideCertainElements() {
      verifyThat("#root > StackPane", node -> ((Parent) node).getChildrenUnmodifiable().size() == 1);
    }

    @Test
    void shouldShowFiveButtonsInMainOptions() {
      verifyThat("#mainOptions", hasChildren(5, ".button:visible"));
    }
  }
}
