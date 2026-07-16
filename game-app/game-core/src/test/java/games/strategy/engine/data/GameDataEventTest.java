package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.changefactory.ObjectPropertyChange;
import games.strategy.triplea.Constants;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GameDataEventTest {

  @Test
  void findsTechnologyAttachmentChangeInsideNestedCompositeChange() {
    final ChangeAttachmentChange attachmentChange = mock(ChangeAttachmentChange.class);
    when(attachmentChange.getAttachmentName()).thenReturn(Constants.TECH_ATTACHMENT_NAME);
    final Change change =
        new CompositeChange(new CompositeChange(new CompositeChange(attachmentChange)));

    assertThat(
        GameDataEvent.lookupEvents(change), is(Set.of(GameDataEvent.TECH_ATTACHMENT_CHANGED)));
  }

  @Test
  void ignoresOtherAttachmentChangeInsideCompositeChange() {
    final ChangeAttachmentChange attachmentChange = mock(ChangeAttachmentChange.class);
    when(attachmentChange.getAttachmentName()).thenReturn("otherAttachment");

    assertThat(GameDataEvent.lookupEvents(new CompositeChange(attachmentChange)), is(Set.of()));
  }

  @Test
  void findsAllEventsInsideCompositeChange() {
    final ChangeAttachmentChange attachmentChange = mock(ChangeAttachmentChange.class);
    when(attachmentChange.getAttachmentName()).thenReturn(Constants.TECH_ATTACHMENT_NAME);
    final ObjectPropertyChange moveChange = mock(ObjectPropertyChange.class);
    when(moveChange.getProperty()).thenReturn(Unit.PropertyName.ALREADY_MOVED.toString());

    assertThat(
        GameDataEvent.lookupEvents(new CompositeChange(moveChange, attachmentChange)),
        is(Set.of(GameDataEvent.UNIT_MOVED, GameDataEvent.TECH_ATTACHMENT_CHANGED)));
  }
}
