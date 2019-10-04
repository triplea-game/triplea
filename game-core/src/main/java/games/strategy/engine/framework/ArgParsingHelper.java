package games.strategy.engine.framework;

import java.util.Arrays;
import java.util.Properties;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * A helper class for parsing args. Looks for args beginning with "-P" and will return them as part
 * of a {@code Properties} object.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ArgParsingHelper {
  public static final String TRIPLEA_PROPERTY_PREFIX = "P";

  /**
   * Parses the set of input parameters for things that look like "-Pkey=value" and will return the
   * key/value pairs as a Properties object.
   */
  public static Properties getTripleaProperties(final String... args) {
    final Options options = getOptions();
    final CommandLineParser parser = new DefaultParser();
    try {
      final CommandLine cli = parser.parse(options, args);
      return cli.getOptionProperties(TRIPLEA_PROPERTY_PREFIX);
    } catch (final ParseException e) {
      throw new IllegalArgumentException("Failed to parse args: " + Arrays.toString(args), e);
    }
  }

  private static Options getOptions() {
    final Options options = new Options();
    options.addOption(
        Option.builder(TRIPLEA_PROPERTY_PREFIX)
            .argName("key=value")
            .hasArgs()
            .numberOfArgs(2)
            .valueSeparator()
            .build());
    // See https://github.com/triplea-game/triplea/pull/2574 for more information
    options.addOption(Option.builder("console").build());
    return options;
  }
}
