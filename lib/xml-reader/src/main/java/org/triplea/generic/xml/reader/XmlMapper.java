package org.triplea.generic.xml.reader;

import com.google.common.base.Preconditions;
import java.io.Closeable;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.extern.slf4j.Slf4j;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.Tag;
import org.triplea.generic.xml.reader.annotations.TagList;
import org.triplea.generic.xml.reader.exceptions.JavaDataModelException;
import org.triplea.generic.xml.reader.exceptions.XmlParsingException;

@Slf4j
public class XmlMapper implements Closeable {
  private final XMLStreamReader xmlStreamReader;

  public XmlMapper(final InputStream inputStream) throws XmlParsingException {
    final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    try {
      xmlStreamReader = inputFactory.createXMLStreamReader(inputStream);
    } catch (final XMLStreamException e) {
      throw new XmlParsingException("Exception reading XML file, " + e.getMessage(), e);
    }
  }

  public <T> T mapXmlToObject(final Class<T> pojo) throws XmlParsingException {
    return mapXmlToObject(pojo, pojo.getSimpleName());
  }

  private <T> T mapXmlToObject(final Class<T> pojo, final String tagName)
      throws XmlParsingException {
    // At this point in parsing the XML cursor is just beyond the start tag.
    // We can read attributes directly off of the stream at this point.
    // If we do nothing more then the cursor will keep moving down and will not
    // descend into any child tags or the body text of the current tag.
    //
    // Beyond mapping attributes, we should scan for any handlers defined
    // on the POJO, whether those are handlers for more child tags, tag lists or
    // body content. If we see any of those we will set up callbacks on the
    // XML parsing such that when the cursor hits tags with matching names
    // or finishes reading the body content it will execute the callback.
    //
    // Each callback will get data from the XML parsing cursor and the callback
    // only then needs to set that data on the current object that we are building and will
    // be returning.
    //
    // The callbacks are of two types: tags or body content.
    //
    // Body content callback is pretty simple, we'll read all of the body content into a buffer
    // and when done will execute the body content setter callback.
    //
    // The tag callbacks are a map of tag name to setter callback method. When the XML
    // cursor sees a tag with a matching name it'll call the setter callback.
    // The setter callback will invoke this method again to create a fully formed object
    // out of that tag (and then set it).

    try {
      // Create the object to return, it is a java representation of the "current tag".
      final T instance = ReflectionUtils.newInstance(pojo);
      final AnnotatedFields<T> annotatedFields = new AnnotatedFields<>(pojo);

      // set attributes on the current object
      for (final Field field : annotatedFields.getAttributeFields()) {

        final String[] attributeNames =
            getNamesFromAnnotationOrDefault(
                field.getAnnotation(Attribute.class).names(), field.getName());

        final String attributeValue =
            Arrays.stream(attributeNames)
                .map(attributeName -> xmlStreamReader.getAttributeValue(null, attributeName))
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);

        final Object value = new AttributeValueCasting(field).castAttributeValue(attributeValue);
        field.set(instance, value);
      }

      // Check if we have any more work to do, if only attributes can go ahead and return now.
      if (annotatedFields.getTagFields().isEmpty()
          && annotatedFields.getTagListFields().isEmpty()
          && annotatedFields.getBodyTextFields().isEmpty()) {
        return instance;
      }

      // This parser will do the work of parsing the current tag, it'll look at all
      // child tags and the body text and invoke the right callback that we will define below.
      final XmlParser tagParser = new XmlParser(tagName);

      // Set up tag parsing, as we scan through more elements when we see a matching
      // tag name we'll call the child tag handler. The child tag handler will
      // create a java model representing the child tag and set the field instance
      // on our current running instance object.
      for (final Field field : annotatedFields.getTagFields()) {

        final String[] tagNames =
            getNamesFromAnnotationOrDefault(
                field.getAnnotation(Tag.class).names(), field.getType().getSimpleName());

        Arrays.stream(tagNames)
            .forEach(
                expectedTagName ->
                    tagParser.childTagHandler(
                        expectedTagName,
                        () ->
                            field.set(instance, mapXmlToObject(field.getType(), expectedTagName))));
      }

      // Set up tag list parsing, similar to tag parsing except we set the field
      // value to a list and each time we see a new child tag we'll add it back to that list.
      for (final Field field : annotatedFields.getTagListFields()) {
        final List<Object> tagList = new ArrayList<>();
        field.set(instance, tagList);
        final Class<?> listType = ReflectionUtils.getGenericType(field);

        final String[] tagNames =
            getNamesFromAnnotationOrDefault(
                field.getAnnotation(TagList.class).names(), listType.getSimpleName());

        Arrays.stream(tagNames)
            .forEach(
                expectedTagName ->
                    tagParser.childTagHandler(
                        expectedTagName,
                        () -> tagList.add(mapXmlToObject(listType, expectedTagName))));
      }

      // Set up body text handler. The XML cursor will iterate over each line of body
      // content and we will buffer that content, when the full content is read it is trimmed
      // and the setter callback below is executed.
      for (final Field field : annotatedFields.getBodyTextFields()) {
        Preconditions.checkState(annotatedFields.getBodyTextFields().size() == 1);
        tagParser.bodyHandler(
            textContent -> {
              try {
                field.set(instance, textContent);
              } catch (final IllegalAccessException e) {
                throw new JavaDataModelException(field, "Unexpected illegal access", e);
              }
            });
      }

      tagParser.parse(xmlStreamReader);
      return instance;
    } catch (final Throwable e) {
      if (e instanceof XmlParsingException xmlParsingException) {
        throw xmlParsingException;
      } else {
        throw new XmlParsingException(xmlStreamReader, pojo, e);
      }
    }
  }

  private static String[] getNamesFromAnnotationOrDefault(
      final String[] annotationValues, final String defaultValue) {
    return annotationValues.length == 1 && annotationValues[0].isEmpty()
        ? new String[] {defaultValue}
        : annotationValues;
  }

  @Override
  public void close() {
    try {
      xmlStreamReader.close();
    } catch (final XMLStreamException e) {
      log.info("Failed to close xml stream", e);
    }
  }
}
