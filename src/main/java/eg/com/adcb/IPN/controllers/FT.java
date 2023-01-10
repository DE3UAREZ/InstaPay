package eg.com.adcb.IPN.controllers;


import eg.com.adcb.IPN.*;
import eg.com.adcb.IPN.consts.ApiUrl;
import eg.com.adcb.IPN.consts.Logging;
import eg.com.adcb.IPN.dtos.Request.BalanceEnqReqDto;
import eg.com.adcb.IPN.dtos.Request.CheckStatusReqDto;
import eg.com.adcb.IPN.dtos.Request.FundTransferReqDto;
import eg.com.adcb.IPN.dtos.Request.GetAccountsReqDto;
import eg.com.adcb.IPN.dtos.Response.*;
import eg.com.adcb.IPN.models.*;
import eg.com.adcb.IPN.repositories.*;
import eg.com.adcb.IPN.utils.ExceptionStackTrace;
import eg.com.adcb.IPN.utils.Request;
import eg.com.adcb.IPN.utils.XMLParser;
//import org.apache.log4j.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.util.Streamable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.json.*;

public class FT {

    public static Environment env;
   // private static Logging Logging = Logging.getLogging(Logging.class);
    public static AccountsRepository accountsRepository;
    public static AcceptedLogsRepository acceptedLogsRepository;
    public static ReversalLogsRepository reversalLogsRepository;
    public static RejectedLogsRepository rejectedLogsRepository;
    public static LoginsRepository loginsRepository;
    public static CardsRepository cardsRepository;
    public static ErrorsRepository errorsRepository;
    public static AllLogsRepsitory allLogsRepsitory;

   // private static mfieldsRepository mfieldsRepoX;

 /*
    public static FundTransferResDto ProcessTransaction(Environment env, AccountsRepository accountsRepository, AcceptedLogsRepository acceptedLogsRepository, RejectedLogsRepository rejectedLogsRepository, LoginsRepository loginsRepository, CardsRepository cardsRepository, ErrorsRepository errorsRepository, AllLogsRepsitory allLogsRepsitory,ReversalLogsRepository reversalLogsRepository,mfieldsRepository mfieldsRepoX) {
        this.env = env;
        this.accountsRepository = accountsRepository;
        this.acceptedLogsRepository = acceptedLogsRepository;
        this.rejectedLogsRepository = rejectedLogsRepository;
        this.loginsRepository = loginsRepository;
        this.cardsRepository = cardsRepository;
        this.errorsRepository = errorsRepository;
        this.allLogsRepsitory = allLogsRepsitory;
        this.reversalLogsRepository = reversalLogsRepository;
        this.mfieldsRepoX=mfieldsRepoX;
    }
*/


