package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

final class DefaultAttachmentTest {
  @ExtendWith(MockitoExtension.class)
  @Nested
  final class EqualsTest {
    @NonNls private static final String NAME = "attachment";
    @NonNls private static final String OTHER_NAME = "otherAttachment";

    @Mock private Attachable attachable;
    @Mock private Attachable otherAttachable;
    private final GameData gameData = new GameData();

    private class TestDefaultAttachment extends DefaultAttachment {
      private static final long serialVersionUID = 1L;

      private final String stringValue;

      TestDefaultAttachment(
          final String name,
          final Attachable attachable,
          final GameData gameData,
          final String stringValue) {
        super(name, attachable, gameData);

        this.stringValue = stringValue;
      }

      @Override
      public void validate(final GameState data) {}

      @Override
      public Optional<MutableProperty<?>> getPropertyOrEmpty(final @NonNls String propertyName) {
        return Optional.empty();
      }

      @Override
      public String toString() {
        return stringValue;
      }
    }

    private DefaultAttachment givenAttachment(final String name, final Attachable attachable) {
      return givenAttachment(name, attachable, name);
    }

    private DefaultAttachment givenAttachment(
        final String name, final Attachable attachable, final String stringValue) {
      return new TestDefaultAttachment(name, attachable, gameData, stringValue);
    }

    @Test
    void shouldReturnTrueWhenOtherIsSameInstance() {
      final DefaultAttachment attachment = givenAttachment(NAME, attachable);
      final DefaultAttachment otherAttachment = attachment;

      assertThat(attachment.equals(otherAttachment), is(true));
    }

    @Test
    void shouldReturnFalseWhenOtherIsNull() {
      final DefaultAttachment attachment = givenAttachment(NAME, attachable);
      final DefaultAttachment otherAttachment = null;

      assertThat(attachment.equals(otherAttachment), is(false));
    }

    @Test
    void shouldReturnFalseWhenOtherIsDifferentClass() {
      final DefaultAttachment attachment = givenAttachment(NAME, attachable);
      final DefaultAttachment otherAttachment =
          new TestDefaultAttachment(NAME, attachable, gameData, NAME) {
            private static final long serialVersionUID = 1L;
          };

      assertThat(attachment.equals(otherAttachment), is(false));
    }

    @Test
    void shouldReturnFalseWhenThisAttachableIsNotNullAndOtherAttachableIsNull() {
      final DefaultAttachment attachment = givenAttachment(NAME, attachable);
      final DefaultAttachment otherAttachment = givenAttachment(NAME, null);

      assertThat(attachment.equals(otherAttachment), is(false));
    }

    @Test
    void shouldReturnFalseWhenThisAttachableIsNullAndOtherAttachableIsNotNull() {
      final DefaultAttachment attachment = givenAttachment(NAME, null);
      final DefaultAttachment otherAttachment = givenAttachment(NAME, otherAttachable);

      assertThat(attachment.equals(otherAttachment), is(false));
    }

    @Test
    void shouldReturnTrueWhenThisAttachableIsNullAndOtherAttachableIsNullAndNameEqual() {
      final DefaultAttachment attachment = givenAttachment(NAME, null);
      final DefaultAttachment otherAttachment = givenAttachment(NAME, null);

      assertThat(attachment.equals(otherAttachment), is(true));
    }

    @Test
    void shouldReturnFalseWhenAttachableNotEqual() {
      final DefaultAttachment attachment = givenAttachment(NAME, attachable);
      final DefaultAttachment otherAttachment = givenAttachment(NAME, otherAttachable);

      assertThat(attachment.equals(otherAttachment), is(false));
    }

    @Test
    void shouldReturnFalseWhenAttachableEqualAndNameNotEqual() {
      final DefaultAttachment attachment = givenAttachment(NAME, attachable);
      final DefaultAttachment otherAttachment = givenAttachment(OTHER_NAME, attachable);

      assertThat(attachment.equals(otherAttachment), is(false));
    }

    @Test
    void shouldReturnTrueWhenAttachableEqualAndNameEqual() {
      final DefaultAttachment attachment = givenAttachment(NAME, attachable);
      final DefaultAttachment otherAttachment = givenAttachment(NAME, attachable);

      assertThat(attachment.equals(otherAttachment), is(true));
    }

    @Test
    void shouldReturnTrueWhenAttachableEqualAndNameNotEqualAndToStringEqual() {
      final DefaultAttachment attachment = givenAttachment(NAME, attachable, "stringValue");
      final DefaultAttachment otherAttachment =
          givenAttachment(OTHER_NAME, attachable, "stringValue");

      assertThat(attachment.equals(otherAttachment), is(true));
    }
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  final class GetAttachmentTest {
    @Mock private NamedAttachable namedAttachable;

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

      assertThrows(
          IllegalStateException.class,
          () ->
              DefaultAttachment.getAttachment(
                  namedAttachable, "attachmentName", FakeAttachment.class));
    }

    @Test
    void shouldThrowExceptionWhenAttachmentHasWrongType() {
      when(namedAttachable.getAttachment(any())).thenReturn(mock(IAttachment.class));

      assertThrows(
          ClassCastException.class,
          () ->
              DefaultAttachment.getAttachment(
                  namedAttachable, "attachmentName", FakeAttachment.class));
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
