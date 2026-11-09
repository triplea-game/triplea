package games.strategy.engine.framework.startup.ui.posted.game.pbem;

import static games.strategy.engine.framework.startup.ui.posted.game.pbem.EmailProviderPreset.lookupByName;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailProviderPresetTest {
  @Nested
  class VerifyLookupByNamePositiveCases {

    @Test
    void lookupGmail() {
      assertThat(lookupByName("Gmail")).contains(EmailProviderPreset.GMAIL);
    }

    @Test
    void lookupHotmail() {
      assertThat(lookupByName("Hotmail")).contains(EmailProviderPreset.HOTMAIL);
    }
  }

  @Nested
  class VerifyLookupByNameNegativeCases {
    @Test
    void nullLookupIsEmpty() {
      assertThat(lookupByName(null)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          EmailSenderEditorViewModel.PROVIDER_DISABLED,
          "",
          "  ",
          "DNE",
          "  Gmail  ", // this value is not trimmed
          "GMAIL" // casing is not right
        })
    @DisplayName("Verify for a set of strings that are not a preset, we get an empty result")
    void invalidValues(final String value) {
      assertThat(lookupByName(value))
          .as("Searching for value is not expected to match any of the presets: " + value)
          .isEmpty();
    }
  }
}