    public static CombinedResDto Check_Status_FT(CheckStatusReqDto data) throws Exception {


        String fimiTrxRef=null;
        String refMDP=null;

        String balance = "";
        double deductedAmount=0;

        if(data.bearFee!=null) {
            deductedAmount = Float.parseFloat(data.amount.get("orgValue"))+Float.parseFloat(data.bankFee.get("value"))+Float.parseFloat(data.pspFee.get("value"))-Float.parseFloat(data.bearFee.get("value"));
        }
        else {
            deductedAmount = Float.parseFloat(data.amount.get("orgValue"))+Float.parseFloat(data.bankFee.get("value"))+Float.parseFloat(data.pspFee.get("value"));
        }
        DecimalFormat df = new DecimalFormat("#.##");
        deductedAmount = Double.valueOf(df.format(deductedAmount));
        try{
            String body = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                    "   <soap:Header/>\n" +
                    "   <soap:Body>\n" +
                    "      <dat:CustomerDetails>\n" +
                    "         <dat:CUSTNO> 1127363 </dat:CUSTNO>\n" +
                    "      </dat:CustomerDetails>\n" +
                    "   </soap:Body>\n" +
                    "</soap:Envelope>";
            String responseBody = Request.SendRequest(env.getProperty("IPNBaseURL"), "CustomerDetails", body);
            Document doc = XMLParser.ReadXML(responseBody);
            Map<String, String> customerDetails = new HashMap<>();
            String nationalId = doc.getElementsByTagName("LEGALID").item(0).getTextContent();
            String mobileNumber = doc.getElementsByTagName("TELNO").item(0).getTextContent();
        }

        catch (Exception ex){
            CombinedResDto response= new CombinedResDto ();
            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={400}; RESPONSE={ Service is currently not available from your bank, try again later}; ");
            Logging.warn(ExceptionStackTrace.GetStackTrace(ex));

            response.errorres=new ErrorsResDto(data.requestId,data.timestamp,"11013","Service is currently not available from your bank, try again later");
            return response;
        }


        String nullCheck = "";//checkNull(data, "fundtransfer");
        if (nullCheck != "") {
            CombinedResDto response = new CombinedResDto();
            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
            response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "12015", "Invalid technical data");
            return response;
        }
        if (data.atmID!=null){
            if (data.atmID.length()>16){
                CombinedResDto response = new CombinedResDto();
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "12015", "Invalid technical data");
                return response;
            }
        }
        if (data.atmLocation!=null){
            if (data.atmLocation.length()>100){
                CombinedResDto response = new CombinedResDto();
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "12015", "Invalid technical data");
                return response;
            }
        }


        if (data.requestType.equals("REVERSAL")||data.requestType.equalsIgnoreCase("CREDIT")){
            if (data.debitPoolAccount==null){
                CombinedResDto response = new CombinedResDto();
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "12015", "Invalid technical data");
                return response;
            }

        }
        if (data.mandateID!=null){
            if (data.mandateID.equals("")||data.mandateID.length()>100){
                CombinedResDto response = new CombinedResDto();
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "12015", "Invalid technical data");
                return response;
            }
        }
        if (data.creditAccountType!=null) {
            if (data.creditAccountType.equalsIgnoreCase("ACCOUNT")&&data.requestType.equalsIgnoreCase("CREDIT")) {
                String ReqBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                        "   <soapenv:Header/>\n" +
                        "   <soapenv:Body>\n" +
                        "      <dat:ACCOUNTDETAILS>\n" +
                        "         <dat:ACCNO>" + data.creditAccountId.substring(data.creditAccountId.length() - 16) + "</dat:ACCNO>\n" +
                        "      </dat:ACCOUNTDETAILS>\n" +
                        "   </soapenv:Body>\n" +
                        "</soapenv:Envelope>";
                String res_Balance = Request.SendRequest(env.getProperty("IPNBaseURL"), "ACCOUNTDETAILS", ReqBody);
                if (res_Balance.contains("No records were found")) {
                    CombinedResDto response = new CombinedResDto();
                    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={ is not found}; ");
                    response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13001", "The beneficiary account number is invalid");
                    return response;
                }
                Document doc_balance = XMLParser.ReadXML(res_Balance);
                String currency = doc_balance.getElementsByTagName("CCYDESC").item(0).getTextContent();
                if (currency.equals("USD")) {
                    CombinedResDto response = new CombinedResDto();
                    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={ is not found}; ");
                    response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                    return response;
                }

            }
        }

        String daccountId=data.debitAccountId.substring(3);
        if (!daccountId.matches("[0-9]+")){
            CombinedResDto response = new CombinedResDto();
            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={ is not found}; ");
            response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "12015", "Invalid technical data");
            return response;
        }
        if (data.requestType.equalsIgnoreCase("REVERSAL")){

            if(!data.orgTxnId.equalsIgnoreCase(data.transactionId)){

                CombinedResDto response = new CombinedResDto();
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={ is not found}; ");
                response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "12031", "Original transaction not found");
                return response;
            }
            else{

                if(Check_Transaction_Status(data).equalsIgnoreCase("rejected")||Check_Transaction_Status(data).equalsIgnoreCase("TXn_NOT_EXIST"))  {

                    CombinedResDto response = new CombinedResDto();
                    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={ is not found}; ");
                    response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "12031", "Original transaction not found");
                    return response;

                }
                else if(Check_Transaction_Status(data).equalsIgnoreCase("reversed"))  {

                    CombinedResDto response = new CombinedResDto();
                    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={ is not found}; ");
                    response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "12030", "Reversal already processed");
                    return response;

                }
                else{

                    if(Check_Account_Existence(data)!=null){

                        CombinedResDto response = new CombinedResDto();
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={ is not found}; ");
                        response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "12031", "Account not found");
                        return response;
                    }
                    else{
                        String CustomerNumber;
                        CustomerNumber = data.requestType.equalsIgnoreCase("DEBIT")
                                ? data.debitAccountId.substring(data.debitAccountId.length()-15, 8)
                                : data.requestType.equalsIgnoreCase("CREDIT")
                                ? data.creditAccountId.substring(data.creditAccountId.length()-15, 8)
                                : data.debitAccountId.substring(0,3).equals("EGP")
                                ?data.creditAccountId.substring(data.creditAccountId.length()-15, 8)
                                :data.creditAccountId.substring(0,3).equals("EGP")
                                ?data.debitAccountId.substring(data.debitAccountId.length()-15, 8)
                                :null;

                        return  Credit_Account(CustomerNumber,data);

                    }

                }


            }
        }
        if (data.amount.get("orgCurr").equals("USD")) {
            CombinedResDto response = new CombinedResDto();
            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={ is not found}; ");
            response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "12005", "Restricted transaction, please contact your bank");
            return response;
        }
        if (data.debitAccountType.equalsIgnoreCase("ACCOUNT")){
            String ReqBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <dat:ACCOUNTDETAILS>\n" +
                    "         <dat:ACCNO>" + (data.debitAccountId.length()==29?data.debitAccountId.substring(data.debitAccountId.length() - 16):data.debitAccountId )+ "</dat:ACCNO>\n" +
                    "      </dat:ACCOUNTDETAILS>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope>";
            boolean check= false;

            String res_Balance = Request.SendRequest(env.getProperty("IPNBaseURL"), "ACCOUNTDETAILS", ReqBody);
            if (res_Balance.contains("No records were found")&&data.requestType.equalsIgnoreCase("DEBIT")){
                CombinedResDto response = new CombinedResDto();
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={ is not found}; ");
                response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "12011", "Transaction cannot be processed by your bank, try again later");
                return response;
            }
            try {
                Document doc2 = XMLParser.ReadXML(res_Balance);
                String phoneNum = doc2.getElementsByTagName("PHONENUMBER").item(0).getTextContent();
                String accStatus = doc2.getElementsByTagName("STATUS").item(0).getTextContent();
                if (accStatus.equals("D")){
                    CombinedResDto response = new CombinedResDto();
                    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={ is not found}; ");
                    response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "12013", "Restricted account, please contact your bank");
                    return response;
                }
                phoneNum=AddCountryCode(phoneNum);
                if (!data.payerMobileNumber.equals(phoneNum)) {
                    CombinedResDto response = new CombinedResDto();
                    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={ is not found}; ");
                    response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "12004", "Your mobile number is not matching with bank records, please contact your bank");
                    return response;
                }
            }
            catch (Exception e){

            }
        }
        if (data.creditAccountId!=null){
            if (!data.creditAccountId.substring(3).matches("[0-9]+")){
                CombinedResDto response= new CombinedResDto ();
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={400}; RESPONSE={ Service is currently not available from your bank, try again later}; ");
                response.errorres=new ErrorsResDto(data.requestId,data.timestamp,"13001","The beneficiary account number is invalid");
                return response;
            }
        }
        if (!data.debitAccountId.substring(3).matches("[0-9]+")){
            CombinedResDto response= new CombinedResDto ();
            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={400}; RESPONSE={ Service is currently not available from your bank, try again later}; ");
            response.errorres=new ErrorsResDto(data.requestId,data.timestamp,"13001","The beneficiary account number is invalid");
            return response;
        }
        try {



            //Balance Check
            try {

                if (data.creditAccountType!=null) {
                    if (data.creditAccountType.equals("ACCOUNT")&&data.requestType.equalsIgnoreCase("CREDIT")){
                        String ReqBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                                "   <soapenv:Header/>\n" +
                                "   <soapenv:Body>\n" +
                                "      <dat:ACCOUNTDETAILS>\n" +
                                "         <dat:ACCNO>" + data.creditAccountId.substring(data.creditAccountId.length() - 16) + "</dat:ACCNO>\n" +
                                "      </dat:ACCOUNTDETAILS>\n" +
                                "   </soapenv:Body>\n" +
                                "</soapenv:Envelope>";
                        String res_Balance = Request.SendRequest(env.getProperty("IPNBaseURL"), "ACCOUNTDETAILS", ReqBody);
                        if (res_Balance.contains("No records were found")){
                            CombinedResDto response = new CombinedResDto();
                            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                            response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13001", "The beneficiary account number is invalid");
                            return response;
                        }
                        Document doc_balance = XMLParser.ReadXML(res_Balance);
                        String currency = doc_balance.getElementsByTagName("CCYDESC").item(0).getTextContent();
                        if (currency.equals("USD")){
                            CombinedResDto response = new CombinedResDto();
                            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                            response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                            return response;
                        }

                    }
                    if (data.debitAccountType.equals("ACCOUNT")) {


                        String ReqBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                                "   <soapenv:Header/>\n" +
                                "   <soapenv:Body>\n" +
                                "      <dat:ACCOUNTDETAILS>\n" +
                                "         <dat:ACCNO>" ;

                        if(data.requestType.equalsIgnoreCase("CREDIT")) {
                            if(data.creditAccountType.equalsIgnoreCase("CARD")&&data.creditAccountId.startsWith("45"))
                            {

                                ReqBody += Get_account_from_card_number(data.creditAccountId);

                            }
                            else {
                                ReqBody += (data.creditAccountId.length() == 29 ? data.creditAccountId.substring(data.creditAccountId.length() - 16) : data.creditAccountId);
                            }
                        }
                        else{
                            ReqBody+=  (data.debitAccountId.length() == 29 ? data.debitAccountId.substring(data.debitAccountId.length() - 16) : data.debitAccountId);
                        }
                        ReqBody+= "</dat:ACCNO>\n" +
                                "      </dat:ACCOUNTDETAILS>\n" +
                                "   </soapenv:Body>\n" +
                                "</soapenv:Envelope>";
                        String res_Balance = Request.SendRequest(env.getProperty("IPNBaseURL"), "ACCOUNTDETAILS", ReqBody);
                        if (res_Balance.contains("No records were found") &&data.requestType.equalsIgnoreCase("DEBIT")){
                            CombinedResDto response = new CombinedResDto();
                            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                            response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13001", "The beneficiary account number is invalid");
                            return response;
                        }
                        try {
                            Document doc_balance = XMLParser.ReadXML(res_Balance);
                            balance = doc_balance.getElementsByTagName("BAL_AVAIL").item(0).getTextContent();
                            System.out.println("sadsd" + balance);
                            if (Float.parseFloat(balance) < Float.parseFloat(data.amount.get("orgValue"))) {
                                String ErrorDesc = errorsRepository.getErrorDes("INSUFFICIENT BALANCE");
                                String ErrorCode = errorsRepository.getErrorCode("INSUFFICIENT BALANCE");
                                CombinedResDto errResp = new CombinedResDto();
                                errResp.errorres=new ErrorsResDto(data.requestId, data.timestamp, "12001", "Insufficient balance to execute the transaction");
                                return errResp;
                            }
                        }
                        catch (Exception e){}
                    }

                }
            }
            catch (Exception ex){

            }

            //========================================================

            List<Card> cards = Streamable.of(cardsRepository.findAll()).toList();
            String debitAccount = null, creditAccount = null, customerNumber = null;
//            if (data.creditAccountType.equals("CARD")&&(!cards.stream().anyMatch(x -> x.getIssuer().equals("NI") && x.getStartsWith().equals(data.creditAccountId.substring(0, 6)))||!cards.stream().anyMatch(x -> x.getIssuer().equals("MDP") && x.getStartsWith().equals(data.creditAccountId.substring(0, 6))))){
//                CombinedResDto response = new CombinedResDto(data.requestId, data.timestamp, "13001", "The beneficiary account number is invalid");
//                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
//                return response;
//
//            }

            if (data.creditAccountType!=null){
                if (data.creditAccountType.equals("ACCOUNT")&&data.requestType.equalsIgnoreCase("CREDIT")){
                    String test_body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <dat:ACCOUNTDETAILS>\n" +
                            "         <dat:ACCNO>" + data.creditAccountId.substring(data.creditAccountId.length() - 16) + "</dat:ACCNO>\n" +
                            "      </dat:ACCOUNTDETAILS>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";
                    String res_TEST = Request.SendRequest(env.getProperty("IPNBaseURL"), "ACCOUNTDETAILS", test_body);
                    Document doc2 = XMLParser.ReadXML(res_TEST);
                    if (res_TEST.contains("No records were found")&& data.requestType.equalsIgnoreCase("CREDIT")){
                        CombinedResDto response = new CombinedResDto();
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                        response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13001", "The beneficiary account number is invalid");
                        return response;
                    }
                    try{
                        balance = doc2.getElementsByTagName("BAL_AVAIL").item(0).getTextContent();
                        String currency = doc2.getElementsByTagName("CCYDESC").item(0).getTextContent();
                        String status = doc2.getElementsByTagName("STATUS").item(0).getTextContent();
                        String phoneNum = doc2.getElementsByTagName("PHONENUMBER").item(0).getTextContent();
                        phoneNum=AddCountryCode(phoneNum);
                        if (data.payeeMobileNumber!=null){
                            if (!phoneNum.equals(data.payeeMobileNumber)){
                                CombinedResDto response = new CombinedResDto();
                                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                                response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13002", "Transaction rejected from beneficiary bank due to invalid beneficiary information");
                                return response;
                            }
                        }
                        if (!currency.equals("EGP")){
                            CombinedResDto response = new CombinedResDto();
                            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                            response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                            return response;
                        }
                        if (status.equals("D")){
                            CombinedResDto response = new CombinedResDto();
                            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                            response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                            return response;
                        }
                    }
                    catch (Exception e){}

                }
                else if (data.debitAccountType.equals("ACCOUNT")&&data.requestType.equalsIgnoreCase("DEBIT")){
                    String test_body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <dat:ACCOUNTDETAILS>\n" +
                            "         <dat:ACCNO>" + data.debitAccountId.substring(data.debitAccountId.length() - 16) + "</dat:ACCNO>\n" +
                            "      </dat:ACCOUNTDETAILS>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";
                    String res_TEST = Request.SendRequest(env.getProperty("IPNBaseURL"), "ACCOUNTDETAILS", test_body);
                    Document doc2 = XMLParser.ReadXML(res_TEST);
                    if (res_TEST.contains("No records were found")&& data.requestType.equalsIgnoreCase("DEBIT")){
                        CombinedResDto response = new CombinedResDto();
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                        response.errorres=new ErrorsResDto (data.requestId, data.timestamp, "13001", "The beneficiary account number is invalid");
                        return response;
                    }
                    try{
                        balance = doc2.getElementsByTagName("BAL_AVAIL").item(0).getTextContent();
                        String currency = doc2.getElementsByTagName("CCYDESC").item(0).getTextContent();
                        String status = doc2.getElementsByTagName("STATUS").item(0).getTextContent();
                        String phoneNum = doc2.getElementsByTagName("PHONENUMBER").item(0).getTextContent();
                        phoneNum=AddCountryCode(phoneNum);
                        if (data.payerMobileNumber!=null){
                            if (!phoneNum.equals(data.payerMobileNumber)){
                                CombinedResDto response = new CombinedResDto();
                                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                                response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13002", "Transaction rejected from beneficiary bank due to invalid beneficiary information");
                                return response;
                            }
                        }
                        if (!currency.equals("EGP")){
                            CombinedResDto response = new CombinedResDto();
                            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                            response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                            return response;
                        }
                        if (status.equals("D")){
                            CombinedResDto response = new CombinedResDto();
                            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                            response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                            return response;
                        }
                    }
                    catch (Exception e){}

                }
                else if (data.creditAccountType.equals("CARD")&&data.requestType.equalsIgnoreCase("CREDIT") && cards.stream().anyMatch(x -> x.getIssuer().equals("MDP") && x.getStartsWith().equals(data.creditAccountId.substring(0, 6)))) {
                    String mdpCardInfo2="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:mdp=\"http://ws.wso2.org/dataservice/mdpCardInfo\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <mdp:_getmdpcardinfo>\n" +
                            "         <mdp:cardNumber>"+data.creditAccountId+"</mdp:cardNumber>\n" +
                            "      </mdp:_getmdpcardinfo>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";
                    String resMdpCardInfo2 = Request.SendRequest(env.getProperty("MDPBaseURL"), "_getmdpcardinfo", mdpCardInfo2);
                    if (resMdpCardInfo2.contains("Unable to find card")){
                        CombinedResDto response= new CombinedResDto ();
                        response.errorres=new ErrorsResDto(data.requestId,data.timestamp,"13001","The beneficiary account number is invalid");
                        return response;
                    }

                    Document docMdp2 = XMLParser.ReadXML(resMdpCardInfo2);
                    String innerXMLmdpCard = docMdp2.getElementsByTagName("response").item(0).getTextContent();
                    docMdp2 = XMLParser.ReadXML(innerXMLmdpCard);
                    String state = docMdp2.getElementsByTagName("state").item(0).getTextContent();
                    if (state.equals("Closed")){
                        CombinedResDto response = new CombinedResDto();
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                        response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                        return response;
                    }
                    String mdpCardInfo="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:cus=\"http://ws.wso2.org/dataservice/customerInfoFromCardNumber\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <cus:_getcustomerinfofromcardnumber>\n" +
                            "         <cus:cardNumber>"+data.creditAccountId+"</cus:cardNumber>\n" +
                            "      </cus:_getcustomerinfofromcardnumber>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";
                    String resMdpCardInfo = Request.SendRequest(env.getProperty("MDPBaseURL"), "_getcustomerinfofromcardnumber", mdpCardInfo);
                    if(resMdpCardInfo.contains("Unable to find card")){
                        CombinedResDto response= new CombinedResDto();
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={400}; RESPONSE={ Service is currently not available from your bank, try again later}; ");
                        response.errorres=new ErrorsResDto(data.requestId,data.timestamp,"13001","The beneficiary account number is invalid");
                        return response;
                    }
                    Document docMdp = XMLParser.ReadXML(resMdpCardInfo);
                    String innerXMLmdp = docMdp.getElementsByTagName("response").item(0).getTextContent();
                    docMdp = XMLParser.ReadXML(innerXMLmdp);
                    String mobileNumber = docMdp.getElementsByTagName("mobileNumber").item(0).getTextContent();

                    mobileNumber = AddCountryCode(mobileNumber);

                   // mobileNumber="002"+mobileNumber;

                    if (data.payeeMobileNumber!=null) {
                        if (!data.payeeMobileNumber.equals(mobileNumber)) {
                            CombinedResDto response = new CombinedResDto();
                            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                            response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13002", "Transaction rejected from beneficiary bank due to invalid beneficiary information");
                            return response;
                        }
                    }
                }
                else if (data.debitAccountType.equals("CARD")&&cards.stream().anyMatch(x -> x.getIssuer().equals("MDP") && x.getStartsWith().equals(data.debitAccountId.substring(0, 6)))){
                    String mdpCardInfo2="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:mdp=\"http://ws.wso2.org/dataservice/mdpCardInfo\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <mdp:_getmdpcardinfo>\n" +
                            "         <mdp:cardNumber>"+data.debitAccountId+"</mdp:cardNumber>\n" +
                            "      </mdp:_getmdpcardinfo>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";
                    String resMdpCardInfo2 = Request.SendRequest(env.getProperty("MDPBaseURL"), "_getmdpcardinfo", mdpCardInfo2);
                    if (resMdpCardInfo2.contains("Unable to find card")){
                        CombinedResDto response= new CombinedResDto ();
                        response.errorres=new ErrorsResDto(data.requestId,data.timestamp,"12011","Transaction cannot be processed by your bank, try again later");
                        return response;
                    }

                    Document docMdp2 = XMLParser.ReadXML(resMdpCardInfo2);
                    String innerXMLmdpCard = docMdp2.getElementsByTagName("response").item(0).getTextContent();
                    docMdp2 = XMLParser.ReadXML(innerXMLmdpCard);
                    String state = docMdp2.getElementsByTagName("state").item(0).getTextContent();
                    String expDate = docMdp2.getElementsByTagName("expirDate").item(0).getTextContent();
                    if (state.equals("Closed")&&data.requestType.equalsIgnoreCase("DEBIT")){
                        CombinedResDto errResp = new CombinedResDto();
                        errResp.errorres=new ErrorsResDto(data.requestId, data.timestamp, "12013", "Restricted account, please contact your bank");
                        return errResp;
                    }
                    balance=docMdp2.getElementsByTagName("avalBalance").item(0).getTextContent();
                    if (Float.parseFloat(balance) < Float.parseFloat(data.amount.get("orgValue"))&&data.requestType.equalsIgnoreCase("DEBIT")) {

                        CombinedResDto errResp = new CombinedResDto();
                        errResp.errorres=new ErrorsResDto(data.requestId, data.timestamp, "12001", "Insufficient balance to execute the transaction");
                        return errResp;
                    }


                }
                else if (data.creditAccountType.equals("CARD")&&data.requestType.equalsIgnoreCase("CREDIT")&&!cards.stream().anyMatch(x -> x.getIssuer().equals("NI") && x.getStartsWith().equals(data.creditAccountId.substring(0, 6)))){
                    CombinedResDto response = new CombinedResDto();
                    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                    response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13001", "The beneficiary account number is invalid");
                    return response;
                }


                if (data.creditAccountType.equals("CARD")&&data.requestType.equalsIgnoreCase("CREDIT")&&cards.stream().anyMatch(x -> x.getIssuer().equals("MDP") && x.getStartsWith().equals(data.creditAccountId.substring(0, 6)))){
                    String mdpCardInfo2="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:mdp=\"http://ws.wso2.org/dataservice/mdpCardInfo\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <mdp:_getmdpcardinfo>\n" +
                            "         <mdp:cardNumber>"+data.creditAccountId+"</mdp:cardNumber>\n" +
                            "      </mdp:_getmdpcardinfo>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";
                    String resMdpCardInfo2 = Request.SendRequest(env.getProperty("MDPBaseURL"), "_getmdpcardinfo", mdpCardInfo2);
                    if (resMdpCardInfo2.contains("Unable to find card")){
                        CombinedResDto response= new CombinedResDto ();
                        response.errorres=new ErrorsResDto(data.requestId,data.timestamp,"12013","Restricted account, please contact your bank");
                        return response;
                    }

                    Document docMdp2 = XMLParser.ReadXML(resMdpCardInfo2);
                    String innerXMLmdpCard = docMdp2.getElementsByTagName("response").item(0).getTextContent();
                    docMdp2 = XMLParser.ReadXML(innerXMLmdpCard);
                    //String backOfficeStatus = docMdp.getElementsByTagName("backOfficeStatus").item(0).getTextContent();
                    String expDate = docMdp2.getElementsByTagName("expirDate").item(0).getTextContent();
                    balance=docMdp2.getElementsByTagName("avalBalance").item(0).getTextContent();
                    String statusCode=docMdp2.getElementsByTagName("statusCode").item(0).getTextContent();
                    if (!statusCode.equals("0")){
                        CombinedResDto response = new CombinedResDto();
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                        response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                        return response;
                    }

                }


            }
            try{
                if (data.creditAccountType==null&&data.creditAccountId.length()>=16&&data.requestType.equalsIgnoreCase("CREDIT")){
                    String test_body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <dat:ACCOUNTDETAILS>\n" +
                            "         <dat:ACCNO>" + data.creditAccountId.substring(data.creditAccountId.length() - 16) + "</dat:ACCNO>\n" +
                            "      </dat:ACCOUNTDETAILS>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";
                    String res_TEST = Request.SendRequest(env.getProperty("IPNBaseURL"), "ACCOUNTDETAILS", test_body);
                    Document doc2 = XMLParser.ReadXML(res_TEST);
                    if (res_TEST.contains("No records were found")&& data.requestType.equalsIgnoreCase("CREDIT")){
                        CombinedResDto response = new CombinedResDto();
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                        response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13001", "The beneficiary account number is invalid");
                        return response;
                    }

                    balance = doc2.getElementsByTagName("BAL_AVAIL").item(0).getTextContent();
                    String currency = doc2.getElementsByTagName("CCYDESC").item(0).getTextContent();
                    String status = doc2.getElementsByTagName("STATUS").item(0).getTextContent();
                    String phoneNum = doc2.getElementsByTagName("PHONENUMBER").item(0).getTextContent();
                    phoneNum=AddCountryCode(phoneNum);
                    if (data.payeeMobileNumber!=null){
                        if (!phoneNum.equals(data.payeeMobileNumber)){
                            CombinedResDto response = new CombinedResDto();
                            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                            response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13002", "Transaction rejected from beneficiary bank due to invalid beneficiary information");
                            return response;
                        }
                    }
                    if (!currency.equals("EGP")){
                        CombinedResDto response = new CombinedResDto();
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                        response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                        return response;
                    }
                    if (status.equals("D")){
                        CombinedResDto response = new CombinedResDto();
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                        response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                        return response;
                    }
                }


            }
            catch (Exception e){}
            if (data.creditAccountType != null) {
                if (data.debitAccountType.equalsIgnoreCase("ACCOUNT") && data.creditAccountType.equalsIgnoreCase("ACCOUNT")) {
                    debitAccount = data.requestType.equalsIgnoreCase("DEBIT")
                            ? data.debitAccountId
                            : data.requestType.equalsIgnoreCase("CREDIT")
                            ? data.debitPoolAccount
                            : null; // REVERSAL, BLOCK, UNBLOCK !!! TODO: not handled
                    creditAccount = data.requestType.equalsIgnoreCase("DEBIT")
                            ? data.creditAccountId
                            : data.requestType.equalsIgnoreCase("CREDIT")
                            ? data.debitAccountId
                            : null; // REVERSAL, BLOCK, UNBLOCK !!! TODO: not handled

                    // ---------------------------------------------------------------------- //
                    customerNumber = data.requestType.equalsIgnoreCase("DEBIT")
                            ? data.debitAccountId.substring(1, 8)
                            : data.requestType.equalsIgnoreCase("CREDIT")
                            ? data.creditAccountId.substring(1, 8)
                            : null;
                } else if (data.debitAccountType.equalsIgnoreCase("CARD") || data.creditAccountType.equalsIgnoreCase("CARD")) {
                    debitAccount = data.requestType.equalsIgnoreCase("DEBIT")
                            ? data.debitAccountId
                            : data.requestType.equalsIgnoreCase("CREDIT")
                            ? data.debitPoolAccount
                            : null; // REVERSAL, BLOCK, UNBLOCK !!! TODO: not handled
                    creditAccount = data.requestType.equalsIgnoreCase("DEBIT")
                            ? data.creditPoolAccount
                            : data.requestType.equalsIgnoreCase("CREDIT")
                            ? data.creditAccountId
                            : null; // REVERSAL, BLOCK, UNBLOCK !!! TODO: not handled
                    // ---------------------------------------------------------------------- //
                    if (data.creditAccountType==null&&data.creditAccountId.length()>=16&&data.requestType.equalsIgnoreCase("CREDIT")){
                        String test_body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                                "   <soapenv:Header/>\n" +
                                "   <soapenv:Body>\n" +
                                "      <dat:ACCOUNTDETAILS>\n" +
                                "         <dat:ACCNO>" + data.creditAccountId.substring(data.creditAccountId.length() - 16) + "</dat:ACCNO>\n" +
                                "      </dat:ACCOUNTDETAILS>\n" +
                                "   </soapenv:Body>\n" +
                                "</soapenv:Envelope>";
                        String res_TEST = Request.SendRequest(env.getProperty("IPNBaseURL"), "ACCOUNTDETAILS", test_body);
                        Document doc2 = XMLParser.ReadXML(res_TEST);
                        if (res_TEST.contains("No records were found")&& data.requestType.equalsIgnoreCase("CREDIT")){
                            CombinedResDto response = new CombinedResDto();
                            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                            response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13001", "The beneficiary account number is invalid");
                            return response;
                        }
                        try{
                            balance = doc2.getElementsByTagName("BAL_AVAIL").item(0).getTextContent();
                            String currency = doc2.getElementsByTagName("CCYDESC").item(0).getTextContent();
                            String status = doc2.getElementsByTagName("STATUS").item(0).getTextContent();
                            String phoneNum = doc2.getElementsByTagName("PHONENUMBER").item(0).getTextContent();
                            phoneNum=AddCountryCode(phoneNum);
                            if (data.payeeMobileNumber!=null){
                                if (!phoneNum.equals(data.payeeMobileNumber)){
                                    CombinedResDto response = new CombinedResDto();
                                    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                                    response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13002", "Transaction rejected from beneficiary bank due to invalid beneficiary information");
                                    return response;
                                }
                            }
                            if (!currency.equalsIgnoreCase("EGP")){
                                CombinedResDto response = new CombinedResDto();
                                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                                response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                                return response;
                            }
                            if (status.equals("D")){
                                CombinedResDto response = new CombinedResDto();
                                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                                response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                                return response;
                            }
                        }
                        catch (Exception e){}

                    }

                    else if ((data.debitAccountType.equals("CARD") || data.creditAccountType.equals("CARD")) && cards.stream().anyMatch(x -> x.getIssuer().equals("NI") && (x.getStartsWith().equals(data.creditAccountId.substring(0, 6))||x.getStartsWith().equals(data.debitAccountId.substring(0, 6))))) {

                        // call NI to fetch customer number from deibt account id
                        String body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                                "   <soapenv:Header/>\n" +
                                "   <soapenv:Body>\n" +
                                "      <dat:GetCardInfoRq>\n" +
                                "         <dat:Request1><![CDATA[\n" +
                                "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:fimi=\"http://schemas.compassplus.com/two/1.0/fimi.xsd\" xmlns:fimi1=\"http://schemas.compassplus.com/two/1.0/fimi_types.xsd\">\n" +
                                "\t<soap:Header/>\n" +
                                "\t<soap:Body>\n" +
                                "\t\t<fimi:GetCardInfoRq>\n" +
                                "\t\t\t<fimi:Request   __HEADERPARAM__  >\n" +
                                "\t\t\t\t<fimi1:PAN>" + (data.requestType.equalsIgnoreCase("DEBIT") || data.debitAccountId.charAt(0) == '4' ? data.debitAccountId : data.creditAccountId) + "</fimi1:PAN>\n" +
                                "\t\t\t</fimi:Request>\n" +
                                "\t\t</fimi:GetCardInfoRq>\n" +
                                "\t</soap:Body>\n" +
                                "</soap:Envelope>\n" +
                                "]]></dat:Request1>\n" +
                                "      </dat:GetCardInfoRq>\n" +
                                "   </soapenv:Body>\n" +
                                "</soapenv:Envelope>";
                        // (data.requestType.equalsIgnoreCase("DEBIT") ? data.debitAccountId : data.creditAccountId)
                        String res = Request.SendRequest(env.getProperty("NIBaseURL"), "GetCardInfoRq", body);
                        Document doc = XMLParser.ReadXML(res);
                        String innerXml = doc.getElementsByTagName("FEMI_RESPONSE").item(0).getTextContent();
                        innerXml = innerXml.replaceAll(":", "_");
                        doc = XMLParser.ReadXML(innerXml);
                        customerNumber = doc.getElementsByTagName("m0_PersonExtId").item(0).getTextContent();

                        if (data.creditAccountId.substring(0, 2).equalsIgnoreCase("45")) {

                            balance=Get_Account_Balance(Get_account_from_card_number(data.creditAccountId));//case credit to debit card number

                        } else
                        {
                            balance = doc.getElementsByTagName("m0_AvailBalance").item(0).getTextContent();
                        }

                        System.out.println("customer Number ni " + balance);
                    } else if ((data.debitAccountType.equals("CARD") || data.creditAccountType.equals("CARD")) && cards.stream().anyMatch(x -> x.getIssuer().equals("MDP") && x.getStartsWith().equals(data.debitAccountId.substring(0, 6)))) {
                        // call meeza to fetch customer number from debit account id
                        String body = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:cus=\"http://ws.wso2.org/dataservice/customerInfoFromCardNumber\">\n" +
                                "   <soap:Header/>\n" +
                                "   <soap:Body>\n" +
                                "      <cus:_getcustomerinfofromcardnumber>\n" +
                                "         <cus:cardNumber>" + (data.requestType.equalsIgnoreCase("DEBIT") ? data.debitAccountId : data.creditAccountId) + "</cus:cardNumber>\n" +
                                "      </cus:_getcustomerinfofromcardnumber>\n" +
                                "   </soap:Body>\n" +
                                "</soap:Envelope>";
                        String res = Request.SendRequest(env.getProperty("MDPBaseURL"), "_getcustomerinfofromcardnumber", body);

                        Document doc = XMLParser.ReadXML(res);
                        String innerXml = doc.getElementsByTagName("response").item(0).getTextContent();
                        doc = XMLParser.ReadXML(innerXml);
                        String mNumber=doc.getElementsByTagName("mobileNumber").item(0).getTextContent();

                        mNumber=AddCountryCode(mNumber);

                        if (!mNumber.equals(data.payerMobileNumber)){
                            CombinedResDto response = new CombinedResDto();
                            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                            response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "12004", "Your mobile number is not matching with bank records, please contact your bank");
                            return response;
                        }
                        customerNumber = doc.getElementsByTagName("customerNumber").item(0).getTextContent();
                        System.out.println("Customer Number 10/10: " + customerNumber);
                    }

                }
                if (data.requestType.equalsIgnoreCase("CREDIT")&&cards.stream().anyMatch(x -> x.getIssuer().equals("MDP") && x.getStartsWith().equals(data.creditAccountId.substring(0, 6)))){
                    String mdpCardInfo2="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:mdp=\"http://ws.wso2.org/dataservice/mdpCardInfo\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <mdp:_getmdpcardinfo>\n" +
                            "         <mdp:cardNumber>"+data.creditAccountId+"</mdp:cardNumber>\n" +
                            "      </mdp:_getmdpcardinfo>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";
                    String resMdpCardInfo2 = Request.SendRequest(env.getProperty("MDPBaseURL"), "_getmdpcardinfo", mdpCardInfo2);
                    if (resMdpCardInfo2.contains("Unable to find card")){
                        CombinedResDto response= new CombinedResDto();
                        response.errorres=new ErrorsResDto(data.requestId,data.timestamp,"12013","Restricted account, please contact your bank");
                        return response;
                    }

                    Document docMdp2 = XMLParser.ReadXML(resMdpCardInfo2);
                    String innerXMLmdpCard = docMdp2.getElementsByTagName("response").item(0).getTextContent();
                    docMdp2 = XMLParser.ReadXML(innerXMLmdpCard);
                    //String backOfficeStatus = docMdp.getElementsByTagName("backOfficeStatus").item(0).getTextContent();
                    String expDate = docMdp2.getElementsByTagName("expirDate").item(0).getTextContent();
                    balance=docMdp2.getElementsByTagName("avalBalance").item(0).getTextContent();
                    String statusCode=docMdp2.getElementsByTagName("statusCode").item(0).getTextContent();
                    if (!statusCode.equals("0")){
                        CombinedResDto response = new CombinedResDto();
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                        response.errorres=new ErrorsResDto (data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                        return response;
                    }

                }
            } else {
                if (data.requestType.equalsIgnoreCase("CREDIT")&&cards.stream().anyMatch(x -> x.getIssuer().equals("MDP") && x.getStartsWith().equals(data.creditAccountId.substring(0, 6)))){
                    String mdpCardInfo2="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:mdp=\"http://ws.wso2.org/dataservice/mdpCardInfo\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <mdp:_getmdpcardinfo>\n" +
                            "         <mdp:cardNumber>"+data.creditAccountId+"</mdp:cardNumber>\n" +
                            "      </mdp:_getmdpcardinfo>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";
                    String resMdpCardInfo2 = Request.SendRequest(env.getProperty("MDPBaseURL"), "_getmdpcardinfo", mdpCardInfo2);
                    if (resMdpCardInfo2.contains("Unable to find card")){
                        CombinedResDto response= new CombinedResDto();
                        response.errorres=new ErrorsResDto(data.requestId,data.timestamp,"12013","Restricted account, please contact your bank");
                        return response;
                    }

                    Document docMdp2 = XMLParser.ReadXML(resMdpCardInfo2);
                    String innerXMLmdpCard = docMdp2.getElementsByTagName("response").item(0).getTextContent();
                    docMdp2 = XMLParser.ReadXML(innerXMLmdpCard);
                    //String backOfficeStatus = docMdp.getElementsByTagName("backOfficeStatus").item(0).getTextContent();
                    String expDate = docMdp2.getElementsByTagName("expirDate").item(0).getTextContent();
                    balance=docMdp2.getElementsByTagName("avalBalance").item(0).getTextContent();
                    String statusCode=docMdp2.getElementsByTagName("statusCode").item(0).getTextContent();
                    if (!statusCode.equals("0")){
                        CombinedResDto response = new CombinedResDto();
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                        response.errorres=new ErrorsResDto (data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                        return response;
                    }

                }
                else if (data.requestType.equalsIgnoreCase("DEBIT")&&cards.stream().anyMatch(x -> x.getIssuer().equals("MDP") && x.getStartsWith().equals(data.debitAccountId.substring(0, 6)))){
                    String mdpCardInfo2="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:mdp=\"http://ws.wso2.org/dataservice/mdpCardInfo\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <mdp:_getmdpcardinfo>\n" +
                            "         <mdp:cardNumber>"+data.debitAccountId+"</mdp:cardNumber>\n" +
                            "      </mdp:_getmdpcardinfo>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";
                    String resMdpCardInfo2 = Request.SendRequest(env.getProperty("MDPBaseURL"), "_getmdpcardinfo", mdpCardInfo2);
                    if (resMdpCardInfo2.contains("Unable to find card")){
                        CombinedResDto response= new CombinedResDto();
                        response.errorres=new ErrorsResDto (data.requestId,data.timestamp,"12013","Restricted account, please contact your bank");
                        return response;
                    }


                    Document docMdp2 = XMLParser.ReadXML(resMdpCardInfo2);
                    String innerXMLmdpCard = docMdp2.getElementsByTagName("response").item(0).getTextContent();
                    docMdp2 = XMLParser.ReadXML(innerXMLmdpCard);
                    //String backOfficeStatus = docMdp.getElementsByTagName("backOfficeStatus").item(0).getTextContent();
                    String expDate = docMdp2.getElementsByTagName("expirDate").item(0).getTextContent();
                    balance=docMdp2.getElementsByTagName("avalBalance").item(0).getTextContent();
                    String statusCode=docMdp2.getElementsByTagName("statusCode").item(0).getTextContent();
                    if (!statusCode.equals("0")){
                        CombinedResDto response = new CombinedResDto();
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                        response.errorres=new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                        return response;
                    }

                }

                if (data.debitAccountType.equalsIgnoreCase("ACCOUNT")) {
                    debitAccount = data.requestType.equalsIgnoreCase("DEBIT")
                            ? data.debitAccountId
                            : data.requestType.equalsIgnoreCase("CREDIT")
                            ? data.debitPoolAccount
                            : null; // REVERSAL, BLOCK, UNBLOCK !!! TODO: not handled
                    creditAccount = data.requestType.equalsIgnoreCase("DEBIT")
                            ? data.creditPoolAccount
                            : data.requestType.equalsIgnoreCase("CREDIT")
                            ? data.debitAccountId
                            : null; // REVERSAL, BLOCK, UNBLOCK !!! TODO: not handled

                    // ---------------------------------------------------------------------- //
                    customerNumber = data.requestType.equalsIgnoreCase("DEBIT")
                            ? data.debitAccountId.substring(1, 8)
                            : data.requestType.equalsIgnoreCase("CREDIT")
                            ? data.creditAccountId.substring(1, 8)
                            : null;
                } else if (data.debitAccountType.equals("CARD")) {
                    debitAccount = data.requestType.equalsIgnoreCase("DEBIT")
                            ? data.debitAccountId
                            : data.requestType.equalsIgnoreCase("CREDIT")
                            ? data.debitPoolAccount
                            : null; // REVERSAL, BLOCK, UNBLOCK !!! TODO: not handled
                    creditAccount = data.requestType.equalsIgnoreCase("DEBIT")
                            ? data.creditPoolAccount
                            : data.requestType.equalsIgnoreCase("CREDIT")
                            ? data.creditAccountId
                            : null; // REVERSAL, BLOCK, UNBLOCK !!! TODO: not handled
                    // ---------------------------------------------------------------------- //


                    if (data.debitAccountType.equals("CARD") && cards.stream().anyMatch(x -> x.getIssuer().equals("NI") && x.getStartsWith().equals(data.debitAccountId.substring(0, 6)))) {

                        // call NI to fetch customer number from deibt account id
                        String body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                                "   <soapenv:Header/>\n" +
                                "   <soapenv:Body>\n" +
                                "      <dat:GetCardInfoRq>\n" +
                                "         <dat:Request1><![CDATA[\n" +
                                "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:fimi=\"http://schemas.compassplus.com/two/1.0/fimi.xsd\" xmlns:fimi1=\"http://schemas.compassplus.com/two/1.0/fimi_types.xsd\">\n" +
                                "\t<soap:Header/>\n" +
                                "\t<soap:Body>\n" +
                                "\t\t<fimi:GetCardInfoRq>\n" +
                                "\t\t\t<fimi:Request   __HEADERPARAM__  >\n" +
                                "\t\t\t\t<fimi1:PAN>" + (data.requestType.equalsIgnoreCase("DEBIT") ? data.debitAccountId : data.creditAccountId) + "</fimi1:PAN>\n" +
                                "\t\t\t</fimi:Request>\n" +
                                "\t\t</fimi:GetCardInfoRq>\n" +
                                "\t</soap:Body>\n" +
                                "</soap:Envelope>\n" +
                                "]]></dat:Request1>\n" +
                                "      </dat:GetCardInfoRq>\n" +
                                "   </soapenv:Body>\n" +
                                "</soapenv:Envelope>";
                        // (data.requestType.equalsIgnoreCase("DEBIT") ? data.debitAccountId : data.creditAccountId)
                        String res = Request.SendRequest(env.getProperty("NIBaseURL"), "GetCardInfoRq", body);
                        Document doc = XMLParser.ReadXML(res);
                        String innerXml = doc.getElementsByTagName("FEMI_RESPONSE").item(0).getTextContent();
                        innerXml = innerXml.replaceAll(":", "_");
                        doc = XMLParser.ReadXML(innerXml);
                        customerNumber = doc.getElementsByTagName("m0_PersonExtId").item(0).getTextContent();
                        System.out.println("customer Number ni " + customerNumber);
                    } else if (((data.debitAccountType.equals("CARD")&&data.requestType.equalsIgnoreCase("DEBIT")) && cards.stream().anyMatch(x -> x.getIssuer().equals("MDP") && x.getStartsWith().equals(data.debitAccountId.substring(0, 6))))) {
                        // call meeza to fetch customer number from debit account id
                        String body = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:cus=\"http://ws.wso2.org/dataservice/customerInfoFromCardNumber\">\n" +
                                "   <soap:Header/>\n" +
                                "   <soap:Body>\n" +
                                "      <cus:_getcustomerinfofromcardnumber>\n" +
                                "         <cus:cardNumber>" +data.debitAccountId+ "</cus:cardNumber>\n" +
                                "      </cus:_getcustomerinfofromcardnumber>\n" +
                                "   </soap:Body>\n" +
                                "</soap:Envelope>";
                        String res = Request.SendRequest(env.getProperty("MDPBaseURL"), "_getcustomerinfofromcardnumber", body);

                        Document doc = XMLParser.ReadXML(res);
                        String innerXml = doc.getElementsByTagName("response").item(0).getTextContent();
                        doc = XMLParser.ReadXML(innerXml);
                        customerNumber = doc.getElementsByTagName("customerNumber").item(0).getTextContent();
                        System.out.println("Customer Number 10/10: " + customerNumber);
                    }

                }
            }
            String t24DebitAcc = data.requestType.equalsIgnoreCase("DEBIT") && data.debitAccountType.equals("CARD") && cards.stream().anyMatch(x -> x.getIssuer().equals("MDP"))
                    ? env.getProperty("t24.poolaccount.mdpDebitPool")
                    : data.requestType.equalsIgnoreCase("DEBIT") && data.debitAccountType.equals("CARD") && cards.stream().anyMatch(x -> x.getIssuer().equals("NI"))
                    ? env.getProperty("t24.poolaccount.niDebitPool")
                    : debitAccount;
            String t24CreditAcc = "";
            if (data.creditAccountType != null) {
                t24CreditAcc =data.requestType.equalsIgnoreCase("DEBIT") && data.creditAccountType.equals("CARD")&& cards.stream().anyMatch(x -> x.getIssuer().equals("MDP"))
                        ? env.getProperty("t24.poolaccount.mdpDebitPool"):
                        data.requestType.equalsIgnoreCase("CREDIT") && data.creditAccountType.equals("ACCOUNT")
                                ? data.creditAccountId.substring(data.creditAccountId.length() - 16)
                                :data.requestType.equalsIgnoreCase("DEBIT") && data.creditAccountType.equals("ACCOUNT")
                                ?env.getProperty(" data.creditPoolAccount")
                                :data.requestType.equalsIgnoreCase("DEBIT") && data.creditAccountType.equals("WALLET")
                                ?env.getProperty(" data.creditPoolAccount"):
                                data.requestType.equalsIgnoreCase("CREDIT") && data.creditAccountType.equals("CARD") && cards.stream().anyMatch(x -> x.getIssuer().equals("MDP") )
                                        ? env.getProperty("t24.poolaccount.mdpCreditPool")
                                        : data.requestType.equalsIgnoreCase("CREDIT") && data.creditAccountType.equals("CARD") && cards.stream().anyMatch(x -> x.getIssuer().equals("NI"))
                                        ? env.getProperty("t24.poolaccount.niCreditPool")
                                        : data.requestType.equalsIgnoreCase("CREDIT") && data.creditAccountType.equals("CARD") && cards.stream().anyMatch(x -> x.getIssuer().equals("NI") &&data.creditAccountId.substring(0,2).equalsIgnoreCase("45"))
                                        ?(Get_account_from_card_number(data.creditAccountId))
                                        : creditAccount;

            } else {
                t24CreditAcc = data.requestType.equalsIgnoreCase("CREDIT") && cards.stream().anyMatch(x -> x.getIssuer().equals("MDP"))
                        ? env.getProperty("t24.poolaccount.mdpCreditPool")
                        :data.requestType.equalsIgnoreCase("CREDIT") &&!cards.stream().anyMatch(x -> x.getIssuer().equals("MDP"))&& cards.stream().anyMatch(y -> y.getIssuer().equals("NI"))
                        ? env.getProperty("t24.poolaccount.niCreditPool")
                        :data.requestType.equalsIgnoreCase("CREDIT") && data.creditAccountId.length()>16
                        ? data.creditAccountId
                        :data.requestType.equalsIgnoreCase("DEBIT") &&!cards.stream().anyMatch(x -> x.getIssuer().equals("MDP"))&& cards.stream().anyMatch(y -> y.getIssuer().equals("NI"))
                        ?env.getProperty(" data.creditPoolAccount")
                        :data.requestType.equalsIgnoreCase("DEBIT")
                        ?env.getProperty(" data.creditPoolAccount")
                        : data.requestType.equalsIgnoreCase("CREDIT") && cards.stream().anyMatch(x -> x.getIssuer().equals("NI"))
                        ? env.getProperty("t24.poolaccount.niCreditPool")
                        : creditAccount;
                if (data.requestType.equalsIgnoreCase("CREDIT")&&(data.creditAccountId.length()>16||data.creditAccountId.charAt(0)=='1')){
                    t24CreditAcc=data.creditAccountId;
                }

            }
            if (t24DebitAcc==null||t24DebitAcc.equals("")){
                t24DebitAcc=data.debitAccountId;
            }
            LocalDateTime now = LocalDateTime.now();
            // String currentTimestamp = String.valueOf(Timestamp.valueOf(now));
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

            if(data.requestType.equalsIgnoreCase("CREDIT")&&data.creditAccountType.equalsIgnoreCase("CARD")&&data.creditAccountId.startsWith("45")){

                t24CreditAcc=Get_account_from_card_number(data.creditAccountId);
            }



            String body = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:dat=\"http://ws.wso2.org/dataservice\">" +
                    "   <soap:Header/>" +
                    "   <soap:Body>" +
                    "      <dat:REVERSALFTCONTRACT>" +
                    "         <dat:DBACCOUNT>" + t24DebitAcc + "</dat:DBACCOUNT>" +
                    "         <dat:DRCCY>EGP</dat:DRCCY>" +
                    "         <dat:CRAMT>" + deductedAmount + "</dat:CRAMT>" +
                    "         <dat:CRACCOUNT>" + t24CreditAcc + "</dat:CRACCOUNT>" +
                    "         <dat:CRCCY>" + data.amount.get("orgCurr") + "</dat:CRCCY>" +
                    "         <dat:XREF></dat:XREF>" +
                    "         <dat:DRVDT></dat:DRVDT>" +
                    "         <dat:CRVDT></dat:CRVDT>" +
                    "         <dat:INTRMKS>"+data.requestType+"</dat:INTRMKS>" +
                    "         <dat:OrderingBank>ADCB</dat:OrderingBank>" +
                    "      </dat:REVERSALFTCONTRACT>" +
                    "   </soap:Body>" +
                    "</soap:Envelope>";
//            dateFormat.format(Timestamp.valueOf(now.plusDays(-5)))
//            dateFormat.format(Timestamp.valueOf(now.plusDays(-5)))
            String response = Request.SendRequest(env.getProperty("IPNBaseURL"), "REVERSALFTCONTRACT", body);
            Document doc = XMLParser.ReadXML(response);
            LocalDateTime now2 = LocalDateTime.now();
            String currentTimestamp2 = String.valueOf(Timestamp.valueOf(now2));
            String ref = doc.getElementsByTagName("HOSTREF").item(0).getTextContent();

            //fraudBody
            String FraudBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <dat:AFINTERNALFTCONTRACT>\n" +
                    "\t\t<dat:smh_tran_type>TRX</dat:smh_tran_type>\n" +
                    "\t\t\t<dat:smh_cust_type>I</dat:smh_cust_type>\n" +
                    "\t\t\t<dat:smh_acct_type>CS</dat:smh_acct_type>\n" +
                    "\t\t\t<dat:smh_authenticate_mtd>NC</dat:smh_authenticate_mtd>\n" +
                    "\t\t\t<dat:smh_channel_type>O</dat:smh_channel_type>\n" +
                    "\t\t\t<dat:smh_activity_type>BF</dat:smh_activity_type>\n" +
                    "\t\t\t<dat:smh_activity_detail1>NAP</dat:smh_activity_detail1>\n" +
                    "\t\t\t<dat:smh_activity_detail2>NAP</dat:smh_activity_detail2>\n" +
                    "\t\t\t<dat:smh_activity_detail3>NAP</dat:smh_activity_detail3>\n" +
                    "\t\t\t<dat:smh_priority>5</dat:smh_priority>\n" +
                    "\t\t\t<dat:smh_msg_type>1</dat:smh_msg_type>\n" +
                    "\t\t\t<dat:smh_resp_req>0</dat:smh_resp_req>\n" +
                    "\t\t\t<dat:smh_sdd_ind>1</dat:smh_sdd_ind>\n" +
                    "\t\t\t<dat:smh_dest>SFME</dat:smh_dest>\n" +
                    "\t\t\t<dat:smh_multi_org_name>IPN</dat:smh_multi_org_name>\n" +
                    "\t\t\t<dat:rqo_tran_utc_flag>Y</dat:rqo_tran_utc_flag>\n" +
                    "\t\t\t<dat:unm_auth_mtd1>L</dat:unm_auth_mtd1>\n" +
                    "\t\t\t<dat:unm_auth_result1>P</dat:unm_auth_result1>\n" +
                    "\t\t\t<dat:unm_auth_mtd2>1</dat:unm_auth_mtd2>\n" +
                    "\t\t\t<dat:unm_auth_result2>P</dat:unm_auth_result2>\n" +
                    "\t\t\t<dat:tbt_tran_type>T</dat:tbt_tran_type>\n" +
                    "\t\t\t<dat:tbt_tran_status>P</dat:tbt_tran_status>\n" +
                    "\t\t\t<dat:tbt_revision_code>O</dat:tbt_revision_code>\n" +
                    "\t\t\t<dat:tbt_sch_flag>I</dat:tbt_sch_flag>\n" +
                    "\t\t\t<dat:tbt_self_acct_ind>Y</dat:tbt_self_acct_ind>\n" +
                    "\t\t\t<dat:tpp_payee_payer_ind>E</dat:tpp_payee_payer_ind>\n" +
                    "\t\t\t<dat:tpp_entity_type>S</dat:tpp_entity_type>\n" +
                    "\t\t\t<dat:tpp_bank_type>S</dat:tpp_bank_type>\n" +
                    "\t\t\t<dat:tpp_acct_type>SV</dat:tpp_acct_type>\n" +
                    "\t\t\t<dat:channeltype>MB</dat:channeltype>\n" +
                    "\t\t\t<dat:DRVDT>" + data.timestamp + "</dat:DRVDT>\n" +
                    "\t\t\t<dat:rqo_tran_time>" + data.timestamp + "</dat:rqo_tran_time>\n" +
                    "\t\t\t<dat:rqo_tran_date_alt>" + data.timestamp + "</dat:rqo_tran_date_alt>\n" +
                    "\t\t\t<dat:rqo_tran_time_alt>" + data.timestamp + "</dat:rqo_tran_time_alt>\n" +
                    "\t\t\t<dat:CUSTNO>" + data.payerMobileNumber + "</dat:CUSTNO>\n" +
                    "\t\t\t<dat:DBACCOUNT>" + debitAccount + "</dat:DBACCOUNT>\n" +
                    "\t\t\t<dat:DRCCY>EGP</dat:DRCCY>\n" +
                    "\t\t\t<dat:CODBRANCH>11029</dat:CODBRANCH>\n" +
                    "\t\t\t<dat:AvailableBal>" + balance + "</dat:AvailableBal>\n" +
                    "\t\t\t<dat:username>" + data.payerMobileNumber + "</dat:username>\n" +
                    "\t\t\t<dat:hqo_entity_use_ob_userid>H</dat:hqo_entity_use_ob_userid>\n" +
                    "\t\t\t<dat:hqo_device_id>IPN</dat:hqo_device_id>\n" +
                    "\t\t\t<dat:hob_ip_address>84.36.99.114</dat:hob_ip_address>\n" +
                    "\t\t\t<dat:hob_ip_cntry_code>84.36.99.114</dat:hob_ip_cntry_code>\n" +
                    "\t\t\t<dat:hob_match_ind>Y</dat:hob_match_ind>\n" +
                    "\t\t\t<dat:hob_website_name>IPN.adcb.com.eg</dat:hob_website_name>\n" +
                    "\t\t\t<dat:hob_webpage_code>NO</dat:hob_webpage_code>\n" +
                    "\t\t\t<dat:DRAMT>" + data.amount.get("orgValue") + "</dat:DRAMT>\n" +
                    "\t\t\t<dat:CRAMT>" + data.interChange.get("value") + "</dat:CRAMT>\n" +
                    "\t\t\t<dat:CRCCY1>EGP</dat:CRCCY1>\n" +
                    "\t\t\t<dat:transactiontype>Y</dat:transactiontype>\n" +
                    "\t\t\t<dat:BenfID>" + data.payeeMobileNumber + "</dat:BenfID>\n" +
                    "\t\t\t<dat:CRACCOUNT>" + data.creditAccountId + "</dat:CRACCOUNT>\n" +
                    "\t\t\t<dat:tpp_bank_cntry_code>EG</dat:tpp_bank_cntry_code>\n" +
                    "\t\t\t<dat:tpp_bank_city>GIZA</dat:tpp_bank_city>\n" +
                    "\t\t\t\n" +
                    "\t\t\t<dat:CRCCY>EGP</dat:CRCCY>\n" +
                    "\t\t\t<dat:XREF>" + ref + "</dat:XREF>\n" +
                    "\t\t\t<dat:INTRMKS>ADCB P2P - SendMony</dat:INTRMKS>\n" +
                    "\t\t\t<dat:tpp_name>" + data.payerMobileNumber + "</dat:tpp_name>\n" +
                    "      </dat:AFINTERNALFTCONTRACT>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope>";
            String resFruad = Request.SendRequest(env.getProperty("IPNBaseURL"), "AFINTERNALFTCONTRACT", FraudBody);
            Logging.info("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + FraudBody + "}; RESPONSE CODE={200}; RESPONSE={" + resFruad + "}; ");
            if (data.creditAccountType != null) {
                if(data.creditAccountType.equals("CARD")&&data.requestType.equalsIgnoreCase("CREDIT") && cards.stream().anyMatch(x -> x.getIssuer().equals("MDP") && x.getStartsWith().equals(data.creditAccountId.substring(0, 6)))){
                    body = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:cred=\"http://ws.wso2.org/dataservice/creditAccountPresentment\">\n" +
                            "   <soap:Header/>\n" +
                            "   <soap:Body>\n" +
                            "      <cred:_postcreditaccountpresentment>\n" +
                            "         <cred:cardNumber>" + data.creditAccountId + "</cred:cardNumber>\n" +
                            "         <cred:amount>" + deductedAmount + "</cred:amount>\n" +
                            "      </cred:_postcreditaccountpresentment>\n" +
                            "   </soap:Body>\n" +
                            "</soap:Envelope>";
                    String soapAction = "_postcreditaccountpresentment";
                    String res = Request.SendRequest(env.getProperty("MDPBaseURL"), soapAction, body);
                    doc = XMLParser.ReadXML(res);
                    String innerXml = doc.getElementsByTagName("response").item(0).getTextContent();
                    doc = XMLParser.ReadXML(innerXml);
                    refMDP = doc.getElementsByTagName("referenceNumber").item(0).getTextContent();
                    System.out.println("refMDP: " + refMDP);
                }

                else if ((data.debitAccountType.equals("CARD") || data.creditAccountType.equals("CARD")) && cards.stream().anyMatch(x -> x.getIssuer().equals("MDP") && x.getStartsWith().equals(data.debitAccountId.substring(0, 6)))) {
                    String soapAction = null;
                    if (data.requestType.equalsIgnoreCase("DEBIT")) {

                        body = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:deb=\"http://ws.wso2.org/dataservice/debitAccountPresentment\">\n" +
                                "   <soap:Header/>\n" +
                                "   <soap:Body>\n" +
                                "      <deb:_postdebitaccountpresentment>\n" +
                                "         <deb:cardNumber>" + debitAccount + "</deb:cardNumber>\n" +
                                "         <deb:amount>" + deductedAmount + "</deb:amount>\n" +
                                "      </deb:_postdebitaccountpresentment>\n" +
                                "   </soap:Body>\n" +
                                "</soap:Envelope>";
                        soapAction = "_postdebitaccountpresentment";
                    } else if (data.requestType.equalsIgnoreCase("CREDIT")&&data.creditAccountId.length()<=16) {
                        body = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:cred=\"http://ws.wso2.org/dataservice/creditAccountPresentment\">\n" +
                                "   <soap:Header/>\n" +
                                "   <soap:Body>\n" +
                                "      <cred:_postcreditaccountpresentment>\n" +
                                "         <cred:cardNumber>" + data.creditAccountId + "</cred:cardNumber>\n" +
                                "         <cred:amount>" + deductedAmount + "</cred:amount>\n" +
                                "      </cred:_postcreditaccountpresentment>\n" +
                                "   </soap:Body>\n" +
                                "</soap:Envelope>";
                        soapAction = "_postcreditaccountpresentment";
                    }

                    String res = Request.SendRequest(env.getProperty("MDPBaseURL"), soapAction, body);
                    doc = XMLParser.ReadXML(res);
                    String innerXml = doc.getElementsByTagName("response").item(0).getTextContent();
                    doc = XMLParser.ReadXML(innerXml);
                    refMDP = doc.getElementsByTagName("referenceNumber").item(0).getTextContent();
                    System.out.println("refMDP: " + refMDP);
                } else if ((data.debitAccountType.equals("CARD") || data.creditAccountType.equals("CARD")) && cards.stream().anyMatch(x -> x.getIssuer().equals("NI") && x.getStartsWith().equals(data.debitAccountId.substring(0, 6)))) {
                    String tranCode = "140";
                    if (data.requestType.equalsIgnoreCase("DEBIT")) {
                        tranCode = "142";
                    }
                    body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <dat:POSRequestRq>\n" +
                            "         <dat:Request1><![CDATA[\n" +
                            "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:fimi=\"http://schemas.compassplus.com/two/1.0/fimi.xsd\" xmlns:fimi1=\"http://schemas.compassplus.com/two/1.0/fimi_types.xsd\">\n" +
                            "  <soap:Header/>\n" +
                            "  <soap:Body>\n" +
                            "    <fimi:POSRequestRq>\n" +
                            "      <fimi:Request __HEADERPARAM__ >\n" +
                            "        <!--TermName,Condition,EntryMode,PAN-MBR,TRACK2,FromAcctType,Amount,TranCode& TranType-->\n" +
                            "        <!--Optional:-->\n" +
                            "       <fimi1:TranType>200</fimi1:TranType>\n" +
                            "        <fimi1:TranCode>" + tranCode + "</fimi1:TranCode>\n" +
                            "        <fimi1:Amount>" + deductedAmount + "</fimi1:Amount>\n" +
                            "        <fimi1:TermName>"+env.getProperty("NI_Transaction_Description")+"</fimi1:TermName>\n" +
                            "<fimi1:TranNumber>IPN-"+data.transactionId+"</fimi1:TranNumber>\n"+
                            "        <fimi1:Condition>00</fimi1:Condition>\n" +
                            "        <fimi1:EntryMode>01</fimi1:EntryMode>\n" +
                            "        <fimi1:PAN>" + env.getProperty("t24.poolaccount.niCreditPool") + "</fimi1:PAN>\n" +
                            "         <fimi1:FromAcctType>31</fimi1:FromAcctType>\n" +
                            "        \n" +
                            "      </fimi:Request>\n" +
                            "    </fimi:POSRequestRq>\n" +
                            "  </soap:Body>\n" +
                            "</soap:Envelope>\n" +
                            "]]></dat:Request1>\n" +
                            "      </dat:POSRequestRq>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";

                    String res = Request.SendRequest(env.getProperty("NIBaseURL"), "POSRequestRq", body);
                    fimiTrxRef=Get_Transaction_Refrence_Fimi(res);

                    String msg = data.requestType.equalsIgnoreCase("DEBIT") ? deductedAmount + " has been debited from your ADCB account No ****" + data.debitAccountId.substring(data.debitAccountId.length() - 4, data.debitAccountId.length()) + " on " + currentTimestamp2.substring(0, 10) + " to , ref. " + data.transactionId.substring(data.transactionId.length() - 8, data.transactionId.length()) + "" :
                            data.requestType.equalsIgnoreCase("CREDIT") ? deductedAmount + " has been credited to your ADCB account No ****" + data.creditAccountId.substring(data.creditAccountId.length() - 4, data.creditAccountId.length()) + " on " + currentTimestamp2.substring(0, 10) + " from " + data.payeeName + ", ref." + data.transactionId.substring(data.transactionId.length() - 8, data.transactionId.length()) + "" : "";

                    body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <dat:SendSMS>\n" +
                            "         <dat:PhoneNumber>" + data.payerMobileNumber + "</dat:PhoneNumber>\n" +
                            "         <dat:Message>" + msg + "</dat:Message>\n" +
                            "      </dat:SendSMS>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";

                    res = Request.SendRequest(env.getProperty("IPNBaseURL"), "SendSMS", body);
                }
                else if (data.debitAccountType.equals("CARD") && cards.stream().anyMatch(x -> x.getIssuer().equals("NI") && x.getStartsWith().equals(data.debitAccountId.substring(0, 6)))) {
                    String tranCode = "140";
                    if (data.requestType.equalsIgnoreCase("DEBIT")) {
                        tranCode = "142";
                    }
                    body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <dat:POSRequestRq>\n" +
                            "         <dat:Request1><![CDATA[\n" +
                            "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:fimi=\"http://schemas.compassplus.com/two/1.0/fimi.xsd\" xmlns:fimi1=\"http://schemas.compassplus.com/two/1.0/fimi_types.xsd\">\n" +
                            "  <soap:Header/>\n" +
                            "  <soap:Body>\n" +
                            "    <fimi:POSRequestRq>\n" +
                            "      <fimi:Request __HEADERPARAM__ >\n" +
                            "        <!--TermName,Condition,EntryMode,PAN-MBR,TRACK2,FromAcctType,Amount,TranCode& TranType-->\n" +
                            "        <!--Optional:-->\n" +
                            "       <fimi1:TranType>200</fimi1:TranType>\n" +
                            "        <fimi1:TranCode>" + tranCode + "</fimi1:TranCode>\n" +
                            "        <fimi1:Amount>" + deductedAmount + "</fimi1:Amount>\n" +
                            "        <fimi1:TermName>"+env.getProperty("NI_Transaction_Description")+"</fimi1:TermName>\n" +
                            "<fimi1:TranNumber>IPN-"+data.transactionId+"</fimi1:TranNumber>\n"+
                            "        <fimi1:Condition>00</fimi1:Condition>\n" +
                            "        <fimi1:EntryMode>01</fimi1:EntryMode>\n" +
                            "        <fimi1:PAN>" + data.debitAccountId + "</fimi1:PAN>\n" +
                            "         <fimi1:FromAcctType>31</fimi1:FromAcctType>\n" +
                            "        \n" +
                            "      </fimi:Request>\n" +
                            "    </fimi:POSRequestRq>\n" +
                            "  </soap:Body>\n" +
                            "</soap:Envelope>\n" +
                            "]]></dat:Request1>\n" +
                            "      </dat:POSRequestRq>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";

                    String res = Request.SendRequest(env.getProperty("NIBaseURL"), "POSRequestRq", body);
                    fimiTrxRef=Get_Transaction_Refrence_Fimi(res);

                    String msg = data.requestType.equalsIgnoreCase("DEBIT") ? deductedAmount + " has been debited from your ADCB account No ****" + data.debitAccountId.substring(data.debitAccountId.length() - 4, data.debitAccountId.length()) + " on " + currentTimestamp2.substring(0, 10) + " to , ref. " + data.transactionId.substring(data.transactionId.length() - 8, data.transactionId.length()) + "" :
                            data.requestType.equalsIgnoreCase("CREDIT") ? deductedAmount + " has been credited to your ADCB account No ****" + data.creditAccountId.substring(data.creditAccountId.length() - 4, data.creditAccountId.length()) + " on " + currentTimestamp2.substring(0, 10) + " from " + data.payeeName + ", ref." + data.transactionId.substring(data.transactionId.length() - 8, data.transactionId.length()) + "" : "";

                    body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <dat:SendSMS>\n" +
                            "         <dat:PhoneNumber>" + data.payerMobileNumber + "</dat:PhoneNumber>\n" +
                            "         <dat:Message>" + msg + "</dat:Message>\n" +
                            "      </dat:SendSMS>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";

                    res = Request.SendRequest(env.getProperty("IPNBaseURL"), "SendSMS", body);
                }
                else if (data.creditAccountType.equals("CARD")&&data.requestType.equalsIgnoreCase("CREDIT") && cards.stream().anyMatch(x -> x.getIssuer().equals("NI") && x.getStartsWith().equals(data.creditAccountId.substring(0, 6)))) {
                    String tranCode = "140";
                    if (data.requestType.equalsIgnoreCase("DEBIT")) {
                        tranCode = "142";
                    }
                    body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <dat:POSRequestRq>\n" +
                            "         <dat:Request1><![CDATA[\n" +
                            "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:fimi=\"http://schemas.compassplus.com/two/1.0/fimi.xsd\" xmlns:fimi1=\"http://schemas.compassplus.com/two/1.0/fimi_types.xsd\">\n" +
                            "  <soap:Header/>\n" +
                            "  <soap:Body>\n" +
                            "    <fimi:POSRequestRq>\n" +
                            "      <fimi:Request __HEADERPARAM__ >\n" +
                            "        <!--TermName,Condition,EntryMode,PAN-MBR,TRACK2,FromAcctType,Amount,TranCode& TranType-->\n" +
                            "        <!--Optional:-->\n" +
                            "       <fimi1:TranType>200</fimi1:TranType>\n" +
                            "        <fimi1:TranCode>" + tranCode + "</fimi1:TranCode>\n" +
                            "        <fimi1:Amount>" + deductedAmount + "</fimi1:Amount>\n" +
                            "        <fimi1:TermName>"+env.getProperty("NI_Transaction_Description")+"</fimi1:TermName>\n" +
                            "<fimi1:TranNumber>IPN-"+data.transactionId+"</fimi1:TranNumber>\n"+
                            "        <fimi1:Condition>00</fimi1:Condition>\n" +
                            "        <fimi1:EntryMode>01</fimi1:EntryMode>\n" +
                            "        <fimi1:PAN>" + data.creditAccountId + "</fimi1:PAN>\n" +
                            "         <fimi1:FromAcctType>31</fimi1:FromAcctType>\n" +
                            "        \n" +
                            "      </fimi:Request>\n" +
                            "    </fimi:POSRequestRq>\n" +
                            "  </soap:Body>\n" +
                            "</soap:Envelope>\n" +
                            "]]></dat:Request1>\n" +
                            "      </dat:POSRequestRq>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";

                    String res = Request.SendRequest(env.getProperty("NIBaseURL"), "POSRequestRq", body);
                    fimiTrxRef=Get_Transaction_Refrence_Fimi(res);

                    String msg = data.requestType.equalsIgnoreCase("DEBIT") ? deductedAmount + " has been debited from your ADCB account No ****" + data.debitAccountId.substring(data.debitAccountId.length() - 4, data.debitAccountId.length()) + " on " + currentTimestamp2.substring(0, 10) + " to , ref. " + data.transactionId.substring(data.transactionId.length() - 8, data.transactionId.length()) + "" :
                            data.requestType.equalsIgnoreCase("CREDIT") ? deductedAmount + " has been credited to your ADCB account No ****" + data.creditAccountId.substring(data.creditAccountId.length() - 4, data.creditAccountId.length()) + " on " + currentTimestamp2.substring(0, 10) + " from " + data.payeeName + ", ref." + data.transactionId.substring(data.transactionId.length() - 8, data.transactionId.length()) + "" : "";

                    body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <dat:SendSMS>\n" +
                            "         <dat:PhoneNumber>" + data.payerMobileNumber + "</dat:PhoneNumber>\n" +
                            "         <dat:Message>" + msg + "</dat:Message>\n" +
                            "      </dat:SendSMS>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";

                    res = Request.SendRequest(env.getProperty("IPNBaseURL"), "SendSMS", body);
                }
            }
            else {

                if (data.debitAccountType.equals("CARD")&& cards.stream().anyMatch(x -> x.getIssuer().equals("MDP") && x.getStartsWith().equals(data.debitAccountId.substring(0, 6)))) {
                    String soapAction = null;
                    if (data.requestType.equalsIgnoreCase("DEBIT")) {

                        body = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:deb=\"http://ws.wso2.org/dataservice/debitAccountPresentment\">\n" +
                                "   <soap:Header/>\n" +
                                "   <soap:Body>\n" +
                                "      <deb:_postdebitaccountpresentment>\n" +
                                "         <deb:cardNumber>" + debitAccount + "</deb:cardNumber>\n" +
                                "         <deb:amount>" + deductedAmount + "</deb:amount>\n" +
                                "      </deb:_postdebitaccountpresentment>\n" +
                                "   </soap:Body>\n" +
                                "</soap:Envelope>";
                        soapAction = "_postdebitaccountpresentment";
                    } else if (data.requestType.equalsIgnoreCase("CREDIT")&&data.creditAccountId.length()<=16) {
                        body = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:cred=\"http://ws.wso2.org/dataservice/creditAccountPresentment\">\n" +
                                "   <soap:Header/>\n" +
                                "   <soap:Body>\n" +
                                "      <cred:_postcreditaccountpresentment>\n" +
                                "         <cred:cardNumber>" + data.creditAccountId + "</cred:cardNumber>\n" +
                                "         <cred:amount>" + deductedAmount + "</cred:amount>\n" +
                                "      </cred:_postcreditaccountpresentment>\n" +
                                "   </soap:Body>\n" +
                                "</soap:Envelope>";
                        soapAction = "_postcreditaccountpresentment";
                    }
                    try{
                        String res = Request.SendRequest(env.getProperty("MDPBaseURL"), soapAction, body);
                        doc = XMLParser.ReadXML(res);
                        String innerXml = doc.getElementsByTagName("response").item(0).getTextContent();
                        doc = XMLParser.ReadXML(innerXml);
                        refMDP = doc.getElementsByTagName("referenceNumber").item(0).getTextContent();
                        System.out.println("refMDP: " + refMDP);
                    }
                    catch(Exception msg){

                    }

                } else if (data.debitAccountType.equals("CARD") && cards.stream().anyMatch(x -> x.getIssuer().equals("NI") && x.getStartsWith().equals(data.debitAccountId.substring(0, 6)))) {
                    String tranCode = "140";
                    if (data.requestType.equalsIgnoreCase("DEBIT")) {
                        tranCode = "142";
                    }
                    body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <dat:POSRequestRq>\n" +
                            "         <dat:Request1><![CDATA[\n" +
                            "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:fimi=\"http://schemas.compassplus.com/two/1.0/fimi.xsd\" xmlns:fimi1=\"http://schemas.compassplus.com/two/1.0/fimi_types.xsd\">\n" +
                            "  <soap:Header/>\n" +
                            "  <soap:Body>\n" +
                            "    <fimi:POSRequestRq>\n" +
                            "      <fimi:Request __HEADERPARAM__ >\n" +
                            "        <!--TermName,Condition,EntryMode,PAN-MBR,TRACK2,FromAcctType,Amount,TranCode& TranType-->\n" +
                            "        <!--Optional:-->\n" +
                            "       <fimi1:TranType>200</fimi1:TranType>\n" +
                            "        <fimi1:TranCode>" + tranCode + "</fimi1:TranCode>\n" +
                            "        <fimi1:Amount>" + deductedAmount + "</fimi1:Amount>\n" +
                            "        <fimi1:TermName>"+env.getProperty("NI_Transaction_Description")+"</fimi1:TermName>\n" +
                            "<fimi1:TranNumber>IPN-"+data.transactionId+"</fimi1:TranNumber>\n"+
                            "        <fimi1:Condition>00</fimi1:Condition>\n" +
                            "        <fimi1:EntryMode>01</fimi1:EntryMode>\n" +
                            "        <fimi1:PAN>" + data.debitAccountId + "</fimi1:PAN>\n" +
                            "         <fimi1:FromAcctType>31</fimi1:FromAcctType>\n" +
                            "        \n" +
                            "      </fimi:Request>\n" +
                            "    </fimi:POSRequestRq>\n" +
                            "  </soap:Body>\n" +
                            "</soap:Envelope>\n" +
                            "]]></dat:Request1>\n" +
                            "      </dat:POSRequestRq>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";

                    String res = Request.SendRequest(env.getProperty("NIBaseURL"), "POSRequestRq", body);
                    fimiTrxRef=Get_Transaction_Refrence_Fimi(res);

                    String msg = data.requestType.equalsIgnoreCase("DEBIT") ? deductedAmount + " has been debited from your ADCB account No ****" + data.debitAccountId.substring(data.debitAccountId.length() - 4, data.debitAccountId.length()) + " on " + currentTimestamp2.substring(0, 10) + " to , ref. " + data.transactionId.substring(data.transactionId.length() - 8, data.transactionId.length()) + "" :
                            data.requestType.equalsIgnoreCase("CREDIT") ? deductedAmount + " has been credited to your ADCB account No ****" + data.creditAccountId.substring(data.creditAccountId.length() - 4, data.creditAccountId.length()) + " on " + currentTimestamp2.substring(0, 10) + " from " + data.payeeName + ", ref." + data.transactionId.substring(data.transactionId.length() - 8, data.transactionId.length()) + "" : "";

                    body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <dat:SendSMS>\n" +
                            "         <dat:PhoneNumber>" + data.payerMobileNumber + "</dat:PhoneNumber>\n" +
                            "         <dat:Message>" + msg + "</dat:Message>\n" +
                            "      </dat:SendSMS>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";

                    res = Request.SendRequest(env.getProperty("IPNBaseURL"), "SendSMS", body);
                }
                else if (data.creditAccountType != null && data.creditAccountType.equals("CARD")&&data.requestType.equalsIgnoreCase("CREDIT") && cards.stream().anyMatch(x -> x.getIssuer().equals("NI") && x.getStartsWith().equals(data.creditAccountId.substring(0, 6)))) {
                    String tranCode = "140";
                    if (data.requestType.equalsIgnoreCase("DEBIT")) {
                        tranCode = "142";
                    }
                    body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <dat:POSRequestRq>\n" +
                            "         <dat:Request1><![CDATA[\n" +
                            "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:fimi=\"http://schemas.compassplus.com/two/1.0/fimi.xsd\" xmlns:fimi1=\"http://schemas.compassplus.com/two/1.0/fimi_types.xsd\">\n" +
                            "  <soap:Header/>\n" +
                            "  <soap:Body>\n" +
                            "    <fimi:POSRequestRq>\n" +
                            "      <fimi:Request __HEADERPARAM__ >\n" +
                            "        <!--TermName,Condition,EntryMode,PAN-MBR,TRACK2,FromAcctType,Amount,TranCode& TranType-->\n" +
                            "        <!--Optional:-->\n" +
                            "       <fimi1:TranType>200</fimi1:TranType>\n" +
                            "        <fimi1:TranCode>" + tranCode + "</fimi1:TranCode>\n" +
                            "        <fimi1:Amount>" + deductedAmount + "</fimi1:Amount>\n" +
                            "        <fimi1:TermName>"+env.getProperty("NI_Transaction_Description")+"</fimi1:TermName>\n" +
                            "<fimi1:TranNumber>IPN-"+data.transactionId+"</fimi1:TranNumber>\n"+
                            "        <fimi1:Condition>00</fimi1:Condition>\n" +
                            "        <fimi1:EntryMode>01</fimi1:EntryMode>\n" +
                            "        <fimi1:PAN>" + data.creditAccountId + "</fimi1:PAN>\n" +
                            "         <fimi1:FromAcctType>31</fimi1:FromAcctType>\n" +
                            "        \n" +
                            "      </fimi:Request>\n" +
                            "    </fimi:POSRequestRq>\n" +
                            "  </soap:Body>\n" +
                            "</soap:Envelope>\n" +
                            "]]></dat:Request1>\n" +
                            "      </dat:POSRequestRq>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";

                    String res = Request.SendRequest(env.getProperty("NIBaseURL"), "POSRequestRq", body);
                    fimiTrxRef=Get_Transaction_Refrence_Fimi(res);

                    String msg = data.requestType.equalsIgnoreCase("DEBIT") ? deductedAmount + " has been debited from your ADCB account No ****" + data.debitAccountId.substring(data.debitAccountId.length() - 4, data.debitAccountId.length()) + " on " + currentTimestamp2.substring(0, 10) + " to , ref. " + data.transactionId.substring(data.transactionId.length() - 8, data.transactionId.length()) + "" :
                            data.requestType.equalsIgnoreCase("CREDIT") ? deductedAmount + " has been credited to your ADCB account No ****" + data.creditAccountId.substring(data.creditAccountId.length() - 4, data.creditAccountId.length()) + " on " + currentTimestamp2.substring(0, 10) + " from " + data.payeeName + ", ref." + data.transactionId.substring(data.transactionId.length() - 8, data.transactionId.length()) + "" : "";

                    body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <dat:SendSMS>\n" +
                            "         <dat:PhoneNumber>" + data.payerMobileNumber + "</dat:PhoneNumber>\n" +
                            "         <dat:Message>" + msg + "</dat:Message>\n" +
                            "      </dat:SendSMS>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";

                    res = Request.SendRequest(env.getProperty("IPNBaseURL"), "SendSMS", body);
                }
            }
            // Request to get customer details
            // TODO REMOVE BELOW TWO LINES WHILE DEPLOYING ON PRODUCTION
            if (data.requestType.equalsIgnoreCase("DEBIT")){customerNumber="1127363";}
            else if (data.requestType.equalsIgnoreCase("CREDIT")){customerNumber="1007817";}

            Login customer = loginsRepository.getLoginByCustomerReference(customerNumber);
            String docType = customer.getDocumentType().equals("NationalId") ? "NID" : customer.getDocumentType();


            if(data.requestType.equalsIgnoreCase("CREDIT")||data.requestType.equalsIgnoreCase("REVERSAL")) {
                balance = Get_Account_Balance(t24CreditAcc);
            }

            else{

                balance = Get_Account_Balance(t24DebitAcc);

            }
            String transactionrefrence=null;
            if(fimiTrxRef!=null){

                transactionrefrence=fimiTrxRef;
            }
            else if( refMDP !=null){

                transactionrefrence=refMDP;
            }
            else{
                transactionrefrence=null;
            }


            DecimalFormat  formatter = new DecimalFormat("#0.00");
            balance=formatter.format(Double.valueOf(balance));
            CombinedResDto res = new CombinedResDto();
            res.checkstatusres=new CheckStatusResDto(data.requestId, currentTimestamp2, "00000", "SUCCESS", balance, "EGP", ref, customer.getName(), customer.getAddress(), docType, customer.getLegalId());
            acceptedLogsRepository.save(new AcceptedLog(data.transactionId, now, data.toString(), res.checkstatusres.toString(),transactionrefrence));

            return res;
        } catch (Exception ex) {
            String stackTrace = ExceptionStackTrace.GetStackTrace(ex);
            rejectedLogsRepository.save(new RejectedLog(data.requestId, LocalDateTime.now(), data.toString(), stackTrace,null));
            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={400}; RESPONSE={"+stackTrace+"}; ");
            CombinedResDto response= new CombinedResDto();
            response.errorres=new ErrorsResDto(data.requestId,data.timestamp,"12015","Invalid technical data");
            return response;
            //CombinedResDto response= new CombinedResDto (data.requestId,data.timestamp,"11013","Invalid technical data");
            //Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={400}; RESPONSE={ Service is currently not available from your bank, try again later}; ");
            //return response;
        }
    }


