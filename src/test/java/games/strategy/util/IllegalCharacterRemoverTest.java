package games.strategy.util;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;

import org.junit.Test;

public final class IllegalCharacterRemoverTest {
  @Test
  public void removeIllegalCharacter_ShouldRemoveIllegalCharacters() {
    assertThat(IllegalCharacterRemover.removeIllegalCharacter(IllegalCharacterRemover.ILLEGAL_CHARACTERS), is(""));
  }

  @Test
  public void removeIllegalCharacter_ShouldNotRemoveLegalCharacters() {
    assertThat(IllegalCharacterRemover.removeIllegalCharacter("AZaz09!-"), is("AZaz09!-"));
  }

  @Test
  public void replaceIllegalCharacter_ShouldReplaceIllegalCharacters() {
    assertThat(IllegalCharacterRemover.replaceIllegalCharacter(IllegalCharacterRemover.ILLEGAL_CHARACTERS, '_'),
        is(String.join("", Collections.nCopies(IllegalCharacterRemover.ILLEGAL_CHARACTERS.length(), "_"))));
  }

  @Test
  public void replaceIllegalCharacter_ShouldNotReplaceLegalCharacters() {
    assertThat(IllegalCharacterRemover.replaceIllegalCharacter("AZaz09!-", '_'), is("AZaz09!-"));
  }
}
