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

    public static String tryGetFileExtension(String fileName) {
        if (fileName == null)
            return null;
        var split = fileName.split("\\.");
        return split.length > 1 ? split[split.length - 1] : "";
    }
}
