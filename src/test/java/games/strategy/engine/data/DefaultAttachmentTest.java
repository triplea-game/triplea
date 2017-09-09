package games.strategy.engine.data;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
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

    catchException(() -> DefaultAttachment.getAttachment(namedAttachable, "attachmentName", FakeAttachment.class));

    assertThat(caughtException(), is(instanceOf(IllegalStateException.class)));
  }

  @Test
  public void getAttachment_ShouldThrowExceptionWhenAttachmentHasWrongType() {
    when(namedAttachable.getAttachment(any())).thenReturn(mock(IAttachment.class));

    catchException(() -> DefaultAttachment.getAttachment(namedAttachable, "attachmentName", FakeAttachment.class));

    assertThat(caughtException(), is(instanceOf(ClassCastException.class)));
  }
}
