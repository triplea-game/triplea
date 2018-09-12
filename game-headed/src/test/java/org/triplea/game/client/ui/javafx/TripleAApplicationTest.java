package org.triplea.game.client.ui.javafx;

import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.hasChildren;
import static org.testfx.matcher.base.NodeMatchers.isInvisible;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

import org.junit.jupiter.api.Test;
import org.sonatype.goodies.prefs.memory.MemoryPreferences;
import org.testfx.framework.junit5.ApplicationTest;
import org.triplea.test.common.Integration;

import games.strategy.triplea.settings.ClientSetting;
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
  public void start(final Stage stage) throws Exception {
    new TripleA().start(stage);
  }

  @Test
  public void testDisplay() {
    verifyThat("#mainOptions", isVisible());
    verifyThat("#gameOptions", isInvisible());
    verifyThat("#aboutSection", isInvisible());
    verifyThat("#mainOptions", hasChildren(5, ".button:visible"));
  }
}
