package org.triplea.http.client;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import feign.gson.GsonDecoder;
import java.time.Instant;
import java.util.List;
import lombok.experimental.UtilityClass;

/**
 * Custom decoder that allows for <code>Instant</code> values represented as a floating point number
 * to be decoded. Code snippet to do this is inspired by:
 * https://stackoverflow.com/questions/22310143/java-8-localdatetime-deserialized-using-gson
 *
 * <p>The expected representation that we will be parsing is: {@code [epoch second].[epoch nano]}.
 * This representation is created by the server when it sends JSON responses back.
 */
@UtilityClass
final class JsonDecoder {

  static GsonDecoder gsonDecoder() {
    return new GsonDecoder(decoder());
  }

  @VisibleForTesting
  static Gson decoder() {
    return new GsonBuilder()
        .registerTypeAdapter(
            Instant.class,
            (JsonDeserializer<Instant>)
                (json, type, jsonDeserializationContext) -> {
                  Preconditions.checkState(
                      json.getAsJsonPrimitive().getAsString().contains("."),
                      "Unexpected json date format, expected {[epoch second].[epoch nano]}, "
                          + "value received was: "
                          + json.getAsJsonPrimitive().getAsString());

                  final long[] timeStampParts = splitTimestamp(json);
                  return Instant.ofEpochSecond(timeStampParts[0], timeStampParts[1]);
                })
        .create();
  }

  private static long[] splitTimestamp(final JsonElement json) {
    final String[] split = json.getAsJsonPrimitive().getAsString().split("\\.");
    Preconditions.checkState(
        split.length == 2,
        "Unexpected JSON {[epoch second].[epoch nano]} timestamp format, value was: "
            + json.getAsJsonPrimitive().getAsString()
            + ", and was split into: "
            + List.of(split));
    return new long[] {Long.parseLong(split[0]), Long.parseLong(split[1])};
  }
}
