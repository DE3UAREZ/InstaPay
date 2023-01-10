package eg.com.adcb.IPN.consts;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public  class ErrorsCode {
    HashMap<String, String> Errors = new HashMap<String, String>();

public static final String ERROR_11013= "11013";
public static final String ERROR_11013_DESC="Service is currently not available from your bank, try again later";
public static final String ERROR_11017="11017";
public static final String ERROR_11017_DESC="Invalid technical data";
public static final String ERROR_11005 ="11005";
public static final String ERROR_11005_DESC=  "Unsupported card, please use debit card or Meeza prepaid card";

public static final String ERROR_11014 ="11014";
public static final String ERROR_11014_DESC= "Duplicated transaction from your bank";

public static final String ERROR_11008 ="11008";
public static final String ERROR_11008_DESC =  "Selected mobile number is not registered at your bank, please update your information";

public static final String ERROR_11018 ="11018";
public static final String ERROR_11018_DESC = "Restricted account, please contact your bank";

public static final String ERROR_11001 ="11001";
public static final String ERROR_11001_DESC = "Invalid card number or PIN, please try again";

public static final String ERROR_11002 ="11002";
public static final String ERROR_11002_DESC ="Expired card, please contact your bank";
public static final String ERROR_11003 ="11003";
public static final String ERROR_11003_DESC ="inactive card, please contact your bank";
public static final String ERROR_11006 ="11016";
public static final String ERROR_11006_DESC = "PIN trial exceeded";
    public static final String ERROR_12010 ="12010";
    public static final String ERROR_12010_DESC ="inactive card, please contact your bank";

}
