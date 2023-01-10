package eg.com.adcb.IPN.dtos.Response;


import eg.com.adcb.IPN.models.Account;

import java.util.Dictionary;
import java.util.List;
import java.util.Map;

public class GetAccountsResDto {
    public String requestId;
    public String timestamp;
    public String respCode;
    public String respDesc;
    public String customerReference;
    public String customerIdType;
    public String customerIdValue;
    public Map<String, Object> accountList;


    public GetAccountsResDto(String requestId, String timestamp, String respCode, String respDesc) {
        this.requestId = requestId;
        this.timestamp = timestamp;
        this.respCode = respCode;
        this.respDesc = respDesc;
    }
    public GetAccountsResDto(String requestId, String timestamp, String respCode, String respDesc, String customerReference, String customerIdType, String customerIdValue, Map<String, Object> accountList) {
        this.requestId = requestId;
        this.timestamp = timestamp;
        this.respCode = respCode;
        this.respDesc = respDesc;
        this.customerReference = customerReference;
        this.customerIdType = customerIdType;
        this.customerIdValue = customerIdValue;
        this.accountList = accountList;

    }

    @Override
    public String toString() {
        return "GetAccountsResDto{" +
                "requestId='" + requestId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", respCode='" + respCode + '\'' +
                ", respDesc='" + respDesc + '\'' +
                ", customerReference='" + customerReference + '\'' +
                ", customerIdType='" + customerIdType + '\'' +
                ", customerIdValue='" + customerIdValue + '\'' +
                ", accountList=" + accountList +
                '}';
    }

//        else {
//        return "GetAccountsResDto{" +
//                "requestId='" + "asdasdasdas" + '\'' +
//                ", timestamp='" + timestamp + '\'' +
//                ", respCode='" + respCode + '\'' +
//                ", respDesc='" + respDesc + '\'' +
//                ", customerReference='" + customerReference + '\'' +
//                ", customerIdType='" + customerIdType + '\'' +
//                ", customerIdValue='" + customerIdValue + '\'' +
//                ", accountList=" + accountList +
//                '}';
//        }
//    }
}
