package eg.com.adcb.IPN.dtos.Response;

public class AuthenticateResDto {
    public String requestId;
    public String timestamp;
    public String respCode;
    public String respDesc;
    public String customerReference;
    public String authenticationApprovalToken;

    public AuthenticateResDto(String requestId, String timestamp, String respCode, String respDesc, String customerReference, String authenticationApprovalToken) {
        this.requestId = requestId;
        this.timestamp = timestamp;
        this.respCode = respCode;
        this.respDesc = respDesc;
        this.customerReference = customerReference;
        this.authenticationApprovalToken = authenticationApprovalToken;
    }


    @Override
    public String toString() {
        return "AuthenticateResDto{" +
                "requestId='" + requestId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", respCode='" + respCode + '\'' +
                ", respDesc='" + respDesc + '\'' +
                ", customerReference='" + customerReference + '\'' +
                ", authenticationApprovalToken='" + authenticationApprovalToken + '\'' +
                '}';
    }


}
