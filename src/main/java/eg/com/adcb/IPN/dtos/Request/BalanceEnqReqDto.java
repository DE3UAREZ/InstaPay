package eg.com.adcb.IPN.dtos.Request;

public class BalanceEnqReqDto {
    public String requestId;
    public String timestamp;
    public String settlementCycleId;
    public String lastTransactions;
    public String mobileNumber;
    public String rrn;
    public String accountId;
    public String accountType;
    public String schemeId;

    @Override
    public String toString() {
        return "BalanceEnqReqDto{" +
                "requestId='" + requestId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", settlementCycleId='" + settlementCycleId + '\'' +
                ", lastTransactions='" + lastTransactions + '\'' +
                ", mobileNumber='" + mobileNumber + '\'' +
                ", rrn='" + rrn + '\'' +
                ", accountId='" + accountId + '\'' +
                ", accountType='" + accountType + '\'' +
                ", schemeId='" + schemeId + '\'' +
                '}';
    }
}
