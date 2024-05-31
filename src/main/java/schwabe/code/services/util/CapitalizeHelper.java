package schwabe.code.services.util;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Locale;

public class CapitalizeHelper implements Serializable {
    public static String convertCamelCaseToReadableName(String name) {
        return StringUtils.capitalize(StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(name), StringUtils.SPACE));
    }

    public static String convertToReadableName(String name) {
        return StringUtils.capitalize(name.toLowerCase(Locale.ROOT));
    }
}