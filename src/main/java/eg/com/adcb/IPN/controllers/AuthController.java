package eg.com.adcb.IPN.controllers;


import eg.com.adcb.IPN.consts.*;
import eg.com.adcb.IPN.dtos.Request.AuthenticateReqDto;
import eg.com.adcb.IPN.dtos.Request.FundTransferReqDto;
import eg.com.adcb.IPN.dtos.Response.AuthenticateResDto;
import eg.com.adcb.IPN.dtos.Response.ErrorsResDto;
import eg.com.adcb.IPN.models.*;
import eg.com.adcb.IPN.repositories.*;
import eg.com.adcb.IPN.utils.ExceptionStackTrace;
import eg.com.adcb.IPN.utils.Request;
import eg.com.adcb.IPN.utils.XMLParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
//import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.core.env.Environment;
import org.springframework.data.util.Streamable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import javax.net.ssl.SSLContext;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.ssl.SSLContexts;


@RestController
@RequestMapping("authenticate")
public class AuthController {
    private Environment env;
   // private static Logger logger = Logger.getLogger(Logging.class);
    private AccountsRepository accountsRepository;
    private AcceptedLogsRepository acceptedLogsRepository;
    private RejectedLogsRepository rejectedLogsRepository;
    private LoginsRepository loginsRepository;
    private CardsRepository cardsRepository;
    private ErrorsRepository errorsRepository;
    private mfieldsRepository mfieldsRepoX;


    public AuthController(Environment env,AccountsRepository accountsRepository,
                          AcceptedLogsRepository acceptedLogsRepository,
                          RejectedLogsRepository rejectedLogsRepository,
                          LoginsRepository loginsRepository,
                          CardsRepository cardsRepository, ErrorsRepository errorsRepository,mfieldsRepository mfieldsRepoX) {
        this.env = env;
        this.accountsRepository = accountsRepository;
        this.acceptedLogsRepository = acceptedLogsRepository;
        this.rejectedLogsRepository = rejectedLogsRepository;
        this.loginsRepository = loginsRepository;
        this.cardsRepository = cardsRepository;
        this.errorsRepository=errorsRepository;
        this.mfieldsRepoX=mfieldsRepoX;
    }


