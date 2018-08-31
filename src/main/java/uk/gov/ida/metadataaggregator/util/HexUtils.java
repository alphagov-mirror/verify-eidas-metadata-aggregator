package uk.gov.ida.metadataaggregator.util;

import org.apache.commons.codec.binary.Hex;

public class HexUtils {

    public static String encodeString(String value) {
        return Hex.encodeHexString(value.getBytes());
    }

}
