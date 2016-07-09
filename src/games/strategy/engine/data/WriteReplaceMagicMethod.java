package games.strategy.engine.data;


/**
 * Annotation to mark magic 'writeReplace' methods that are used to support the serialization proxy pattern:
 * - http://blog.codefx.org/design/patterns/serialization-proxy-pattern/
 * - https://dzone.com/articles/serialization-proxy-pattern
 * - http://vard-lokkur.blogspot.com/2014/06/serialization-proxy-pattern-example.html
 *
 * A typical usage of this annotation will look like this:
 * <code>
 *    @WriteReplaceMagicMethod
 *    public Object writeReplace() {
 *      return new SerializationProxy(this);
 *    }
 * </code>
 */
public @interface WriteReplaceMagicMethod {
}
