package org.triplea.http.client;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import feign.RequestTemplate;
import feign.codec.Encoder;
import feign.gson.GsonEncoder;

/**
 * A custom encoder that will be a pass-thru to Jackson encoder when encoding JSON objects,
 * and a pass-thru of simple String values. Without this, String values are encoded to have surrounding
 * quotes, this custom encoder will fix that so that Strings are passed as simple string values without
 * extra surrounding quotes.
 */
public class JsonEncoder implements Encoder {
  private static final GsonEncoder gsonEncoder = new GsonEncoder();

  @Override
  public void encode(final Object object, final Type bodyType, final RequestTemplate template) {
    if (bodyType.getTypeName().equals(String.class.getName())) {
      template.body(object.toString());
    } else {
//      Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").create();
//      template.body(gson.toJson(object, bodyType));
//
//
//
      gsonEncoder.encode(object, bodyType, template);
    }
  }
}
