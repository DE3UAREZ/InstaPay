package eg.com.adcb.IPN.dtos.Request;

import java.util.Map;

public class FundTransferReqDto {

    public String requestId;
    public String payerMobileNumber;
    public String payeeMobileNumber;
    public String timestamp;
    public String settlementCycleId;
    public String requestType;
    public String transactionId;
    public String msgId;
    public String orgTxnId;
    public String txnType;
    public String debitAccountType;
    public String creditAccountType;
    public String rrn;
    public String debitAccountBankId;
    public String debitAccountId;
    public String creditAccountBankId;
    public String creditAccountId;
    public String creditPoolAccount;
    public String debitPoolAccount;
    public String payerIpa;
    public String payerName;
    public String payerType;
    public String payerMerchantId;
    public String payerMcc;
    public String payerMerchantType;
    public String payerMerchantSubType;
    public String payeeIpa;
    public String payeeName;
    public String payeeType;
    public String payeeMerchantId;
    public String payeeMcc;
    public String payeeMerchantType;
    public String payeeMerchantSubType;
    public String blockId;
    public String schemeId;
    public String merchantRef;
    public String atmID;
    public String atmLocation;
    public String blockExpDateTime;
    public String mandateID;
    public String allowPartialCollect;


    public Map<String, String> pspFee;
    public Map<String, String> bankFee;
    public Map<String, String> bearFee;
    public Map<String, String> amount;
    public Map<String, String> settAmount;
    public Map<String, String> interChange;
    public Map<String, String> convenienceFee;
    public Map<String, String> merchantMDR;
    public Map<String, String> remainingHoldAmount;

    @Override
    public String toString() {
        return "FundTransferReqDto{" +
                "requestId='" + requestId + '\'' +
                ", payerMobileNumber='" + payerMobileNumber + '\'' +
                ", payeeMobileNumber='" + payeeMobileNumber + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", settlementCycleId='" + settlementCycleId + '\'' +
                ", requestType='" + requestType + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", msgId='" + msgId + '\'' +
                ", orgTxnId='" + orgTxnId + '\'' +
                ", txnType='" + txnType + '\'' +
                ", debitAccountType='" + debitAccountType + '\'' +
                ", creditAccountType='" + creditAccountType + '\'' +
                ", rrn='" + rrn + '\'' +
                ", debitAccountBankId='" + debitAccountBankId + '\'' +
                ", debitAccountId='" + debitAccountId + '\'' +
                ", creditAccountBankId='" + creditAccountBankId + '\'' +
                ", creditAccountId='" + creditAccountId + '\'' +
                ", creditPoolAccount='" + creditPoolAccount + '\'' +
                ", debitPoolAccount='" + debitPoolAccount + '\'' +
                ", payerIpa='" + payerIpa + '\'' +
                ", payerName='" + payerName + '\'' +
                ", payerType='" + payerType + '\'' +
                ", payerMerchantId='" + payerMerchantId + '\'' +
                ", payerMcc='" + payerMcc + '\'' +
                ", payerMerchantType='" + payerMerchantType + '\'' +
                ", payerMerchantSubType='" + payerMerchantSubType + '\'' +
                ", payeeIpa='" + payeeIpa + '\'' +
                ", payeeName='" + payeeName + '\'' +
                ", payeeType='" + payeeType + '\'' +
                ", payeeMerchantId='" + payeeMerchantId + '\'' +
                ", payeeMcc='" + payeeMcc + '\'' +
                ", payeeMerchantType='" + payeeMerchantType + '\'' +
                ", payeeMerchantSubType='" + payeeMerchantSubType + '\'' +
                ", blockId='" + blockId + '\'' +
                ", schemeId='" + schemeId + '\'' +
                ", merchantRef='" + merchantRef + '\'' +
                ", atmID='" + atmID + '\'' +
                ", atmLocation='" + atmLocation + '\'' +
                ", blockExpDateTime='" + blockExpDateTime + '\'' +
                ", mandateID='" + mandateID + '\'' +
                ", allowPartialCollect='" + allowPartialCollect + '\'' +
                ", pspFee=" + pspFee +
                ", bankFee=" + bankFee +
                ", bearFee=" + bearFee +
                ", amount=" + amount +
                ", settAmount=" + settAmount +
                ", interChange=" + interChange +
                ", convenienceFee=" + convenienceFee +
                ", merchantMDR=" + merchantMDR +
                ", remainingHoldAmount=" + remainingHoldAmount +
                '}';
    }
}
