package eg.com.adcb.IPN.dtos.Request;

import com.sun.istack.NotNull;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

public class AuthenticateReqDto {

    public String requestId;
    public String timestamp;
    public String customerType;
    public String customerReference;
    public String settlementCycleId;
    public String mobileNumber;
    public Map<String, String> ac;
    public Map<String, String> credmessage;

    @Override
    public String toString() {
        return "Authenticate{" +
                "requestId='" + requestId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", customerType='" + customerType + '\'' +
                ", customerReference='" + customerReference + '\'' +
                ", settlementCycleId='" + settlementCycleId + '\'' +
                ", mobileNumber='" + mobileNumber + '\'' +
                ", ac=" + ac +
                ", credmessage=" + credmessage +
                '}';
    }
}

