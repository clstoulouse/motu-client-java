package cls.atoll.motu.utils;

/**
 * @author vpignot
 * <p>
 * List of getMessages used into the DUCK tool
 */
public final class MotuConstants {

    private MotuConstants() {
        // helper class do nothing
    }

    // XML content
    public static final String XML_TAG_TIME_COVERAGE = "timeCoverage";
    public static final String XML_TAG_AVAILABLE_TIMES = "availableTimes";
    public static final String XML_TAG_AVAILABLE_DEPTHS = "availableDepths";
    public static final String XML_TAG_VARIABLES = "variables";
    public static final String XML_TAG_VARIABLE = "variable";
    public static final String XML_TAG_GEO_COV = "dataGeospatialCoverage";
    public static final String XML_TAG_AXIS = "axis";
    public static final String XML_ATT_STANDARD = "standardName";

    public static final String DATE_FORMAT_LONG = "yyyy-MM-dd[['T'][ ]HH:mm[:ss[.SSS]]['Z']]";
}
