package games.strategy.net;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * MultiPartFormOutputStream is used to write
 * "multipart/form-data" to a java.net.URLConnection for
 * POSTing. This is primarily for file uploading to HTTP servers.
 */
public class MultiPartFormOutputStream {
  /**
   * The line end characters.
   */
  private static final String NEWLINE = "\r\n";
  /**
   * The boundary prefix.
   */
  private static final String PREFIX = "--";
  /**
   * The output stream to write to.
   */
  private DataOutputStream out = null;
  /**
   * The multipart boundary string.
   */
  private String boundary = null;

  /**
   * Creates a new <code>MultiPartFormOutputStream</code> object using
   * the specified output stream and boundary. The boundary is required
   * to be created before using this method, as described in the
   * description for the <code>getContentType(String)</code> method.
   * The boundary is only checked for <code>null</code> or empty string,
   * but it is recommended to be at least 6 characters. (Or use the
   * static createBoundary() method to create one.)
   *
   * @param os
   *        the output stream
   * @param boundary
   *        the boundary
   */
  public MultiPartFormOutputStream(final OutputStream os, final String boundary) {
    if (os == null) {
      throw new IllegalArgumentException("Output stream is required.");
    }
    if (boundary == null || boundary.length() == 0) {
      throw new IllegalArgumentException("Boundary stream is required.");
    }
    this.out = new DataOutputStream(os);
    this.boundary = boundary;
  }

  /**
   * Writes an boolean field value.
   *
   * @param name
   *        the field name (required)
   * @param value
   *        the field value
   * @throws java.io.IOException
   *         on input/output errors
   */
  public void writeField(final String name, final boolean value) throws java.io.IOException {
    writeField(name, Boolean.toString(value));
  }

  /**
   * Writes an double field value.
   *
   * @param name
   *        the field name (required)
   * @param value
   *        the field value
   * @throws java.io.IOException
   *         on input/output errors
   */
  public void writeField(final String name, final double value) throws java.io.IOException {
    writeField(name, Double.toString(value));
  }

  /**
   * Writes an float field value.
   *
   * @param name
   *        the field name (required)
   * @param value
   *        the field value
   * @throws java.io.IOException
   *         on input/output errors
   */
  public void writeField(final String name, final float value) throws java.io.IOException {
    writeField(name, Float.toString(value));
  }

  /**
   * Writes an long field value.
   *
   * @param name
   *        the field name (required)
   * @param value
   *        the field value
   * @throws java.io.IOException
   *         on input/output errors
   */
  public void writeField(final String name, final long value) throws java.io.IOException {
    writeField(name, Long.toString(value));
  }

  /**
   * Writes an int field value.
   *
   * @param name
   *        the field name (required)
   * @param value
   *        the field value
   * @throws java.io.IOException
   *         on input/output errors
   */
  public void writeField(final String name, final int value) throws java.io.IOException {
    writeField(name, Integer.toString(value));
  }

  /**
   * Writes an short field value.
   *
   * @param name
   *        the field name (required)
   * @param value
   *        the field value
   * @throws java.io.IOException
   *         on input/output errors
   */
  public void writeField(final String name, final short value) throws java.io.IOException {
    writeField(name, Short.toString(value));
  }

  /**
   * Writes an char field value.
   *
   * @param name
   *        the field name (required)
   * @param value
   *        the field value
   * @throws java.io.IOException
   *         on input/output errors
   */
  public void writeField(final String name, final char value) throws java.io.IOException {
    writeField(name, Character.toString(value));
  }

