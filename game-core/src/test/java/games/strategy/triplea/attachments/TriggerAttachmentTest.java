package games.strategy.triplea.attachments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionFrontierList;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.ProductionRuleList;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.history.IDelegateHistoryWriter;

@ExtendWith(MockitoExtension.class)
public class TriggerAttachmentTest {

  private final Set<TriggerAttachment> satisfiedTriggers = new HashSet<>();
  @Mock
  private IDelegateBridge bridge;
  @Mock
  private IDelegateHistoryWriter historyWriter;

  @BeforeEach
  public void setUp() {
    final GameData gameData = new GameData();

    final TriggerAttachment triggerAttachment = mock(TriggerAttachment.class);
    satisfiedTriggers.add(triggerAttachment);
    when(triggerAttachment.getProductionRule())
        .thenReturn(Arrays.asList("frontier:rule1", "frontier:-rule2", "frontier:rule3"));
    when(triggerAttachment.getName()).thenReturn("mockedTriggerAttachment");
    when(bridge.getData()).thenReturn(gameData);
    when(bridge.getHistoryWriter()).thenReturn(historyWriter);

    final ProductionRuleList productionRuleList = gameData.getProductionRuleList();
    productionRuleList.addProductionRule(new ProductionRule("rule1", gameData));
    final ProductionRule productionRule2 = new ProductionRule("rule2", gameData);
    productionRuleList.addProductionRule(productionRule2);
    productionRuleList.addProductionRule(new ProductionRule("rule3", gameData));

    final ProductionFrontierList productionFrontierList = gameData.getProductionFrontierList();
    productionFrontierList
        .addProductionFrontier(
            new ProductionFrontier("frontier", gameData, Collections.singletonList(productionRule2)));
  }

  @Test
  public void testTriggerProductionFrontierEditChange() {
    TriggerAttachment.triggerProductionFrontierEditChange(
        satisfiedTriggers,
        bridge,
        "beforeOrAfter",
        "stepName",
        false, // useUses
        false, // testUses
        false, // testChance
        false); // testWhen
    final ArgumentCaptor<CompositeChange> argument = ArgumentCaptor.forClass(CompositeChange.class);
    verify(bridge).addChange(argument.capture());
    final CompositeChange change = argument.getValue();
    assertFalse(change.isEmpty());
    final ArgumentCaptor<String> ruleAddArgument = ArgumentCaptor.forClass(String.class);
    verify(historyWriter, times(3)).startEvent(ruleAddArgument.capture());
    final List<String> allValues = ruleAddArgument.getAllValues();
    assertEquals(3, allValues.size());
    assertTrue(allValues.stream().anyMatch(s -> s.contains("rule1") && s.contains("added")));
    assertTrue(allValues.stream().anyMatch(s -> s.contains("rule2") && s.contains("removed")));
    assertTrue(allValues.stream().anyMatch(s -> s.contains("rule3") && s.contains("added")));
    assertTrue(allValues.stream().allMatch(s -> s.contains("frontier")));
  }
}