    @PostMapping
    public Object authenticate(@RequestBody AuthenticateReqDto data) throws Exception {

        try{
        	CheckService();
        }

        catch (Exception ex){
        	Logging.warn(ExceptionStackTrace.GetStackTrace(ex));
            return GenerateErrorResponse(ErrorsCode.ERROR_11013, ErrorsCode.ERROR_11013_DESC, data);
        }
        String nullCheck=checkNull(data,"authenticate");
        if(nullCheck!=""){
             return GenerateErrorResponse(ErrorsCode.ERROR_11017, ErrorsCode.ERROR_11017_DESC, data);           
        }

        try {
            if (data.ac != null) {
                if (data.ac.get("name") == null || data.ac.get("authPINLen") == null || data.ac.get("accountId") == null || data.ac.get("subType") == null || data.ac.get("type") == null || data.ac.get("authType") == null) {
                       return GenerateErrorResponse(ErrorsCode.ERROR_11017, ErrorsCode.ERROR_11017_DESC,data, "data.ac   is not found ");  
                } else if (data.customerReference == null) {
                    return GenerateErrorResponse(ErrorsCode.ERROR_11017, ErrorsCode.ERROR_11017_DESC,data, "data.ac  is not found ");

                } else if (data.credmessage.get("authAccountId").startsWith("5078") && (!data.credmessage.get("authAccountId").equalsIgnoreCase(data.ac.get("accountId")) || !data.ac.get("type").equalsIgnoreCase("CARD"))) {

                    return GenerateErrorResponse(ErrorsCode.ERROR_11005, ErrorsCode.ERROR_11005_DESC, data );
                }
            }
        }
        catch(Exception ex){
        	Logging.warn(ExceptionStackTrace.GetStackTrace(ex));
            return GenerateErrorResponse(ErrorsCode.ERROR_11017, ErrorsCode.ERROR_11017_DESC ,data, "data,data.ac   is not found ");

        }
        if (data.customerReference!=null){
            if (data.ac==null) {
            return GenerateErrorResponse(ErrorsCode.ERROR_11017, ErrorsCode.ERROR_11017_DESC , data, "data.ac is not found ");
            }
        }
        if (data.credmessage!=null){
            if (data.credmessage.get("type")==null||data.credmessage.get("kIndex")==null||data.credmessage.get("authAccountId")==null||data.credmessage.get("authBlock")==null){
                return GenerateErrorResponse(ErrorsCode.ERROR_11017, ErrorsCode.ERROR_11017_DESC , data, "data.ac is not found ");
            }
            if (data.credmessage.get("kIndex").length()!=8||!data.credmessage.get("kIndex").matches("[0-9]+")){
                return GenerateErrorResponse(ErrorsCode.ERROR_11017, ErrorsCode.ERROR_11017_DESC , data, "data.ac is not found ");
            }
        }
        
        if(data.settlementCycleId!=null) {
            if (data.settlementCycleId.length() < 9 || data.settlementCycleId.length() > 11) {
                   return GenerateErrorResponse(ErrorsCode.ERROR_11017, ErrorsCode.ERROR_11017_DESC , data, "data.ac is not found ");
            }
        }
        if(data.customerReference!=null) {
            if ( data.customerReference.length() > 100) {
                 return GenerateErrorResponse(ErrorsCode.ERROR_11017, ErrorsCode.ERROR_11017_DESC , data, "data.ac is not found ");
            }
        }

        boolean resultOnlyNumbers = data.mobileNumber.matches("[0-9]+");
        if (data.mobileNumber.length() != 14 || data.credmessage.get("authAccountId").length() > 50||!resultOnlyNumbers) {
             return GenerateErrorResponse(ErrorsCode.ERROR_11017, ErrorsCode.ERROR_11017_DESC , data, "data.ac is not found ");
        }
        if (data.ac!=null) {
            if (data.ac.get("name")!=null){
                if (data.ac.get("name").length() >100) {
                     return GenerateErrorResponse(ErrorsCode.ERROR_11017, ErrorsCode.ERROR_11017_DESC , data, "data.ac is not found ");
                }
            }
            if (data.ac.get("accountId")!=null) {
                if ( data.ac.get("accountId").length() < 4 || data.ac.get("accountId").length() > 36) {
                    return GenerateErrorResponse(ErrorsCode.ERROR_11017, ErrorsCode.ERROR_11017_DESC , data, "data.ac is not found ");
                }
            }
        }
        String RequesID=loginsRepository.getLoginByRequestID(data.requestId);
        if (RequesID!=null){
            return GenerateErrorResponse(ErrorsCode.ERROR_11014, ErrorsCode.ERROR_11014_DESC , data );
        }
        String accountStatus = "";
        if (data.ac!=null  && data.ac.get("accountId").length()==16 && data.ac.get("accountId").startsWith("11")) {
            String test_body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <dat:ACCOUNTDETAILS>\n" +
                    "         <dat:ACCNO>" + data.ac.get("accountId").substring(data.ac.get("accountId").length() - 16) + "</dat:ACCNO>\n" +
                    "      </dat:ACCOUNTDETAILS>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope>";
            String res_TEST = Request.SendRequest(env.getProperty("IPNBaseURL"), "ACCOUNTDETAILS", test_body);
            Logging.host("For Testing the CheckService response -------------" + res_TEST);
            Document doc = XMLParser.ReadXML(res_TEST);
                if (!res_TEST.contains("No records were found")) {
                    String phoneNum = doc.getElementsByTagName("PHONENUMBER").item(0).getTextContent();
                    accountStatus = doc.getElementsByTagName("STATUS").item(0).getTextContent();
                    
                    phoneNum = AddCountryCode(phoneNum);
                    if (!data.mobileNumber.equals(phoneNum)) {
                       return GenerateErrorResponse(ErrorsCode.ERROR_11008, ErrorsCode.ERROR_11008_DESC , data );
                    }
                }

        }
        System.out.println(accountStatus);
        if (accountStatus.equals("D")){
            return GenerateErrorResponse(ErrorsCode.ERROR_11018, ErrorsCode.ERROR_11018_DESC , data );
        }

        if (data.credmessage.get("authAccountId").substring(0,4).equals("4103")){
            return GenerateErrorResponse(ErrorsCode.ERROR_11005, ErrorsCode.ERROR_11005_DESC , data );
        }
        if (data.customerType!=null) {
            if (data.customerType.equals("MERCHANT")) { //TODO SHould white list the allowed types
                return GenerateErrorResponse(ErrorsCode.ERROR_11013, ErrorsCode.ERROR_11013_DESC , data );
            }
            if (!data.customerType.equals("CONSUMER")) {
                return GenerateErrorResponse(ErrorsCode.ERROR_11017, ErrorsCode.ERROR_11017_DESC , data );
            }
        }


        try {
            List<Card> cards = Streamable.of(cardsRepository.findAll()).toList();
            String issuer = null;
            for (Card card : cards) {
                int length = card.getStartsWith().length();
                String cardIdt = data.credmessage.get("authAccountId").substring(0, length);
                if (cardIdt.equals(card.getStartsWith())) {
                    issuer = card.getIssuer();
                    break;
                }
            }

            String customerNumber = null;
        try {
    customerNumber = Get_CIF(data);
            }
        catch(Exception ex){
        	Logging.warn(ExceptionStackTrace.GetStackTrace(ex));
            }
            if (issuer==null){

               // ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"11005","Unsupported card, please use debit card or Meeza prepaid card");
                return GenerateErrorResponse(ErrorsCode.ERROR_11005, ErrorsCode.ERROR_11005_DESC , data );
            }

            if (issuer.equals("MDP")) {
                if (data.ac!=null && data.ac.get("accountId").length()==16 && data.ac.get("accountId").startsWith("11")){
                    if (!data.ac.get("accountId").substring(data.ac.get("accountId").length()-16).equals(data.credmessage.get("authAccountId"))){
                        return GenerateErrorResponse(ErrorsCode.ERROR_11005, ErrorsCode.ERROR_11005_DESC , data );
                    }
                }
                String mdpCardInfo="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:mdp=\"http://ws.wso2.org/dataservice/mdpCardInfo\">\n" +
                        "   <soapenv:Header/>\n" +
                        "   <soapenv:Body>\n" +
                        "      <mdp:_getmdpcardinfo>\n" +
                        "         <mdp:cardNumber>"+data.credmessage.get("authAccountId")+"</mdp:cardNumber>\n" +
                        "      </mdp:_getmdpcardinfo>\n" +
                        "   </soapenv:Body>\n" +
                        "</soapenv:Envelope>";
                String resMdpCardInfo = Request.SendRequest(env.getProperty("MDPBaseURL"), "_getmdpcardinfo", mdpCardInfo);
                Logging.host("For Testing the CheckService response -------------" + resMdpCardInfo);
                if(resMdpCardInfo.contains("Unable to find card")){
                    return GenerateErrorResponse(ErrorsCode.ERROR_11001, ErrorsCode.ERROR_11001_DESC , data );
                }
                if(resMdpCardInfo.contains("ERRORweblogic")){
                	Logging.host(resMdpCardInfo);
                    return GenerateErrorResponse(ErrorsCode.ERROR_11013, ErrorsCode.ERROR_11013_DESC , data );
                }
                Document docMdp = XMLParser.ReadXML(resMdpCardInfo);
                String innerXMLmdp = docMdp.getElementsByTagName("response").item(0).getTextContent();
                docMdp = XMLParser.ReadXML(innerXMLmdp);
                String backOfficeStatus = docMdp.getElementsByTagName("backOfficeStatus").item(0).getTextContent();
                String expDate = docMdp.getElementsByTagName("expirDate").item(0).getTextContent();
                String statusCode=docMdp.getElementsByTagName("statusCode").item(0).getTextContent();
                String state = docMdp.getElementsByTagName("state").item(0).getTextContent();
                expDate=expDate.replace(".","-");
                String [] Expiry=expDate.split("-");
                LocalDate today = LocalDate.now();
                int month = today.getMonthValue();
                int year  = today.getYear();
                if(year>Integer.parseInt(Expiry[1])){
                    return GenerateErrorResponse(ErrorsCode.ERROR_11002, ErrorsCode.ERROR_11002_DESC , data );
                }
                else if (year==Integer.parseInt(Expiry[1])){
                    if (month>Integer.parseInt(Expiry[0])||month==Integer.parseInt(Expiry[0])){
                        return GenerateErrorResponse(ErrorsCode.ERROR_11002, ErrorsCode.ERROR_11002_DESC , data );
                    }
                }
                if (state.equalsIgnoreCase("Closed")){ //TODO SHould white list the allowed types
                    return GenerateErrorResponse(ErrorsCode.ERROR_11003, ErrorsCode.ERROR_11003_DESC , data );

                }
                if (statusCode.equals("20")||statusCode.equals("12")){
                	return GenerateErrorResponse(ErrorsCode.ERROR_11003, ErrorsCode.ERROR_11003_DESC , data );
                }

                if (data.ac!=null){
                    String accountAuthId=loginsRepository.getLastAuthAccountId(data.mobileNumber);
//                    if (!data.credmessage.get("authAccountId").equals(accountAuthId)){
//                        ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"11005","Unsupported card, please use debit card or Meeza prepaid card");
//                        return response;
//                    }
                }
                customerNumber = accountsRepository.fetchCustomerNumberFromMDP(data.credmessage.get("authAccountId"));

                Map<String, String> customerDetails = accountsRepository.getCustomerDetails(customerNumber);
                String returnedMobile = customerDetails.get("mobileNumber").replaceAll("[+]2", "002");
                String mobile = data.mobileNumber.replaceAll("[+]2", "002");
                if (!mobile.equals(returnedMobile))
                {
                    return GenerateErrorResponse(ErrorsCode.ERROR_11008, ErrorsCode.ERROR_11008_DESC , data );
                }
                // Call Meeza API
                String meezaBody = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:pin=\"http://ws.wso2.org/dataservice/pinAuthorization\">\n" +
                        "   <soap:Header/>\n" +
                        "   <soap:Body>\n" +
                        "      <pin:_postpinauthorization>\n" +
                        "         <pin:cardNumber>" + data.credmessage.get("authAccountId") + "</pin:cardNumber>\n" +
                        "         <pin:pinBlock>" + data.credmessage.get("authBlock") + "</pin:pinBlock>\n" +
                        "      </pin:_postpinauthorization>\n" +
                        "   </soap:Body>\n" +
                        "</soap:Envelope>";

                String meezaResBody = Request.SendRequest(env.getProperty("MDPBaseURL"), "_postpinauthorizationasjson", meezaBody);
                Logging.host("For Testing the CheckService response -------------" + meezaResBody);
                if (meezaResBody.contains("Do not honor transaction")&&meezaResBody.contains("ECODE")){
                    return GenerateErrorResponse(ErrorsCode.ERROR_11006, ErrorsCode.ERROR_11006_DESC , data );
                }
                if(meezaResBody.contains("Pin is invalid")||meezaResBody.contains("ECODE")){
                	if(meezaResBody.contains("Do not honor transaction"))
                    return GenerateErrorResponse(  ErrorsCode.ERROR_11006, ErrorsCode.ERROR_11006_DESC , data );
                	else
                		return GenerateErrorResponse(ErrorsCode.ERROR_11001, ErrorsCode.ERROR_11001_DESC , data );

                }
                
               

            } else {
                try {
                    String reqNI = "<soapenv:Envelope\n" +
                            "\txmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
                            "\txmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                            "\t<soapenv:Header/>\n" +
                            "\t<soapenv:Body>\n" +
                            "\t\t<dat:GetCardInfoRq>\n" +
                            "\t\t\t<dat:Request1>\n" +
                            "\t\t\t\t<![CDATA[\n" +
                            "<soap:Envelope\n" +
                            "\t\t\t\txmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
                            "\t\t\t\txmlns:fimi=\"http://schemas.compassplus.com/two/1.0/fimi.xsd\"\n" +
                            "\t\t\t\txmlns:fimi1=\"http://schemas.compassplus.com/two/1.0/fimi_types.xsd\"><soap:Header/><soap:Body><fimi:GetCardInfoRq><fimi:Request   __HEADERPARAM__  ><fimi1:PAN>" + data.credmessage.get("authAccountId") + "</fimi1:PAN></fimi:Request></fimi:GetCardInfoRq></soap:Body></soap:Envelope>\n" +
                            "]]>\n" +
                            "\t\t\t</dat:Request1>\n" +
                            "\t\t</dat:GetCardInfoRq>\n" +
                            "\t</soapenv:Body>\n" +
                            "</soapenv:Envelope>";
                    String responseBody = Request.SendRequest(env.getProperty("NIBaseURL"), "GetCardInfoRq", reqNI);
                    Logging.host("For Testing the CheckService response -------------" + responseBody);
                    Document doc = XMLParser.ReadXML(responseBody);
                    String innerXML = doc.getElementsByTagName("FEMI_RESPONSE").item(0).getTextContent();
                    doc = XMLParser.ReadXML(innerXML);

                    String accs = doc.getElementsByTagName("m0:Status").item(doc.getElementsByTagName("m0:Status").getLength() - 1).getTextContent();


                    if (accs.equals("0")) {

                    	return GenerateErrorResponse(ErrorsCode.ERROR_11003, ErrorsCode.ERROR_11003_DESC , data );
                    }


                    if (accs.equals("15")) {

                    	return GenerateErrorResponse(ErrorsCode.ERROR_11002, ErrorsCode.ERROR_11002_DESC , data );
                    }

                    if (responseBody.contains("PersonExtId")) {
                        String PersonExtId = null;
                        PersonExtId = doc.getElementsByTagName("m0:PersonExtId").item(doc.getElementsByTagName("m0:PersonExtId").getLength() - 1).getTextContent();
                        String AcctNo = null;
                        AcctNo = doc.getElementsByTagName("m0:AcctNo").item(doc.getElementsByTagName("m0:AcctNo").getLength() - 1).getTextContent();

                        if (!AcctNo.contains(PersonExtId)) {
                        	return GenerateErrorResponse(ErrorsCode.ERROR_11005, ErrorsCode.ERROR_11005_DESC , data );
                        }
                    } else {

                    	return GenerateErrorResponse(ErrorsCode.ERROR_11005, ErrorsCode.ERROR_11005_DESC , data );
                    }

                } catch (Exception msg) {
                	Logging.warn(ExceptionStackTrace.GetStackTrace(msg));
                	return GenerateErrorResponse(ErrorsCode.ERROR_11001, ErrorsCode.ERROR_11001_DESC , data );
                }
                // Authenticate NI
                try {

                    String expDate = accountsRepository.fetchExpDate(data.credmessage.get("authAccountId"));
                    LocalDate today = LocalDate.now();
                    LocalDate datex = LocalDate.parse(expDate.substring(0, 10));

                    System.out.println(today);
                    if (today.isAfter(datex)) {
                    	return GenerateErrorResponse(ErrorsCode.ERROR_11002, ErrorsCode.ERROR_11002_DESC , data );
                    }

                    if (data.ac != null) {
                        String accounRealted = data.ac.get("accountId");
                        if(accounRealted.startsWith("50")){

                            accounRealted=getcustomerinfofromcardnumber_FUNC(accounRealted);

                        }

                        if (!accounRealted.contains(customerNumber)) {
                        	return GenerateErrorResponse(ErrorsCode.ERROR_11008, ErrorsCode.ERROR_11008_DESC , data );
                        }
                    }

                    // customerNumber="1127363";
                    String bodyForRelatedAcc = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                            "   <soap:Header/>" +
                            "   <soap:Body>" +
                            "      <dat:ACCTDETAILS>" +
                            "         <dat:CUSTNO>" + customerNumber + "</dat:CUSTNO>" +
                            "      </dat:ACCTDETAILS>" +
                            "   </soap:Body>" +
                            "</soap:Envelope>";

                    String responseBody = Request.SendRequest(env.getProperty("IPNBaseURL"), "ACCTDETAILS", bodyForRelatedAcc);
                    Logging.host("For Testing the CheckService response -------------" + responseBody);
                    if (data.ac != null) {
                        String accounRealted = data.ac.get("accountId");

                        if(accounRealted.startsWith("50")){

                            accounRealted=getcustomerinfofromcardnumber_FUNC(accounRealted);
                            Document doc;
                            doc = XMLParser.ReadXML(accounRealted);
                            String innerXml = doc.getElementsByTagName("response").item(0).getTextContent();
                            doc = XMLParser.ReadXML(innerXml);


                            accounRealted = doc.getElementsByTagName("customerNumber").item(0).getTextContent();




                        }
if(accounRealted.startsWith("EG"))
    accounRealted.substring(accounRealted.length() - 16);

                        if (!responseBody.contains(accounRealted)) {
                        	return GenerateErrorResponse(ErrorsCode.ERROR_11018, ErrorsCode.ERROR_11018_DESC , data );
                        }
                    }
                } catch (Exception ex) {
                	Logging.warn(ExceptionStackTrace.GetStackTrace(ex));
                	return GenerateErrorResponse(ErrorsCode.ERROR_11001, ErrorsCode.ERROR_11001_DESC , data );
                }
                try {
                    customerNumber = accountsRepository.fetchCustomerNumberFromNIPAN(data.credmessage.get("authAccountId"));
                } catch (Exception e) {
                	Logging.warn(ExceptionStackTrace.GetStackTrace(e));
                }
                //customerNumber="1127363";
                Map<String, String> customerDetails = accountsRepository.getCustomerDetails(customerNumber);
                String returnedMobile = customerDetails.get("mobileNumber").replaceAll("[+]2", "002");
                String mobile = data.mobileNumber.replaceAll("[+]2", "002");
                if (!mobile.equals(returnedMobile)) {
                	return GenerateErrorResponse(ErrorsCode.ERROR_11008, ErrorsCode.ERROR_11008_DESC , data );
                }
                
                String KEYPASS=env.getProperty("KEYPASS");
                CloseableHttpClient httpclient;
                if(KEYPASS!=null) {
                SSLContext sslContext = SSLContexts.custom()
                        .loadKeyMaterial(readStore(), KEYPASS.toCharArray())
                        .loadTrustMaterial(null, TrustAllStrategy.INSTANCE) // use null as second param if you don't have a separate key password
                        .build();
                httpclient = HttpClients.custom().setSSLContext(sslContext).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).build();
                Logging.host("Will send Certificate based Auth Request to NI");
                }
                
                else
                  httpclient = HttpClients
                        .custom()
                        .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                        .build();
                
                HttpPost httpPost = new HttpPost(env.getProperty("NIAuthURL"));
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setHeader("Accept", "application/json");
                httpPost.setEntity(new StringEntity("{\n" +
                        "    \"timestamp\": \"" + data.timestamp + "\",\n" +
                        "    \"requestId\": \"" + data.requestId + "\",\n" +
                        "    \"ac\": {\n" +
                        "        \"authType\": \"ATMPIN\",\n" +
                        "        \"authPINLen\": \"4\"\n" +
                        "    },\n" +
                        "    \"credmessage\": {\n" +
                        "        \"authBlock\": \"" + data.credmessage.get("authBlock") + "\",\n" +
                        "        \"authAccountId\": \"" + data.credmessage.get("authAccountId") + "\",\n" +
                        "        \"kIndex\": \"" + data.credmessage.get("kIndex") + "\",\n" +
                        "        \"type\": \"" + data.credmessage.get("type") + "\"\n" +
                        "    }\n" +
                        "}"));
                httpPost.getEntity();

                try {

                    HttpResponse httpResponse = httpclient.execute(httpPost);
                    HttpEntity entity = httpResponse.getEntity();

                    String responseString = EntityUtils.toString(entity, "UTF-8");
                    Logging.host("Response From NI:" + responseString);
                    
                    System.out.println(responseString);
                    JSONObject json = new JSONObject(responseString);
                    System.out.println(json.toString());
                    String respDesc = json.getString("respDesc");
                    String respCode = json.getString("respCode");
                    String msg = "PIN tries was exceeded";
//                    if (respDesc.substring(0,msg.length()).equals(msg)){
//                        ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"11016","PIN trial exceeded");
//                        return response;
//                    }

                    if (!respCode.equals("00000")){

                        ErrorsResDto response;

                        if (respDesc.equals("Invalid Message") || respDesc.equals("Invalid Data") || respDesc.contains("Invalid PIN")) {
                            response = new ErrorsResDto(data.requestId, data.timestamp, "11001", "Invalid card number or PIN, please try again");

                        }

                        else if (respCode.equals("80029")) {
                             response = new ErrorsResDto(data.requestId, data.timestamp, "11016", "PIN trial exceeded");

                        }

                       else if (respCode.equals("99994")) {
                             response = new ErrorsResDto(data.requestId, data.timestamp, "11003", "inactive card, please contact your bank");

                        }


                   else{

                             response = new ErrorsResDto(data.requestId, data.timestamp, "11001", "Invalid card number or PIN, please try again");


                        }

                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={"+response.respCode+"}; RESPONSE={"+response+"}; ");
                        rejectedLogsRepository.save(new RejectedLog(data.requestId, LocalDateTime.now(), data.toString(), response.toString(),null));

                        return response;

                }
            }
                catch (Exception ex){
                    String stackTrace = ExceptionStackTrace.GetStackTrace(ex);
                	Logging.warn(ExceptionStackTrace.GetStackTrace(ex));

                    return GenerateErrorResponse(ErrorsCode.ERROR_11013, ErrorsCode.ERROR_11013_DESC , data ,stackTrace);
                }

               // System.out.println(EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8));

            }

                if (data.customerReference != null) {
                    if (!customerNumber.equals(data.customerReference)) {
                         return GenerateErrorResponse(ErrorsCode.ERROR_11017, ErrorsCode.ERROR_11017_DESC , data );
                    }
                }

            Map<String, String> customerDetails = accountsRepository.getCustomerDetails(customerNumber);
            String returnedMobile = customerDetails.get("mobileNumber").replaceAll("[+]2", "002");
            String mobile = data.mobileNumber.replaceAll("[+]2", "002");
            if (!mobile.equals(returnedMobile))
            {

            	return GenerateErrorResponse(ErrorsCode.ERROR_11008, ErrorsCode.ERROR_11008_DESC , data );
            }
            //if (data.ac != null || data.customerReference != null) throw new Exception("Reset MPin Not Implemented");
            if (!data.credmessage.get("type").equals("AUTHENTICATION")) {
                String ErrorDesc=errorsRepository.getErrorDes("AUTHENTICATION");
                String ErrorCode=errorsRepository.getErrorCode("AUTHENTICATION");
                return GenerateErrorResponse(ErrorCode, ErrorDesc , data );
            }
            //fraudBody
            Fraud_Authentication_Registration(data,"P",customerNumber,customerDetails.get("name"));

            LocalDateTime now = LocalDateTime.now();
            String currentTimestamp = String.valueOf(Timestamp.valueOf(now));
            String approvalToken = UUID.randomUUID().toString();
            AuthenticateResDto response = new AuthenticateResDto(
                    data.requestId,
                    currentTimestamp,
                    "00000",
                    "SUCCESS",
                    customerNumber,
                    approvalToken
            );
            loginsRepository.deleteByCustomerReference(customerNumber);
            if(data.customerType==null){

                loginsRepository.save(new Login(customerNumber, data.requestId, now, data.mobileNumber,"", data.settlementCycleId==null?"":data.settlementCycleId, customerDetails.get("nationalId"), "NationalId", approvalToken, customerDetails.get("name"), customerDetails.get("address"),data.credmessage.get("authAccountId") ));

            }
            else {

                loginsRepository.save(new Login(customerNumber, data.requestId, now, data.mobileNumber, data.customerType, data.settlementCycleId==null?"":data.settlementCycleId, customerDetails.get("nationalId"), "NationalId", approvalToken, customerDetails.get("name"), customerDetails.get("address"),data.credmessage.get("authAccountId") ));

            }

            acceptedLogsRepository.save(new AcceptedLog(data.requestId, now, data.toString(), response.toString(),null));
            return response;
        } catch (Exception ex) {

            //fraudBody
            Fraud_Authentication_Registration(data,"F","Failed","Failed");
            String stackTrace = ExceptionStackTrace.GetStackTrace(ex);
            return GenerateErrorResponse(ErrorsCode.ERROR_11017, ErrorsCode.ERROR_11017_DESC , data,stackTrace );
        }
    }

    public String checkNull(Object data,String reqName) throws IllegalAccessException {
        Field[] fields=data.getClass().getDeclaredFields();
        for (int i=0;i< fields.length;i++){
            String req=fields[i].toString();
            int LastIndexOfDot=req.lastIndexOf(".");
            String indexReq=req.substring(LastIndexOfDot+1,req.length());
            String returned=mfieldsRepoX.checkMandatoryFields(indexReq,reqName);
            if(returned==null){
                continue;
            }
            else {

                if (fields[i].get(data)== null ||fields[i].get(data)== ""){
                    return indexReq;
                }

            }

        }
      return "";
    }


    public String Get_CIF(AuthenticateReqDto data) throws Exception {
        String CIF=null;

            if(data.credmessage.get("authAccountId").length()==16 && data.credmessage.get("authAccountId").startsWith("11")){

                CIF=data.credmessage.get("authAccountId").substring(1,8);
            }
            else if (data.credmessage.get("authAccountId").length()==29 && data.credmessage.get("authAccountId").startsWith("EG")){

                String accno=data.credmessage.get("authAccountId").substring(14);
                CIF=accno.substring(0,7);
            }
            else if(data.credmessage.get("authAccountId").length()==16 && (data.credmessage.get("authAccountId").startsWith("41") || data.credmessage.get("authAccountId").startsWith("45"))){

                Document doc = XMLParser.ReadXML(NI_Get_Card_Info_FUNC(data.credmessage.get("authAccountId")));
                String innerXml = doc.getElementsByTagName("FEMI_RESPONSE").item(0).getTextContent();
                innerXml = innerXml.replaceAll(":", "_");
                doc = XMLParser.ReadXML(innerXml);
                CIF = doc.getElementsByTagName("m0_PersonExtId").item(0).getTextContent();

            }
            else if(data.credmessage.get("authAccountId").length()==16 && (data.credmessage.get("authAccountId").startsWith("5078") || data.credmessage.get("authAccountId").startsWith("12345"))){

                Document docMdp = XMLParser.ReadXML(getcustomerinfofromcardnumber_FUNC(data.credmessage.get("authAccountId")));
                String innerXMLmdp = docMdp.getElementsByTagName("response").item(0).getTextContent();
                docMdp = XMLParser.ReadXML(innerXMLmdp);
                CIF = docMdp.getElementsByTagName("customerNumber").item(0).getTextContent();


                if(CIF.length()>7){

                    docMdp = XMLParser.ReadXML(getmdpcustomerdetailsbycustomernumber_FUNC(CIF));
                    innerXMLmdp = docMdp.getElementsByTagName("response").item(0).getTextContent();
                    docMdp = XMLParser.ReadXML(innerXMLmdp);
                    innerXMLmdp = docMdp.getElementsByTagName("documents").item(0).getTextContent();
                    docMdp = XMLParser.ReadXML(innerXMLmdp);
                    String NID = docMdp.getElementsByTagName("idNumber").item(0).getTextContent();

                    docMdp = XMLParser.ReadXML(GETCUSTOMERBYID_FUNC(NID));
                    CIF=docMdp.getElementsByTagName("CUSTNO").item(0).getTextContent();

                }


            }




        return CIF;
    }


    public String NI_Get_Card_Info_FUNC(String pan) throws Exception {

        String response=null;

        String body = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                "   <soap:Header/>\n" +
                "   <soap:Body>\n" +
                "      <dat:GetCardInfoRq>\n" +
                "         <dat:Request1><![CDATA[\n" +
                "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:fimi=\"http://schemas.compassplus.com/two/1.0/fimi.xsd\" xmlns:fimi1=\"http://schemas.compassplus.com/two/1.0/fimi_types.xsd\">\n" +
                "\t<soap:Header/>\n" +
                "\t<soap:Body>\n" +
                "\t\t<fimi:GetCardInfoRq>\n" +
                "\t\t\t<fimi:Request   __HEADERPARAM__  >\n" +
                "\t\t\t\t<fimi1:PAN>" + pan + "</fimi1:PAN>\n" +
                "\t\t\t</fimi:Request>\n" +
                "\t\t</fimi:GetCardInfoRq>\n" +
                "\t</soap:Body>\n" +
                "</soap:Envelope>\n" +
                "]]>\n" +
                "</dat:Request1>\n" +
                "      </dat:GetCardInfoRq>\n" +
                "   </soap:Body>\n" +
                "</soap:Envelope>";
        response = Request.SendRequest(env.getProperty("NIBaseURL"), "GetCardInfoRq", body);
        Logging.host("For Testing the CheckService response -------------" + response);



        return response;
    }

    public String getcustomerinfofromcardnumber_FUNC(String CardNumber) throws Exception {

        String mdpCardInfo="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:cus=\"http://ws.wso2.org/dataservice/customerInfoFromCardNumber\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <cus:_getcustomerinfofromcardnumber>\n" +
                "         <cus:cardNumber>"+CardNumber+"</cus:cardNumber>\n" +
                "      </cus:_getcustomerinfofromcardnumber>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";
        String resMdpCardInfo = Request.SendRequest(env.getProperty("MDPBaseURL"), "_getcustomerinfofromcardnumber", mdpCardInfo);
        Logging.host("For Testing the CheckService response -------------" + resMdpCardInfo);
        return resMdpCardInfo;

    }

    public String getmdpcustomerdetailsbycustomernumber_FUNC(String customernumber) throws Exception {

        String mdpCardInfo="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:cus=\"http://ws.wso2.org/dataservice/customerInfoFromCardNumber\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <mdp:_getmdpcustomerdetailsbycustomernumber>\n" +
                "         <mdp:CustomerNumber>"+customernumber+"</mdp:CustomerNumber>\n" +
                "      </mdp:_getmdpcustomerdetailsbycustomernumber>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";
        String resMdpCardInfo = Request.SendRequest(env.getProperty("MDPBaseURL"), "_getmdpcustomerdetailsbycustomernumber", mdpCardInfo);
        Logging.host("For Testing the CheckService response -------------" + resMdpCardInfo);
        return resMdpCardInfo;

    }

    public String GETCUSTOMERBYID_FUNC(String NID) throws Exception {

        String body="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n"+
                "<soapenv:Header/>\n"+
                "<soapenv:Body>\n"+
                "<dat:GETCUSTOMERBYID>\n"+
                "<dat:LEGALID>"+NID+"</dat:LEGALID>\n"+
                "</dat:GETCUSTOMERBYID>\n"+
                "</soapenv:Body>\n"+
                "</soapenv:Envelope>";
        String response= Request.SendRequest(env.getProperty("IPNBaseURL"), "GETCUSTOMERBYID", body);
        Logging.host("For Testing the CheckService response -------------" + response);
        return response;

    }
    
    public String AddCountryCode(String phoneNumber){

        if(phoneNumber.startsWith("01")) {
            phoneNumber = "002" + phoneNumber;
        }
        else if(phoneNumber.startsWith("2")) {
            phoneNumber = "00" + phoneNumber;

        }
        else if(phoneNumber.startsWith("+")) {
            phoneNumber = "00" + phoneNumber.substring(1);

        }
        return phoneNumber;
    }
    
    /**
     * Validate if WSO2 service is running
     * @return "My Mobile Number if WSO2 and T24 Are working
     * @throws Exception
     */
    public String CheckService() throws Exception {
    	 String body = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                 "   <soap:Header/>\n" +
                 "   <soap:Body>\n" +
                 "      <dat:CustomerDetails>\n" +
                 "         <dat:CUSTNO> 1127363 </dat:CUSTNO>\n" +
                 "      </dat:CustomerDetails>\n" +
                 "   </soap:Body>\n" +
                 "</soap:Envelope>";
    	 Logging.host("For Testing the CheckService Request -------------" + body);
         String responseBody = Request.SendRequest(env.getProperty("IPNBaseURL"), "CustomerDetails", body);
         Logging.host("For Testing the CheckService response -------------" + responseBody);
         Document doc = XMLParser.ReadXML(responseBody);
         
         String mobileNumber = doc.getElementsByTagName("TELNO").item(0).getTextContent();
         return mobileNumber;
         
    }
    public ErrorsResDto GenerateErrorResponse(String errorCode,String errorText,AuthenticateReqDto data )
    {

        Fraud_Authentication_Registration(data,"F","Failed","Failed");

        ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,errorCode,errorText);
         Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={400}; RESPONSE={ "+response+"}; ");
         rejectedLogsRepository.save(new RejectedLog(data.requestId, LocalDateTime.now(), data.toString(), response.toString(),null));

         return response;
    	
    }
    public ErrorsResDto GenerateErrorResponse(String errorCode,String errorText,AuthenticateReqDto data,String optionalDATA)
    {
        Fraud_Authentication_Registration(data,"F","Failed","Failed");

        ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,errorCode,errorText);
         Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={400}; RESPONSE={ "+response+"}; " + optionalDATA);
         rejectedLogsRepository.save(new RejectedLog(data.requestId, LocalDateTime.now(), data.toString(), response.toString(),null));

         return response;
    	
    }



    public void Fraud_Authentication_Registration (AuthenticateReqDto data ,String Loginstatus ,String CIF,String CustomerName) {

        try {

            DateFormat inputFormat =
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            DateFormat dateoutputFormat =
                    new SimpleDateFormat("yyyyMMdd");
            DateFormat timeoutputFormat =
                    new SimpleDateFormat("HH:mm:ss");
            String inputdate = data.timestamp;
            String inputtime = data.timestamp;
            Date date = inputFormat.parse(inputdate);
            Date time = inputFormat.parse(inputtime);
            inputdate = dateoutputFormat.format(date);
            inputtime = timeoutputFormat.format(time);

            String fraudbody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                    "   <soapenv:Header/>\n" +
                    " \t<soapenv:Body>\n" +
                    "\t\t<dat:Authentication_Registration>\n" +
                    "\t\t\t<dat:smh_tran_type>TRX</dat:smh_tran_type>\n" +
                    "\t\t\t<dat:smh_cust_type>I</dat:smh_cust_type>\n" +
                    "\t\t\t<dat:smh_acct_type>NA</dat:smh_acct_type>\n" +
                    "\t\t\t<dat:smh_authenticate_mtd>NC</dat:smh_authenticate_mtd>\n" +
                    "\t\t\t<dat:smh_channel_type>O</dat:smh_channel_type>\n" +
                    "\t\t\t<dat:smh_activity_type>NM</dat:smh_activity_type>\n" +
                    "\t\t\t<dat:smh_activity_detail1>NAP</dat:smh_activity_detail1>\n" +
                    "\t\t\t<dat:smh_activity_detail2>NAP</dat:smh_activity_detail2>\n" +
                    "\t\t\t<dat:smh_activity_detail3>NAP</dat:smh_activity_detail3>\n" +
                    "\t\t\t<dat:smh_client_tran_type>ONST_LF</dat:smh_client_tran_type>\n" +
                    "\t\t\t<dat:smh_priority>0</dat:smh_priority>\n" +
                    "\t\t\t<dat:smh_msg_type>2</dat:smh_msg_type>\n" +
                    "\t\t\t<dat:smh_resp_req>1</dat:smh_resp_req>\n" +
                    "\t\t\t<dat:smh_sdd_ind>1</dat:smh_sdd_ind>\n" +
                    "\t\t\t<dat:smh_dest>SFME</dat:smh_dest>\n" +
                    "\t\t\t<dat:smh_multi_org_name>GLOBAL</dat:smh_multi_org_name>\n" +
                    "\t\t\t<dat:rqo_tran_utc_flag>M</dat:rqo_tran_utc_flag>\n" +
                    "\t\t\t<dat:unm_auth_mtd1>L</dat:unm_auth_mtd1>\n" +
                    "\t\t\t<dat:unm_auth_result1>"+Loginstatus+"</dat:unm_auth_result1>\n" +
                    "\t\t\t<dat:hqo_entity_use_ob_userid>H</dat:hqo_entity_use_ob_userid>\n" +
                    "\t\t\t<dat:tng_category>R</dat:tng_category>\n" +
                    "\t\t\t<dat:tng_tran_status>N</dat:tng_tran_status>\n" +
                    "\t\t\t<dat:smh_source>IPN</dat:smh_source>\n" +
                    "\t\t\t<dat:xqo_cust_num>" + CIF + "</dat:xqo_cust_num>\n" +
                    "\t\t\t<dat:xqo_cust_name>IPN-"+CustomerName+"</dat:xqo_cust_name>\n" +
                    "\t\t\t<dat:hqo_device_id>InstaPay</dat:hqo_device_id>\n" +
                    "\t\t\t<dat:hob_ip_address>196.218.31.218</dat:hob_ip_address>\n" +
                    "\t\t\t<dat:hob_ip_cntry_code>196.218.31.218</dat:hob_ip_cntry_code>\n" +
                    "\t\t\t<dat:hob_match_ind>N</dat:hob_match_ind>\n" +
                    "\t\t\t<dat:tng_tran_type>OL</dat:tng_tran_type>\n" +
                    "\t\t\t<dat:tng_entity>X</dat:tng_entity>\n" +
                    "\t\t\t<dat:tng_details_ind>N</dat:tng_details_ind>\n" +
                    "\t\t\t<dat:hob_website_name>IPN.adcb.com.eg</dat:hob_website_name>\n" +
                    "\t\t\t<dat:hob_webpage_code>A</dat:hob_webpage_code>\n" +
                    "\t\t\t<dat:rqo_tran_date>" + inputdate + "</dat:rqo_tran_date>\n" +
                    "\t\t\t<dat:rqo_tran_time>" + inputtime + "</dat:rqo_tran_time>\n" +
                    "\t\t\t<dat:rqo_tran_date_alt>" + inputdate + "</dat:rqo_tran_date_alt>\n" +
                    "\t\t\t<dat:rqo_tran_time_alt>" + inputtime + "</dat:rqo_tran_time_alt>\n" +
                    "\t\t</dat:Authentication_Registration>\n" +
                    "\t</soapenv:Body>\n" +
                    "</soapenv:Envelope>";

           String response= Request.SendRequest(env.getProperty("IPNBaseURL"), "Authentication_Registration", fraudbody);

            Logging.host("RequestID:"+data.requestId+"||Request{"+fraudbody+"} ||Response{"+response+"}");

        }
        catch(Exception ex){
        	Logging.warn(ExceptionStackTrace.GetStackTrace(ex));

        }

    }

    KeyStore readStore() throws Exception {
    	String KEYSTOREPATH=env.getProperty("KEYSTOREPATH");
    	String KEYSTOREPASS=env.getProperty("KEYSTOREPASS");
    	
        try (InputStream keyStoreStream = this.getClass().getResourceAsStream(KEYSTOREPATH)) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12"); // or "JKS"
            keyStore.load(keyStoreStream, KEYSTOREPASS.toCharArray());
            return keyStore;
        }
    }

}

