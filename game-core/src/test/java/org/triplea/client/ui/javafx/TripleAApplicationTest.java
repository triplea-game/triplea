package org.triplea.client.ui.javafx;

import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.hasChildren;
import static org.testfx.matcher.base.NodeMatchers.isInvisible;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sonatype.goodies.prefs.memory.MemoryPreferences;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;

import games.strategy.triplea.settings.ClientSetting;

@ExtendWith(ApplicationExtension.class)
public class TripleAApplicationTest {

  static {
    System.setProperty("testfx.robot", "glass");
    System.setProperty("testfx.headless", String.valueOf(true));
    System.setProperty("prism.order", "sw");
    System.setProperty("prism.text", "t2k");
    System.setProperty("testfx.setup.timeout", String.valueOf(30_000));
  }


  @BeforeAll
  public static void setup() throws Exception {
    ClientSetting.setPreferences(new MemoryPreferences());
    FxToolkit.registerPrimaryStage();
    FxToolkit.setupApplication(TripleA.class);
  }

  @Test
  public void testDisplay() {
    verifyThat("#mainOptions", isVisible());
    verifyThat("#gameOptions", isInvisible());
    verifyThat("#aboutSection", isInvisible());
    verifyThat("#mainOptions", hasChildren(5, ".button:visible"));
  }
}
