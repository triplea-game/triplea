package games.strategy.triplea.ui;

import static org.hamcrest.MatcherAssert.assertThat;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.triplea.EngineImageLoader;
import org.junit.jupiter.api.Test;
import util.UiTest;

class UnitChooserTest {
    @Test
    void getUiContextDecoration() {
      getUiContextDecoration("big_world\\map\\games\\Big_World_1942_v3rules.xml");
      getUiContextDecoration("270bc_wars\\map\\games\\270BC_Wars.xml");
      getUiContextDecoration("conquest_of_the_world\\map\\games\\conquest_world.xml");
    }

  private void getUiContextDecoration(final String gameFile) {
    final var gsm = new GameSelectorModel();
    UiTest.setup(new String[0]);
    final var userMapsFolder = ClientFileSystemHelper.getUserMapsFolder();
    gsm.load(userMapsFolder.resolve(gameFile));
    final var gameData = gsm.getGameData();

    final var uiContext = new UiContext();
    uiContext.setDefaultMapDir(gameData);
    uiContext.getMapData().verify(gameData);

    final var uiContextDecoration = UnitChooser.UiContextDecoration.get(uiContext);

    final int smallHeight =
        EngineImageLoader.loadImage("units","generic", "non-withdrawable_small.png")
        .getHeight(null);

    final int largeHeight =
        EngineImageLoader.loadImage("units","generic", "non-withdrawable.png")
        .getHeight(null);

    final int unitImageHeight = uiContextDecoration.unitImageFactory.getUnitImageHeight();
    final double factorNonWithdrawableImage =
        UnitChooser.UiContextDecoration.getNonWithdrawableImageHeight(1);

    final String requirementMinimumUnitImageHeight = String.format(
    "Height of unit images in UnitChooser is at least %f * height of small non-whithdrawable image",
        1.0 / factorNonWithdrawableImage);

    assertThat(requirementMinimumUnitImageHeight,
        unitImageHeight >= (double) smallHeight / factorNonWithdrawableImage);

    final String requirementMaximumUnitImageHeight = String.format(
        "Height of unit images in UnitChooser is at most %f * height of non-whithdrawable image",
        1.0 / factorNonWithdrawableImage);

    assertThat(requirementMaximumUnitImageHeight,
        unitImageHeight <= (double) largeHeight / factorNonWithdrawableImage);

    final String requirementProportion = String.format(
        "In UnitChooser height of non withdrawable image is %d %% of height of unit image",
        (int) (factorNonWithdrawableImage * 100)
    );

    assertThat(requirementProportion,
        (double) uiContextDecoration.nonWithdrawableImage.getHeight()
                / (double) unitImageHeight
        == factorNonWithdrawableImage);
  }
}
