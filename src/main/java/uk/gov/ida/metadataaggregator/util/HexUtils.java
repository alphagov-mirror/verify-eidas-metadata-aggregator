package uk.gov.ida.metadataaggregator.util;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class HexUtils {

    public static String encodeString(String value) {
        return Hex.encodeHexString(value.getBytes());
    }

    public static String decodeString(String value) throws DecoderException {
        return new String(Hex.decodeHex(value.toCharArray()));
    }

}
