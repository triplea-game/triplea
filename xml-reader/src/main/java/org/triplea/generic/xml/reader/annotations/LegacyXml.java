package org.triplea.generic.xml.reader.annotations;

/**
 * Marker annotation to indicate that an item is legacy, we keep it around so old XMLs will continue
 * to work, but the annotated entity is not "the preferred way to do it."
 */
public @interface LegacyXml {}
