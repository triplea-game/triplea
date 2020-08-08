package org.triplea.http.client;

import feign.RequestTemplate;
import feign.codec.Encoder;
import feign.gson.GsonEncoder;
import java.lang.reflect.Type;

/**
 * A custom encoder that will be a pass-thru to Jackson encoder when encoding JSON objects, and a
 * pass-thru of simple String values. Without this, String values are encoded to have surrounding
 * quotes, this custom encoder will fix that so that Strings are passed as simple string values
 * without extra surrounding quotes.
 */
public class JsonEncoder implements Encoder {
  private static final GsonEncoder gsonEncoder = new GsonEncoder();

  @SuppressWarnings("deprecation")
  @Override
  public void encode(final Object object, final Type bodyType, final RequestTemplate template) {
    if (bodyType.getTypeName().equals(String.class.getName())) {
      template.body(object.toString());
    } else {
      gsonEncoder.encode(object, bodyType, template);
    }
  }
}