  /**
   * Writes an string field value. If the value is null, an empty string
   * is sent ("").
   *
   * @param name
   *        the field name (required)
   * @param value
   *        the field value
   * @throws java.io.IOException
   *         on input/output errors
   */
  public void writeField(final String name, String value) throws java.io.IOException {
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null or empty.");
    }
    if (value == null) {
      value = "";
    }
    /*
     * --boundary\r\n
     * Content-Disposition: form-data; name="<fieldName>"\r\n
     * \r\n
     * <value>\r\n
     */
    // write boundary
    out.writeBytes(PREFIX);
    out.writeBytes(boundary);
    out.writeBytes(NEWLINE);
    // write content header
    out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"");
    out.writeBytes(NEWLINE);
    out.writeBytes(NEWLINE);
    // write content
    out.writeBytes(value);
    out.writeBytes(NEWLINE);
    out.flush();
  }

  /**
   * Writes a file's contents. If the file is null, does not exists, or
   * is a directory, a <code>java.lang.IllegalArgumentException</code> will be thrown.
   *
   * @param name
   *        the field name
   * @param mimeType
   *        the file content type (optional, recommended)
   * @param file
   *        the file (the file must exist)
   * @throws java.io.IOException
   *         on input/output errors
   */
  public void writeFile(final String name, final String mimeType, final File file) throws java.io.IOException {
    if (file == null) {
      throw new IllegalArgumentException("File cannot be null.");
    }
    if (!file.exists()) {
      throw new IllegalArgumentException("File does not exist.");
    }
    if (file.isDirectory()) {
      throw new IllegalArgumentException("File cannot be a directory.");
    }
    writeFile(name, mimeType, file.getCanonicalPath(), new FileInputStream(file));
  }

  /**
   * Writes a input stream's contents. If the input stream is null, a <code>java.lang.IllegalArgumentException</code>
   * will be thrown.
   *
   * @param name
   *        the field name
   * @param mimeType
   *        the file content type (optional, recommended)
   * @param fileName
   *        the file name (required)
   * @param is
   *        the input stream
   * @throws java.io.IOException
   *         on input/output errors
   */
  public void writeFile(final String name, final String mimeType, final String fileName, final InputStream is)
      throws java.io.IOException {
    if (is == null) {
      throw new IllegalArgumentException("Input stream cannot be null.");
    }
    if (fileName == null) {
      throw new IllegalArgumentException("File name cannot be null.");
    }
    /*
     * --boundary\r\n
     * Content-Disposition: form-data; name="<fieldName>"; filename="<filename>"\r\n
     * Content-Type: <mime-type>\r\n
     * \r\n
     * <file-data>\r\n
     */
    // write boundary
    out.writeBytes(PREFIX);
    out.writeBytes(boundary);
    out.writeBytes(NEWLINE);
    // write content header
    out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"");
    out.writeBytes(NEWLINE);
    if (mimeType != null) {
      out.writeBytes("Content-Type: " + mimeType);
      out.writeBytes(NEWLINE);
    }
    out.writeBytes(NEWLINE);
    // write content
    final byte[] data = new byte[1024];
    int r = 0;
    while ((r = is.read(data, 0, data.length)) != -1) {
      out.write(data, 0, r);
    }
    // close input stream, but ignore any possible exception for it
    try {
      is.close();
    } catch (final Exception e) {
    }
    out.writeBytes(NEWLINE);
    out.flush();
  }

  /**
   * Writes the given bytes. The bytes are assumed to be the contents
   * of a file, and will be sent as such. If the data is null, a <code>java.lang.IllegalArgumentException</code> will be
   * thrown.
   *
   * @param name
   *        the field name
   * @param mimeType
   *        the file content type (optional, recommended)
   * @param fileName
   *        the file name (required)
   * @param data
   *        the file data
   * @throws java.io.IOException
   *         on input/output errors
   */
  public void writeFile(final String name, final String mimeType, final String fileName, final byte[] data)
      throws java.io.IOException {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null.");
    }
    if (fileName == null) {
      throw new IllegalArgumentException("File name cannot be null.");
    }
    /*
     * --boundary\r\n
     * Content-Disposition: form-data; name="<fieldName>"; filename="<filename>"\r\n
     * Content-Type: <mime-type>\r\n
     * \r\n
     * <file-data>\r\n
     */
    // write boundary
    out.writeBytes(PREFIX);
    out.writeBytes(boundary);
    out.writeBytes(NEWLINE);
    // write content header
    out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"");
    out.writeBytes(NEWLINE);
    if (mimeType != null) {
      out.writeBytes("Content-Type: " + mimeType);
      out.writeBytes(NEWLINE);
    }
    out.writeBytes(NEWLINE);
    // write content
    out.write(data, 0, data.length);
    out.writeBytes(NEWLINE);
    out.flush();
  }

  /**
   * Flushes the stream. Actually, this method does nothing, as the only
   * write methods are highly specialized and automatically flush.
   *
   * @throws java.io.IOException
   *         on input/output errors
   */
  public void flush() throws java.io.IOException {
    // out.flush();
  }

  /**
   * Closes the stream. <br />
   * <br />
   * <b>NOTE:</b> This method <b>MUST</b> be called to finalize the
   * multipart stream.
   *
   * @throws java.io.IOException
   *         on input/output errors
   */
  public void close() throws java.io.IOException {
    // write final boundary
    out.writeBytes(PREFIX);
    out.writeBytes(boundary);
    out.writeBytes(PREFIX);
    out.writeBytes(NEWLINE);
    out.flush();
    out.close();
  }

  /**
   * Gets the multipart boundary string being used by this stream.
   *
   * @return the boundary
   */
  public String getBoundary() {
    return this.boundary;
  }
}
