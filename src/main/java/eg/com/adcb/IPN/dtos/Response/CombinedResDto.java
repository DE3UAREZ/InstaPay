package eg.com.adcb.IPN.dtos.Response;

public class CombinedResDto {


    public String requestId;
    public String timestamp;
    public String respCode;
    public String respDesc;
    public String balance;
    public String balanceCurr;
    public String authCode;
    public String name;
    public String address;
    public String idtype;
    public String idvalue;

public CheckStatusResDto checkstatusres=null;
    public ErrorsResDto errorres=null;

    /*
    public CombinedResDto(String requestId, String timestamp, String respCode, String respDesc, String balance, String balanceCurr, String authCode, String name, String address, String idtype, String idvalue) {
        this.requestId = requestId;
        this.timestamp = timestamp;
        this.respCode = respCode;
        this.respDesc = respDesc;
        this.balance = balance;
        this.balanceCurr = balanceCurr;
        this.authCode = authCode;
        this.name = name;
        this.address = address;
        this.idtype = idtype;
        this.idvalue = idvalue;
    }
*/



    public String CombinedSuccess(String requestId, String timestamp, String respCode, String respDesc, String balance, String balanceCurr, String authCode, String name, String address, String idtype, String idvalue) {
        return "CheckStatusResDto{" +
                "requestId='" + requestId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", respCode='" + respCode + '\'' +
                ", respDesc='" + respDesc + '\'' +
                ", balance='" + balance + '\'' +
                ", balanceCurr='" + balanceCurr + '\'' +
                ", authCode='" + authCode + '\'' +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", idtype='" + idtype + '\'' +
                ", idvalue='" + idvalue + '\'' +
                '}';
    }


    public String CombinedError(String requestId, String timestamp, String respCode, String respDesc){

        return "ErrorsResDto{" +
                "requestId='" + requestId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", respCode='" + respCode + '\'' +
                ", respDesc='" + respDesc + '\'' +
                '}';
    }


}