/*
    public static String checkNull(Object data,String reqName) throws IllegalAccessException {
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

                if (fields[i].get(data)== null ||fields[i].get(data)== ""||fields[i].get(data).equals("null")){
                    return indexReq;
                }

            }

        }
        return "";
    }
*/
    public static String Get_account_from_card_number(String PAN){
        try {
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
                    "\t\t\t\t<fimi1:PAN>" + PAN + "</fimi1:PAN>\n" +
                    "\t\t\t</fimi:Request>\n" +
                    "\t\t</fimi:GetCardInfoRq>\n" +
                    "\t</soap:Body>\n" +
                    "</soap:Envelope>\n" +
                    "]]>\n" +
                    "</dat:Request1>\n" +
                    "      </dat:GetCardInfoRq>\n" +
                    "   </soap:Body>\n" +
                    "</soap:Envelope>";
            String res = Request.SendRequest(env.getProperty("NIBaseURL"), "GetCardInfoRq", body);
            Document doc = XMLParser.ReadXML(res);
            String innerXml = doc.getElementsByTagName("FEMI_RESPONSE").item(0).getTextContent();
            innerXml = innerXml.replaceAll(":", "_");
            doc = XMLParser.ReadXML(innerXml);
            String Account_Number = doc.getElementsByTagName("m0_AcctNo").item(0).getTextContent();
            return Account_Number;
        }
        catch(Exception ex){

            Logging.warn("Failed fetching account number from NI");
            return ex.getMessage();
        }
    }

    public static String Get_Account_Balance(String ACCNO) {
        try {
            String ReqBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <dat:ACCOUNTDETAILS>\n" +
                    "         <dat:ACCNO>";


            ReqBody += ACCNO.substring(ACCNO.length()-16,ACCNO.length());

            ReqBody += "</dat:ACCNO>\n" +
                    "      </dat:ACCOUNTDETAILS>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope>";
            String res_Balance = Request.SendRequest(env.getProperty("IPNBaseURL"), "ACCOUNTDETAILS", ReqBody);
            if (res_Balance.contains("No records were found")) {

                Logging.warn(res_Balance.toString() + "   " + ACCNO);

            }

            Document doc_balance = XMLParser.ReadXML(res_Balance);
            String balance = doc_balance.getElementsByTagName("BAL_AVAIL").item(0).getTextContent();
            return balance;
        } catch (Exception e) {

            return "0";
        }
    }


    public static ErrorsResDto Check_Account_Existence(CheckStatusReqDto data) throws Exception {

        if (data.creditAccountType != null) {
            if (data.creditAccountType.equals("ACCOUNT") && data.requestType.equalsIgnoreCase("CREDIT")) {
                String ReqBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                        "   <soapenv:Header/>\n" +
                        "   <soapenv:Body>\n" +
                        "      <dat:ACCOUNTDETAILS>\n" +
                        "         <dat:ACCNO>" + data.creditAccountId.substring(data.creditAccountId.length() - 16) + "</dat:ACCNO>\n" +
                        "      </dat:ACCOUNTDETAILS>\n" +
                        "   </soapenv:Body>\n" +
                        "</soapenv:Envelope>";
                String res_Balance = Request.SendRequest(env.getProperty("IPNBaseURL"), "ACCOUNTDETAILS", ReqBody);
                if (res_Balance.contains("No records were found")) {
                    ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13001", "The beneficiary account number is invalid");
                    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + data.creditAccountId + " is not found}; ");
                    return response;
                }
                Document doc_balance = XMLParser.ReadXML(res_Balance);
                String currency = doc_balance.getElementsByTagName("CCYDESC").item(0).getTextContent();
                if (!currency.equalsIgnoreCase("EGP")) {
                    ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + data.creditAccountId + " is not found}; ");
                    return response;
                }}


        }
        return null;
    }

    public static CombinedResDto Credit_Account (String customerNumber,CheckStatusReqDto data){
        List<Card> cards = Streamable.of(cardsRepository.findAll()).toList();

        try {



            if (data.creditAccountType != null) {

                String t24DebitAcc = data.requestType.equalsIgnoreCase("DEBIT") && data.debitAccountType.equals("CARD") && cards.stream().anyMatch(x -> x.getIssuer().equals("MDP"))
                        ? env.getProperty("t24.poolaccount.mdpDebitPool")
                        : data.requestType.equalsIgnoreCase("DEBIT") && data.debitAccountType.equals("CARD") && cards.stream().anyMatch(x -> x.getIssuer().equals("NI"))
                        ? env.getProperty("t24.poolaccount.niDebitPool")
                        : data.debitAccountId;
                String t24CreditAcc = "";
                if (data.creditAccountType != null) {
                    t24CreditAcc =data.requestType.equalsIgnoreCase("DEBIT") && data.creditAccountType.equals("CARD")&& cards.stream().anyMatch(x -> x.getIssuer().equals("MDP"))
                            ? env.getProperty("t24.poolaccount.mdpDebitPool"):
                            data.requestType.equalsIgnoreCase("CREDIT") && data.creditAccountType.equals("ACCOUNT")
                                    ? data.creditAccountId.substring(data.creditAccountId.length() - 16)
                                    :data.requestType.equalsIgnoreCase("DEBIT") && data.creditAccountType.equals("ACCOUNT")
                                    ?env.getProperty(" data.creditPoolAccount")
                                    :data.requestType.equalsIgnoreCase("DEBIT") && data.creditAccountType.equals("WALLET")
                                    ?env.getProperty(" data.creditPoolAccount"):
                                    data.requestType.equalsIgnoreCase("CREDIT") && data.creditAccountType.equals("CARD") && cards.stream().anyMatch(x -> x.getIssuer().equals("MDP") )
                                            ? env.getProperty("t24.poolaccount.mdpCreditPool")
                                            : data.requestType.equalsIgnoreCase("CREDIT") && data.creditAccountType.equals("CARD") && cards.stream().anyMatch(x -> x.getIssuer().equals("NI"))
                                            ? env.getProperty("t24.poolaccount.niCreditPool")
                                            : data.requestType.equalsIgnoreCase("CREDIT") && data.creditAccountType.equals("CARD") && cards.stream().anyMatch(x -> x.getIssuer().equals("NI") &&data.creditAccountId.substring(0,2).equalsIgnoreCase("45"))
                                            ?(Get_account_from_card_number(data.creditAccountId))
                                            : data.creditAccountId;

                } else {
                    t24CreditAcc = data.requestType.equalsIgnoreCase("CREDIT") && cards.stream().anyMatch(x -> x.getIssuer().equals("MDP"))
                            ? env.getProperty("t24.poolaccount.mdpCreditPool")
                            :data.requestType.equalsIgnoreCase("CREDIT") &&!cards.stream().anyMatch(x -> x.getIssuer().equals("MDP"))&& cards.stream().anyMatch(y -> y.getIssuer().equals("NI"))
                            ? env.getProperty("t24.poolaccount.niCreditPool")
                            :data.requestType.equalsIgnoreCase("CREDIT") && data.creditAccountId.length()>16
                            ? data.creditAccountId
                            :data.requestType.equalsIgnoreCase("DEBIT") &&!cards.stream().anyMatch(x -> x.getIssuer().equals("MDP"))&& cards.stream().anyMatch(y -> y.getIssuer().equals("NI"))
                            ?env.getProperty(" data.creditPoolAccount")
                            :data.requestType.equalsIgnoreCase("DEBIT")
                            ?env.getProperty(" data.creditPoolAccount")
                            : data.requestType.equalsIgnoreCase("CREDIT") && cards.stream().anyMatch(x -> x.getIssuer().equals("NI"))
                            ? env.getProperty("t24.poolaccount.niCreditPool")
                            : data.creditAccountId;
                    if (data.requestType.equalsIgnoreCase("CREDIT")&&(data.creditAccountId.length()>16||data.creditAccountId.charAt(0)=='1')){
                        t24CreditAcc=data.creditAccountId;
                    }
                    else {
                        t24CreditAcc=env.getProperty(" data.creditPoolAccount");
                    }
                }
                if (t24DebitAcc==null||t24DebitAcc.equals("")){
                    t24DebitAcc=data.debitAccountId;
                }
                LocalDateTime now = LocalDateTime.now();
                // String currentTimestamp = String.valueOf(Timestamp.valueOf(now));
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");


                //calculating deducted amount
                double deductedAmount=0;
                if(data.bearFee!=null) {
                    deductedAmount = Float.parseFloat(data.amount.get("orgValue"))+Float.parseFloat(data.bankFee.get("value"))+Float.parseFloat(data.pspFee.get("value"))-Float.parseFloat(data.bearFee.get("value"));
                }
                else {
                    deductedAmount = Float.parseFloat(data.amount.get("orgValue"))+Float.parseFloat(data.bankFee.get("value"))+Float.parseFloat(data.pspFee.get("value"));
                }
                DecimalFormat df = new DecimalFormat("#.##");
                deductedAmount = Double.valueOf(df.format(deductedAmount));


                if(data.requestType.equalsIgnoreCase("REVERSAL")){

                    String temp=t24CreditAcc;
                    t24CreditAcc=t24DebitAcc;
                    t24DebitAcc=temp;

                }
                String Original_Request_Type=Get_Org_Txn_Details(data);


                if(data.requestType.equalsIgnoreCase("REVERSAL")&&Original_Request_Type.equalsIgnoreCase("DE")){


                    t24DebitAcc=env.getProperty("t24.poolaccount.EBCDebitPool");


                }

                if(data.requestType.equalsIgnoreCase("REVERSAL")&&Original_Request_Type.equalsIgnoreCase("CR")){


                    t24CreditAcc= data.creditPoolAccount;


                }



                String body = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:dat=\"http://ws.wso2.org/dataservice\">" +
                        "   <soap:Header/>" +
                        "   <soap:Body>" +
                        "      <dat:REVERSALFTCONTRACT>" +
                        "         <dat:DBACCOUNT>" + t24DebitAcc + "</dat:DBACCOUNT>" +
                        "         <dat:DRCCY>EGP</dat:DRCCY>" +
                        "         <dat:CRAMT>" + deductedAmount + "</dat:CRAMT>" +
                        "         <dat:CRACCOUNT>" + t24CreditAcc + "</dat:CRACCOUNT>" +
                        "         <dat:CRCCY>" + data.amount.get("orgCurr") + "</dat:CRCCY>" +
                        "         <dat:XREF></dat:XREF>" +
                        "         <dat:DRVDT></dat:DRVDT>" +
                        "         <dat:CRVDT></dat:CRVDT>" +
                        "         <dat:INTRMKS>"+data.requestType+"</dat:INTRMKS>" +
                        "         <dat:OrderingBank>ADCB</dat:OrderingBank>" +
                        "      </dat:REVERSALFTCONTRACT>" +
                        "   </soap:Body>" +
                        "</soap:Envelope>";
//            dateFormat.format(Timestamp.valueOf(now.plusDays(-5)))
//            dateFormat.format(Timestamp.valueOf(now.plusDays(-5)))
                String response = Request.SendRequest(env.getProperty("IPNBaseURL"), "REVERSALFTCONTRACT", body);
                Document doc = XMLParser.ReadXML(response);
                LocalDateTime now2 = LocalDateTime.now();
                String currentTimestamp2 = String.valueOf(Timestamp.valueOf(now2));
                String ref = doc.getElementsByTagName("HOSTREF").item(0).getTextContent();


                //TODO Balance Check


                DecimalFormat  formatter = new DecimalFormat("#0.00");
                String balance = "";


                if(data.requestType.equalsIgnoreCase("CREDIT")||data.requestType.equalsIgnoreCase("REVERSAL")) {
                    balance = Get_Account_Balance(t24CreditAcc);
                }

                else{

                    balance = Get_Account_Balance(t24DebitAcc);

                }

                Login customer = loginsRepository.getLoginByMobileNumber(data.payerMobileNumber);
                String docType = customer.getDocumentType().equals("NationalId") ? "NID" : customer.getDocumentType();


                String msg = data.requestType.equalsIgnoreCase("DEBIT") ? deductedAmount + " has been debited from your ADCB account No ****" + data.debitAccountId.substring(data.debitAccountId.length() - 4, data.debitAccountId.length()) + " on " + currentTimestamp2.substring(0, 10) + " to , ref. " + data.transactionId.substring(data.transactionId.length() - 8, data.transactionId.length()) + "" :
                        data.requestType.equalsIgnoreCase("CREDIT") ? deductedAmount + " has been credited to your ADCB account No ****" + data.creditAccountId.substring(data.creditAccountId.length() - 4, data.creditAccountId.length()) + " on " + currentTimestamp2.substring(0, 10) + " from " + data.payeeName + ", ref." + data.transactionId.substring(data.transactionId.length() - 8, data.transactionId.length()) + "" : "";

                SendSMS_FUNC(data.payerMobileNumber,msg);


                //TODO CARD BALANCE CHECK
                balance=formatter.format(Double.valueOf(balance));
                CombinedResDto res = new CombinedResDto();
                res.checkstatusres=new CheckStatusResDto(data.requestId, currentTimestamp2, "00000", "SUCCESS", balance, "EGP", ref, customer.getName(), customer.getAddress(), docType, customer.getLegalId());
                if(data.requestType.equalsIgnoreCase("REVERSAL")){

                    reversalLogsRepository.save(new ReversalLog(data.transactionId, now, data.toString(), res.checkstatusres.toString(),null));


                }
                else
                    acceptedLogsRepository.save(new AcceptedLog(data.transactionId, now, data.toString(), res.checkstatusres.toString(),null));

                return res;


            }
        } catch (Exception ex) {

            Logging.warn(ex.getStackTrace().toString());
            return null;

        }

        return null;
    }

    public static String Check_Account_Type(String AccountID,String AccountType){

        List<Card> cards = Streamable.of(cardsRepository.findAll()).toList();


        if(AccountType==null){

            if(AccountID.length()==16 && (AccountID.startsWith("41") ||AccountID.startsWith("45") || AccountID.startsWith("50")))
            {

                AccountType= "CARD";
                return AccountType;

            }
            else if(AccountID.length()==16 && AccountID.startsWith("11")){

                AccountType= "ACCOUNT";
                return AccountType;

            }
            else if(AccountID.length()==14 && AccountID.startsWith("00201")){

                AccountType="WALLET";
                return AccountType;


            }
        }
        else{

            return AccountType;
        }

        return AccountType;
    }

    public static String Check_Transaction_Status(CheckStatusReqDto data)throws Exception {

        try {
            String[] ReversalstatusDB = reversalLogsRepository.getFt(data.transactionId);

            if( ReversalstatusDB.length>0){

                return "Reversed";
            }


            String[] statusDB = allLogsRepsitory.getResp(data.transactionId);
            String finalStatusDB = statusDB[0];
            if(finalStatusDB.equalsIgnoreCase("REJECTED")){

                return finalStatusDB;
            }

            System.out.println(statusDB[0]);
            String[] respString = acceptedLogsRepository.getFt(data.transactionId);
            int index = respString[0].indexOf("authCode") + 10;
            String ftRequest = "";
            while (respString[0].charAt(index) != '\'') {
                ftRequest += respString[0].charAt(index);
                index += 1;
            }
            //===================================
            String ftBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <dat:GetFTstatus>\n" +
                    "         <dat:FT_ID>" + ftRequest + "</dat:FT_ID>\n" +
                    "      </dat:GetFTstatus>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope>";
            String resFT = Request.SendRequest(env.getProperty("IPNBaseURL"), "GetFTstatus", ftBody);
            int indexAmount = resFT.indexOf("AMOUNT") + 7;
            String amontValue = "";
            String ftStatus = "";
            while (resFT.charAt(indexAmount) != '<') {
                amontValue += resFT.charAt(indexAmount);
                indexAmount += 1;
            }
            if (Float.parseFloat(amontValue) > 0) {
                ftStatus = "ACCEPTED";
            } else {
                ftStatus = "REJECTED";
            }
            return ftStatus;
        }
        catch(Exception ex){
            Logging.warn(ex.getStackTrace().toString());

            throw ex;
        }


    }

    public static String Get_Org_Txn_Details(CheckStatusReqDto data) throws JSONException {

        String[] statusDB = allLogsRepsitory.getReq(data.transactionId);

        String ORG_Request=statusDB[0];
        String Request_Type_Initials=ORG_Request.substring(ORG_Request.toUpperCase().indexOf("REQUESTTYPE")+13,ORG_Request.toUpperCase().indexOf("REQUESTTYPE")+15);
        return Request_Type_Initials;
    }

    public static String Get_Transaction_Refrence_Fimi(String response){

        String trensaction_refrence=response.substring(response.indexOf("<m0:ThisTranId>")+15,response.indexOf("</m0:ThisTranId>"));
        return trensaction_refrence;


    }


    public static String SendSMS_FUNC(String phonenumber,String message) throws Exception {


        String body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <dat:SendSMS>\n" +
                "         <dat:PhoneNumber>" + phonenumber + "</dat:PhoneNumber>\n" +
                "         <dat:Message>" + message + "</dat:Message>\n" +
                "      </dat:SendSMS>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";

        String res = Request.SendRequest(env.getProperty("IPNBaseURL"), "SendSMS", body);

        return res;
    }

    public static String AddCountryCode(String phoneNumber){

        if(phoneNumber.startsWith("01")) {
            phoneNumber = "002" + phoneNumber;
        }
        else if(phoneNumber.startsWith("2")) {
            phoneNumber = "00" + phoneNumber;

        }
        return phoneNumber;
    }
}
