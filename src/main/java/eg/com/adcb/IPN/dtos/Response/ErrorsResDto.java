package eg.com.adcb.IPN.dtos.Response;

import eg.com.adcb.IPN.controllers.AccountsController;
import eg.com.adcb.IPN.models.RejectedLog;
import eg.com.adcb.IPN.repositories.RejectedLogsRepository;

import java.time.LocalDateTime;
import java.util.Map;

public class ErrorsResDto   {

    public String requestId;
    public String timestamp;
    public String respCode;
    public String respDesc;


    public ErrorsResDto(String requestId, String timestamp, String respCode, String respDesc) {

        this.requestId = requestId;
        this.timestamp = timestamp;
        this.respCode = respCode;
        this.respDesc = respDesc;

    }

    @Override
    public String toString() {
        return "ErrorsResDto{" +
                "requestId='" + requestId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", respCode='" + respCode + '\'' +
                ", respDesc='" + respDesc + '\'' +
                '}';
    }
}
