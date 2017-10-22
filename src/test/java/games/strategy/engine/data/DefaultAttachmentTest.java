package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.experimental.extensions.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public final class DefaultAttachmentTest {
  @Mock
  private NamedAttachable namedAttachable;

  @Test
  public void getAttachment_ShouldReturnAttachmentWhenAttachmentIsPresent() {
    final String attachmentName = "attachmentName";
    final FakeAttachment expected = new FakeAttachment(attachmentName);
    when(namedAttachable.getAttachment(attachmentName)).thenReturn(expected);

    final FakeAttachment actual =
        DefaultAttachment.getAttachment(namedAttachable, attachmentName, FakeAttachment.class);

    assertThat(actual, is(sameInstance(expected)));
  }

  @Test
  public void getAttachment_ShouldThrowExceptionWhenAttachmentIsAbsent() {
    when(namedAttachable.getAttachment(any())).thenReturn(null);

    assertThrows(IllegalStateException.class,
        () -> DefaultAttachment.getAttachment(namedAttachable, "attachmentName", FakeAttachment.class));
  }

  @Test
  public void getAttachment_ShouldThrowExceptionWhenAttachmentHasWrongType() {
    when(namedAttachable.getAttachment(any())).thenReturn(mock(IAttachment.class));

    assertThrows(ClassCastException.class,
        () -> DefaultAttachment.getAttachment(namedAttachable, "attachmentName", FakeAttachment.class));
  }
}
