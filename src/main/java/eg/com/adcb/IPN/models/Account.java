package eg.com.adcb.IPN.models;


public class Account {

    private final String type;
    private final String subType;
    private final String accountId;
    private final String maskedAccount;
    private final String name;
    private final String authType;
    private final String authPINLen;

    public Account(String type, String subType, String accountId, String name, String authType, String authPINLen) {
        this.type = type;
        this.subType = subType;
        this.accountId = accountId;
        this.name = name;
        this.authType = authType;
        this.authPINLen = authPINLen;
        maskedAccount = null;
    }
    public Account(String type, String subType, String accountId,String maskedAccount, String name, String authType, String authPINLen) {
        this.type = type;
        this.subType = subType;
        this.accountId = accountId;
        this.name = name;
        this.authType = authType;
        this.authPINLen = authPINLen;
        this.maskedAccount=maskedAccount;
    }
    public String getSubType() {
        return subType;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public String getAuthType() {
        return authType;
    }

    public String getAuthPINLen() {
        return authPINLen;
    }

    public String getType() {
        return this.type;
    }

    public String getMaskedAccount() {
        return this.maskedAccount;
    }

    @Override
    public String toString() {
        if (maskedAccount==null){
            return "Account{" +
                    "type='" + type + '\'' +
                    ", subType='" + subType + '\'' +
                    ", accountId='" + accountId + '\'' +
                    ", name='" + name + '\'' +
                    ", authType='" + authType + '\'' +
                    ", authPINLen='" + authPINLen + '\'' +
                    '}';
        }
        else{
            return "Account{" +
                    "type='" + type + '\'' +
                    ", subType='" + subType + '\'' +
                    ", accountId='" + accountId + '\'' +
                    ", maskedaccountId='" + accountId + '\'' +
                    ", name='" + name + '\'' +
                    ", authType='" + authType + '\'' +
                    ", authPINLen='" + authPINLen + '\'' +
                    '}';
        }

    }
}
