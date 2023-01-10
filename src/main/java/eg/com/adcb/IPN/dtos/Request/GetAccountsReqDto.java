package eg.com.adcb.IPN.dtos.Request;

public class GetAccountsReqDto {

    public String requestId;
    public String timestamp;
    public String settlementCycleId;
    public String mobileNumber;
    public String nationalId;
    public String customerType;
    public String customerReference;
    public String authenticationRequestId;
    public String authenticationApprovalToken;

    @Override
    public String toString() {
        return "GetAccountsReqDto{" +
                "requestId='" + requestId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", settlementCycleId='" + settlementCycleId + '\'' +
                ", mobileNumber='" + mobileNumber + '\'' +
                ", nationalId='" + nationalId + '\'' +
                ", customerType='" + customerType + '\'' +
                ", customerReference='" + customerReference + '\'' +
                ", authenticationRequestId='" + authenticationRequestId + '\'' +
                ", authenticationApprovalToken='" + authenticationApprovalToken + '\'' +
                '}';
    }
}
