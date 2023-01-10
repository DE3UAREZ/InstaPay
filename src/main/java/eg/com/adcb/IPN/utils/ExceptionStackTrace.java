package eg.com.adcb.IPN.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionStackTrace {
    public static String GetStackTrace(Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }
}
