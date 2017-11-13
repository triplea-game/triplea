package games.strategy.triplea.attachments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.experimental.extensions.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

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

  public Set<TriggerAttachment> satisfiedTriggers = new HashSet<>();
  @Mock
  public IDelegateBridge bridge;
  @Mock
  public GameData gameData;
  @Mock
  public ProductionFrontierList productionFrontierList;
  @Mock
  public ProductionRuleList productionRuleList;
  @Mock
  public ProductionFrontier productionFrontier;
  @Mock
  public ProductionRule productionRule1;
  @Mock
  public ProductionRule productionRule2;
  @Mock
  public ProductionRule productionRule3;
  @Mock
  public IDelegateHistoryWriter historyWriter;


  @BeforeEach
  public void setUp() {
    final TriggerAttachment mock = mock(TriggerAttachment.class);
    satisfiedTriggers.add(mock);
    when(mock.getProductionRule())
        .thenReturn(new ArrayList<>(Arrays.asList("frontier:rule1", "frontier:-rule2", "frontier:rule3")));
    when(mock.getName()).thenReturn("mockedTriggerAttachment");
    when(bridge.getData()).thenReturn(gameData);
    when(bridge.getHistoryWriter()).thenReturn(historyWriter);
    when(gameData.getProductionFrontierList()).thenReturn(productionFrontierList);
    when(gameData.getProductionRuleList()).thenReturn(productionRuleList);
    when(productionFrontierList.getProductionFrontier("frontier")).thenReturn(productionFrontier);
    when(productionFrontier.getName()).thenReturn("frontierName");
    when(productionFrontier.getRules()).thenReturn(Arrays.asList(productionRule2));
    when(productionRuleList.getProductionRule("rule1")).thenReturn(productionRule1);
    when(productionRuleList.getProductionRule("rule2")).thenReturn(productionRule2);
    when(productionRuleList.getProductionRule("rule3")).thenReturn(productionRule3);
    when(productionRule1.getName()).thenReturn("rule1Name");
    when(productionRule2.getName()).thenReturn("rule2Name");
    when(productionRule3.getName()).thenReturn("rule3Name");

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
    assertTrue(allValues.stream().anyMatch(s -> s.contains("rule1Name") && s.contains("added")));
    assertTrue(allValues.stream().anyMatch(s -> s.contains("rule2Name") && s.contains("removed")));
    assertTrue(allValues.stream().anyMatch(s -> s.contains("rule3Name") && s.contains("added")));
    assertTrue(allValues.stream().allMatch(s -> s.contains("frontierName")));
  }
}
