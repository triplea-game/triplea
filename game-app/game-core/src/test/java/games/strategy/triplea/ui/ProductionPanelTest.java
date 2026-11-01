package games.strategy.triplea.ui;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.image.ResourceImageFactory;
import games.strategy.ui.ScrollableTextField;
import java.lang.reflect.Field;
import java.util.Set;
import javax.swing.JPanel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

final class ProductionPanelTest {
  private final GameData gameData = new GameData();
  private final GamePlayer player = new GamePlayer("player", gameData);
  private final Resource pus = new Resource("PUs", gameData);

  private ProductionPanel givenProductionPanel() {
    final UiContext uiContext = mock(UiContext.class);
    final ResourceImageFactory resourceImageFactory = mock(ResourceImageFactory.class);
    when(uiContext.getResourceImageFactory()).thenReturn(resourceImageFactory);
    when(resourceImageFactory.getResourcesPanel(any(ResourceCollection.class), eq(player)))
        .thenReturn(new JPanel());

    player.getResources().addResource(pus, 100);
    final ProductionPanel panel = new ProductionPanel(uiContext);
    panel.gamePlayer = player;
    panel.data = gameData;
    return panel;
  }

  private UnitType givenLimitedUnitType(final String name, final int maxBuiltPerPlayer) {
    final UnitType type = new UnitType(name, gameData);
    final UnitAttachment attachment = mock(UnitAttachment.class);
    type.addAttachment(UNIT_ATTACHMENT_NAME, attachment);
    when(attachment.getMaxBuiltPerPlayer()).thenReturn(maxBuiltPerPlayer);
    return type;
  }

  private ProductionRule givenProductionRule(
      final String name, final UnitType type, final int unitsPerPurchase) {
    final ProductionRule rule = new ProductionRule(name, gameData);
    rule.addCost(pus, 1);
    rule.addResult(type, unitsPerPurchase);
    return rule;
  }

  @SuppressWarnings("unchecked")
  private static Set<ScrollableTextField> getTextFields(final ProductionPanel.Rule rule)
      throws ReflectiveOperationException {
    final Field field = rule.getClass().getDeclaredField("textFields");
    field.setAccessible(true);
    return (Set<ScrollableTextField>) field.get(rule);
  }

  private static int calculateMax(final ProductionPanel panel, final ProductionPanel.Rule target)
      throws ReflectiveOperationException {
    // The test observes the real calculateLimits() behavior without loading UI images.
    final ScrollableTextField textField = mock(ScrollableTextField.class);
    getTextFields(target).add(textField);
    panel.calculateLimits();

    final ArgumentCaptor<Integer> maxCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(textField).setMax(maxCaptor.capture());
    return maxCaptor.getValue();
  }

  @Nested
  final class BuildLimitTest {

    @Test
    void shouldPreventPurchasesWhenOwnedUnitsExceedBuildLimit()
        throws ReflectiveOperationException {
      final UnitType tank = givenLimitedUnitType("tank", 5);
      player.getUnitCollection().addAll(tank.create(7, player));

      assertThat(ProductionPanel.getOwnedUnitCount(tank, player, gameData)).isEqualTo(7);

      final ProductionPanel panel = givenProductionPanel();
      final ProductionPanel.Rule purchase =
          panel.new Rule(givenProductionRule("buyTank", tank, 1), player);
      panel.rules.add(purchase);

      assertThat(calculateMax(panel, purchase)).isZero();
    }

    @Test
    void shouldLimitPurchasesOfOneRestrictedUnitType() throws ReflectiveOperationException {
      final ProductionPanel panel = givenProductionPanel();
      final UnitType tank = givenLimitedUnitType("tank", 6);
      final ProductionRule rule = givenProductionRule("buyTankPack", tank, 3);
      final ProductionPanel.Rule purchase = panel.new Rule(rule, player);
      panel.rules.add(purchase);

      assertThat(calculateMax(panel, purchase)).isEqualTo(2);
    }

    @Test
    void shouldAccountForOtherRulesProducingTheSameLimitedUnit()
        throws ReflectiveOperationException {
      final ProductionPanel panel = givenProductionPanel();
      final UnitType tank = givenLimitedUnitType("tank", 6);
      final ProductionPanel.Rule singleTank =
          panel.new Rule(givenProductionRule("buyTank", tank, 1), player);
      final ProductionPanel.Rule tankPack =
          panel.new Rule(givenProductionRule("buyTankPack", tank, 3), player);

      singleTank.setQuantity(3);
      panel.rules.add(singleTank);
      panel.rules.add(tankPack);

      // Three of the six available tanks were selected in a different purchase rule.
      assertThat(calculateMax(panel, tankPack)).isEqualTo(1);
    }

    @Test
    void shouldRespectAllLimitedUnitTypesInOneProductionRule() throws ReflectiveOperationException {
      final ProductionPanel panel = givenProductionPanel();
      final UnitType tank = givenLimitedUnitType("tank", 10);
      final UnitType infantry = givenLimitedUnitType("infantry", 10);
      final ProductionRule rule = givenProductionRule("buyArmyPack", tank, 1);
      rule.addResult(infantry, 1);

      final ProductionPanel.Rule purchase = panel.new Rule(rule, player);
      final UnitType firstType =
          rule.getResults().keySet().stream()
              .filter(UnitType.class::isInstance)
              .map(UnitType.class::cast)
              .findFirst()
              .orElseThrow();
      final UnitType otherType = firstType == tank ? infantry : tank;
      when(otherType.getUnitAttachment().getMaxBuiltPerPlayer()).thenReturn(1);
      panel.rules.add(purchase);

      assertThat(calculateMax(panel, purchase)).isEqualTo(1);
    }
  }
}
