package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

final class DefaultAttachmentTest {
  @ExtendWith(MockitoExtension.class)
  @Nested
  final class GetAttachmentTest {
    @Mock
    private NamedAttachable namedAttachable;

    @Test
    void shouldReturnAttachmentWhenAttachmentIsPresent() {
      final String attachmentName = "attachmentName";
      final FakeAttachment expected = new FakeAttachment(attachmentName);
      when(namedAttachable.getAttachment(attachmentName)).thenReturn(expected);

      final FakeAttachment actual =
          DefaultAttachment.getAttachment(namedAttachable, attachmentName, FakeAttachment.class);

      assertThat(actual, is(sameInstance(expected)));
    }

    @Test
    void shouldThrowExceptionWhenAttachmentIsAbsent() {
      when(namedAttachable.getAttachment(any())).thenReturn(null);

      assertThrows(IllegalStateException.class,
          () -> DefaultAttachment.getAttachment(namedAttachable, "attachmentName", FakeAttachment.class));
    }

    @Test
    void shouldThrowExceptionWhenAttachmentHasWrongType() {
      when(namedAttachable.getAttachment(any())).thenReturn(mock(IAttachment.class));

      assertThrows(ClassCastException.class,
          () -> DefaultAttachment.getAttachment(namedAttachable, "attachmentName", FakeAttachment.class));
    }
  }

  @Nested
  final class SplitOnColonTest {
    @Test
    void shouldSplitValueOnColon() {
      assertThat(DefaultAttachment.splitOnColon(""), is(new String[] {""}));
      assertThat(DefaultAttachment.splitOnColon("a"), is(new String[] {"a"}));
      assertThat(DefaultAttachment.splitOnColon(":"), is(new String[] {"", ""}));
      assertThat(DefaultAttachment.splitOnColon("a:b"), is(new String[] {"a", "b"}));
      assertThat(DefaultAttachment.splitOnColon("a::b"), is(new String[] {"a", "", "b"}));
      assertThat(DefaultAttachment.splitOnColon("::"), is(new String[] {"", "", ""}));
      assertThat(DefaultAttachment.splitOnColon("a:b::c"), is(new String[] {"a", "b", "", "c"}));
      assertThat(DefaultAttachment.splitOnColon(":a:b:c"), is(new String[] {"", "a", "b", "c"}));
      assertThat(DefaultAttachment.splitOnColon("a:b:c:"), is(new String[] {"a", "b", "c", ""}));
    }
  }
}
