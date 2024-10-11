package util;

public class Utilities {
    public static String byteArrayToHexString(byte[] b) {
        StringBuffer resultSb = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            resultSb.append(String.format("%02x", b[i]));
        }
        return resultSb.toString();
    }

    public static String stringToHexString(String s) {
        return byteArrayToHexString(s.getBytes());
    }
}
