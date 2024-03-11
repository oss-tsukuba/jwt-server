package jp.co.canaly.mcp.util;

import java.util.ResourceBundle;

public class PropUtils {

    private static ResourceBundle rb = ResourceBundle.getBundle("system");

    public static String getValue(String key) {
        return rb.getString(key);
    }

}
