package games.strategy.triplea;

/**
 * This annotation indicates a class is used by name in Map XMLs.
 * <br />
 * TODO: we should instead map these names in code, this way we can update the code without breaking XMLs
 *
 * <p>
 * As a tough example:
 * <delegate name="politics" javaClass="games.strategy.triplea.delegate.PoliticsDelegate" display="Politics"/>
 * </p>
 */
public @interface MapSupport {
}
