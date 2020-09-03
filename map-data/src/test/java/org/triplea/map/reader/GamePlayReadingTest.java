package org.triplea.map.reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.triplea.map.data.elements.GamePlay.Offset;
import static org.triplea.map.data.elements.GamePlay.Sequence;
import static org.triplea.map.reader.XmlReaderTestUtils.parseMapXml;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.map.data.elements.Game;
import org.triplea.map.data.elements.GamePlay;
import org.triplea.map.data.elements.GamePlay.Delegate;
import org.triplea.map.data.elements.GamePlay.Sequence.Step;

class GamePlayReadingTest {

  @Test
  void readGamePlayTag() {
    final Game game = parseMapXml("game-play-tag.xml");
    final GamePlay gamePlay = game.getGamePlay();
    final List<Delegate> delegates = gamePlay.getDelegates();
    assertThat(delegates, hasSize(2));
    assertThat(delegates.get(0).getName(), is("delegate1"));
    assertThat(delegates.get(0).getJavaClass(), is("javaDelegate1"));
    assertThat(delegates.get(0).getDisplay(), is("display1"));

    assertThat(delegates.get(1).getName(), is("delegate2"));
    assertThat(delegates.get(1).getJavaClass(), is("javaDelegate2"));

    final Sequence sequence = gamePlay.getSequence();
    final List<Step> steps = sequence.getSteps();
    assertThat(steps, hasSize(3));
    assertThat(steps.get(0).getName(), is("step1"));
    assertThat(steps.get(0).getDelegate(), is("stepDelegate1"));
    assertThat(steps.get(0).getPlayer(), is("player1"));
    assertThat(steps.get(0).getMaxRunCount(), is("1"));
    assertThat(steps.get(0).getDisplay(), is("stepDisplay"));

    assertThat(steps.get(1).getName(), is("step2"));
    assertThat(steps.get(1).getDelegate(), is("stepDelegate2"));

    assertThat(steps.get(2).getName(), is("step3"));
    assertThat(steps.get(2).getDelegate(), is("stepDelegate3"));

    final List<Step.StepProperty> stepProperties = steps.get(2).getStepProperties();
    assertThat(stepProperties, hasSize(2));
    assertThat(stepProperties.get(0).getName(), is("stepProp1"));
    assertThat(stepProperties.get(0).getValue(), is("stepValue1"));

    assertThat(stepProperties.get(1).getName(), is("stepProp2"));
    assertThat(stepProperties.get(1).getValue(), is("stepValue2"));

    final Offset offset = gamePlay.getOffset();
    assertThat(offset.getRound(), is("3"));
  }
}
