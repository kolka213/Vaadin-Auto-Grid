package schwabe.code.services.util;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Locale;

/**
 * Static class that helps with string rendering.
 */
public class CapitalizeHelper implements Serializable {

    /**
     * Converts a camel-case string to a readable capitalized string,
     * like <p><strong>sTring -> String.</strong></p>
     * @param string the input to modify
     * @return capitalized readable string
     */
    public static String convertCamelCaseToReadableName(String string) {
        return StringUtils.capitalize(StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(string), StringUtils.SPACE));
    }

    /**
     * Convert any String to capitalize the first letter and lower case all remaining letters,
     * like <p><strong>sTring_ -> String_.</strong></p>
     * @param string the input to modify
     * @return capitalized readable string
     */
    public static String convertToReadableName(String string) {
        return StringUtils.capitalize(string.toLowerCase(Locale.ROOT));
    }
}