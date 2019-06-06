package org.triplea.http.client;

import java.time.Instant;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

import feign.gson.GsonDecoder;

/**
 * Custom decoder that allows for <code>Instant</code> values represented
 * as a floating point number to be decoded. Code snippet to do this is taken from:
 * https://stackoverflow.com/questions/22310143/java-8-localdatetime-deserialized-using-gson
 */
class JsonDecoder {

  static GsonDecoder gsonDecoder() {
    return new GsonDecoder(decoder());
  }

  @VisibleForTesting
  static Gson decoder() {
    return new GsonBuilder().registerTypeAdapter(
        Instant.class,
        (JsonDeserializer<Instant>) (json, type, jsonDeserializationContext) -> Instant
            .ofEpochSecond(json.getAsJsonPrimitive().getAsLong()))
        .create();
  }
}
