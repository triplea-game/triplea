package games.strategy.triplea.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;

public enum TestMapGameData {
  BIG_WORLD_1942("big_world_1942_test.xml"),
  IRON_BLITZ("iron_blitz_test.xml"),
  LHTR("lhtr_test.xml"),
  PACIFIC_INCOMPLETE("pacific_incomplete_test.xml"),
  PACT_OF_STEEL_2("pact_of_steel_2_test.xml"),
  REVISED("revised_test.xml"),
  VICTORY_TEST("victory_test.xml"),
  WW2V3_1941("ww2v3_1941_test.xml"),
  WW2V3_1942("ww2v3_1942_test.xml"),
  GLOBAL1940("ww2_g40_balanced.xml"),
  TEST("Test.xml"),
  DELEGATE_TEST("DelegateTest.xml"),
  GAME_EXAMPLE("GameExample.xml");

  private static final String TEST_MAP_XML_PATH = "src/test/resources/";

  private final String value;

  TestMapGameData(final String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  private InputStream getInputStream() throws IOException {
    File f = new File(TEST_MAP_XML_PATH + value);
    System.out.println("f .... => " + f.getAbsolutePath());
    return new FileInputStream(f);
  }

  public GameData getGameData() throws Exception {
    return (new GameParser("game name")).parse(getInputStream(), new AtomicReference<>(), false);
  }
}
