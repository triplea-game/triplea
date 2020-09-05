package org.triplea.generic.xml.reader;

import com.google.common.base.Preconditions;
import java.io.Closeable;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.extern.java.Log;
import org.triplea.generic.xml.reader.exceptions.JavaDataModelException;
import org.triplea.generic.xml.reader.exceptions.XmlParsingException;

@Log
public class XmlMapper implements Closeable {
  private XMLStreamReader xmlStreamReader;

  public XmlMapper(final InputStream inputStream) throws XMLStreamException {
    final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    xmlStreamReader = inputFactory.createXMLStreamReader(inputStream);
  }

  public <T> T mapXmlToObject(final Class<T> pojo) throws XmlParsingException {
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
        final String xmlAttributeValue = xmlStreamReader.getAttributeValue(null, field.getName());
        final Object value = new AttributeValueCasting(field).castAttributeValue(xmlAttributeValue);
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
      final XmlParser tagParser = new XmlParser(pojo.getSimpleName());

      // Set up tag parsing, as we scan through more elements when we see a matching
      // tag name we'll call the child tag handler. The child tag handler will
      // create a java model representing the child tag and set the field instance
      // on our current running instance object.
      for (final Field field : annotatedFields.getTagFields()) {
        final String expectedTagName = field.getType().getSimpleName();
        tagParser.childTagHandler(
            expectedTagName, () -> field.set(instance, mapXmlToObject(field.getType())));
      }

      // Set up tag list parsing, similar to tag parsing except we set the field
      // value to a list and each time we see a new child tag we'll add it back to that list.
      for (final Field field : annotatedFields.getTagListFields()) {
        final List<Object> tagList = new ArrayList<>();
        field.set(instance, tagList);
        final Class<?> listType = ReflectionUtils.getGenericType(field);
        tagParser.childTagHandler(
            listType.getSimpleName(), () -> tagList.add(mapXmlToObject(listType)));
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
      if (e instanceof XmlParsingException) {
        throw (XmlParsingException) e;
      } else {
        throw new XmlParsingException(xmlStreamReader, pojo, e);
      }
    }
  }

  @Override
  public void close() {
    try {
      xmlStreamReader.close();
    } catch (final XMLStreamException e) {
      log.log(Level.INFO, "Failed to close xml stream", e);
    }
  }
}
