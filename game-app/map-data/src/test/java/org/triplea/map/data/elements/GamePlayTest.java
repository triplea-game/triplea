package org.triplea.map.data.elements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.triplea.map.data.elements.GamePlay.Offset;
import static org.triplea.map.data.elements.GamePlay.Sequence;
import static org.triplea.map.data.elements.XmlReaderTestUtils.parseMapXml;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.map.data.elements.GamePlay.Delegate;
import org.triplea.map.data.elements.GamePlay.Sequence.Step;

class GamePlayTest {

  @Test
  void readGamePlayTag() {
    final GamePlay gamePlay = parseMapXml("game-play.xml").getGamePlay();
    final List<Delegate> delegates = gamePlay.getDelegates();
    assertThat(delegates).hasSize(2);
    assertThat(delegates.get(0).getName()).isEqualTo("delegate1");
    assertThat(delegates.get(0).getJavaClass()).isEqualTo("javaDelegate1");
    assertThat(delegates.get(0).getDisplay()).isEqualTo("display1");

    assertThat(delegates.get(1).getName()).isEqualTo("delegate2");
    assertThat(delegates.get(1).getJavaClass()).isEqualTo("javaDelegate2");

    final Sequence sequence = gamePlay.getSequence();
    final List<Step> steps = sequence.getSteps();
    assertThat(steps).hasSize(3);
    assertThat(steps.get(0).getName()).isEqualTo("step1");
    assertThat(steps.get(0).getDelegate()).isEqualTo("stepDelegate1");
    assertThat(steps.get(0).getPlayer()).isEqualTo("player1");
    assertThat(steps.get(0).getMaxRunCount()).isEqualTo(1);
    assertThat(steps.get(0).getDisplay()).isEqualTo("stepDisplay");

    assertThat(steps.get(1).getName()).isEqualTo("step2");
    assertThat(steps.get(1).getDelegate()).isEqualTo("stepDelegate2");

    assertThat(steps.get(2).getName()).isEqualTo("step3");
    assertThat(steps.get(2).getDelegate()).isEqualTo("stepDelegate3");

    final List<Step.StepProperty> stepProperties = steps.get(2).getStepProperties();
    assertThat(stepProperties).hasSize(2);
    assertThat(stepProperties.get(0).getName()).isEqualTo("stepProp1");
    assertThat(stepProperties.get(0).getValue()).isEqualTo("stepValue1");

    assertThat(stepProperties.get(1).getName()).isEqualTo("stepProp2");
    assertThat(stepProperties.get(1).getValue()).isEqualTo("stepValue2");

    final Offset offset = gamePlay.getOffset();
    assertThat(offset.getRound()).isEqualTo(3);
  }
}
