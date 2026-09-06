package games.strategy.engine.data;

import static org.assertj.core.api.Assertions.assertThat;
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

    assertThat(GameDataEvent.lookupGameDataChangeEvents(change))
        .isEqualTo(Set.of(GameDataEvent.TECH_ATTACHMENT_CHANGED));
  }

  @Test
  void ignoresOtherAttachmentChangeInsideCompositeChange() {
    final ChangeAttachmentChange attachmentChange = mock(ChangeAttachmentChange.class);
    when(attachmentChange.getAttachmentName()).thenReturn("otherAttachment");

    assertThat(GameDataEvent.lookupGameDataChangeEvents(new CompositeChange(attachmentChange)))
        .isEqualTo(Set.of());
  }

  @Test
  void findsAllGameDataChangeEventsInsideCompositeChange() {
    final ChangeAttachmentChange attachmentChange = mock(ChangeAttachmentChange.class);
    when(attachmentChange.getAttachmentName()).thenReturn(Constants.TECH_ATTACHMENT_NAME);
    final ObjectPropertyChange moveChange = mock(ObjectPropertyChange.class);
    when(moveChange.getProperty()).thenReturn(Unit.PropertyName.ALREADY_MOVED.toString());

    assertThat(
            GameDataEvent.lookupGameDataChangeEvents(
                new CompositeChange(moveChange, attachmentChange)))
        .isEqualTo(Set.of(GameDataEvent.UNIT_MOVED, GameDataEvent.TECH_ATTACHMENT_CHANGED));
  }
}
