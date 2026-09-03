package games.strategy.engine.message;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Compatibility guard for the wire format: every stored fixture is a payload that a released client
 * version put on the wire, and it must still deserialize into its current class and re-serialize as
 * a superset of the stored JSON. Additive-optional evolution stays green (new fields simply appear
 * as extra members on the round-trip); removing, renaming, retyping, or
 * reordering-into-a-different-shape a field goes red, because the stored member can no longer be
 * reproduced.
 *
 * <p>This corpus replaces the old op-code CSV guard, which only checked reflective op-codes and
 * whose documented fix was to edit the CSV to match — silently rubber-stamping breaks. Here the
 * corpus is the source of truth: the {@code manifest.txt} lists the required fixtures, so dropping
 * a guarantee means deleting both a fixture file and a manifest line, a visible reviewable act. See
 * {@code docs/development/networking.md} for the evolution recipe and how to add a fixture.
 */
class WireCompatibilityFixtureTest {
  private static final String MANIFEST = "/wire-compatibility/manifest.txt";
  private static final String FIXTURES_DIR = "/wire-compatibility/fixtures";
  private static final Gson gson = new Gson();

  @ParameterizedTest(name = "{0}")
  @MethodSource("manifestFixtureNames")
  @DisplayName("stored wire payload still deserializes and round-trips as a superset")
  void fixtureRoundTrips(final String fixtureName) throws Exception {
    final JsonObject fixture = readFixture(fixtureName);
    final Class<?> payloadType = Class.forName(fixture.get("type").getAsString());
    final JsonElement storedPayload = fixture.get("payload");

    final Object deserialized = gson.fromJson(storedPayload, payloadType);
    assertThat(
        fixtureName + ": stored payload must deserialize into " + payloadType.getName(),
        deserialized,
        is(notNullValue()));

    final JsonElement roundTripped = gson.toJsonTree(deserialized);
    final Optional<String> missingMember = firstMissingMember(storedPayload, roundTripped, "$");
    assertThat(
        fixtureName
            + ": every field in the stored payload must survive deserialize+serialize into the"
            + " current class"
            + missingMember.map(where -> " — lost or changed at " + where).orElse(""),
        missingMember.isEmpty(),
        is(true));
  }

  @Test
  @DisplayName("manifest and fixture files agree, so no guarantee is silently added or dropped")
  void manifestAndFixtureFilesAgree() throws Exception {
    final Set<String> manifestNames = manifestFixtureNames().collect(Collectors.toSet());
    final Set<String> filesOnDisk = fixtureFilesOnDisk();

    assertThat(
        "every fixture file must be registered in manifest.txt (adding coverage is deliberate)",
        filesOnDisk,
        is(manifestNames));
  }

  private static Stream<String> manifestFixtureNames() {
    return readResourceLines(MANIFEST).stream()
        .map(String::strip)
        .filter(line -> !line.isEmpty() && !line.startsWith("#"));
  }

  private static Set<String> fixtureFilesOnDisk() throws IOException, URISyntaxException {
    final URL dirUrl = WireCompatibilityFixtureTest.class.getResource(FIXTURES_DIR);
    assertThat("fixtures directory must be on the test classpath", dirUrl, is(notNullValue()));
    final Path dir = Path.of(dirUrl.toURI());
    try (Stream<Path> files = Files.list(dir)) {
      return files
          .map(path -> path.getFileName().toString())
          .filter(name -> name.endsWith(".json"))
          .collect(Collectors.toSet());
    }
  }

  private static JsonObject readFixture(final String fixtureName) {
    try (InputStream in =
        WireCompatibilityFixtureTest.class.getResourceAsStream(FIXTURES_DIR + "/" + fixtureName)) {
      assertThat(
          "manifest lists " + fixtureName + " but no such fixture file exists",
          in,
          is(notNullValue()));
      return gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to read fixture " + fixtureName, e);
    }
  }

  private static List<String> readResourceLines(final String resource) {
    try (InputStream in = WireCompatibilityFixtureTest.class.getResourceAsStream(resource);
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      return reader.lines().collect(Collectors.toList());
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to read resource " + resource, e);
    }
  }

  /**
   * Returns the path of the first member of {@code expected} that is absent or differs in {@code
   * actual}, or empty when {@code actual} reproduces every member of {@code expected}. Extra
   * members in {@code actual} are ignored, which is what lets additive-optional field growth stay
   * compatible.
   */
  private static Optional<String> firstMissingMember(
      final JsonElement expected, final JsonElement actual, final String path) {
    if (expected.isJsonObject()) {
      if (!actual.isJsonObject()) {
        return Optional.of(path);
      }
      final JsonObject actualObject = actual.getAsJsonObject();
      for (final Map.Entry<String, JsonElement> member : expected.getAsJsonObject().entrySet()) {
        if (!actualObject.has(member.getKey())) {
          return Optional.of(path + "." + member.getKey());
        }
        final Optional<String> nested =
            firstMissingMember(
                member.getValue(), actualObject.get(member.getKey()), path + "." + member.getKey());
        if (nested.isPresent()) {
          return nested;
        }
      }
      return Optional.empty();
    }
    if (expected.isJsonArray()) {
      if (!actual.isJsonArray()
          || actual.getAsJsonArray().size() != expected.getAsJsonArray().size()) {
        return Optional.of(path);
      }
      final JsonArray expectedArray = expected.getAsJsonArray();
      final JsonArray actualArray = actual.getAsJsonArray();
      for (int i = 0; i < expectedArray.size(); i++) {
        final Optional<String> nested =
            firstMissingMember(expectedArray.get(i), actualArray.get(i), path + "[" + i + "]");
        if (nested.isPresent()) {
          return nested;
        }
      }
      return Optional.empty();
    }
    return expected.equals(actual) ? Optional.empty() : Optional.of(path);
  }
}
