package eg.com.adcb.IPN.repositories;

import eg.com.adcb.IPN.consts.AccountSubType;
import eg.com.adcb.IPN.consts.AccountType;
import eg.com.adcb.IPN.consts.ApiUrl;
import eg.com.adcb.IPN.consts.AuthType;
import eg.com.adcb.IPN.dtos.Response.ErrorsResDto;
import eg.com.adcb.IPN.models.Account;
import eg.com.adcb.IPN.models.Card;
import eg.com.adcb.IPN.models.Login;
import eg.com.adcb.IPN.utils.Request;

import eg.com.adcb.IPN.utils.XMLParser;
import org.springframework.core.env.Environment;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AccountsRepository {
    private Environment env;
    private AcceptedLogsRepository acceptedLogsRepository;
    private RejectedLogsRepository rejectedLogsRepository;
    private LoginsRepository loginsRepository;
    private CardsRepository cardsRepository;
    private ErrorsRepository errorsRepository;

    public AccountsRepository(Environment env,AcceptedLogsRepository acceptedLogsRepository, RejectedLogsRepository rejectedLogsRepository, LoginsRepository loginsRepository, CardsRepository cardsRepository,ErrorsRepository errorsRepository) {
        this.env = env;
        this.acceptedLogsRepository = acceptedLogsRepository;
        this.rejectedLogsRepository = rejectedLogsRepository;
        this.loginsRepository = loginsRepository;
        this.cardsRepository = cardsRepository;
        this.errorsRepository=errorsRepository;
    }

    public String getCustomerNumberByLegalId(String legalId) throws Exception {

        String body = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:dat=\"http://ws.wso2.org/dataservice\">" +
                "   <soap:Header/>" +
                "   <soap:Body>" +
                "      <dat:GETCUSTOMERBYID>" +
                "         <dat:LEGALID>" + legalId + "</dat:LEGALID>" +
                "      </dat:GETCUSTOMERBYID>" +
                "   </soap:Body>" +
                "</soap:Envelope>";

        String responseBody = Request.SendRequest(env.getProperty("IPNBaseURL"), "GETCUSTOMERBYID", body);
        JsonObject jsonResponse = new Gson().fromJson(responseBody, JsonObject.class);

        return jsonResponse
                .getAsJsonObject("CUSTDETAILS")
                .getAsJsonArray("CUSTDETAILSROW")
                .get(0)
                .getAsJsonObject()
                .get("CUSTNO")
                .getAsString();

    }

    public List<Account> getCustomerAccounts(String customerNumber) throws Exception {
        try {
            String body = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                    "   <soap:Header/>" +
                    "   <soap:Body>" +
                    "      <dat:ACCTDETAILS>" +
                    "         <dat:CUSTNO>" + customerNumber + "</dat:CUSTNO>" +
                    "      </dat:ACCTDETAILS>" +
                    "   </soap:Body>" +
                    "</soap:Envelope>";

            String responseBody = Request.SendRequest(env.getProperty("IPNBaseURL"), "ACCTDETAILS", body);

            Document doc = XMLParser.ReadXML(responseBody);

            String name = doc.getElementsByTagName("NAMCUST").item(0).getTextContent();

            NodeList nodes = doc.getElementsByTagName("ACCOUNTROW");
            List<Account> accounts = new ArrayList<>();
            String accountType = AccountType.ACCOUNT;
            String accountSubType = null;
            String curr="";
            String IBAN="";
            String status="";
            String accountNumber="";
            for (int i = 0; i < nodes.getLength(); ++i) {
                Node node = nodes.item(i);
                NodeList childNodes = node.getChildNodes();
                IBAN="";
                status="";
                for (int j = 0; j < childNodes.getLength(); ++j) {

                    Node child = childNodes.item(j);
                    if (child.getNodeName().equals("ACCCCY")) {
                        curr = child.getTextContent();
                    }
                    else if (child.getNodeName().equals("ACCNO")) {
                                  accountNumber = child.getTextContent();


                                  //  String type4digits = accountNumber.substring(8, 12);
                                    String type2digits = accountNumber.substring(8, 10);

                                    if ( type2digits.equals("10")) {
                                        accountSubType = AccountSubType.CHECKING;
                                    } else if (type2digits.equals("11")) {
                                        accountSubType = AccountSubType.OVERDRAFT;
                                    } else {
                                        accountSubType = AccountSubType.SAVING;
                                    }
                                    //String maskedAccountNumber=accountNumber.substring(0,4)+"****"+accountNumber.substring(accountNumber.length()-4);

                    }

                   else if (child.getNodeName().equals("IBAN")){
                        IBAN=child.getTextContent();
                    }
                   else if (child.getNodeName().equals("STATUS")){
                       status=child.getTextContent();
                    }
                   if (accountSubType!=null&&curr.equals("EGP")&&status.equals("E")&&!IBAN.equals("")){
                       //String maskedAccountNumber=IBAN.substring(0,4)+"****"+IBAN.substring(IBAN.length()-4);
                       Account account = new Account(accountType, accountSubType, IBAN, name, AuthType.ATMPIN, "4");
                       //continueFor=false;
                       IBAN="";
                       status="";
                       accounts.add(account);


                   }


                }

            }
            return accounts;
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }
    public List<Account> getCustomerMDPCard(String customerNumber,String last4) throws Exception {
        try {
            // ! TODO: Customer may have more than one card
            List<Account> accounts = new ArrayList<>();

            String body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:cus=\"http://ws.wso2.org/dataservice/customerCardList\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <cus:_getcustomercardlist>\n" +
                    "         <cus:customerNum>"+ customerNumber +"</cus:customerNum>\n" +
                    "      </cus:_getcustomercardlist>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope>";
            String responseBody = Request.SendRequest(env.getProperty("MDPBaseURL"), "_getcustomercardlist", body);

            // parse returned XML
            Document doc = XMLParser.ReadXML(responseBody);
            String innerXML = doc.getElementsByTagName("response").item(0).getTextContent();
            doc = XMLParser.ReadXML(innerXML);
            NodeList nodes = doc.getElementsByTagName("result");
            for (int i = 0; i < nodes.getLength(); ++i) {
                Node node = nodes.item(i);
                NodeList childNodes = node.getChildNodes();
                boolean continueFor=true;
                String id="";
                String cardHolderName="";
                String maskedCard="";
                boolean sameCard=false;
                for (int j = 0; j < childNodes.getLength()&&continueFor; ++j) {
                    Node child = childNodes.item(j);
                    if (child.getNodeName().equals("cardNumber")){id=child.getTextContent();}
                    else if (child.getNodeName().equals("cardholderName")){cardHolderName=child.getTextContent();}
                    else if (child.getNodeName().equals("cardMask")){
                        maskedCard=child.getTextContent();
                        if (maskedCard.substring(maskedCard.length()-4).equals(last4)){
                            sameCard=true;
                        }
                    }
                    if (!id.equals("") && !cardHolderName.equals("")&& sameCard){
                        Account account = new Account(AccountType.CARD, AccountSubType.PREPAID, id,maskedCard, cardHolderName, AuthType.ATMPIN, "4");
                        accounts.add(account);
                        continueFor=false;
                        id="";
                        cardHolderName="";
                    }
                }
            }

            //String id = doc.getElementsByTagName("id").item(0).getTextContent();
            //String cardHolderName = doc.getElementsByTagName("cardholderName").item(0).getTextContent();
            //String maskedAccountNumber=id.substring(0,4)+"****"+id.substring(id.length()-4);
            //Account account = new Account(AccountType.CARD, AccountSubType.PREPAID, id, cardHolderName, AuthType.ATMPIN, "4");

            return accounts;
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }
    public List<Account> getCustomerMDPCards(String customerNumber) throws Exception {
        try {
            // ! TODO: Customer may have more than one card
            List<Account> accounts = new ArrayList<>();

            String body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:cus=\"http://ws.wso2.org/dataservice/customerCardList\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <cus:_getcustomercardlist>\n" +
                    "         <cus:customerNum>"+ customerNumber +"</cus:customerNum>\n" +
                    "      </cus:_getcustomercardlist>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope>";
            String responseBody = Request.SendRequest(env.getProperty("MDPBaseURL"), "_getcustomercardlist", body);

            // parse returned XML
            Document doc = XMLParser.ReadXML(responseBody);
            String innerXML = doc.getElementsByTagName("response").item(0).getTextContent();
            doc = XMLParser.ReadXML(innerXML);
            NodeList nodes = doc.getElementsByTagName("result");
            for (int i = 0; i < nodes.getLength(); ++i) {
                Node node = nodes.item(i);
                NodeList childNodes = node.getChildNodes();
                boolean continueFor=true;
                String id="";
                String maskedCard="";
                String cardHolderName="";
                boolean expired = false;
                boolean active=false;
                String state="";
                String expiredCard="";
                String expDate="";
                for (int j = 0; j < childNodes.getLength()&&continueFor; ++j) {
                    Node child = childNodes.item(j);
                    if (child.getNodeName().equals("cardNumber")){id=child.getTextContent();}
                    else if (child.getNodeName().equals("cardholderName")){cardHolderName=child.getTextContent();}
                    else if (child.getNodeName().equals("cardMask")){
                        maskedCard=child.getTextContent();

                    }
                    else if (child.getNodeName().equals("expirationDate")){
                        expDate=child.getTextContent();
                        expDate=expDate.replace(".","-");
                        String [] Expiry=expDate.split("-");
                        LocalDate today = LocalDate.now();
                        int month = today.getMonthValue();
                        int year  = today.getYear();
                        if(year>Integer.parseInt(Expiry[1])){
                            expiredCard="YES";
                        }
                        else if (year==Integer.parseInt(Expiry[1])){
                            if (month>Integer.parseInt(Expiry[0])||month==Integer.parseInt(Expiry[0])){
                                expiredCard="YES";
                            }
                        }
                        else {
                            expiredCard="NO";
                        }
                    }
                    else if (child.getNodeName().equals("statusCode")){
                        state= child.getTextContent();
                        if (!state.equals("0")){
                            state="Closed";
                        }
                    }
                    if (!id.equals("") && !cardHolderName.equals("")&&!expiredCard.equals("")&&!expiredCard.equals("YES")&&!state.equals("")&&!state.equals("Closed")){

                        Account account = new Account(AccountType.CARD, AccountSubType.PREPAID, id,maskedCard, cardHolderName, AuthType.ATMPIN, "4");
                        accounts.add(account);
                        continueFor=false;
                        id="";
                        cardHolderName="";
                    }
                }
            }

            //String id = doc.getElementsByTagName("id").item(0).getTextContent();
            //String cardHolderName = doc.getElementsByTagName("cardholderName").item(0).getTextContent();
            //String maskedAccountNumber=id.substring(0,4)+"****"+id.substring(id.length()-4);
            //Account account = new Account(AccountType.CARD, AccountSubType.PREPAID, id, cardHolderName, AuthType.ATMPIN, "4");

            return accounts;
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    public List<Account> getCustomerCards(String customerNumber) throws Exception {
        try {
            List<Card> cards = Streamable.of(cardsRepository.findAll()).toList();
            String body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <dat:GetPersonInfo>\n" +
                    "         <dat:Request1>\n" +
                    "         <![CDATA[\n" +
                    "         <soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:fimi1=\"http://schemas.compassplus.com/two/1.0/fimi_types.xsd\" xmlns:fimi=\"http://schemas.compassplus.com/two/1.0/fimi.xsd\">\n" +
                    "\t\t\t<soap:Header/>\n" +
                    "\t\t\t<soap:Body>\n" +
                    "\t\t\t\t<fimi:GetPersonInfoRq>\n" +
                    "\t\t\t\t\t<fimi:Request __HEADERPARAM__>\n" +
                    "\t\t\t\t\t\t<fimi1:PersonExtId>" + customerNumber + "</fimi1:PersonExtId>\n" +
                    "\t\t\t\t\t</fimi:Request>\n" +
                    "\t\t\t\t</fimi:GetPersonInfoRq>\n" +
                    "\t\t\t</soap:Body>\n" +
                    "\t\t</soap:Envelope>\n" +
                    "         ]]>\n" +
                    "         </dat:Request1>\n" +
                    "      </dat:GetPersonInfo>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope>";

            String responseBody = Request.SendRequest(env.getProperty("NIBaseURL"), "GetPersonInfo", body);

            Document doc = XMLParser.ReadXML(responseBody);
            String innerXML = doc.getElementsByTagName("FEMI_RESPONSE").item(0).getTextContent();
            doc = XMLParser.ReadXML(innerXML);

            String name = doc.getElementsByTagName("m0:FIO").item(0).getTextContent();

            NodeList nodes = doc.getElementsByTagName("m0:Cards").item(0).getChildNodes();
            List<Account> accounts = new ArrayList<>();

            for (int i = 0; i < nodes.getLength(); ++i) {
                Node node = nodes.item(i);
                NodeList childNodes = node.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); ++j) {
                    Node child = childNodes.item(j);
                    if (child.getNodeName().equals("m0:PAN")) {
                        String accountNumber = child.getTextContent();
                        String accountType = AccountType.CARD;
                        String accountSubType = null;

                        String firstSix = accountNumber.substring(0, 6);
                        String firstEight = accountNumber.substring(0, 8);

                        List<Card> c = cards
                                .stream()
                                .filter(card -> card.getStartsWith().equals(firstEight))
                                .collect(Collectors.toList());

                        if (c.size() == 0) {
                            c = cards
                                    .stream()
                                    .filter(card -> card.getStartsWith().equals(firstSix))
                                    .collect(Collectors.toList());
                        }

                        if (c.size() == 0) continue;

                        if (c.get(0).getTitle().toLowerCase().contains("credit")) {
                            accountSubType = AccountSubType.OVERDRAFT;
                        }
                        if (c.get(0).getTitle().toLowerCase().contains("debit")) {
                            accountSubType = AccountSubType.CHECKING;
                        }
                        //String maskedAccountNumber=accountNumber.substring(0,4)+"****"+accountNumber.substring(accountNumber.length()-4);
                        Account account = new Account(accountType, accountSubType, accountNumber,name, AuthType.ATMPIN, "6");
                        accounts.add(account);
                    }
                }

            }
            return accounts;
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    public Map<String, String> getCustomerDataFromToken(String token) throws Exception {
        Login login = loginsRepository.getLoginByToken(token);
        Map<String, String> data = new HashMap<>();
        if (login==null){
            return data;
        }

        data.put("NationalId", login.getLegalId());
        data.put("DocumentType", login.getDocumentType());
        data.put("CustomerCode", login.getCustomerReference());
        data.put("RequestId", login.getRequestId());
        return data;
    }

    public Map<String, String> getCustomerDetails(String customerNumber) throws Exception {
        customerNumber=customerNumber.replaceAll("x","");
        String body = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                "   <soap:Header/>\n" +
                "   <soap:Body>\n" +
                "      <dat:CustomerDetails>\n" +
                "         <dat:CUSTNO>" + customerNumber + "</dat:CUSTNO>\n" +
                "      </dat:CustomerDetails>\n" +
                "   </soap:Body>\n" +
                "</soap:Envelope>";
        String responseBody = Request.SendRequest(env.getProperty("IPNBaseURL"), "CustomerDetails", body);
        Document doc = XMLParser.ReadXML(responseBody);
        Map<String, String> customerDetails = new HashMap<>();
        String nationalId = doc.getElementsByTagName("LEGALID").item(0).getTextContent();
        String mobileNumber = doc.getElementsByTagName("TELNO").item(0).getTextContent();
        String name = doc.getElementsByTagName("NAMCUST").item(0).getTextContent();
        String address = doc.getElementsByTagName("ADDRESS1").item(0).getTextContent();
        customerDetails.put("nationalId", nationalId);
        customerDetails.put("mobileNumber", mobileNumber);
        customerDetails.put("name", name);
        customerDetails.put("address", address);
        return customerDetails;
    }

    public String fetchCustomerNumberFromMDP(String cardNumber) throws Exception {
        String body = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:cus=\"http://ws.wso2.org/dataservice/customerInfoFromCardNumber\">\n" +
                "   <soap:Header/>\n" +
                "   <soap:Body>\n" +
                "      <cus:_getcustomerinfofromcardnumber>\n" +
                "         <cus:cardNumber>" + cardNumber + "</cus:cardNumber>\n" +
                "      </cus:_getcustomerinfofromcardnumber>\n" +
                "   </soap:Body>\n" +
                "</soap:Envelope>";
        String responseBody = Request.SendRequest(env.getProperty("MDPBaseURL"), "_getcustomerinfofromcardnumber", body);
        Document doc = XMLParser.ReadXML(responseBody);
        String innerXML = doc.getElementsByTagName("response").item(0).getTextContent();
        doc = XMLParser.ReadXML(innerXML);
        return doc.getElementsByTagName("customerNumber").item(0).getTextContent();
    }


    public String fetchCustomerNumberFromNIPAN(String PAN) throws Exception {
        String body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                "\t<soapenv:Header/>\n" +
                "\t<soapenv:Body>\n" +
                "\t\t<dat:GetCardInfoRq>\n" +
                "\t\t\t<dat:Request1>\n" +
                "\t\t\t\t<![CDATA[\n" +
                "\t\t\t\t<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:fimi1=\"http://schemas.compassplus.com/two/1.0/fimi_types.xsd\" xmlns:fimi=\"http://schemas.compassplus.com/two/1.0/fimi.xsd\">\n" +
                "\t\t\t\t\t<soap:Header/>\n" +
                "\t\t\t\t\t<soap:Body>\n" +
                "\t\t\t\t\t\t<fimi:GetCardInfoRq>\n" +
                "\t\t\t\t\t\t\t<fimi:Request __HEADERPARAM__>\n" +
                "\t\t\t\t\t\t\t<fimi1:PAN>" + PAN + "</fimi1:PAN>\n" +
                "\t\t\t\t\t\t</fimi:Request>\n" +
                "\t\t\t\t\t</fimi:GetCardInfoRq>\n" +
                "\t\t\t\t</soap:Body>\n" +
                "\t\t\t</soap:Envelope>\n" +
                "\t\t\t\t]]></dat:Request1>\n" +
                "\t\t</dat:GetCardInfoRq>\n" +
                "\t</soapenv:Body>\n" +
                "</soapenv:Envelope>";

        String responseBody = Request.SendRequest(env.getProperty("NIBaseURL"), "GetCardInfoRq", body);
        Document doc = XMLParser.ReadXML(responseBody);
        String innerXML = doc.getElementsByTagName("FEMI_RESPONSE").item(0).getTextContent();
        doc = XMLParser.ReadXML(innerXML);
        return doc.getElementsByTagName("m0:PersonExtId").item(0).getTextContent();
    }
    public String fetchExpDate(String PAN) throws Exception {
        String body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                "\t<soapenv:Header/>\n" +
                "\t<soapenv:Body>\n" +
                "\t\t<dat:GetCardInfoRq>\n" +
                "\t\t\t<dat:Request1>\n" +
                "\t\t\t\t<![CDATA[\n" +
                "\t\t\t\t<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:fimi1=\"http://schemas.compassplus.com/two/1.0/fimi_types.xsd\" xmlns:fimi=\"http://schemas.compassplus.com/two/1.0/fimi.xsd\">\n" +
                "\t\t\t\t\t<soap:Header/>\n" +
                "\t\t\t\t\t<soap:Body>\n" +
                "\t\t\t\t\t\t<fimi:GetCardInfoRq>\n" +
                "\t\t\t\t\t\t\t<fimi:Request __HEADERPARAM__>\n" +
                "\t\t\t\t\t\t\t<fimi1:PAN>" + PAN + "</fimi1:PAN>\n" +
                "\t\t\t\t\t\t</fimi:Request>\n" +
                "\t\t\t\t\t</fimi:GetCardInfoRq>\n" +
                "\t\t\t\t</soap:Body>\n" +
                "\t\t\t</soap:Envelope>\n" +
                "\t\t\t\t]]></dat:Request1>\n" +
                "\t\t</dat:GetCardInfoRq>\n" +
                "\t</soapenv:Body>\n" +
                "</soapenv:Envelope>";

        String responseBody = Request.SendRequest(env.getProperty("NIBaseURL"), "GetCardInfoRq", body);
        Document doc = XMLParser.ReadXML(responseBody);
        String innerXML = doc.getElementsByTagName("FEMI_RESPONSE").item(0).getTextContent();
        doc = XMLParser.ReadXML(innerXML);
        return doc.getElementsByTagName("m0:ExpDate").item(0).getTextContent();
    }

    public Map<String, String> fetchUserFromMobile(String mobileNumber) throws Exception {

        String body = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                "   <soap:Header/>" +
                "   <soap:Body>" +
                "      <dat:GetCustomerCodeFromMobile>" +
                "         <dat:Mobile>" + mobileNumber + "</dat:Mobile>" +
                "      </dat:GetCustomerCodeFromMobile>" +
                "   </soap:Body>" +
                "</soap:Envelope>";

        String responseBody = Request.SendRequest(env.getProperty("IPNBaseURL"), "GetCustomerCodeFromMobile", body);

        Document doc = XMLParser.ReadXML(responseBody);
        NodeList list = doc.getElementsByTagName("CustomerCodeRow");

        Map<String, String> result = new HashMap<>();

        if (list.getLength() == 0) throw new Exception("Customer Not Found");

        NodeList childNodes = list.item(0).getChildNodes();
        for (int j = 0; j < childNodes.getLength(); ++j) {
            Node child = childNodes.item(j);
            result.put(child.getNodeName(), child.getTextContent());
        }

        return result;
    }

}
