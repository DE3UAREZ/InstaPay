package eg.com.adcb.IPN.models;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity(name = "Logins")
public class Login {

    @Column(name = "CustomerReference")
    @Id
    private String customerReference;

    @Column(name = "RequestId")
    private String requestId;

    @Column(name = "Timestamp")
    private LocalDateTime timestamp;

    @Column(name = "MobileNumber")
    private String mobileNumber;

    @Column(name = "CustomerType")
    private String customerType;

    @Column(name = "SettlementCycleId")
    private String settlementCycleId;

    @Column(name = "LegalId")
    private String legalId;

    @Column(name = "DocumentType")
    private String documentType;

    @Column(name = "ApprovalToken")
    private String approvalToken;

    @Column(name = "Name")
    private String name;

    @Column(name = "Address")
    private String address;

    @Column(name = "accountid")
    private String accountid;


    public Login() {
    }


    public Login(String customerReference, String requestId, LocalDateTime timestamp, String mobileNumber, String customerType, String settlementCycleId, String legalId, String documentType, String approvalToken, String name, String address,String accountid) {
        this.customerReference = customerReference;
        this.requestId = requestId;
        this.timestamp = timestamp;
        this.mobileNumber = mobileNumber;
        this.customerType = customerType;
        this.settlementCycleId = settlementCycleId;
        this.legalId = legalId;
        this.documentType = documentType;
        this.approvalToken = approvalToken;
        this.name = name;
        this.address = address;
        this.accountid=accountid;
    }

    public String getAccountid() {
        return accountid;
    }

    public void setAccountid(String accountid) {
        this.accountid = accountid;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getCustomerType() {
        return customerType;
    }

    public void setCustomerType(String customerType) {
        this.customerType = customerType;
    }

    public String getSettlementCycleId() {
        return settlementCycleId;
    }

    public void setSettlementCycleId(String settlementCycleId) {
        this.settlementCycleId = settlementCycleId;
    }

    public String getCustomerReference() {
        return customerReference;
    }

    public void setCustomerReference(String customerReference) {
        this.customerReference = customerReference;
    }

    public String getLegalId() {
        return legalId;
    }

    public void setLegalId(String legalId) {
        this.legalId = legalId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getApprovalToken() {
        return approvalToken;
    }

    public void setApprovalToken(String approvalToken) {
        this.approvalToken = approvalToken;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "Login{" +
                "customerReference='" + customerReference + '\'' +
                ", requestId='" + requestId + '\'' +
                ", timestamp=" + timestamp +
                ", mobileNumber='" + mobileNumber + '\'' +
                ", customerType='" + customerType + '\'' +
                ", settlementCycleId='" + settlementCycleId + '\'' +
                ", legalId='" + legalId + '\'' +
                ", documentType='" + documentType + '\'' +
                ", approvalToken='" + approvalToken + '\'' +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Login login = (Login) o;

        return Objects.equals(customerReference, login.customerReference);
    }

    @Override
    public int hashCode() {
        return customerReference != null ? customerReference.hashCode() : 0;
    }
}
