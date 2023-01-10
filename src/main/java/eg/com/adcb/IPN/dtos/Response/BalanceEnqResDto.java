package eg.com.adcb.IPN.dtos.Response;

import java.util.List;
import java.util.Map;

public class BalanceEnqResDto {
    public String requestId;
    public String timestamp;
    public String respCode;
    public String respDesc;
    public String balance;
    public String balanceCurr;
    public List<Map<String, String>> history;

    public BalanceEnqResDto(String requestId, String timestamp, String respCode, String respDesc, String balance, String balanceCurr, List<Map<String, String>> history) {
        this.requestId = requestId;
        this.timestamp = timestamp;
        this.respCode = respCode;
        this.respDesc = respDesc;
        this.balance = balance;
        this.balanceCurr = balanceCurr;
        this.history = history;
    }
        public BalanceEnqResDto(String requestId, String timestamp, String respCode, String respDesc, String balance, String balanceCurr) {
        this.requestId = requestId;
        this.timestamp = timestamp;
        this.respCode = respCode;
        this.respDesc = respDesc;
        this.balance = balance;
        this.balanceCurr = balanceCurr;

    }


    @Override
    public String toString() {
        if (history==null){
            return "BalanceEnqResDto{" +
                    "requestId='" + requestId + '\'' +
                    ", timestamp='" + timestamp + '\'' +
                    ", respCode='" + respCode + '\'' +
                    ", respDesc='" + respDesc + '\'' +
                    ", balance='" + balance + '\'' +
                    ", balanceCurr='" + balanceCurr + '\'' +
                    '}';
        }
        return "BalanceEnqResDto{" +
                "requestId='" + requestId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", respCode='" + respCode + '\'' +
                ", respDesc='" + respDesc + '\'' +
                ", balance='" + balance + '\'' +
                ", balanceCurr='" + balanceCurr + '\'' +
                ", history=" + history +
                '}';
    }
}
