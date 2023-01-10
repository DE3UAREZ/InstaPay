package eg.com.adcb.IPN.controllers;


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
import eg.com.adcb.IPN.controllers.FT;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.json.*;


import static org.springframework.http.HttpStatus.BAD_REQUEST;


@RestController
public class AccountsController {


    @Autowired
    private Environment env;
   // private static Logging Logging = Logging.getLogging(Logging.class);
    private AccountsRepository accountsRepository;
    private AcceptedLogsRepository acceptedLogsRepository;
    private ReversalLogsRepository reversalLogsRepository;
    private RejectedLogsRepository rejectedLogsRepository;
    private LoginsRepository loginsRepository;
    private CardsRepository cardsRepository;
    private ErrorsRepository errorsRepository;
    private AllLogsRepsitory allLogsRepsitory;
    private mfieldsRepository mfieldsRepoX;
    private FT checkstatusFT;
    private String fimiTrxRef=null;

    public AccountsController(Environment env, AccountsRepository accountsRepository, AcceptedLogsRepository acceptedLogsRepository, RejectedLogsRepository rejectedLogsRepository, LoginsRepository loginsRepository, CardsRepository cardsRepository, ErrorsRepository errorsRepository, AllLogsRepsitory allLogsRepsitory,ReversalLogsRepository reversalLogsRepository,mfieldsRepository mfieldsRepoX) {
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

    @PostMapping
    @RequestMapping("accounts")
    public Object getAccountsList(@RequestBody GetAccountsReqDto data) {

        List<Errors> errors = Streamable.of(errorsRepository.findAll()).toList();
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
            Logging.host("Host response -------------" + responseBody);
            Document doc = XMLParser.ReadXML(responseBody);
            Map<String, String> customerDetails = new HashMap<>();
            String nationalId = doc.getElementsByTagName("LEGALID").item(0).getTextContent();
            String mobileNumber = doc.getElementsByTagName("TELNO").item(0).getTextContent();
        }

        catch (Exception ex){

            ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"12010","Service is currently not available from your bank, try again later");
            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/accounts}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={400}; RESPONSE={ Service is currently not available from your bank, try again later}; ");
            Logging.warn(ExceptionStackTrace.GetStackTrace(ex));
            return response;
        }

        try {
            String nullCheck=checkNull(data,"accounts");
            if(nullCheck!=""){
                ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"11017","Invalid technical data");
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/accounts}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={11017}; RESPONSE={"+nullCheck+" is not found}; ");
                return response;
            }

            if(data.settlementCycleId.length()>11){

                ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"11017","Invalid technical data");
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/accounts}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={11017}; RESPONSE={"+response+"} | settlementCycleId not correct}; ");
                return response;

            }

            if(!data.mobileNumber.startsWith("002") ||  !isNumeric(data.mobileNumber) || data.mobileNumber.length()!=14){
                ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"11017","Invalid technical data");
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/accounts}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={11017}; RESPONSE={"+response+"} | mobile number format is not correct}; ");
                return response;

            }

            if(data.customerType!=null && !data.customerType.equalsIgnoreCase("CONSUMER")){

                ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"11017","Invalid technical data");
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/accounts}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={11017}; RESPONSE={"+nullCheck+" } | customerType not correct}; ");
                return response;

            }

//            if (data.authenticationApprovalToken==null){
//                ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"11017","Invalid technical data");
//                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={400}; RESPONSE={authenticationApprovalToken is null }; ");
//                return response;
//            }
//            else if (data.authenticationApprovalToken.isEmpty()||data.authenticationApprovalToken.equals("null")||data.authenticationApprovalToken==""){
//                ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"11017","Invalid technical data");
//                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={400}; RESPONSE={authenticationApprovalToken is invald }; ");
//                return response;
//            }

            String [] RequesID=allLogsRepsitory.getDuplicateRequest(data.requestId);
            if (RequesID.length>0){
                ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"11014","Duplicated transaction from your bank");
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/accounts}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={11014}; RESPONSE={"+response+" }; ");
                return response;
            }


            String authenticationApprovalToken=loginsRepository.getToken(data.authenticationRequestId);
            Map<String, String> customer = accountsRepository.getCustomerDataFromToken(authenticationApprovalToken);

          if(!data.customerReference.equals(customer.get("CustomerCode")))  {

              ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"11017","Invalid technical data");
              Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/accounts}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={11017}; RESPONSE={Invalid technical data}; ");
              return response;
          }

//            if (customer.size()==0){
//                ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"11017","Invalid technical data");
//                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={400}; RESPONSE={login Faild, authenticationApprovalToken is invald}; ");
//
//                return response;
//            }
//            if (!customer.get("RequestId").equals(data.authenticationRequestId)) {
//                String ErrorDesc=errorsRepository.getErrorDes("UNAUTHORIZED");
//                String ErrorCode=errorsRepository.getErrorCode("UNAUTHORIZED");
//                ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,ErrorCode,ErrorDesc);
//                return response;
////                throw new Exception(errorsRepository.getErrorDes("UNAUTHORIZED"));
//            }

            String customerNumber = customer.get("CustomerCode");
            Map<String, String> customerDetails = accountsRepository.getCustomerDetails(customerNumber);
            String returnedMobile = customerDetails.get("mobileNumber").replaceAll("[+]2", "002");
            if (!data.mobileNumber.equals(returnedMobile)){
                ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"11008","Selected mobile number is not registered at your bank, please update your information");
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/accounts}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={11008}; RESPONSE={Selected mobile number is not registered at your bank, please update your information }; ");
                return response;
            }
            String Issuer=loginsRepository.getLastAuthAccountId(data.mobileNumber);
            List<Account> accounts = new ArrayList<>();
            List<Account> cards = new ArrayList<>();
            List<Account> mdpCards = new ArrayList<>();
            List<Account> accountsMdpUsed = new ArrayList<>();
            if (Issuer.charAt(0)=='5'){

                List<Account> meezaUsedCardOnly=accountsRepository.getCustomerMDPCard(customerNumber,Issuer.substring(Issuer.length()-4));
                accountsMdpUsed = Stream.concat(accounts.stream(), meezaUsedCardOnly.stream())
                        .collect(Collectors.toList());
            }

            else {
                accounts = accountsRepository.getCustomerAccounts(customerNumber);
                //cards = accountsRepository.getCustomerCards(customerNumber);
                mdpCards = accountsRepository.getCustomerMDPCards(customerNumber);
                //accounts = Stream.concat(accounts.stream(), cards.stream())
                //.collect(Collectors.toList());
                accounts = Stream.concat(accounts.stream(), mdpCards.stream())
                        .collect(Collectors.toList());
            }


//            if (accounts.size()==0 || cards.size()==0){
//                String ErrorDesc=errorsRepository.getErrorDes("NotFound");
//                String ErrorCode=errorsRepository.getErrorCode("NotFound");
//                ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,ErrorCode,ErrorDesc);
//                return response;
//               throw new Exception(errorsRepository.getErrorDes("NotFound"));
//            }








            LocalDateTime now = LocalDateTime.now();
            String currentTimestamp = String.valueOf(Timestamp.valueOf(now));

            Map<String, Object> accountsWrapper = new Hashtable<>();
            if (accountsMdpUsed.size()==0) {
                accountsWrapper.put("ac", accounts);
            }
            else {
                accountsWrapper.put("ac", accountsMdpUsed);
            }
            if(data.customerType==null){
                GetAccountsResDto response = new GetAccountsResDto(
                        data.requestId,
                        currentTimestamp,
                        "00000",
                        "SUCCESS",
                        data.customerReference,
                        "NID",
                        customer.get("NationalId"),
                        accountsWrapper
                );
                AcceptedLog hi = new AcceptedLog(data.requestId, now, data.toString(), response.toString(),null);
                acceptedLogsRepository.save(hi);
                Logging.info("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/accounts}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={200}; RESPONSE={" + response + "}; ");

                return response;
            }
            else {
                GetAccountsResDto response = new GetAccountsResDto(
                        data.requestId,
                        currentTimestamp,
                        "00000",
                        "SUCCESS",
                        data.customerReference,
                        data.customerType,
                        customer.get("NationalId"),
                        accountsWrapper
                );
                AcceptedLog hi = new AcceptedLog(data.requestId, now, data.toString(), response.toString(),null);
                acceptedLogsRepository.save(hi);
                Logging.info("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/accounts}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={200}; RESPONSE={" + response + "}; ");

                return response;
            }

        } catch (Exception ex) {
            String stackTrace = ExceptionStackTrace.GetStackTrace(ex);
            rejectedLogsRepository.save(new RejectedLog(data.requestId, LocalDateTime.now(), data.toString(), stackTrace,null));
            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/accounts}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={400}; RESPONSE={"+stackTrace+"}; ");
            ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"11017","Invalid technical data");
            return response;

        }
    }

    @PostMapping
    @RequestMapping("fundtransfer")
    public Object fundTransfer(@RequestBody FundTransferReqDto data) throws Exception {
        String balance = "";
        String refMDP=null;
        String CIF="";


        if (data.requestType==null){
            ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12015", "Invalid technical data");
            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12015}; RESPONSE={" + response + " } | requestType is null}; ");
            return response;
        }


        try {

            if (data.requestType.equalsIgnoreCase("CREDIT") || data.requestType.equalsIgnoreCase("DEBIT")) {



                if (data.requestType.equalsIgnoreCase("DEBIT") && data.debitAccountType.equalsIgnoreCase("ACCOUNT") && data.debitAccountId != null) {


                if (!data.debitAccountId.substring(data.debitAccountId.length() - 4).startsWith("01")) {

                    ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12013", "Restricted account, please contact your bank");
                    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12013}; RESPONSE={" + response + " }; ");
                    rejectedLogsRepository.save(new RejectedLog(data.requestId, LocalDateTime.now(), data.toString(), response.toString(), null));
                    return response;

                }
                }

                if (data.requestType.equalsIgnoreCase("CREDIT") && data.creditAccountType.equalsIgnoreCase("ACCOUNT") && data.creditAccountId != null) {

                if ( !data.creditAccountId.substring(data.creditAccountId.length() - 4).startsWith("01") &&(data.creditAccountId.startsWith("11")||data.creditAccountId.startsWith("EG"))) {

                    ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13003}; RESPONSE={" + response + " }; ");
                    rejectedLogsRepository.save(new RejectedLog(data.requestId, LocalDateTime.now(), data.toString(), response.toString(), null));
                    return response;

                }
            }


                    CIF = Get_CIF(data);


                if(CIF==null && data.requestType.equalsIgnoreCase("CREDIT")){

                    ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13001", "The beneficiary account number is invalid");
                    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13001}; RESPONSE={" + response + " }; ");
                    rejectedLogsRepository.save(new RejectedLog(data.requestId, LocalDateTime.now(), data.toString(), response.toString(), null));
                    return response;
                }

                if (CIF.equals("NOT_FOUND")) {

                    if (data.requestType.equalsIgnoreCase("DEBIT")) {

                        ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12005", "Restricted transaction, please contact your bank");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12005}; RESPONSE={" + response + " }; |Get_CIF function ");
                        rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                        return response;
                    } else {
                        ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13001", "The beneficiary account number is invalid");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13001}; RESPONSE={"+ response+" } | Get_CIF function:" + data.creditAccountId + " is not found}; ");
                        rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));

                        return response;
                    }
                } else if (CIF.equals("inavlid_data")) {

                    ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12015", "Invalid technical data");
                    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12015}; RESPONSE={" + response + " }; | Get_CIF function wrong  format");
                    rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));

                    return response;

                }

            }
        }
        catch(Exception ex){

            ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"12010","Service is currently not available from your bank, try again later");
            Logging.warn("FINISHED PROCESSING -Error in Get_CIF : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={400}; RESPONSE={ "+ex.getMessage()+"}; ");
            Logging.warn(ExceptionStackTrace.GetStackTrace(ex));

            return response;

        }
        double deductedAmount=0;

        deductedAmount=Calc_Deducted_Amount(data,data.requestType);

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
            Logging.host("Host response -------------" + responseBody);
            Document doc = XMLParser.ReadXML(responseBody);
            Map<String, String> customerDetails = new HashMap<>();
            String nationalId = doc.getElementsByTagName("LEGALID").item(0).getTextContent();
            String mobileNumber = doc.getElementsByTagName("TELNO").item(0).getTextContent();
        }

        catch (Exception ex){
            ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"12010","Service is currently not available from your bank, try again later");
            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={400}; RESPONSE={ Service is currently not available from your bank, try again later}; ");
            Logging.warn(ExceptionStackTrace.GetStackTrace(ex));

            return response;
        }


        String nullCheck = checkNull(data, "fundtransfer");
        if (nullCheck != "") {
            ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12015", "Invalid technical data");
            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
            return response;
        }
        if (data.atmID!=null){
            if (data.atmID.length()>16){
                ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12015", "Invalid technical data");
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12015}; RESPONSE={" + response + " } | atmID format not correct}; ");
                return response;
            }
        }
        if (data.atmLocation!=null){
            if (data.atmLocation.length()>100){
                ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12015", "Invalid technical data");
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12015}; RESPONSE={" + response + "} | atmLocation is not correct}; ");
                return response;
            }
        }


        if (data.requestType.equalsIgnoreCase("REVERSAL")||data.requestType.equalsIgnoreCase("CREDIT")){
            if (data.debitPoolAccount==null){
                ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12015", "Invalid technical data");
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12015}; RESPONSE={" + response + "} | debitPoolAccount is not found}; ");
                return response;
            }

        }
        if (data.mandateID!=null){
            if (data.mandateID.equals("")||data.mandateID.length()>100){
                ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12015", "Invalid technical data");
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12015}; RESPONSE={" + response + "} | mandateID is not correct}; ");
                return response;
            }
        }
        if (data.creditAccountType!=null) {
            if (data.creditAccountType.equalsIgnoreCase("ACCOUNT")&&data.requestType.equalsIgnoreCase("CREDIT") && data.creditAccountId.length()>=16) {
                String ReqBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                        "   <soapenv:Header/>\n" +
                        "   <soapenv:Body>\n" +
                        "      <dat:ACCOUNTDETAILS>\n" +
                        "         <dat:ACCNO>" + data.creditAccountId.substring(data.creditAccountId.length() - 16) + "</dat:ACCNO>\n" +
                        "      </dat:ACCOUNTDETAILS>\n" +
                        "   </soapenv:Body>\n" +
                        "</soapenv:Envelope>";
                String res_Balance = Request.SendRequest(env.getProperty("IPNBaseURL"), "ACCOUNTDETAILS", ReqBody);
                Logging.host("Host response -------------" + res_Balance);
                if (res_Balance.contains("No records were found")) {
                    ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13003}; RESPONSE={" + response + " }; ");
                    rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                    return response;
                }
                Document doc_balance = XMLParser.ReadXML(res_Balance);
                String currency = doc_balance.getElementsByTagName("CCYDESC").item(0).getTextContent();
                if (!currency.equalsIgnoreCase("EGP")) {
                    ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13003}; RESPONSE={" + response + " }; ");
                    rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                    return response;
                }

            }
            else if(data.creditAccountType.equalsIgnoreCase("ACCOUNT")&&data.requestType.equalsIgnoreCase("CREDIT") && data.creditAccountId.length()<16){
                ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13003}; RESPONSE={" + response + " }; ");
                rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                return response;

            }
        }

        String daccountId=data.debitAccountId.substring(3);
        if (!daccountId.matches("[0-9]+") && data.requestType.equalsIgnoreCase("DEBIT")){
            ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12015", "Invalid technical data");
            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12015}; RESPONSE={ "+response+"} | debitAccountId not correct}; ");
            return response;
        }
        if (data.requestType.equalsIgnoreCase("REVERSAL")){
            if(data.orgTxnId==null){

                ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12015", "Invalid technical data");
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12015}; RESPONSE={ "+response+"} | orgTxnId not found }; ");
                return response;

            }
            if(!data.orgTxnId.equalsIgnoreCase(data.transactionId)){

                ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12031", "Original transaction not found");
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12031}; RESPONSE={ is not found}; ");
                return response;
            }
            else{

                if(Check_Transaction_Status(data).equalsIgnoreCase("TXn_NOT_EXIST")) {
                    ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12031", "Original transaction not found");
                    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12031}; RESPONSE={ is not found}; ");
                    return response;
                }

                if(Check_Transaction_Status(data).equalsIgnoreCase("rejected"))  {


                    if(Get_Org_Txn_Details(data).equalsIgnoreCase("CR")&&data.creditAccountId.startsWith("41")){


                        balance=Get_Credit_Card_Balance(data.creditAccountId);
                    }
                    else if(Get_Org_Txn_Details(data).equalsIgnoreCase("DE")&&data.debitAccountId.startsWith("41")){

                        balance=Get_Credit_Card_Balance(data.debitAccountId);

                    }
                    else if(Get_Org_Txn_Details(data).equalsIgnoreCase("DE")&&data.debitAccountId.startsWith("45")){

                        String Acc=Get_account_from_card_number(data.debitAccountId);
                        balance=Get_Account_Balance(Acc);
                    }
                    else if(Get_Org_Txn_Details(data).equalsIgnoreCase("CR")&&data.creditAccountId.startsWith("50")){


                        balance=Get_Account_Balance(data.creditAccountId);
                    }

                    else if(Get_Org_Txn_Details(data).equalsIgnoreCase("DE")&&data.debitAccountId.startsWith("50")){

                        balance=Get_Account_Balance(data.debitAccountId);
                    }
                    else if(Get_Org_Txn_Details(data).equalsIgnoreCase("CR")){

                        balance=Get_Account_Balance(data.creditAccountId);
                    }
                    else if(Get_Org_Txn_Details(data).equalsIgnoreCase("DE")){

                        balance=Get_Account_Balance(data.debitAccountId);
                    }

                    Login customer = loginsRepository.getLoginByMobileNumber(data.payerMobileNumber);
                    String docType = customer.getDocumentType().equals("NationalId") ? "NID" : customer.getDocumentType();

                    DecimalFormat formatter = new DecimalFormat("#0.00");

                    LocalDateTime now = LocalDateTime.now();
                    balance = formatter.format(Double.valueOf(balance));
                    FundTransferResDto res = new FundTransferResDto(data.requestId, data.timestamp, "00000", "SUCCESS", balance, "EGP", null, customer.getName(), customer.getAddress(), docType, customer.getLegalId());


                    reversalLogsRepository.save(new ReversalLog(data.transactionId, now, data.toString(), res.toString(), null));


                    return res;

                    //  ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12031", "Original transaction not found");
                    //    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={ is not found}; ");
                    //  return response;

                }
                else if(Check_Transaction_Status(data).equalsIgnoreCase("reversed"))  {

                    ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12030", "Reversal already processed");
                    Logging.info("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12030}; RESPONSE={" + response + "}; ");
                    return response;

                }
                else{

                    String Original_Request_Type=Get_Org_Txn_Details(data);

                    if((Original_Request_Type.equalsIgnoreCase("CR")&&data.creditAccountType.equalsIgnoreCase("CARD")&&data.creditAccountId.startsWith("50") )||(Original_Request_Type.equalsIgnoreCase("DE")&&data.debitAccountType.equalsIgnoreCase("CARD")&&data.debitAccountId.startsWith("50") )){

                        return Reversal_Meeza(data);

                    }
                    if((Original_Request_Type.equalsIgnoreCase("CR")&&data.creditAccountType.equalsIgnoreCase("CARD")&&data.creditAccountId.startsWith("41") )||(Original_Request_Type.equalsIgnoreCase("DE")&&data.debitAccountType.equalsIgnoreCase("CARD")&&data.debitAccountId.startsWith("41") )){

                        return Reversal_NI(data);
                    }

                    if((Original_Request_Type.equalsIgnoreCase("CR")&&data.creditAccountType.equalsIgnoreCase("CARD")&&data.creditAccountId.startsWith("45") )||(Original_Request_Type.equalsIgnoreCase("DE")&&data.debitAccountType.equalsIgnoreCase("CARD")&&data.debitAccountId.startsWith("45") )){

                        return Reversal_NI(data);
                    }




                    if(Check_Account_Existence(data)!=null){

                        ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12031", "Account not found");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12031}; RESPONSE={ "+response+"}; ");
                        return response;
                    }
                    else{
                        String CustomerNumber;
                        CustomerNumber = data.requestType.equalsIgnoreCase("DEBIT")
                                ? data.debitAccountId.substring(data.debitAccountId.length()-15, (data.debitAccountId.length()-15)+8)
                                : data.requestType.equalsIgnoreCase("CREDIT")
                                ? data.creditAccountId.substring(data.creditAccountId.length()-15, (data.debitAccountId.length()-15)+8)
                                : data.debitAccountId.substring(0,3).equals("EGP")
                                ?data.creditAccountId.substring(data.creditAccountId.length()-15, (data.debitAccountId.length()-15)+8)
                                :data.creditAccountId.substring(0,3).equals("EGP")
                                ?data.debitAccountId.substring(data.debitAccountId.length()-15, (data.debitAccountId.length()-15)+8)
                                :null;

                        return  Credit_Account(CustomerNumber,data);

                    }

                }


            }
        }

        if(Restrict_Foreign_Currencies(data).equalsIgnoreCase("FOREIGN")){

            ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13003}; RESPONSE={" + response + " }; ");
            rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
            return response;

        }

        if (!"EGP".equalsIgnoreCase(data.amount.get("orgCurr")) && data.amount.get("orgCurr") != null) {
            ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12005", "Restricted transaction, please contact your bank");
            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12005}; RESPONSE={" + response + " }; ");
            rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));

            return response;
        }
        if (data.debitAccountType.equalsIgnoreCase("ACCOUNT") && data.requestType.equalsIgnoreCase("DEBIT")){
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
            Logging.host("Host response -------------" + res_Balance);
            if (res_Balance.contains("No records were found")&&data.requestType.equalsIgnoreCase("DEBIT")){
                ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12011", "Transaction cannot be processed by your bank, try again later");
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12011}; RESPONSE={ "+response+"}; ");
                return response;
            }
            try {
                Document doc2 = XMLParser.ReadXML(res_Balance);
                String phoneNum = doc2.getElementsByTagName("PHONENUMBER").item(0).getTextContent();
                String accStatus = doc2.getElementsByTagName("STATUS").item(0).getTextContent();
                if (accStatus.equals("D")){
                    ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12013", "Restricted account, please contact your bank");
                    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12013}; RESPONSE={ "+response.toString()+"}; ");
                    rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));

                    return response;
                }

                phoneNum = AddCountryCode(phoneNum);




                if (!data.payerMobileNumber.equals(phoneNum)) {
                    ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12004", "Your mobile number is not matching with bank records, please contact your bank");
                    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12004}; RESPONSE={" + response + " }; ");
                    rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                    return response;
                }
            }
            catch (Exception e){

            }
        }
        if (data.requestType.equalsIgnoreCase("CREDIT")&&data.creditAccountId!=null){
            if (!data.creditAccountId.substring(3).matches("[0-9]+")){
                ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"13001","The beneficiary account number is invalid");
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={13001}; RESPONSE={ "+response+"}; ");
                rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));

                return response;
            }
        }

        if (data.requestType.equalsIgnoreCase("DEBIT")&&data.debitAccountId!=null) {

            if (!data.debitAccountId.substring(3).matches("[0-9]+")) {
                ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13001", "The beneficiary account number is invalid");
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13001}; RESPONSE={ " + response + "}; ");
                rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                return response;
            }
        }
        try {



            //Balance Check
            try {

                if (data.creditAccountType!=null) {
                    if (data.creditAccountType.equalsIgnoreCase("ACCOUNT")&&data.requestType.equalsIgnoreCase("CREDIT")){
                        String ReqBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                                "   <soapenv:Header/>\n" +
                                "   <soapenv:Body>\n" +
                                "      <dat:ACCOUNTDETAILS>\n" +
                                "         <dat:ACCNO>" + data.creditAccountId.substring(data.creditAccountId.length() - 16) + "</dat:ACCNO>\n" +
                                "      </dat:ACCOUNTDETAILS>\n" +
                                "   </soapenv:Body>\n" +
                                "</soapenv:Envelope>";
                        String res_Balance = Request.SendRequest(env.getProperty("IPNBaseURL"), "ACCOUNTDETAILS", ReqBody);
                        Logging.host("Host response -------------" + res_Balance);
                        if (res_Balance.contains("No records were found")){
                            ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13001", "The beneficiary account number is invalid");
                            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={13001}; RESPONSE={ "+response+"}; ");
                            rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                            return response;
                        }
                        Document doc_balance = XMLParser.ReadXML(res_Balance);
                        String currency = doc_balance.getElementsByTagName("CCYDESC").item(0).getTextContent();
                        if (!currency.equals("EGP")){
                            ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13003}; RESPONSE={" + response + " }; ");
                            rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                            return response;
                        }

                    }
                    if (data.debitAccountType.equalsIgnoreCase("ACCOUNT")&&data.requestType.equalsIgnoreCase("DEBIT")) {


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
                        Logging.host("Host response -------------" + res_Balance);
                        if (res_Balance.contains("No records were found") &&data.requestType.equalsIgnoreCase("DEBIT")){
                            ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13001", "The beneficiary account number is invalid");
                            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={13001}; RESPONSE={ "+response+"}; ");
                            rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                            return response;
                        }
                        try {
                            Document doc_balance = XMLParser.ReadXML(res_Balance);
                            balance = doc_balance.getElementsByTagName("BAL_AVAIL").item(0).getTextContent();
                            System.out.println("sadsd" + balance);


                            if (Float.parseFloat(balance) < Float.parseFloat(data.amount.get("orgValue"))) {
                                String ErrorDesc = errorsRepository.getErrorDes("INSUFFICIENT BALANCE");
                                String ErrorCode = errorsRepository.getErrorCode("INSUFFICIENT BALANCE");
                                ErrorsResDto errResp = new ErrorsResDto(data.requestId, data.timestamp, "12001", "Insufficient balance to execute the transaction");
                                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12001}; RESPONSE={" + errResp.toString() + " }; ");
                                rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), errResp.toString(),null));

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
//                ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13001", "The beneficiary account number is invalid");
//                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
//                return response;
//
//            }

            if (data.creditAccountType!=null){
                if (data.creditAccountType.equalsIgnoreCase("ACCOUNT")&&data.requestType.equalsIgnoreCase("CREDIT")){
                    String test_body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <dat:ACCOUNTDETAILS>\n" +
                            "         <dat:ACCNO>" + data.creditAccountId.substring(data.creditAccountId.length() - 16) + "</dat:ACCNO>\n" +
                            "      </dat:ACCOUNTDETAILS>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";
                    String res_TEST = Request.SendRequest(env.getProperty("IPNBaseURL"), "ACCOUNTDETAILS", test_body);
                    Logging.host("Host response -------------" + res_TEST);
                    Document doc2 = XMLParser.ReadXML(res_TEST);
                    if (res_TEST.contains("No records were found")&& data.requestType.equalsIgnoreCase("CREDIT")){
                        ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13001", "The beneficiary account number is invalid");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={13001}; RESPONSE={ "+response+"}; ");
                        rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                        return response;
                    }
                    try{
                        balance = doc2.getElementsByTagName("BAL_AVAIL").item(0).getTextContent();
                        String currency = doc2.getElementsByTagName("CCYDESC").item(0).getTextContent();
                        String status = doc2.getElementsByTagName("STATUS").item(0).getTextContent();
                        String phoneNum = doc2.getElementsByTagName("PHONENUMBER").item(0).getTextContent();

                        phoneNum = AddCountryCode(phoneNum);

                        if (data.payeeMobileNumber!=null){
                            if (!phoneNum.equals(data.payeeMobileNumber)){
                                ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13002", "Transaction rejected from beneficiary bank due to invalid beneficiary information");
                                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13002}; RESPONSE={" + response + " }; ");
                                rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                                return response;
                            }
                        }
                        if (!currency.equals("EGP")){
                            ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13003}; RESPONSE={" + response + " }; ");
                            rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                            return response;
                        }
                        if (status.equals("D")){
                            ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13003}; RESPONSE={" + response + " }; ");
                            rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                            return response;
                        }
                    }
                    catch (Exception e){}

                }
                else if (data.debitAccountType.equalsIgnoreCase("ACCOUNT")&&data.requestType.equalsIgnoreCase("DEBIT")){
                    String test_body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <dat:ACCOUNTDETAILS>\n" +
                            "         <dat:ACCNO>" + data.debitAccountId.substring(data.debitAccountId.length() - 16) + "</dat:ACCNO>\n" +
                            "      </dat:ACCOUNTDETAILS>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";
                    String res_TEST = Request.SendRequest(env.getProperty("IPNBaseURL"), "ACCOUNTDETAILS", test_body);
                    Logging.host("Host response -------------" + res_TEST);
                    Document doc2 = XMLParser.ReadXML(res_TEST);
                    if (res_TEST.contains("No records were found")&& data.requestType.equalsIgnoreCase("DEBIT")){
                        ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13001", "The beneficiary account number is invalid");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={13001}; RESPONSE={ "+response+"}; ");
                        rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                        return response;
                    }
                    try{
                        balance = doc2.getElementsByTagName("BAL_AVAIL").item(0).getTextContent();
                        String currency = doc2.getElementsByTagName("CCYDESC").item(0).getTextContent();
                        String status = doc2.getElementsByTagName("STATUS").item(0).getTextContent();
                        String phoneNum = doc2.getElementsByTagName("PHONENUMBER").item(0).getTextContent();
                        phoneNum = AddCountryCode(phoneNum);

                        if (data.payerMobileNumber!=null){
                            if (!phoneNum.equals(data.payerMobileNumber)){
                                ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13002", "Transaction rejected from beneficiary bank due to invalid beneficiary information");
                                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13002}; RESPONSE={" + response + " }; ");
                                rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                                return response;
                            }
                        }
                        if (!currency.equalsIgnoreCase("EGP")){
                            ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13003}; RESPONSE={" + response + " }; ");
                            rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                            return response;
                        }
                        if (status.equalsIgnoreCase("D")){
                            ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13003}; RESPONSE={" + response + " }; ");
                            rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                            return response;
                        }
                    }
                    catch (Exception e){}

                }
                else if (data.creditAccountType.equalsIgnoreCase("CARD")&&data.requestType.equalsIgnoreCase("CREDIT") && cards.stream().anyMatch(x -> x.getIssuer().equals("MDP") && x.getStartsWith().equals(data.creditAccountId.substring(0, 6)))) {
                    String mdpCardInfo2="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:mdp=\"http://ws.wso2.org/dataservice/mdpCardInfo\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <mdp:_getmdpcardinfo>\n" +
                            "         <mdp:cardNumber>"+data.creditAccountId+"</mdp:cardNumber>\n" +
                            "      </mdp:_getmdpcardinfo>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";
                    String resMdpCardInfo2 = Request.SendRequest(env.getProperty("MDPBaseURL"), "_getmdpcardinfo", mdpCardInfo2);
                    Logging.host("Host response -------------" + resMdpCardInfo2);
                    if (resMdpCardInfo2.contains("Unable to find card")){
                        ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"12011","Transaction cannot be processed by your bank, try again later");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12011}; RESPONSE={" + response + " }; ");

                        return response;
                    }


                    Document docMdp2 = XMLParser.ReadXML(resMdpCardInfo2);
                    String innerXMLmdpCard = docMdp2.getElementsByTagName("response").item(0).getTextContent();
                    docMdp2 = XMLParser.ReadXML(innerXMLmdpCard);
                    String state = docMdp2.getElementsByTagName("state").item(0).getTextContent();
                    String statusCode = docMdp2.getElementsByTagName("statusCode").item(0).getTextContent();
                    if (!statusCode.equals("0")){
                        ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13003}; RESPONSE={" + response + " }; ");
                        rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
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
                    Logging.host("Host response -------------" + resMdpCardInfo);
                    if(resMdpCardInfo.contains("Unable to find card")){
                        ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"13001","The beneficiary account number is invalid");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={13001}; RESPONSE={ "+response+"}; ");
                        rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                        return response;
                    }
                    Document docMdp = XMLParser.ReadXML(resMdpCardInfo);
                    String innerXMLmdp = docMdp.getElementsByTagName("response").item(0).getTextContent();
                    docMdp = XMLParser.ReadXML(innerXMLmdp);
                    String mobileNumber = docMdp.getElementsByTagName("mobileNumber").item(0).getTextContent();


                    mobileNumber = AddCountryCode(mobileNumber);

                    if (data.payeeMobileNumber!=null) {
                        if (!data.payeeMobileNumber.equals(mobileNumber)) {
                            ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13002", "Transaction rejected from beneficiary bank due to invalid beneficiary information");
                            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13002}; RESPONSE={" + response + " }; ");
                            rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));

                            return response;
                        }
                    }
                }
                else if (data.debitAccountType.equalsIgnoreCase("CARD")&&cards.stream().anyMatch(x -> x.getIssuer().equals("MDP") && x.getStartsWith().equals(data.debitAccountId.substring(0, 6)))){
                    String mdpCardInfo2="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:mdp=\"http://ws.wso2.org/dataservice/mdpCardInfo\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <mdp:_getmdpcardinfo>\n" +
                            "         <mdp:cardNumber>"+data.debitAccountId+"</mdp:cardNumber>\n" +
                            "      </mdp:_getmdpcardinfo>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";
                    String resMdpCardInfo2 = Request.SendRequest(env.getProperty("MDPBaseURL"), "_getmdpcardinfo", mdpCardInfo2);
                    Logging.host("Host response -------------" + resMdpCardInfo2);
                    if (resMdpCardInfo2.contains("Unable to find card")){
                        ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"12011","Transaction cannot be processed by your bank, try again later");
                        return response;
                    }





                    Document docMdp2 = XMLParser.ReadXML(resMdpCardInfo2);
                    String innerXMLmdpCard = docMdp2.getElementsByTagName("response").item(0).getTextContent();
                    docMdp2 = XMLParser.ReadXML(innerXMLmdpCard);
                    String state = docMdp2.getElementsByTagName("state").item(0).getTextContent();
                    String expDate = docMdp2.getElementsByTagName("expirDate").item(0).getTextContent();
                    String statusCode = docMdp2.getElementsByTagName("statusCode").item(0).getTextContent();

                    if (!statusCode.equals("0")){
                        ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12005", "Restricted transaction, please contact your bank");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12005}; RESPONSE={" + response + " }; ");
                        rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                        return response;
                    }

                    if (state.equals("Closed")&&data.requestType.equalsIgnoreCase("DEBIT")){
                        ErrorsResDto errResp = new ErrorsResDto(data.requestId, data.timestamp, "12013", "Restricted account, please contact your bank");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12013}; RESPONSE={" + errResp + " }; ");
                        rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), errResp.toString(), null));

                        return errResp;
                    }
                    balance=docMdp2.getElementsByTagName("avalBalance").item(0).getTextContent();
                    if (Float.parseFloat(balance) < Float.parseFloat(data.amount.get("orgValue"))&&data.requestType.equalsIgnoreCase("DEBIT")) {

                        ErrorsResDto errResp = new ErrorsResDto(data.requestId, data.timestamp, "12001", "Insufficient balance to execute the transaction");

                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12001}; RESPONSE={" + errResp.toString() + " }; ");
                        rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), errResp.toString(),null));


                        return errResp;
                    }


                }
                else if (data.creditAccountType.equalsIgnoreCase("CARD")&&data.requestType.equalsIgnoreCase("CREDIT")&&!cards.stream().anyMatch(x -> x.getIssuer().equals("NI") && x.getStartsWith().equals(data.creditAccountId.substring(0, 6)))){
                    ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13001", "The beneficiary account number is invalid");
                    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={13001}; RESPONSE={ "+response+"}; ");
                    rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                    return response;
                }


                if (data.creditAccountType.equalsIgnoreCase("CARD")&&data.requestType.equalsIgnoreCase("CREDIT")&&cards.stream().anyMatch(x -> x.getIssuer().equals("MDP") && x.getStartsWith().equals(data.creditAccountId.substring(0, 6)))){
                    String mdpCardInfo2="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:mdp=\"http://ws.wso2.org/dataservice/mdpCardInfo\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <mdp:_getmdpcardinfo>\n" +
                            "         <mdp:cardNumber>"+data.creditAccountId+"</mdp:cardNumber>\n" +
                            "      </mdp:_getmdpcardinfo>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";
                    String resMdpCardInfo2 = Request.SendRequest(env.getProperty("MDPBaseURL"), "_getmdpcardinfo", mdpCardInfo2);
                    Logging.host("Host response -------------" + resMdpCardInfo2);
                    if (resMdpCardInfo2.contains("Unable to find card")){
                        ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"12013","Restricted account, please contact your bank");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12013}; RESPONSE={" + response + " }; ");
                        rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));

                        return response;
                    }

                    Document docMdp2 = XMLParser.ReadXML(resMdpCardInfo2);
                    String innerXMLmdpCard = docMdp2.getElementsByTagName("response").item(0).getTextContent();
                    docMdp2 = XMLParser.ReadXML(innerXMLmdpCard);
                    //String backOfficeStatus = docMdp.getElementsByTagName("backOfficeStatus").item(0).getTextContent();
                    String expDate = docMdp2.getElementsByTagName("expirDate").item(0).getTextContent();
                    balance=docMdp2.getElementsByTagName("avalBalance").item(0).getTextContent();
                    String statusCode=docMdp2.getElementsByTagName("statusCode").item(0).getTextContent();
                    if (statusCode.equals("17")){
                        ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13003}; RESPONSE={" + response + " }; ");
                        rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
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
                    Logging.host("Host response -------------" + res_TEST);
                    Document doc2 = XMLParser.ReadXML(res_TEST);
                    if (res_TEST.contains("No records were found")&& data.requestType.equalsIgnoreCase("CREDIT")){
                        ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13001", "The beneficiary account number is invalid");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={13001}; RESPONSE={ "+response+"}; ");
                        rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                        return response;
                    }

                    balance = doc2.getElementsByTagName("BAL_AVAIL").item(0).getTextContent();
                    String currency = doc2.getElementsByTagName("CCYDESC").item(0).getTextContent();
                    String status = doc2.getElementsByTagName("STATUS").item(0).getTextContent();
                    String phoneNum = doc2.getElementsByTagName("PHONENUMBER").item(0).getTextContent();
                    phoneNum=AddCountryCode(phoneNum);
                    if (data.payeeMobileNumber!=null){
                        if (!phoneNum.equals(data.payeeMobileNumber)){
                            ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13002", "Transaction rejected from beneficiary bank due to invalid beneficiary information");
                            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13002}; RESPONSE={" + response + " }; ");
                            rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                            return response;
                        }
                    }
                    if (!currency.equals("EGP")){
                        ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13003}; RESPONSE={" + response + " }; ");
                        rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                        return response;
                    }
                    if (status.equals("D")){
                        ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13003}; RESPONSE={" + response + " }; ");
                        rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
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
                        Logging.host("Host response -------------" + res_TEST);
                        Document doc2 = XMLParser.ReadXML(res_TEST);
                        if (res_TEST.contains("No records were found")&& data.requestType.equalsIgnoreCase("CREDIT")){
                            ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13001", "The beneficiary account number is invalid");
                            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={13001}; RESPONSE={ "+response+"}; ");
                            rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
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
                                    ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13002", "Transaction rejected from beneficiary bank due to invalid beneficiary information");
                                    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13002}; RESPONSE={" + response + " }; ");
                                    rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                                    return response;
                                }
                            }
                            if (!currency.equals("EGP")){
                                ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13003}; RESPONSE={" + response + " }; ");
                                rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));

                                return response;
                            }
                            if (status.equals("D")){
                                ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13003}; RESPONSE={" + response + " }; ");
                                rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
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
                        Logging.host("Host response -------------" + res);
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
                        Logging.host("Host response -------------" + res);
                        Document doc = XMLParser.ReadXML(res);
                        String innerXml = doc.getElementsByTagName("response").item(0).getTextContent();
                        doc = XMLParser.ReadXML(innerXml);
                        String mNumber=doc.getElementsByTagName("mobileNumber").item(0).getTextContent();
                        mNumber=AddCountryCode(mNumber);
                        if (!mNumber.equals(data.payerMobileNumber)){
                            ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12004", "Your mobile number is not matching with bank records, please contact your bank");
                            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12004}; RESPONSE={" + response + " }; ");
                            rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));

                            return response;
                        }
                        customerNumber = doc.getElementsByTagName("customerNumber").item(0).getTextContent();
                        System.out.println("Customer Number 10/10: " + customerNumber);
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
                    Logging.host("Host response -------------" + resMdpCardInfo2);
                    if (resMdpCardInfo2.contains("Unable to find card")){
                        ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"12013","Restricted account, please contact your bank");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12013}; RESPONSE={" + response + " }; ");
                        rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));

                        return response;
                    }

                    Document docMdp2 = XMLParser.ReadXML(resMdpCardInfo2);
                    String innerXMLmdpCard = docMdp2.getElementsByTagName("response").item(0).getTextContent();
                    docMdp2 = XMLParser.ReadXML(innerXMLmdpCard);
                    //String backOfficeStatus = docMdp.getElementsByTagName("backOfficeStatus").item(0).getTextContent();
                    String expDate = docMdp2.getElementsByTagName("expirDate").item(0).getTextContent();
                    balance=docMdp2.getElementsByTagName("avalBalance").item(0).getTextContent();
                    String statusCode=docMdp2.getElementsByTagName("statusCode").item(0).getTextContent();
                    if (statusCode.equals("17")){
                        ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13003}; RESPONSE={" + response + " }; ");
                        rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
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
                    Logging.host("Host response -------------" + resMdpCardInfo2);
                    if (resMdpCardInfo2.contains("Unable to find card")){
                        ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"12013","Restricted account, please contact your bank");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12013}; RESPONSE={" + response + " }; ");
                        rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));

                        return response;
                    }

                    Document docMdp2 = XMLParser.ReadXML(resMdpCardInfo2);
                    String innerXMLmdpCard = docMdp2.getElementsByTagName("response").item(0).getTextContent();
                    docMdp2 = XMLParser.ReadXML(innerXMLmdpCard);
                    //String backOfficeStatus = docMdp.getElementsByTagName("backOfficeStatus").item(0).getTextContent();
                    String expDate = docMdp2.getElementsByTagName("expirDate").item(0).getTextContent();
                    balance=docMdp2.getElementsByTagName("avalBalance").item(0).getTextContent();
                    String statusCode=docMdp2.getElementsByTagName("statusCode").item(0).getTextContent();
                    if (statusCode.equals("17")){
                        ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13003}; RESPONSE={" + response + " }; ");
                        rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
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
                        Logging.host("Host response -------------" + res);
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
                        Logging.host("Host response -------------" + res);
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
                        ?  data.creditPoolAccount:
                        data.requestType.equalsIgnoreCase("CREDIT") && data.creditAccountType.equalsIgnoreCase("ACCOUNT")
                                ? data.creditAccountId.substring(data.creditAccountId.length() - 16)
                                :data.requestType.equalsIgnoreCase("DEBIT") && data.creditAccountType.equalsIgnoreCase("ACCOUNT")
                                ? data.creditPoolAccount
                                :data.requestType.equalsIgnoreCase("DEBIT") && data.creditAccountType.equalsIgnoreCase("WALLET")
                                ? data.creditPoolAccount:
                                data.requestType.equalsIgnoreCase("CREDIT") && data.creditAccountType.equalsIgnoreCase("CARD") && cards.stream().anyMatch(x -> x.getIssuer().equals("MDP") )
                                        ? env.getProperty("t24.poolaccount.mdpCreditPool")
                                        : data.requestType.equalsIgnoreCase("CREDIT") && data.creditAccountType.equals("CARD") && cards.stream().anyMatch(x -> x.getIssuer().equals("NI"))
                                        ? env.getProperty("t24.poolaccount.niCreditPool")
                                        : data.requestType.equalsIgnoreCase("CREDIT") && data.creditAccountType.equalsIgnoreCase("CARD") && cards.stream().anyMatch(x -> x.getIssuer().equals("NI") &&data.creditAccountId.substring(0,2).equalsIgnoreCase("45"))
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
                        ? data.creditPoolAccount
                        :data.requestType.equalsIgnoreCase("DEBIT")
                        ? data.creditPoolAccount
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

            String transactionrefrence = null;

            if(
                    (data.requestType.equalsIgnoreCase("CREDIT")&&data.creditAccountType.equalsIgnoreCase("CARD")&& !data.creditAccountId.startsWith("45"))
                            ||
                            (data.requestType.equalsIgnoreCase("DEBIT")&&data.debitAccountType.equalsIgnoreCase("CARD")&& !data.debitAccountId.startsWith("45"))
            ) {
                transactionrefrence = Card_Transaction(data, deductedAmount);

                if (transactionrefrence.equals("Transaction_Refused")) {
                    if (data.requestType.equalsIgnoreCase("CREDIT")) {
                        ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13007", "Transaction cannot be processed by beneficiary bank, try again later");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13007}; RESPONSE={" + response + " }; ");
                        rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));

                        return response;
                    } else {

                        ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12005", "Restricted transaction, please contact your bank");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12005}; RESPONSE={" + response + " }; ");
                        rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), response.toString(), null));
                        return response;

                    }
                }
            }

            String SoapAction="IPNAccountPayment";
            if(data.requestType.equalsIgnoreCase("REVERSAL")){

                SoapAction="REVERSALFTCONTRACT";
            }
            else if(data.requestType.equalsIgnoreCase("CREDIT")){

                if(data.creditAccountType.equalsIgnoreCase("CARD")&&data.creditAccountId.startsWith("4103")){
                    SoapAction="IPNCardPayment";
                }
                else if (data.creditAccountType.equalsIgnoreCase("CARD")&&data.creditAccountId.startsWith("5078")){
                    SoapAction="IPNMeezaPayment";
                }
                else if(data.creditAccountType.equalsIgnoreCase("WALLET")){
                    SoapAction="IPNWalletPayment";
                }

            }
            else if (data.requestType.equalsIgnoreCase("DEBIT")){


                if(data.debitAccountType.equalsIgnoreCase("CARD")&&data.debitAccountId.startsWith("4103")){
                    SoapAction="IPNCardPayment";
                }
                else if (data.debitAccountType.equalsIgnoreCase("CARD")&&data.debitAccountId.startsWith("5078")){
                    SoapAction="IPNMeezaPayment";
                }
                else if(data.debitAccountType.equalsIgnoreCase("WALLET")){
                    SoapAction="IPNWalletPayment";
                }


            }


            String body = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:dat=\"http://ws.wso2.org/dataservice\">" +
                    "   <soap:Header/>" +
                    "   <soap:Body>" +
                    "      <dat:"+SoapAction+">" +
                    "         <dat:DBACCOUNT>" + t24DebitAcc + "</dat:DBACCOUNT>" +
                    "         <dat:DRCCY>EGP</dat:DRCCY>" +
                    "         <dat:CRAMT>" + deductedAmount + "</dat:CRAMT>" +
                    "         <dat:CRACCOUNT>" + t24CreditAcc + "</dat:CRACCOUNT>" +
                    "         <dat:CRCCY>" + data.amount.get("orgCurr") + "</dat:CRCCY>" +
                    "         <dat:XREF>"+CIF+"</dat:XREF>" +
                    "         <dat:DRVDT></dat:DRVDT>" +
                    "         <dat:CRVDT></dat:CRVDT>" +
                    "         <dat:INTRMKS>"+data.transactionId+"</dat:INTRMKS>" +
                    "         <dat:OrderingBank>ADCB</dat:OrderingBank>" +
                    "      </dat:"+SoapAction+">" +
                    "   </soap:Body>" +
                    "</soap:Envelope>";
//            dateFormat.format(Timestamp.valueOf(now.plusDays(-5)))
//            dateFormat.format(Timestamp.valueOf(now.plusDays(-5)))
            String response = Request.SendRequest(env.getProperty("IPNBaseURL"), SoapAction, body);
            Logging.host("Host response -------------" + response);
            Document doc = XMLParser.ReadXML(response);
            LocalDateTime now2 = LocalDateTime.now();
            String currentTimestamp2 = String.valueOf(Timestamp.valueOf(now2));
            String ref = doc.getElementsByTagName("HOSTREF").item(0).getTextContent();

            //fraud call

            Fraud_FT(data.transactionId,data.requestId,data.timestamp,CIF,debitAccount,balance,data.payerMobileNumber,data.amount.get("orgValue"),data.payeeMobileNumber,data.creditAccountId,data.payerName);

            String msg=null;
            String phoneNum=null;
            if(data.requestType.equalsIgnoreCase("CREDIT")){

                msg =  deductedAmount + " has been credited to your ADCB account No ****" + data.creditAccountId.substring(data.creditAccountId.length() - 4, data.creditAccountId.length()) + " on " + currentTimestamp2.substring(0, 10) + " from " + data.payeeName + ", ref." + data.transactionId.substring(data.transactionId.length() - 8, data.transactionId.length()) + "" ;
                phoneNum=data.payeeMobileNumber;

            }
            else{

                msg =deductedAmount + " has been debited from your ADCB account No ****" + data.debitAccountId.substring(data.debitAccountId.length() - 4, data.debitAccountId.length()) + " on " + currentTimestamp2.substring(0, 10) + " to , ref. " + data.transactionId.substring(data.transactionId.length() - 8, data.transactionId.length()) + "" ;
                phoneNum=data.payerMobileNumber;
            }

            SendSMS_FUNC(phoneNum,msg);

            // Request to get customer details
            // TODO REMOVE BELOW TWO LINES WHILE DEPLOYING ON PRODUCTION
            if (data.requestType.equalsIgnoreCase("DEBIT")){customerNumber="1127363";}
            else if (data.requestType.equalsIgnoreCase("CREDIT")){customerNumber="1007817";}

            Login customer = loginsRepository.getLoginByCustomerReference(customerNumber);
            String docType = customer.getDocumentType().equals("NationalId") ? "NID" : customer.getDocumentType();
            //TODO Balance Check

            if(data.requestType.equalsIgnoreCase("CREDIT")||data.requestType.equalsIgnoreCase("REVERSAL")) {

                balance = Get_Account_Balance(data.creditAccountId);
            }

            else{

                balance = Get_Account_Balance(data.debitAccountId);

            }









            DecimalFormat  formatter = new DecimalFormat("#0.00");
            balance=formatter.format(Double.valueOf(balance));
            FundTransferResDto res = new FundTransferResDto(data.requestId, currentTimestamp2, "00000", "SUCCESS", balance, "EGP", ref, customer.getName(), customer.getAddress(), docType, customer.getLegalId());
            acceptedLogsRepository.save(new AcceptedLog(data.transactionId, now, data.toString(), res.toString(),transactionrefrence));
            Logging.info("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={200}; RESPONSE={"+res+"}; ");
            return res;
        } catch (Exception ex) {
            String stackTrace = ExceptionStackTrace.GetStackTrace(ex);
            rejectedLogsRepository.save(new RejectedLog(data.transactionId, LocalDateTime.now(), data.toString(), stackTrace,null));
            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={400}; RESPONSE={"+stackTrace+"}; ");
            ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"12015","Invalid technical data");
            return response;
            //ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"11013","Invalid technical data");
            //Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={400}; RESPONSE={ Service is currently not available from your bank, try again later}; ");
            //return response;
        }
    }

    @PostMapping
    @RequestMapping("accounthistory")
    public Object balanceEnqResDto(@RequestBody BalanceEnqReqDto data) {
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
            Logging.host("Host response -------------" + responseBody);
            Document doc = XMLParser.ReadXML(responseBody);
            Map<String, String> customerDetails = new HashMap<>();
            String nationalId = doc.getElementsByTagName("LEGALID").item(0).getTextContent();
            String mobileNumber = doc.getElementsByTagName("TELNO").item(0).getTextContent();
        }

        catch (Exception ex){
            ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"12010","Service is currently not available from your bank, try again later");
            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={400}; RESPONSE={ Service is currently not available from your bank, try again later}; ");
            Logging.warn(ExceptionStackTrace.GetStackTrace(ex));

            return response;
        }
        String err="";
        try {

            List<Card> cards = Streamable.of(cardsRepository.findAll()).toList();
            String nullCheck = checkNull(data, "accounthistory");
            if (nullCheck != "") {
                ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12015", "Invalid technical data");
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                return response;
            }
            if (data.settlementCycleId!=null){
                if(data.settlementCycleId.length() < 9 || data.settlementCycleId.length() > 11){
                    ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12015", "Invalid technical data");
                    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                    return response;
                }
            }
            String accountidSub = data.accountId.substring(2);
            if (data.accountId.length() > 36 || !data.lastTransactions.matches("[0-9]+")
                    || data.accountId.length() < 3 || data.rrn.length() != 12 || data.mobileNumber.length() != 14
                    || !data.mobileNumber.matches("[0-9]+") || !accountidSub.matches("[0-9]+")||data.lastTransactions.equals("100")) {
                ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12015", "Invalid technical data");
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                return response;
            }
            String mobileNumberInBank=loginsRepository.checkMobileNumber(data.mobileNumber);
            if (mobileNumberInBank==null||mobileNumberInBank.equals("")){
                ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12004", "Your mobile number is not matching with bank records, please contact your bank");
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12004}; RESPONSE={" + response + " }; ");
                rejectedLogsRepository.save(new RejectedLog(data.requestId, LocalDateTime.now(), data.toString(), response.toString(), null));

                return response;
            }
            String accountStatus = "";
            if (data.accountType.equals("ACCOUNT")) {

                String test_body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                        "   <soapenv:Header/>\n" +
                        "   <soapenv:Body>\n" +
                        "      <dat:ACCOUNTDETAILS>\n" +
                        "         <dat:ACCNO>" + data.accountId.substring(data.accountId.length() - 16) + "</dat:ACCNO>\n" +
                        "      </dat:ACCOUNTDETAILS>\n" +
                        "   </soapenv:Body>\n" +
                        "</soapenv:Envelope>";
                String res_TEST = Request.SendRequest(env.getProperty("IPNBaseURL"), "ACCOUNTDETAILS", test_body);
                Logging.host("Host response -------------" + res_TEST);
                Document docStatus = XMLParser.ReadXML(res_TEST);
                if (!res_TEST.contains("No records were found")) {
                    String phoneNum = docStatus.getElementsByTagName("PHONENUMBER").item(0).getTextContent();
                    accountStatus = docStatus.getElementsByTagName("STATUS").item(0).getTextContent();
                    System.out.println(res_TEST.indexOf("asd" + phoneNum));
                    phoneNum = AddCountryCode(phoneNum) ;
                    LocalDateTime now=LocalDateTime.now();
                    if (!data.mobileNumber.equals(phoneNum)) {

                        ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12015", "Invalid technical data");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={400}; RESPONSE={" + nullCheck + " is not found}; ");
                        return response;

                    }
                    if (accountStatus.equals("D")) {

                        ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12005", "Restricted transaction, please contact your bank");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12005}; RESPONSE={" + response + " }; ");
                        rejectedLogsRepository.save(new RejectedLog(data.requestId, LocalDateTime.now(), data.toString(), response.toString(), null));

                        return response;
                    }
                    if (data.lastTransactions.equals("0")){
                        String balance = docStatus.getElementsByTagName("BAL_AVAIL").item(0).getTextContent();
                        BalanceEnqResDto resEnq = new BalanceEnqResDto(data.requestId, data.timestamp, "00000", "SUCCESS", balance, "EGP");

                        acceptedLogsRepository.save(new AcceptedLog(data.requestId, now, data.toString(), resEnq.toString(),null));
                        Logging.info("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/accountshistory}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={200}; RESPONSE={" + resEnq + "}; ");


                        return resEnq;
                    }
                }
                else{

                    ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12013", "Restricted account, please contact your bank");
                    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12013}; RESPONSE={ "+response+"}; ");
                    rejectedLogsRepository.save(new RejectedLog(data.requestId, LocalDateTime.now(), data.toString(), response.toString(), null));

                    return response;

                }
            }

            if (cards.stream().anyMatch(x -> x.getIssuer().equals("NI") && x.getStartsWith().equals(data.accountId.substring(0, 6)))){
                err="invalid accountId";
                String body="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                        "   <soapenv:Header/>\n" +
                        "   <soapenv:Body>\n" +
                        "      <dat:GetCardInfoRq>\n" +
                        "         <dat:Request1>\n" +
                        "<![CDATA[\n" +
                        "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:fimi=\"http://schemas.compassplus.com/two/1.0/fimi.xsd\" xmlns:fimi1=\"http://schemas.compassplus.com/two/1.0/fimi_types.xsd\">\n" +
                        "                <soap:Header/>\n" +
                        "                <soap:Body>\n" +
                        "                                <fimi:GetCardInfoRq>\n" +
                        "                                                <fimi:Request   __HEADERPARAM__  >\n" +
                        "                                                                <fimi1:PAN>"+data.accountId+"</fimi1:PAN>\n" +
                        "                                                </fimi:Request>\n" +
                        "                                </fimi:GetCardInfoRq>\n" +
                        "                </soap:Body>\n" +
                        "</soap:Envelope>\n" +
                        "]]>\n" +
                        "\n" +
                        "</dat:Request1>\n" +
                        "      </dat:GetCardInfoRq>\n" +
                        "   </soapenv:Body>\n" +
                        "</soapenv:Envelope>\n";
                String res = Request.SendRequest(env.getProperty("NIBaseURL"), "GetCardInfoRq", body);
                Logging.host("Host response -------------" + res);
                Document doc = XMLParser.ReadXML(res);
                String innerXml = doc.getElementsByTagName("FEMI_RESPONSE").item(0).getTextContent();
                innerXml = innerXml.replaceAll(":", "_");
                doc = XMLParser.ReadXML(innerXml);
                String balance = doc.getElementsByTagName("m0_AvailBalance").item(0).getTextContent();
                body="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                        "   <soapenv:Header/>\n" +
                        "   <soapenv:Body>\n" +
                        "      <dat:GetCardStatementRq>\n" +
                        "         <dat:Request1>\n" +
                        "\n" +
                        "<![CDATA[\n" +
                        "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:fimi1=\"http://schemas.compassplus.com/two/1.0/fimi_types.xsd\" xmlns:fimi=\"http://schemas.compassplus.com/two/1.0/fimi.xsd\">\n" +
                        "                <soap:Header/>\n" +
                        "                <soap:Body>\n" +
                        "                                <fimi:GetCardStatementRq>\n" +
                        "                                                <fimi:Request __HEADERPARAM__ >\n" +
                        "                                                                                               <fimi1:Count>100</fimi1:Count>\n" +
                        "                \n" +
                        "                                                                <fimi1:PAN>"+data.accountId+"</fimi1:PAN>\n" +
                        "                                                                <fimi1:FromTime>2022-01-01T00:00:00</fimi1:FromTime>\n" +
                        "                                                                <fimi1:ToTime>2025-04-30T23:59:59</fimi1:ToTime>\n" +
                        "                                                </fimi:Request>\n" +
                        "                                </fimi:GetCardStatementRq>\n" +
                        "                </soap:Body>\n" +
                        "                </soap:Envelope>\n" +
                        "]]>\n" +
                        "\n" +
                        "</dat:Request1>\n" +
                        "      </dat:GetCardStatementRq>\n" +
                        "   </soapenv:Body>\n" +
                        "</soapenv:Envelope>\n";
                res = Request.SendRequest(env.getProperty("NIBaseURL"), "GetCardStatementRq", body);
                Logging.host("Host response -------------" + res);
                doc = XMLParser.ReadXML(res);
                innerXml = doc.getElementsByTagName("FEMI_RESPONSE").item(0).getTextContent();
                innerXml = innerXml.replaceAll(":", "_");
                doc = XMLParser.ReadXML(innerXml);
                LocalDateTime now = LocalDateTime.now();
                String currentTimestamp = String.valueOf(Timestamp.valueOf(now));
                NodeList nodes = doc.getElementsByTagName("m0_Row");
                List<Map<String, String>> historyList = new ArrayList<>();

                if (nodes.getLength()>99){
                    ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"12015","Invalid technical data");
                    Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={400}; RESPONSE={"+nullCheck+" is not found}; ");
                    return response;
                }
                int getTransActions=0;

             /*   if(Integer.parseInt(data.lastTransactions)>98){getTransActions=10;}
                else {getTransActions=Integer.parseInt(data.lastTransactions);}
                */
                getTransActions=Integer.parseInt(data.lastTransactions);
                if(nodes.getLength()<getTransActions)
                    getTransActions=nodes.getLength();
                String action="D";
                for (int i = 0; i < getTransActions; ++i) {
                    Node node = nodes.item(i);
                    NodeList childNodes = node.getChildNodes();
                    Map<String, String> history = new HashMap<>();
                    for (int j = 0; j < childNodes.getLength(); ++j) {

                        Node child = childNodes.item(j);
                        String nodeName = child.getNodeName();
                        String nodeValue = child.getTextContent();
                       if(nodeName.equals("m0_Amount")) {
                           history.put("amount", nodeValue);

                           if(nodeValue.contains("-")){

                               history.put("action", "D");
                               action="D";
                           }
                           else
                               history.put("action", "C");
                                action="C";
                       }
                       if(nodeName.equals("m0_TranTime")) {
                           history.put("date", LocalDate.parse(nodeValue.substring(0, 10), DateTimeFormatter.ofPattern("yyyy-MM-dd")).format(DateTimeFormatter.ofPattern("ddMMyy")));
                       }
                           if(nodeName.equals("m0_TermLocation")){

                               if(nodeValue.contains(env.getProperty("NI_Transaction_Description"))) {
                                   if(action.equalsIgnoreCase("C"))
                                   history.put("desc", "Instant Transfer Fro");
                                   if(action.equalsIgnoreCase("D"))
                                       history.put("desc", "Instant Transfer To");

                               }
                               else
                                   if(nodeValue.length()>20)
                                       history.put("desc", nodeValue.substring(0,20));
                                    else
                                        history.put("desc", nodeValue);
                           }



                    }
                    historyList.add(history);
                }
                BalanceEnqResDto resEnq = new BalanceEnqResDto(data.requestId, currentTimestamp, "00000", "SUCCESS", balance, "EGP",historyList);

                acceptedLogsRepository.save(new AcceptedLog(data.requestId, now, data.toString(), resEnq.toString(),null));
                Logging.info("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/accountshistory}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={200}; RESPONSE={" + resEnq + "}; ");


                return resEnq;

            }
            else {
                //Validate MobileNumberif
                if (data.accountId.charAt(0)=='5'&&data.accountType.equals("CARD")) {
                    String mdpCardInfo2="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:mdp=\"http://ws.wso2.org/dataservice/mdpCardInfo\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <mdp:_getmdpcardinfo>\n" +
                            "         <mdp:cardNumber>"+data.accountId+"</mdp:cardNumber>\n" +
                            "      </mdp:_getmdpcardinfo>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";
                    String resMdpCardInfo2 = Request.SendRequest(env.getProperty("MDPBaseURL"), "_getmdpcardinfo", mdpCardInfo2);
                    Logging.host("Host response -------------" + resMdpCardInfo2);
                    if (resMdpCardInfo2.contains("Unable to find card")||resMdpCardInfo2.contains("Card is not activated")){
                        ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"12013","Restricted account, please contact your bank");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12013}; RESPONSE={ "+response+"}; ");

                        rejectedLogsRepository.save(new RejectedLog(data.requestId, LocalDateTime.now(), data.toString(), response.toString(), null));

                        return response;
                    }

                    Document docMdp2 = XMLParser.ReadXML(resMdpCardInfo2);
                    String innerXMLmdpCard = docMdp2.getElementsByTagName("response").item(0).getTextContent();
                    docMdp2 = XMLParser.ReadXML(innerXMLmdpCard);
                    String state = docMdp2.getElementsByTagName("state").item(0).getTextContent();
                    String expDate = docMdp2.getElementsByTagName("expirDate").item(0).getTextContent();
                    String balance=docMdp2.getElementsByTagName("avalBalance").item(0).getTextContent();
//                        String statusCode=docMdp.getElementsByTagName("statusCode").item(0).getTextContent();
//                        if (!backOfficeStatus.equals("Valid card")){
//                            ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"11003","Inactive card, please contact your bank");
//                            return response;
//                        }
//                        if (statusCode.equals("20")||statusCode.equals("12")){
//                            ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"11003","Inactive card, please contact your bank");
//                            return response;
//                        }
                    if (state.equals("Closed")){
                        ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"12005","Restricted transaction, please contact your bank");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12005}; RESPONSE={" + response + " }; ");
                        rejectedLogsRepository.save(new RejectedLog(data.requestId, LocalDateTime.now(), data.toString(), response.toString(), null));

                        return response;
                    }
                    expDate=expDate.replace(".","-");
                    String [] Expiry=expDate.split("-");
                    LocalDate today = LocalDate.now();
                    int month = today.getMonthValue();
                    int year  = today.getYear();
                    if(year>Integer.parseInt(Expiry[1])){
                        ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"12005","Restricted transaction, please contact your bank");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12005}; RESPONSE={" + response + " }; ");
                        rejectedLogsRepository.save(new RejectedLog(data.requestId, LocalDateTime.now(), data.toString(), response.toString(), null));

                        return response;
                    }
                    else if (year==Integer.parseInt(Expiry[1])){
                        if (month>Integer.parseInt(Expiry[0])||month==Integer.parseInt(Expiry[0])){
                            ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"12005","Restricted transaction, please contact your bank");
                            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12005}; RESPONSE={" + response + " }; ");
                            rejectedLogsRepository.save(new RejectedLog(data.requestId, LocalDateTime.now(), data.toString(), response.toString(), null));

                            return response;
                        }
                    }

                    String requestCustomerInfo = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:cus=\"http://ws.wso2.org/dataservice/customerInfoFromCardNumber\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <cus:_getcustomerinfofromcardnumber>\n" +
                            "         <cus:cardNumber>" + data.accountId + "</cus:cardNumber>\n" +
                            "      </cus:_getcustomerinfofromcardnumber>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";
                    String resMdpCardInfo = Request.SendRequest(env.getProperty("MDPBaseURL"), "_getcustomerinfofromcardnumber", requestCustomerInfo);
                    Logging.host("Host response -------------" + resMdpCardInfo);
                    Document docMdp = XMLParser.ReadXML(resMdpCardInfo);
                    String innerXMLmdp = docMdp.getElementsByTagName("response").item(0).getTextContent();
                    docMdp = XMLParser.ReadXML(innerXMLmdp);
                    String mdpMobileNumber = docMdp.getElementsByTagName("mobileNumber").item(0).getTextContent();
                    mdpMobileNumber = AddCountryCode(mdpMobileNumber) ;

                    String cNumber= docMdp.getElementsByTagName("customerNumber").item(0).getTextContent();
                    String cardId=  docMdp.getElementsByTagName("cardId").item(0).getTextContent();

                    LocalDateTime now = LocalDateTime.now();
                    if (!mdpMobileNumber.equals(data.mobileNumber)) {
                        ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "12004", "Your mobile number is not matching with bank records, please contact your bank");
                        Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={12004}; RESPONSE={" + response + " }; ");
                        rejectedLogsRepository.save(new RejectedLog(data.requestId, LocalDateTime.now(), data.toString(), response.toString(), null));


                        return response;
                    }
                    else if (data.lastTransactions.equals("0")){

                        BalanceEnqResDto resEnq = new BalanceEnqResDto(data.requestId, data.timestamp, "00000", "SUCCESS", balance, "EGP");
                        acceptedLogsRepository.save(new AcceptedLog(data.requestId, now, data.toString(), resEnq.toString(),null));
                        Logging.info("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/accountshistory}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={200}; RESPONSE={" + resEnq + "}; ");


                        return resEnq;
                    }
                    String transactionRequest="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:car=\"http://ws.wso2.org/dataservice/cardTransactionsIPN\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <car:_getcardtransactionsipn>\n" +
                            "         <car:customerNumber>"+cNumber+"</car:customerNumber>\n" +
                            "         <car:cardId>"+cardId+"</car:cardId>\n" +
                            "         <car:pageIndex>1</car:pageIndex>\n" +
                            "         <car:pageSize>"+data.lastTransactions+"</car:pageSize>\n" +
                            "      </car:_getcardtransactionsipn>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";
                    String transactionsMDP = Request.SendRequest(env.getProperty("MDPBaseURL"), "_getcardtransactionsipn", transactionRequest);
                    Logging.host("Host response -------------" + transactionsMDP);
                    Document transactionsDOC = XMLParser.ReadXML(transactionsMDP);
                    String innerXML = transactionsDOC.getElementsByTagName("response").item(0).getTextContent();
                    transactionsDOC = XMLParser.ReadXML(innerXML);
                    String totalSize=transactionsDOC.getElementsByTagName("totalSize").item(0).getTextContent();
                     now = LocalDateTime.now();
                    String currentTimestamp = String.valueOf(Timestamp.valueOf(now));
                    List<Map<String, String>> historyList = new ArrayList<>();
                    NodeList nodes = transactionsDOC.getElementsByTagName("transaction");
                    int getTransActions=0;
                    DateFormat inputFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                    DateFormat outputFormat = new SimpleDateFormat("ddMMYY");


                    //if(Integer.parseInt(data.lastTransactions)==99){getTransActions=10;}


                    if (Integer.parseInt(data.lastTransactions)>Integer.parseInt(totalSize)){
                        getTransActions=Integer.parseInt(totalSize);
                    }

                    if(nodes.getLength()<getTransActions)
                        getTransActions=nodes.getLength();

                    else {getTransActions=Integer.parseInt(data.lastTransactions);}
                    for (int i = 0; i < getTransActions; ++i) {
                        Node node = nodes.item(i);
                        NodeList childNodes = node.getChildNodes();
                        Map<String, String> history = new HashMap<>();
                        for (int j = 0; j < childNodes.getLength(); ++j) {

                            Node child = childNodes.item(j);
                            String nodeName = child.getNodeName();
                            String nodeValue = child.getTextContent();
                          //  String action="D";
                          //  String desc="Instant Transfer To";
//                        if(nodeName.equals("m0_Amount"))  history.put("amount", nodeValue);
//                        if(nodeName.equals("m0_TranTime"))  history.put("date", LocalDate.parse(nodeValue.substring(0, 10), DateTimeFormatter.ofPattern("yyyy-MM-dd")).format(DateTimeFormatter.ofPattern("ddMMyy")));
//                        if(nodeName.equals("m0_TermLocation")){history.put("desc", nodeValue.substring(0, Math.min(nodeValue.length(),20)));}

                            if (nodeName.equals("billingAmount")){history.put("amount",nodeValue);}
                          /*  else if (nodeName.equals("transactionType"))
                            {

                                if (nodeValue.equals("MONEY_IN")){
                                    action="C";
                                }
                                else {
                                    action="D";
                                }
                                history.put("action",action);
                                //String creditOrdDebit=nodeValue;
                                if (action.equals("D")){

                                   desc="Instant Transfer To";
                                }
                                else if (action.equals("C")){
                                    desc="Instant Transfer Fro";
                                }

                            }*/

                            else if (nodeName.equals("reverse")){

                                if (nodeValue.equals("true")){

                                    if(nodes.item(i).getTextContent().contains("MONEY_OUT")){
                                        history.put("action","C");
                                    }
                                    else{
                                        history.put("action","D");

                                    }

                                    history.put("desc","Instant Transfer Rev.");

                                }
                                else{
                                    if(nodes.item(i).getTextContent().contains("MONEY_OUT")){
                                        history.put("action","D");
                                        history.put("desc","Instant Transfer To");
                                    }
                                    else{
                                        history.put("action","C");
                                        history.put("desc","Instant Transfer Fro");


                                    }



                                }

                            }
                            else if (nodeName.equals("transactionDate")){

                                String inputdate = nodeValue;
                                Date date = inputFormat.parse(inputdate);
                                String outputdate = outputFormat.format(date);
                                history.put("date", outputdate);
                               // history.put("date",nodeValue);

                            }

                            else {
                                continue;
                            }
                        }
                        historyList.add(history);
                    }
                    BalanceEnqResDto resEnq = new BalanceEnqResDto(data.requestId, currentTimestamp, "00000", "SUCCESS", balance, "EGP",historyList);

                    acceptedLogsRepository.save(new AcceptedLog(data.requestId, now, data.toString(), resEnq.toString(),null));
                    Logging.info("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/accountshistory}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={200}; RESPONSE={" + resEnq + "}; ");
                    return resEnq;
                }
                err="invalid accountId";
                String body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                        "   <soapenv:Header/>\n" +
                        "   <soapenv:Body>\n" +
                        "      <dat:ACCOUNTACTIVITY>\n" +
                        "         <dat:ACCOUNT>" + data.accountId.substring(data.accountId.length()-16) + "</dat:ACCOUNT>\n" +
                        "         <dat:SEARCHTYPE>1</dat:SEARCHTYPE>\n" +
                        "         <dat:FRMDATE></dat:FRMDATE>\n" +
                        "         <dat:TODATE></dat:TODATE>\n" +
                        "         <dat:NOTXN></dat:NOTXN>\n" +
                        "         <dat:NODATE></dat:NODATE>\n" +
                        "      </dat:ACCOUNTACTIVITY>\n" +
                        "   </soapenv:Body>\n" +
                        "</soapenv:Envelope>";
                String res = Request.SendRequest(env.getProperty("IPNBaseURL"), "ACCOUNTACTIVITY", body);
                Logging.host("Host response -------------" + res);
                Document doc = XMLParser.ReadXML(res);
                DecimalFormat  formatter = new DecimalFormat("#0.00");

                String balance = doc.getElementsByTagName("OPENBAL").item(0).getTextContent();
                LocalDateTime now = LocalDateTime.now();
                String currentTimestamp = String.valueOf(Timestamp.valueOf(now));
                NodeList nodes = doc.getElementsByTagName("TxnDetail");
                List<Map<String, String>> historyList = new ArrayList<>();
                String creditOrdDebit="";
                Integer lTransactions=Integer.parseInt(data.lastTransactions);
                //if(lTransactions==99){lTransactions=10;}

                if(nodes.getLength()<lTransactions)
                    lTransactions=nodes.getLength();

                for (int i = 0; i <lTransactions; ++i) {
                    Node node = nodes.item(i);

                    NodeList childNodes = node.getChildNodes();
                    Map<String, String> history = new HashMap<>();

                    SimpleDateFormat dateformat = new SimpleDateFormat("dd-MM-yy");

                    DateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy");
                    DateFormat outputFormat = new SimpleDateFormat("ddMMYY");

                    if (!node.getTextContent().equals("")){

                        for (int j = 0; j < childNodes.getLength(); ++j) {
                            Node child = childNodes.item(j);
                            String nodeName = child.getNodeName();
                            String nodeValue = child.getTextContent();

                            if (nodeName.equals("TXNAMT")) {

                                String Txnbalance = nodeValue;
                                Txnbalance = formatter.format(Double.valueOf(Txnbalance));

                                history.put("amount", Txnbalance.replaceAll("-",""));

                            } else if (nodeName.equalsIgnoreCase("CREDITORDEBIT")) {
                                history.put("action", nodeValue);
                               // creditOrdDebit = nodeValue;


                               /* if (creditOrdDebit.equals("D")) {
                                    history.put("desc", "Instant Transfer To");
                                } else if (creditOrdDebit.equals("C")) {
                                    history.put("desc", "Instant Transfer Fro");
                                }*/

                            } else if (nodeName.equals("POSTDATE")) {


                                String inputdate = nodeValue;
                                Date date = inputFormat.parse(inputdate);
                                String outputdate = outputFormat.format(date);
                                history.put("date", outputdate);


                            }

                            else if (nodeName.equals("TXNCODE")){


                                if(nodeValue.length()>20) {
                                    history.put("desc", nodeValue.substring(0, 20));
                                }
                                else{
                                    history.put("desc", nodeValue);

                                }
                            }
                            else {
                                continue;
                            }

                            //history.put(nodeName, nodeValue);
                        }

                        historyList.add(history);
                }
                    else{
                        lTransactions+=1;

                    }


                }

                balance=formatter.format(Double.valueOf(balance));

                 BalanceEnqResDto resEnq = new BalanceEnqResDto(data.requestId, currentTimestamp, "00000", "SUCCESS", balance, "EGP",historyList);
                AcceptedLog hi = new AcceptedLog(data.requestId, now, data.toString(), resEnq.toString(),null);
                acceptedLogsRepository.save(hi);
                Logging.info("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/accounts}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={200}; RESPONSE={" + resEnq + "}; ");

                return resEnq;
            }
            //    NodeList nodes = doc.getElementsByTagName("ACCOUNTROW");
//    List<Account> accounts = new ArrayList<>();
        } catch (Exception ex) {

            String stackTrace = ExceptionStackTrace.GetStackTrace(ex);
            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/accounthistory}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={400}; RESPONSE={"+stackTrace+"}; ");
            rejectedLogsRepository.save(new RejectedLog(data.requestId, LocalDateTime.now(), data.toString(), stackTrace,null));
            ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"12015","Invalid technical data");
            return response;
//            String ErrorDesc=errorsRepository.getErrorDes(err);
//            String ErrorCode=errorsRepository.getErrorCode(err);
//            ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,ErrorCode,ErrorDesc);
//            return response;

        }

    }

    @PostMapping
    @RequestMapping("checkstatus")
    public Object checkStatus(@RequestBody CheckStatusReqDto data) {


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
            Logging.host("Host response -------------" + responseBody);
            Document doc = XMLParser.ReadXML(responseBody);
            Map<String, String> customerDetails = new HashMap<>();
            String nationalId = doc.getElementsByTagName("LEGALID").item(0).getTextContent();
            String mobileNumber = doc.getElementsByTagName("TELNO").item(0).getTextContent();
        }

        catch (Exception ex){
            ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"12010","Service is currently not available from your bank, try again later");
            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={400}; RESPONSE={ Service is currently not available from your bank, try again later}; ");
            Logging.warn(ExceptionStackTrace.GetStackTrace(ex));

            return response;
        }
        try {
            String nullCheck=checkNull(data,"checkstatus");
            if(nullCheck!=""){
                ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"11017","Invalid technical data");
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/auth}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={400}; RESPONSE={"+nullCheck+" is not found}; ");
                return response;
            }
            String test_body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <dat:ACCOUNTDETAILS>\n" +
                    "         <dat:ACCNO>"+data.debitPoolAccount+"</dat:ACCNO>\n" +
                    "      </dat:ACCOUNTDETAILS>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope>";
            String res_TEST = Request.SendRequest(env.getProperty("IPNBaseURL"), "ACCOUNTDETAILS", test_body);
            Logging.host("Host response -------------" + res_TEST);
            System.out.println(res_TEST.indexOf("BAL_AVAIL"));
            String accountBalance="";
            int i=res_TEST.indexOf("BAL_AVAIL")+10;
            while (res_TEST.charAt(i)!='<'){

                accountBalance+=res_TEST.charAt(i);
                i+=1;
            }

            checkstatusFT.env =env;

            checkstatusFT.accountsRepository=accountsRepository;
            checkstatusFT.acceptedLogsRepository=acceptedLogsRepository;
            checkstatusFT.reversalLogsRepository=reversalLogsRepository;
            checkstatusFT.rejectedLogsRepository=rejectedLogsRepository;
            checkstatusFT.loginsRepository=loginsRepository;
            checkstatusFT.cardsRepository=cardsRepository;
            checkstatusFT.errorsRepository=errorsRepository;
            checkstatusFT.allLogsRepsitory=allLogsRepsitory;
            //=============================================================================
            String [] statusDB=allLogsRepsitory.getResp(data.transactionId);

            if(  statusDB.length <1){

             //   String ErrorDesc=errorsRepository.getErrorDes("invalid accountId");
             //   String ErrorCode=errorsRepository.getErrorCode("invalid accountId");

//                @RequestMapping( value = "fundtransfer",method = RequestMethod.POST,consumes = "application/jason" )
//                public String foundtarns (){}

                CombinedResDto response=new CombinedResDto();
                response=checkstatusFT.Check_Status_FT(data);
              //  ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"12031","Original transaction not found");
                if(response.checkstatusres!=null)
                return response.checkstatusres;

                return response.errorres;

            }

            else{
               if(Check_Transaction_Status(data).equalsIgnoreCase("ACCEPTED")){


                return  Original_Res_Details(data);
                //   ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"00000","Original transaction already processed");
                //   return response;

               }
               else if(Check_Transaction_Status(data).equalsIgnoreCase("REVERSED")){

                   ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"12030","Reversal already processed");
                   return response;

                }


            }
            String finalStatusDB= statusDB[0] ;

            System.out.println(finalStatusDB);
            String [] respString= acceptedLogsRepository.getFt(data.requestId);
            int index=respString[0].indexOf("authCode")+10;
            String ftRequest="";
            while (respString[0].charAt(index)!='\''){
                ftRequest+=respString[0].charAt(index);
                index+=1;
            }
            //===================================
            String ftBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <dat:GetFTstatus>\n" +
                    "         <dat:FT_ID>"+ftRequest+"</dat:FT_ID>\n" +
                    "      </dat:GetFTstatus>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope>";
            String resFT = Request.SendRequest(env.getProperty("IPNBaseURL"), "GetFTstatus", ftBody);
            Logging.host("Host response -------------" + resFT);
            int indexAmount=resFT.indexOf("AMOUNT")+7;
            String amontValue="";
            String ftStatus="";
            while (resFT.charAt(indexAmount)!='<'){
                amontValue+=resFT.charAt(indexAmount);
                indexAmount+=1;
            }
            if(Float.parseFloat(amontValue)>0){
                ftStatus="ACCEPTED";
            }else{
                ftStatus="REJECTED";
            }
            //===================================
            if (!ftStatus.equals("ACCEPTED")||!finalStatusDB.equals("ACCEPTED")){
                String ErrorDesc=errorsRepository.getErrorDes("invalid accountId");
                String ErrorCode=errorsRepository.getErrorCode("invalid accountId");
                ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,ErrorCode,ErrorDesc);

            }
            //System.out.println(ftRequest);
            LocalDateTime now = LocalDateTime.now();
            String currentTimestamp = String.valueOf(Timestamp.valueOf(now));
            List<Card> cards = Streamable.of(cardsRepository.findAll()).toList();
            String debitAccount = null, creditAccount = null, customerNumber = null;
            if (data.debitAccountType.equals("ACCOUNT") && data.creditAccountType.equals("ACCOUNT")) {
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
            } else if (Check_Account_Type (data.debitAccountId,data.debitAccountType).equals("CARD") || Check_Account_Type(data.creditAccountId,data.creditAccountType).equals("CARD")) {
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


                if ((data.debitAccountType.equals("CARD") || data.creditAccountType.equals("CARD")) && cards.stream().anyMatch(x -> x.getIssuer().equals("NI") && x.getStartsWith().equals(data.debitAccountId.substring(0, 6)))) {
                    // call NI to fetch customer number from deibt account id
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
                            "\t\t\t\t<fimi1:PAN>" + (data.requestType.equalsIgnoreCase("DEBIT") ? data.debitAccountId : data.creditAccountId) + "</fimi1:PAN>\n" +
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
                    Logging.host("Host response -------------" + res);
                    Document doc = XMLParser.ReadXML(res);
                    String innerXml = doc.getElementsByTagName("FEMI_RESPONSE").item(0).getTextContent();
                    innerXml = innerXml.replaceAll(":", "_");
                    doc = XMLParser.ReadXML(innerXml);
                    customerNumber = doc.getElementsByTagName("m0_PersonExtId").item(0).getTextContent();
                    System.out.println("customer Number ni " + customerNumber);
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
                    Logging.host("Host response -------------" + res);
                    Document doc = XMLParser.ReadXML(res);
                    String innerXml = doc.getElementsByTagName("response").item(0).getTextContent();
                    doc = XMLParser.ReadXML(innerXml);
                    customerNumber = doc.getElementsByTagName("customerNumber").item(0).getTextContent();
                    System.out.println("Customer Number 10/10: " + customerNumber);
                }

            }
            customerNumber = "1127363"; //

            String t24DebitAcc = data.requestType.equalsIgnoreCase("DEBIT") && data.debitAccountType.equals("CARD") && cards.stream().anyMatch(x -> x.getIssuer().equals("MDP"))
                    ? env.getProperty("t24.poolaccount.mdpDebitPool")
                    : data.requestType.equalsIgnoreCase("DEBIT") && data.debitAccountType.equals("CARD") && cards.stream().anyMatch(x -> x.getIssuer().equals("NI"))
                    ? env.getProperty("t24.poolaccount.niDebitPool")
                    : debitAccount;

            String t24CreditAcc = data.requestType.equalsIgnoreCase("CREDIT") && data.debitAccountType.equals("CARD") && cards.stream().anyMatch(x -> x.getIssuer().equals("MDP"))
                    ? env.getProperty("t24.poolaccount.mdpCreditPool")
                    : data.requestType.equalsIgnoreCase("CREDIT") && data.debitAccountType.equals("CARD") && cards.stream().anyMatch(x -> x.getIssuer().equals("NI"))
                    ? env.getProperty("t24.poolaccount.niCreditPool")
                    : creditAccount;




            // Request to get customer details
            Login customer = loginsRepository.getLoginByCustomerReference(customerNumber);
            String docType = customer.getDocumentType().equals("NationalId") ? "NID" : customer.getDocumentType();
            //TODO Balance Check



            CheckStatusResDto res = new CheckStatusResDto(data.requestId, currentTimestamp, "00000", "SUCCESS", accountBalance, "EGP", ftRequest, customer.getName(), customer.getAddress(), docType, customer.getLegalId());

            return res;
        } catch (Exception ex) {
            String stackTrace = ExceptionStackTrace.GetStackTrace(ex);
            Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/checkstatus}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={400}; RESPONSE={"+stackTrace+"}; ");
            ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,"11017","Invalid technical data");
            return response;
//            String ErrorDesc=errorsRepository.getErrorDes("invalid accountId");
//            String ErrorCode=errorsRepository.getErrorCode("invalid accountId");
//            ErrorsResDto response= new ErrorsResDto (data.requestId,data.timestamp,ErrorCode,ErrorDesc);
//            return response;
//            throw new ResponseStatusException(BAD_REQUEST,stackTrace);
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

                if (fields[i].get(data)== null ||fields[i].get(data)== ""||fields[i].get(data).equals("null")){
                    return indexReq;
                }

            }

        }
        return "";
    }

    public String Get_account_from_card_number(String PAN){
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
    Logging.host("Host response -------------" + res);
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

    public String Get_Account_Balance(String AccountNO) {
        try {
            String balance="0.00";

            if( AccountNO.startsWith("50"))
            {
                String mdpCardInfo2="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:mdp=\"http://ws.wso2.org/dataservice/mdpCardInfo\">\n" +
                        "   <soapenv:Header/>\n" +
                        "   <soapenv:Body>\n" +
                        "      <mdp:_getmdpcardinfo>\n" +
                        "         <mdp:cardNumber>"+AccountNO+"</mdp:cardNumber>\n" +
                        "      </mdp:_getmdpcardinfo>\n" +
                        "   </soapenv:Body>\n" +
                        "</soapenv:Envelope>";
                String resMdpCardInfo2 = Request.SendRequest(env.getProperty("MDPBaseURL"), "_getmdpcardinfo", mdpCardInfo2);
                Logging.host("Host response -------------" + resMdpCardInfo2);
                Document docMdp2 = XMLParser.ReadXML(resMdpCardInfo2);
                String innerXMLmdpCard = docMdp2.getElementsByTagName("response").item(0).getTextContent();
                docMdp2 = XMLParser.ReadXML(innerXMLmdpCard);
                balance=docMdp2.getElementsByTagName("avalBalance").item(0).getTextContent();
                return balance;
            }

            else if(AccountNO.startsWith("41"))
            {
               String response= NI_Get_Card_Info_FUNC(AccountNO);
                Document doc = XMLParser.ReadXML(response);
                String innerXml = doc.getElementsByTagName("FEMI_RESPONSE").item(0).getTextContent();
                innerXml = innerXml.replaceAll(":", "_");
                doc = XMLParser.ReadXML(innerXml);
                 balance = doc.getElementsByTagName("m0_AvailBalance").item(0).getTextContent();
                return balance;
            }

            else if (AccountNO.startsWith("45")){

                AccountNO= Get_account_from_card_number(AccountNO);

            }



            String ReqBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <dat:ACCOUNTDETAILS>\n" +
                    "         <dat:ACCNO>";


            ReqBody += AccountNO.substring(AccountNO.length()-16,AccountNO.length());

            ReqBody += "</dat:ACCNO>\n" +
                    "      </dat:ACCOUNTDETAILS>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope>";
            String res_Balance = Request.SendRequest(env.getProperty("IPNBaseURL"), "ACCOUNTDETAILS", ReqBody);
            Logging.host("Host response -------------" + res_Balance);
            if (res_Balance.contains("No records were found")) {

                Logging.warn(res_Balance.toString() + "   " + AccountNO);

            }

            Document doc_balance = XMLParser.ReadXML(res_Balance);
            balance = doc_balance.getElementsByTagName("BAL_AVAIL").item(0).getTextContent();
            return balance;
        } catch (Exception e) {

            return "0";
        }
    }

    public String Check_Transaction_Status(FundTransferReqDto data) throws Exception {

try {
    String[] ReversalstatusDB = reversalLogsRepository.getFt(data.transactionId);

    if( ReversalstatusDB.length>0){

        return "Reversed";
    }




    String[] statusDB = allLogsRepsitory.getResp(data.transactionId);

    if(statusDB.length==0){

        return "TXn_NOT_EXIST";

    }
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
    Logging.host("Host response -------------" + resFT);
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

    public ErrorsResDto Check_Account_Existence(FundTransferReqDto data) throws Exception {


        if (data.creditAccountType.equals("ACCOUNT") && data.requestType.equalsIgnoreCase("CREDIT") && data.creditAccountType != null) {
            String ReqBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <dat:ACCOUNTDETAILS>\n" +
                    "         <dat:ACCNO>" + data.creditAccountId.substring(data.creditAccountId.length() - 16) + "</dat:ACCNO>\n" +
                    "      </dat:ACCOUNTDETAILS>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope>";
            String res_Balance = Request.SendRequest(env.getProperty("IPNBaseURL"), "ACCOUNTDETAILS", ReqBody);
            Logging.host("Host response -------------" + res_Balance);
            if (res_Balance.contains("No records were found")) {
                ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13001", "The beneficiary account number is invalid");
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={13001}; RESPONSE={ "+response+"}; ");
                rejectedLogsRepository.save(new RejectedLog(data.requestId, LocalDateTime.now(), data.toString(), response.toString(), null));
                return response;
            }
            Document doc_balance = XMLParser.ReadXML(res_Balance);
            String currency = doc_balance.getElementsByTagName("CCYDESC").item(0).getTextContent();
            if (!currency.equalsIgnoreCase("EGP")) {
                ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13003}; RESPONSE={" + response + " }; ");
                rejectedLogsRepository.save(new RejectedLog(data.requestId, LocalDateTime.now(), data.toString(), response.toString(), null));
                return response;
            }}

        else if(data.debitAccountType.equals("ACCOUNT") && data.requestType.equalsIgnoreCase("DEBIT") && data.debitAccountId != null){

            String ReqBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <dat:ACCOUNTDETAILS>\n" +
                    "         <dat:ACCNO>" + data.debitAccountId.substring(data.debitAccountId.length() - 16) + "</dat:ACCNO>\n" +
                    "      </dat:ACCOUNTDETAILS>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope>";
            String res_Balance = Request.SendRequest(env.getProperty("IPNBaseURL"), "ACCOUNTDETAILS", ReqBody);
            Logging.host("Host response -------------" + res_Balance);
            if (res_Balance.contains("No records were found")) {
                ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13001", "The beneficiary account number is invalid");
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={"+data+"}; RESPONSE CODE={13001}; RESPONSE={ "+response+"}; ");
                rejectedLogsRepository.save(new RejectedLog(data.requestId, LocalDateTime.now(), data.toString(), response.toString(), null));
                return response;
            }
            Document doc_balance = XMLParser.ReadXML(res_Balance);
            String currency = doc_balance.getElementsByTagName("CCYDESC").item(0).getTextContent();
            if (!currency.equalsIgnoreCase("EGP")) {
                ErrorsResDto response = new ErrorsResDto(data.requestId, data.timestamp, "13003", "The beneficiary account cannot accept transactions");
                Logging.warn("FINISHED PROCESSING : METHOD={POST}; REQUESTURI={/fundtransfer}; REQUEST PAYLOAD={" + data + "}; RESPONSE CODE={13003}; RESPONSE={" + response + " }; ");
                rejectedLogsRepository.save(new RejectedLog(data.requestId, LocalDateTime.now(), data.toString(), response.toString(), null));
                return response;
            }

        }


    return null;
    }

    public FundTransferResDto Credit_Account (String customerNumber,FundTransferReqDto data){
            List<Card> cards = Streamable.of(cardsRepository.findAll()).toList();
        double deductedAmount=0;

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
                                        ? data.creditPoolAccount
                                        :data.requestType.equalsIgnoreCase("DEBIT") && data.creditAccountType.equals("WALLET")
                                        ? data.creditPoolAccount:
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
                                ? data.creditPoolAccount
                                :data.requestType.equalsIgnoreCase("DEBIT")
                                ? data.creditPoolAccount
                                : data.requestType.equalsIgnoreCase("CREDIT") && cards.stream().anyMatch(x -> x.getIssuer().equals("NI"))
                                ? env.getProperty("t24.poolaccount.niCreditPool")
                                : data.creditAccountId;
                        if (data.requestType.equalsIgnoreCase("CREDIT")&&(data.creditAccountId.length()>16||data.creditAccountId.charAt(0)=='1')){
                            t24CreditAcc=data.creditAccountId;
                        }
                        else {
                            t24CreditAcc= data.creditPoolAccount;
                        }
                    }
                    if (t24DebitAcc==null||t24DebitAcc.equals("")){
                        t24DebitAcc=data.debitAccountId;
                    }
                    LocalDateTime now = LocalDateTime.now();
                    // String currentTimestamp = String.valueOf(Timestamp.valueOf(now));
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");


                    //calculating deducted amount


                    if(data.requestType.equalsIgnoreCase("REVERSAL")){

                        String temp=t24CreditAcc;
                        t24CreditAcc=t24DebitAcc;
                        t24DebitAcc=temp;

                    }

                   String Original_Request_Type=Get_Org_Txn_Details(data);


                    deductedAmount=Calc_Deducted_Amount(data,Original_Request_Type);

                    if(data.requestType.equalsIgnoreCase("REVERSAL")&&Original_Request_Type.equalsIgnoreCase("DE")){

                        t24DebitAcc=env.getProperty("t24.poolaccount.EBCDebitPool");

                    }

                    if(data.requestType.equalsIgnoreCase("REVERSAL")&&Original_Request_Type.equalsIgnoreCase("CR")){

                        t24CreditAcc= env.getProperty("t24.poolaccount.EBCCreditPool");

                    }

                    String SoapAction="IPNAccountPayment";
                    if(data.requestType.equalsIgnoreCase("REVERSAL")){

                        SoapAction="REVERSALFTCONTRACT";
                    }
                    else if(data.requestType.equalsIgnoreCase("CREDIT")){

                        if(data.creditAccountType.equalsIgnoreCase("CARD")&&data.creditAccountId.startsWith("4103")){
                            SoapAction="IPNCardPayment";
                        }
                        else if (data.creditAccountType.equalsIgnoreCase("CARD")&&data.creditAccountId.startsWith("5078")){
                            SoapAction="IPNMeezaPayment";
                        }
                        else if(data.creditAccountType.equalsIgnoreCase("WALLET")){
                            SoapAction="IPNWalletPayment";
                        }

                    }
                    else if (data.requestType.equalsIgnoreCase("DEBIT")){


                        if(data.debitAccountType.equalsIgnoreCase("CARD")&&data.debitAccountId.startsWith("4103")){
                            SoapAction="IPNCardPayment";
                        }
                        else if (data.debitAccountType.equalsIgnoreCase("CARD")&&data.debitAccountId.startsWith("5078")){
                            SoapAction="IPNMeezaPayment";
                        }
                        else if(data.debitAccountType.equalsIgnoreCase("WALLET")){
                            SoapAction="IPNWalletPayment";
                        }


                    }

                    String body = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:dat=\"http://ws.wso2.org/dataservice\">" +
                            "   <soap:Header/>" +
                            "   <soap:Body>" +
                            "      <dat:"+SoapAction+">" +
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
                            "      </dat:"+SoapAction+">" +
                            "   </soap:Body>" +
                            "</soap:Envelope>";
//            dateFormat.format(Timestamp.valueOf(now.plusDays(-5)))
//            dateFormat.format(Timestamp.valueOf(now.plusDays(-5)))
                    String response = Request.SendRequest(env.getProperty("IPNBaseURL"), SoapAction, body);
                    Logging.host("Host response -------------" + response);
                    Document doc = XMLParser.ReadXML(response);
                    LocalDateTime now2 = LocalDateTime.now();
                    String currentTimestamp2 = String.valueOf(Timestamp.valueOf(now2));
                    String ref = doc.getElementsByTagName("HOSTREF").item(0).getTextContent();


                    //TODO Balance Check
                    DecimalFormat  formatter = new DecimalFormat("#0.00");
                    String balance = "";
                    if(data.requestType.equalsIgnoreCase("CREDIT")||data.requestType.equalsIgnoreCase("REVERSAL")) {
                        balance = Get_Account_Balance(data.creditAccountId);
                    }

                    else{

                        balance = Get_Account_Balance(data.debitAccountId);

                    }

                    String msg = data.requestType.equalsIgnoreCase("DEBIT") ? deductedAmount + " has been debited from your ADCB account No ****" + data.debitAccountId.substring(data.debitAccountId.length() - 4, data.debitAccountId.length()) + " on " + currentTimestamp2.substring(0, 10) + " to , ref. " + data.transactionId.substring(data.transactionId.length() - 8, data.transactionId.length()) + "" :
                            data.requestType.equalsIgnoreCase("CREDIT") ? deductedAmount + " has been credited to your ADCB account No ****" + data.creditAccountId.substring(data.creditAccountId.length() - 4, data.creditAccountId.length()) + " on " + currentTimestamp2.substring(0, 10) + " from " + data.payeeName + ", ref." + data.transactionId.substring(data.transactionId.length() - 8, data.transactionId.length()) + "" : "";

                    SendSMS_FUNC(data.payerMobileNumber, msg);

                    Login customer = loginsRepository.getLoginByMobileNumber(data.payerMobileNumber);
                    String docType = customer.getDocumentType().equals("NationalId") ? "NID" : customer.getDocumentType();

                    //TODO CARD BALANCE CHECK

                    if(Original_Request_Type.equalsIgnoreCase("CR")) {

                        balance = Get_Account_Balance(data.creditAccountId);
                    }

                    else{

                        balance = Get_Account_Balance(data.debitAccountId);

                    }


                    balance=formatter.format(Double.valueOf(balance));
                    FundTransferResDto res = new FundTransferResDto(data.requestId, currentTimestamp2, "00000", "SUCCESS", balance, "EGP", ref, customer.getName(), customer.getAddress(), docType, customer.getLegalId());

                    if(data.requestType.equalsIgnoreCase("REVERSAL")){

                        reversalLogsRepository.save(new ReversalLog(data.transactionId, now, data.toString(), res.toString(),null));


                    }
                    else
                    acceptedLogsRepository.save(new AcceptedLog(data.transactionId, now, data.toString(), res.toString(),null));

                    return res;


                }
            } catch (Exception ex) {

                Logging.warn(ex.getStackTrace().toString());
                return null;

            }

            return null;
        }

    public String Check_Account_Type(String AccountID,String AccountType){

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

    public String Check_Transaction_Status(CheckStatusReqDto data)throws Exception {

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
            Logging.host("Host response -------------" + resFT);
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

    public String Get_Org_Txn_Details(FundTransferReqDto data) throws JSONException {

        String[] statusDB = allLogsRepsitory.getReq(data.transactionId);

        try {
            String ORG_Request = statusDB[0];
            String Request_Type_Initials = ORG_Request.substring(ORG_Request.toUpperCase().indexOf("REQUESTTYPE") + 13, ORG_Request.toUpperCase().indexOf("REQUESTTYPE") + 15);
            return Request_Type_Initials;
        }
        catch (Exception e){return "N/A";}
    }

    public CheckStatusResDto Original_Res_Details(CheckStatusReqDto data){

        Login customer = loginsRepository.getLoginByMobileNumber(data.payerMobileNumber);
        String docType = customer.getDocumentType().equals("NationalId") ? "NID" : customer.getDocumentType();


        String[] statusDB = allLogsRepsitory.getResDetails(data.transactionId);
        String ORG_Request=statusDB[0];
        String accountBalance="";
        String ftRequest="";
        String currentTimestamp="";


        accountBalance=ORG_Request.substring(ORG_Request.indexOf("balance")+9,ORG_Request.indexOf("balanceCurr")-3);
        ftRequest=ORG_Request.substring(ORG_Request.indexOf("authCode")+10,ORG_Request.indexOf("name")-3);
        currentTimestamp=ORG_Request.substring(ORG_Request.indexOf("timestamp")+11,ORG_Request.indexOf("respCode")-3);

        CheckStatusResDto res = new CheckStatusResDto(data.requestId, currentTimestamp, "00000", "SUCCESS", accountBalance, "EGP", ftRequest, customer.getName(), customer.getAddress(), docType, customer.getLegalId());


        return res;
    }

    public String Get_Transaction_Refrence_Fimi(String response){

        response=response.replaceAll("&lt;","<");

        response=response.replaceAll("&gt;",">");

        String trensaction_refrence=response.substring(response.indexOf("<m0:ThisTranId>")+15,response.indexOf("</m0:ThisTranId>"));
        return trensaction_refrence;


    }

    public String Get_PrePaid_Card_Balance(String CardNumber){
try {
    String mdpCardInfo = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:mdp=\"http://ws.wso2.org/dataservice/mdpCardInfo\">\n" +
            "   <soapenv:Header/>\n" +
            "   <soapenv:Body>\n" +
            "      <mdp:_getmdpcardinfo>\n" +
            "         <mdp:cardNumber>" + CardNumber + "</mdp:cardNumber>\n" +
            "      </mdp:_getmdpcardinfo>\n" +
            "   </soapenv:Body>\n" +
            "</soapenv:Envelope>";
    String resMdpCardInfo = Request.SendRequest(env.getProperty("MDPBaseURL"), "_getmdpcardinfo", mdpCardInfo);
    Logging.host("Host response -------------" + resMdpCardInfo);

    Document docMdp2 = XMLParser.ReadXML(resMdpCardInfo);
    String innerXMLmdpCard = docMdp2.getElementsByTagName("response").item(0).getTextContent();
    docMdp2 = XMLParser.ReadXML(innerXMLmdpCard);
    String state = docMdp2.getElementsByTagName("state").item(0).getTextContent();
    String expDate = docMdp2.getElementsByTagName("expirDate").item(0).getTextContent();
    String balance=docMdp2.getElementsByTagName("avalBalance").item(0).getTextContent();

    return balance;
}
catch(Exception ex){

    return null;
}

}

    public FundTransferResDto Reversal_Meeza (FundTransferReqDto data){
        List<Card> cards = Streamable.of(cardsRepository.findAll()).toList();
        String t24DebitAcc=null;
        String t24CreditAcc = null;
        String refMDP=null;
        double deductedAmount=0;



        try {

            String Original_Request_Type=Get_Org_Txn_Details(data);
            deductedAmount=Calc_Deducted_Amount(data,Original_Request_Type);

            if(Original_Request_Type.equalsIgnoreCase("DE")){

                t24CreditAcc=env.getProperty("t24.poolaccount.mdpCreditPool");
                t24DebitAcc=env.getProperty("t24.poolaccount.EBCDebitPool");



            }
            else if(Original_Request_Type.equalsIgnoreCase("CR")){

                t24CreditAcc= env.getProperty("t24.poolaccount.EBCCreditPool");
                t24DebitAcc=env.getProperty("t24.poolaccount.mdpDebitPool");

            }


            if (data.creditAccountType != null) {


                String balance = "";
                LocalDateTime now = LocalDateTime.now();
                // String currentTimestamp = String.valueOf(Timestamp.valueOf(now));
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");


                String SoapAction="IPNAccountPayment";
                if(data.requestType.equalsIgnoreCase("REVERSAL")){

                    SoapAction="REVERSALFTCONTRACT";
                }
                else if(data.requestType.equalsIgnoreCase("CREDIT")){

                    if(data.creditAccountType.equalsIgnoreCase("CARD")&&data.creditAccountId.startsWith("4103")){
                        SoapAction="IPNCardPayment";
                    }
                    else if (data.creditAccountType.equalsIgnoreCase("CARD")&&data.creditAccountId.startsWith("5078")){
                        SoapAction="IPNMeezaPayment";
                    }
                    else if(data.creditAccountType.equalsIgnoreCase("WALLET")){
                        SoapAction="IPNWalletPayment";
                    }

                }
                else if (data.requestType.equalsIgnoreCase("DEBIT")){


                    if(data.debitAccountType.equalsIgnoreCase("CARD")&&data.debitAccountId.startsWith("4103")){
                        SoapAction="IPNCardPayment";
                    }
                    else if (data.debitAccountType.equalsIgnoreCase("CARD")&&data.debitAccountId.startsWith("5078")){
                        SoapAction="IPNMeezaPayment";
                    }
                    else if(data.debitAccountType.equalsIgnoreCase("WALLET")){
                        SoapAction="IPNWalletPayment";
                    }


                }

                String body = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:dat=\"http://ws.wso2.org/dataservice\">" +
                        "   <soap:Header/>" +
                        "   <soap:Body>" +
                        "      <dat:"+SoapAction+">" +
                        "         <dat:DBACCOUNT>" + t24DebitAcc + "</dat:DBACCOUNT>" +
                        "         <dat:DRCCY>EGP</dat:DRCCY>" +
                        "         <dat:CRAMT>" + deductedAmount + "</dat:CRAMT>" +
                        "         <dat:CRACCOUNT>" + t24CreditAcc + "</dat:CRACCOUNT>" +
                        "         <dat:CRCCY>" + data.amount.get("orgCurr") + "</dat:CRCCY>" +
                        "         <dat:XREF>ReversalMeeza</dat:XREF>" +
                        "         <dat:DRVDT></dat:DRVDT>" +
                        "         <dat:CRVDT></dat:CRVDT>" +
                        "         <dat:INTRMKS>"+data.requestType+"</dat:INTRMKS>" +
                        "         <dat:OrderingBank>ADCB</dat:OrderingBank>" +
                        "      </dat:"+SoapAction+">" +
                        "   </soap:Body>" +
                        "</soap:Envelope>";
//            dateFormat.format(Timestamp.valueOf(now.plusDays(-5)))
//            dateFormat.format(Timestamp.valueOf(now.plusDays(-5)))
                String response = Request.SendRequest(env.getProperty("IPNBaseURL"), SoapAction, body);
                Logging.host("Host response -------------" + response);
                Document doc = XMLParser.ReadXML(response);
                LocalDateTime now2 = LocalDateTime.now();
                String currentTimestamp2 = String.valueOf(Timestamp.valueOf(now2));
                String ref =null;
                ref=doc.getElementsByTagName("HOSTREF").item(0).getTextContent();

                if(ref!=null){

                    if(Original_Request_Type.equalsIgnoreCase("DE")){

                        if(Meeza_Card_ID_Detector(data)){

                            refMDP=postcreditaccountpresentmentid_FUNC(data.creditAccountId,deductedAmount);
                        }
                        else if ((data.debitAccountType.equals("CARD") || data.creditAccountType.equals("CARD")) && cards.stream().anyMatch(x -> x.getIssuer().equals("MDP") && x.getStartsWith().equals(data.debitAccountId.substring(0, 6)))){

                            String[] body_list = allLogsRepsitory.getMeezaReversal(data.transactionId,data.debitAccountId,data.rrn.substring(6,12));

                            refMDP = Meeza_Reverse_IPN_Transaction(body_list[0],data.debitAccountId,deductedAmount);
                        }
                        balance=Get_PrePaid_Card_Balance(data.debitAccountId);

                    }
                    else if(Original_Request_Type.equalsIgnoreCase("CR")){

                        if(Meeza_Card_ID_Detector(data)){

                            refMDP=postdebitaccountpresentmentid_FUNC(data.creditAccountId,deductedAmount);
                        }
                        else if ((data.debitAccountType.equals("CARD") || data.creditAccountType.equals("CARD")) && cards.stream().anyMatch(x -> x.getIssuer().equals("MDP") && x.getStartsWith().equals(data.debitAccountId.substring(0, 6)))) {

                            String[] body_list = allLogsRepsitory.getMeezaReversal(data.transactionId,data.creditAccountId,data.rrn.substring(6,12));

                            refMDP = Meeza_Reverse_IPN_Transaction(body_list[0],data.creditAccountId,deductedAmount);

                        }
                            balance = Get_PrePaid_Card_Balance(data.creditAccountId);

                    }


                }

                String msg = data.requestType.equalsIgnoreCase("DEBIT") ? deductedAmount + " has been debited from your ADCB account No ****" + data.debitAccountId.substring(data.debitAccountId.length() - 4, data.debitAccountId.length()) + " on " + currentTimestamp2.substring(0, 10) + " to , ref. " + data.transactionId.substring(data.transactionId.length() - 8, data.transactionId.length()) + "" :
                        data.requestType.equalsIgnoreCase("CREDIT") ? deductedAmount + " has been credited to your ADCB account No ****" + data.creditAccountId.substring(data.creditAccountId.length() - 4, data.creditAccountId.length()) + " on " + currentTimestamp2.substring(0, 10) + " from " + data.payeeName + ", ref." + data.transactionId.substring(data.transactionId.length() - 8, data.transactionId.length()) + "" : "";

                SendSMS_FUNC(data.payerMobileNumber,msg);



                Login customer = loginsRepository.getLoginByMobileNumber(data.payerMobileNumber);
                String docType = customer.getDocumentType().equals("NationalId") ? "NID" : customer.getDocumentType();
                DecimalFormat formatter = new DecimalFormat("#.##");
                balance=formatter.format(Double.valueOf(balance));
                FundTransferResDto res = new FundTransferResDto(data.requestId, currentTimestamp2, "00000", "SUCCESS", balance, "EGP", ref, customer.getName(), customer.getAddress(), docType, customer.getLegalId());



                    reversalLogsRepository.save(new ReversalLog(data.transactionId, now, data.toString(), res.toString(),refMDP));



                return res;


            }
        } catch (Exception ex) {

            Logging.warn(ex.getStackTrace().toString());
            return null;

        }

        return null;
    }

    public FundTransferResDto Reversal_NI (FundTransferReqDto data) {

        List<Card> cards = Streamable.of(cardsRepository.findAll()).toList();
        String t24DebitAcc=null;
        String t24CreditAcc = null;
        String trxRefrence = null;
        double deductedAmount = 0;

        String[] transactionRefrenceDB = acceptedLogsRepository.getTransactionRefrence(data.transactionId);
        String OriginaltxnRefrenceDB= transactionRefrenceDB[0];

        try {

            if(OriginaltxnRefrenceDB!=null) {

                String Original_Request_Type = Get_Org_Txn_Details(data);
                deductedAmount=Calc_Deducted_Amount(data,Original_Request_Type);

                if (Original_Request_Type.equalsIgnoreCase("DE")) {

                    t24CreditAcc=env.getProperty("t24.poolaccount.niCreditPool");
                    t24DebitAcc=env.getProperty("t24.poolaccount.EBCDebitPool");


                }
                else if(Original_Request_Type.equalsIgnoreCase("CR")){

                    t24CreditAcc= env.getProperty("t24.poolaccount.EBCCreditPool");
                    t24DebitAcc=env.getProperty("t24.poolaccount.niDebitPool");

                }


                if (data.creditAccountType != null) {


                    String balance = "";
                    LocalDateTime now = LocalDateTime.now();
                    // String currentTimestamp = String.valueOf(Timestamp.valueOf(now));
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");


                    DecimalFormat df = new DecimalFormat("#.##");

                    String SoapAction="IPNAccountPayment";
                    if(data.requestType.equalsIgnoreCase("REVERSAL")){

                        SoapAction="REVERSALFTCONTRACT";
                    }
                    else if(data.requestType.equalsIgnoreCase("CREDIT")){

                        if(data.creditAccountType.equalsIgnoreCase("CARD")&&data.creditAccountId.startsWith("4103")){
                            SoapAction="IPNCardPayment";
                        }
                        else if (data.creditAccountType.equalsIgnoreCase("CARD")&&data.creditAccountId.startsWith("5078")){
                            SoapAction="IPNMeezaPayment";
                        }
                        else if(data.creditAccountType.equalsIgnoreCase("WALLET")){
                            SoapAction="IPNWalletPayment";
                        }

                    }
                    else if (data.requestType.equalsIgnoreCase("DEBIT")){


                        if(data.debitAccountType.equalsIgnoreCase("CARD")&&data.debitAccountId.startsWith("4103")){
                            SoapAction="IPNCardPayment";
                        }
                        else if (data.debitAccountType.equalsIgnoreCase("CARD")&&data.debitAccountId.startsWith("5078")){
                            SoapAction="IPNMeezaPayment";
                        }
                        else if(data.debitAccountType.equalsIgnoreCase("WALLET")){
                            SoapAction="IPNWalletPayment";
                        }


                    }

                    String body = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:dat=\"http://ws.wso2.org/dataservice\">" +
                            "   <soap:Header/>" +
                            "   <soap:Body>" +
                            "      <dat:"+SoapAction+">" +
                            "         <dat:DBACCOUNT>" + t24DebitAcc + "</dat:DBACCOUNT>" +
                            "         <dat:DRCCY>EGP</dat:DRCCY>" +
                            "         <dat:CRAMT>" + deductedAmount + "</dat:CRAMT>" +
                            "         <dat:CRACCOUNT>" + t24CreditAcc + "</dat:CRACCOUNT>" +
                            "         <dat:CRCCY>" + data.amount.get("orgCurr") + "</dat:CRCCY>" +
                            "         <dat:XREF>ReversalMeeza</dat:XREF>" +
                            "         <dat:DRVDT></dat:DRVDT>" +
                            "         <dat:CRVDT></dat:CRVDT>" +
                            "         <dat:INTRMKS>"+data.requestType+"</dat:INTRMKS>" +
                            "         <dat:OrderingBank>ADCB</dat:OrderingBank>" +
                            "      </dat:"+SoapAction+">" +
                            "   </soap:Body>" +
                            "</soap:Envelope>";
//            dateFormat.format(Timestamp.valueOf(now.plusDays(-5)))
//            dateFormat.format(Timestamp.valueOf(now.plusDays(-5)))
                    String response = Request.SendRequest(env.getProperty("IPNBaseURL"), SoapAction, body);
                    Logging.host("Host response -------------" + response);
                    Document doc = XMLParser.ReadXML(response);
                    LocalDateTime now2 = LocalDateTime.now();
                    String currentTimestamp2 = String.valueOf(Timestamp.valueOf(now2));
                    String ref = null;
                    ref = doc.getElementsByTagName("HOSTREF").item(0).getTextContent();

                    if (ref != null) {



                            body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                                    "   <soap:Header/>\n" +
                                    "   <soap:Body>\n" +
                                    "      <dat:ReverseTransaction>\n" +
                                    "<dat:Request1> <![CDATA["+
                                    "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:fimi=\"http://schemas.compassplus.com/two/1.0/fimi.xsd\" xmlns:fimi1=\"http://schemas.compassplus.com/two/1.0/fimi_types.xsd\">"+
                                    " <soap:Header/>"+
                                    "   <soap:Body>\n"+
                                    "      <fimi:ReverseTransactionRq>\n"+
                                    "         <fimi:Request __HEADERPARAM__>\n"+
                                    "<fimi1:Id>"+OriginaltxnRefrenceDB+"</fimi1:Id>"+
                                    "         </fimi:Request>\n"+
                                    "      </fimi:ReverseTransactionRq>\n"+
                                    "   </soap:Body>\n"+
                                    "</soap:Envelope>\n"+
                                    "]]></dat:Request1>\n"+
                                    "      </dat:ReverseTransaction>\n"+
                                    "   </soapenv:Body>\n"+
                                    "</soapenv:Envelope>\n"
                                    ;
                            String soapAction = "ReverseTransaction";
                            String res = Request.SendRequest(env.getProperty("NIBaseURL"), soapAction, body);
                            Logging.host("Host response -------------" + res);
                             trxRefrence=Get_Transaction_Refrence_Fimi(res);




                    }

                    String msg = data.requestType.equalsIgnoreCase("DEBIT") ? deductedAmount + " has been debited from your ADCB account No ****" + data.debitAccountId.substring(data.debitAccountId.length() - 4, data.debitAccountId.length()) + " on " + currentTimestamp2.substring(0, 10) + " to , ref. " + data.transactionId.substring(data.transactionId.length() - 8, data.transactionId.length()) + "" :
                            data.requestType.equalsIgnoreCase("CREDIT") ? deductedAmount + " has been credited to your ADCB account No ****" + data.creditAccountId.substring(data.creditAccountId.length() - 4, data.creditAccountId.length()) + " on " + currentTimestamp2.substring(0, 10) + " from " + data.payeeName + ", ref." + data.transactionId.substring(data.transactionId.length() - 8, data.transactionId.length()) + "" : "";


                    SendSMS_FUNC(data.payerMobileNumber,msg);


                    if(Get_Org_Txn_Details(data).equalsIgnoreCase("CR")&&data.creditAccountId.startsWith("41")){


                        balance=Get_Credit_Card_Balance(data.creditAccountId);
                    }
                    else if(Get_Org_Txn_Details(data).equalsIgnoreCase("DE")&&data.debitAccountId.startsWith("41")){

                        balance=Get_Credit_Card_Balance(data.debitAccountId);

                    }
                    else if(Get_Org_Txn_Details(data).equalsIgnoreCase("DE")&&data.debitAccountId.startsWith("45")){

                        String Acc=Get_account_from_card_number(data.debitAccountId);
                        balance=Get_Account_Balance(Acc);
                    }
                    else{

                        String Acc=Get_account_from_card_number(data.creditAccountId);
                        balance=Get_Account_Balance(Acc);
                    }

                    Login customer = loginsRepository.getLoginByMobileNumber(data.payerMobileNumber);
                    String docType = customer.getDocumentType().equals("NationalId") ? "NID" : customer.getDocumentType();
                    DecimalFormat formatter = new DecimalFormat("#.##");
                    balance = formatter.format(Double.valueOf(balance));
                    FundTransferResDto res = new FundTransferResDto(data.requestId, currentTimestamp2, "00000", "SUCCESS", balance, "EGP", ref, customer.getName(), customer.getAddress(), docType, customer.getLegalId());


                    reversalLogsRepository.save(new ReversalLog(data.transactionId, now, data.toString(), res.toString(), trxRefrence));


                    return res;


                }
            }
        } catch (Exception ex) {

            Logging.warn(ex.getStackTrace().toString());
            return null;

        }

        return null;
    }

    public String Get_Credit_Card_Balance(String PAN) throws Exception {

        String body="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <dat:GetCardInfoRq>\n" +
                "         <dat:Request1>\n" +
                "<![CDATA[\n" +
                "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:fimi=\"http://schemas.compassplus.com/two/1.0/fimi.xsd\" xmlns:fimi1=\"http://schemas.compassplus.com/two/1.0/fimi_types.xsd\">\n" +
                "                <soap:Header/>\n" +
                "                <soap:Body>\n" +
                "                                <fimi:GetCardInfoRq>\n" +
                "                                                <fimi:Request   __HEADERPARAM__  >\n" +
                "                                                                <fimi1:PAN>"+PAN+"</fimi1:PAN>\n" +
                "                                                </fimi:Request>\n" +
                "                                </fimi:GetCardInfoRq>\n" +
                "                </soap:Body>\n" +
                "</soap:Envelope>\n" +
                "]]>\n" +
                "\n" +
                "</dat:Request1>\n" +
                "      </dat:GetCardInfoRq>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>\n";
        String res = Request.SendRequest(env.getProperty("NIBaseURL"), "GetCardInfoRq", body);
        Logging.host("Host response -------------" + res);
        Document doc = XMLParser.ReadXML(res);
        String innerXml = doc.getElementsByTagName("FEMI_RESPONSE").item(0).getTextContent();
        innerXml = innerXml.replaceAll(":", "_");
        doc = XMLParser.ReadXML(innerXml);
        String balance = doc.getElementsByTagName("m0_AvailBalance").item(0).getTextContent();

        return balance;

    }

    public String Restrict_Foreign_Currencies(FundTransferReqDto data) throws Exception {

        String account="";

        if(data.requestType.equalsIgnoreCase("CREDIT")){

            account=data.creditAccountId;
        }
        else{

            account=data.debitAccountId;


        }

        String ReqBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <dat:ACCOUNTDETAILS>\n" +
                "         <dat:ACCNO>" + account + "</dat:ACCNO>\n" +
                "      </dat:ACCOUNTDETAILS>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";
        String res_Balance = Request.SendRequest(env.getProperty("IPNBaseURL"), "ACCOUNTDETAILS", ReqBody);
        Logging.host("Host response -------------" + res_Balance);
        if (res_Balance.contains("No records were found")) {
           return "No records were found";
        }
        Document doc_balance = XMLParser.ReadXML(res_Balance);
        String currency = doc_balance.getElementsByTagName("CCYDESC").item(0).getTextContent();

        if (!currency.equalsIgnoreCase("EGP")){

             return "Foreign";

        }
return "Local";
    }

    public boolean Meeza_Card_ID_Detector(FundTransferReqDto data){

        if(data.requestType.equalsIgnoreCase("DEBIT") && data.debitAccountId.length()==12 && data.debitAccountId.startsWith("1")){

            return true;
        }
        else if(data.requestType.equalsIgnoreCase("CREDIT") && data.creditAccountId.length()==12 && data.creditAccountId.startsWith("1")){

            return true;
        }

        return false;
    }

    public String postdebitaccountpresentmentid_FUNC(String CardID, double amount) throws Exception {

        String body = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:deb=\"http://ws.wso2.org/dataservice/debitAccountPresentment\">\n" +
                "   <soap:Header/>\n" +
                "   <soap:Body>\n" +
                "      <deb:_postdebitaccountpresentmentid>\n" +
                "         <deb:cardID>" + CardID + "</deb:cardID>\n" +
                "         <deb:amount>" + amount + "</deb:amount>\n" +
                "      </deb:_postdebitaccountpresentmentid>\n" +
                "   </soap:Body>\n" +
                "</soap:Envelope>";
        String soapAction = "_postdebitaccountpresentmentid";
        String res = Request.SendRequest(env.getProperty("MDPBaseURL"), soapAction, body);
        Logging.host("Host response -------------" + res);
        Document doc = XMLParser.ReadXML(res);
        doc = XMLParser.ReadXML(res);
        String innerXml = doc.getElementsByTagName("response").item(0).getTextContent();
        doc = XMLParser.ReadXML(innerXml);
        String refMDP = doc.getElementsByTagName("referenceNumber").item(0).getTextContent();

        return refMDP;

    }

    public String postcreditaccountpresentmentid_FUNC(String CardID, double amount) throws Exception {

        String body = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:cred=\"http://ws.wso2.org/dataservice/creditAccountPresentment\">\n" +
                "   <soap:Header/>\n" +
                "   <soap:Body>\n" +
                "      <cred:_postcreditaccountpresentmentid>\n" +
                "         <cred:cardNumber>" + CardID + "</cred:cardNumber>\n" +
                "         <cred:amount>" + amount + "</cred:amount>\n" +
                "      </cred:_postcreditaccountpresentmentid>\n" +
                "   </soap:Body>\n" +
                "</soap:Envelope>";
        String soapAction = "_postcreditaccountpresentmentid";
        String res = Request.SendRequest(env.getProperty("MDPBaseURL"), soapAction, body);
        Logging.host("Host response -------------" + res);
        Document doc = XMLParser.ReadXML(res);
        doc = XMLParser.ReadXML(res);
        String innerXml = doc.getElementsByTagName("response").item(0).getTextContent();
        doc = XMLParser.ReadXML(innerXml);
        String refMDP = doc.getElementsByTagName("referenceNumber").item(0).getTextContent();

        return refMDP;

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
        Logging.host("Host response -------------" + response);



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
        Logging.host("Host response -------------" + resMdpCardInfo);
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
        Logging.host("Host response -------------" + response);
        return response;

    }

    public String Get_CIF(FundTransferReqDto data) throws Exception {
        String CIF=null;
        ErrorsResDto returned_res;
        String regex = "[0-9]+";

       // data.matches(regex)

        if(data.requestType.equalsIgnoreCase("DEBIT") && data.debitAccountId !=null){

            if(data.debitAccountId.length()==16 && data.debitAccountId.startsWith("11")){

                returned_res=Check_Account_Existence(data);

                if (returned_res==null)
                CIF=data.debitAccountId.substring(1,8);

                else
                    return "NOT_FOUND";
            }
            else if (data.debitAccountId.length()==29 && data.debitAccountId.startsWith("EG")){
                    if(!data.debitAccountId.substring(2).matches(regex)) {
                        return  "inavlid_data";
                    }

                        returned_res = Check_Account_Existence(data);

                        if (returned_res == null) {
                            String accno = data.debitAccountId.substring(14);
                            CIF = accno.substring(0, 7);
                        }


                        else
                            return "NOT_FOUND";

            }
            else if(data.debitAccountId.length()==16 && (data.debitAccountId.startsWith("41") || data.debitAccountId.startsWith("45"))){

                Document doc = XMLParser.ReadXML(NI_Get_Card_Info_FUNC(data.debitAccountId));
                String innerXml = doc.getElementsByTagName("FEMI_RESPONSE").item(0).getTextContent();
                innerXml = innerXml.replaceAll(":", "_");
                doc = XMLParser.ReadXML(innerXml);
                CIF = doc.getElementsByTagName("m0_PersonExtId").item(0).getTextContent();

            }
            else if(data.debitAccountId.length()==16 && (data.debitAccountId.startsWith("5078") || data.debitAccountId.startsWith("12345"))){

                Document docMdp = XMLParser.ReadXML(getcustomerinfofromcardnumber_FUNC(data.debitAccountId));
                String innerXMLmdp = docMdp.getElementsByTagName("response").item(0).getTextContent();

                if(innerXMLmdp.contains("</ECODE>"))
                    return "NOT_FOUND";

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
        }

        else if(data.requestType.equalsIgnoreCase("CREDIT") && data.creditAccountId !=null){

            if(data.creditAccountId.length()==16 && data.creditAccountId.startsWith("11")){

                returned_res=Check_Account_Existence(data);

                if (returned_res==null)
                CIF=data.creditAccountId.substring(1,8);

                else
                    return "NOT_FOUND";
            }

            else if (data.creditAccountId.length()==29 && data.creditAccountId.startsWith("EG")){

                if(!data.creditAccountId.substring(2).matches(regex)) {
                    return  "inavlid_data";
                }

                returned_res=Check_Account_Existence(data);

                if (returned_res==null){

                String accno=data.creditAccountId.substring(14);
                CIF=accno.substring(0,7);
                }

                else
                    return "NOT_FOUND";
            }


            else if(data.creditAccountId.length()==16 && (data.creditAccountId.startsWith("41") || data.creditAccountId.startsWith("45"))){

                Document doc = XMLParser.ReadXML(NI_Get_Card_Info_FUNC(data.creditAccountId));
                String innerXml = doc.getElementsByTagName("FEMI_RESPONSE").item(0).getTextContent();
                innerXml = innerXml.replaceAll(":", "_");
                doc = XMLParser.ReadXML(innerXml);
                CIF = doc.getElementsByTagName("m0_PersonExtId").item(0).getTextContent();

            }
            else if(data.creditAccountId.length()==16 && (data.creditAccountId.startsWith("5078") || data.creditAccountId.startsWith("12345"))){

                Document docMdp = XMLParser.ReadXML(getcustomerinfofromcardnumber_FUNC(data.creditAccountId));
                String innerXMLmdp = docMdp.getElementsByTagName("response").item(0).getTextContent();
if(innerXMLmdp.contains("</ECODE>"))
    return "NOT_FOUND";
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
        }

        return CIF;
    }

    public String SendSMS_FUNC(String phonenumber,String message) throws Exception {


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
       Logging.host("Host response -------------" + res);
return res;
    }

    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public String AddCountryCode(String phoneNumber){

    if(phoneNumber.startsWith("01")) {
        phoneNumber = "002" + phoneNumber;
    }
    else if(phoneNumber.startsWith("2")) {
        phoneNumber = "00" + phoneNumber;

    }
    return phoneNumber;
}

    /**
     * this function is to credit/debit card from either NI or MEEZA
     * @param data the request itself
     * @param deductedAmount
     * @return the transaction refrence or transaction refused in case of error
     * @throws Exception
     */

    private String Card_Transaction(FundTransferReqDto data,double deductedAmount) throws Exception {

        String error_response = "Transaction_Refused";
        String refMDP, body = null;
        Document doc;
        if (data.requestType.equalsIgnoreCase("CREDIT")) {

//credit meeza
            if (data.creditAccountType.equalsIgnoreCase("CARD") && data.creditAccountId.startsWith("50")) {

                if (Meeza_Card_ID_Detector(data)) {

                   return refMDP = postcreditaccountpresentmentid_FUNC(data.creditAccountId, deductedAmount);
                } else if (data.creditAccountType.equalsIgnoreCase("CARD") && data.creditAccountId.startsWith("50")) {

                    String Meeza_Customized_Ref_Num=data.timestamp;

                    DateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    DateFormat outputFormat =new SimpleDateFormat("yyMMddHHmmss");

                    String inputdate = Meeza_Customized_Ref_Num;
                    Date date = inputFormat.parse(inputdate);
                    Meeza_Customized_Ref_Num = outputFormat.format(date);
                    Meeza_Customized_Ref_Num=Meeza_Customized_Ref_Num.substring(0,6)+data.rrn.substring(6,12);

                   return refMDP=Meeza_Credit_IPN_Transaction(data.creditAccountId,deductedAmount,Meeza_Customized_Ref_Num);


                }

            }

//credit NI
            if (data.creditAccountType.equals("CARD") && data.creditAccountId.startsWith("41")) {
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
                Logging.host("Host response -------------" + res);
                if (!res.toLowerCase().contains("declinereason")) {
                    fimiTrxRef = Get_Transaction_Refrence_Fimi(res);

                    return fimiTrxRef;

                } else {

                    return error_response;
                }

            }


        }
        else if (data.requestType.equalsIgnoreCase("DEBIT")) {

            //debit meeza
            if (data.requestType.equalsIgnoreCase("DEBIT") && data.debitAccountType.equalsIgnoreCase("CARD") && data.debitAccountId.startsWith("50")) {
                String soapAction = null;


                if (Meeza_Card_ID_Detector(data)) {

                   return refMDP = postdebitaccountpresentmentid_FUNC(data.debitAccountId, deductedAmount);
                } else {

                    String Meeza_Customized_Ref_Num=data.timestamp;

                    DateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    DateFormat outputFormat =new SimpleDateFormat("yyMMddHHmmss");

                    String inputdate = Meeza_Customized_Ref_Num;
                    Date date = inputFormat.parse(inputdate);
                    Meeza_Customized_Ref_Num = outputFormat.format(date);
                    Meeza_Customized_Ref_Num=Meeza_Customized_Ref_Num.substring(0,6)+data.rrn.substring(6,12);


                    return refMDP =Meeza_Debit_IPN_Transaction(data.debitAccountId,deductedAmount,Meeza_Customized_Ref_Num);




                }
            }

            //debit NI
            if (data.debitAccountType.equals("CARD") && data.debitAccountId.startsWith("41")) {
                String tranCode = "142";

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
                Logging.host("Host response -------------" + res);
                if (!res.toLowerCase().contains("declinereason")) {
                    fimiTrxRef = Get_Transaction_Refrence_Fimi(res);

                    return fimiTrxRef;

                } else {

                    return error_response;
                }

            }

        }


            return error_response;
    }


    private double  Calc_Deducted_Amount(FundTransferReqDto data,String requestType) {

    double deductedAmount = 0;

    if (requestType.startsWith("DE")) {


        if (data.bearFee != null)
            deductedAmount = Float.parseFloat(data.amount.get("orgValue")) + Float.parseFloat(data.bankFee.get("value")) + Float.parseFloat(data.pspFee.get("value")) - Float.parseFloat(data.bearFee.get("value"));
         else
            deductedAmount = Float.parseFloat(data.amount.get("orgValue")) + Float.parseFloat(data.bankFee.get("value")) + Float.parseFloat(data.pspFee.get("value"));

     } else {
        if (data.bearFee != null)
            deductedAmount = Float.parseFloat(data.amount.get("orgValue")) - Float.parseFloat(data.bankFee.get("value")) - Float.parseFloat(data.pspFee.get("value")) + Float.parseFloat(data.bearFee.get("value"));
        else
            deductedAmount = Float.parseFloat(data.amount.get("orgValue")) - Float.parseFloat(data.bankFee.get("value")) - Float.parseFloat(data.pspFee.get("value"));
    }
    DecimalFormat df = new DecimalFormat("#.##");
    deductedAmount = Double.valueOf(df.format(deductedAmount));
    return deductedAmount;
}


    private String Meeza_Debit_IPN_Transaction(String cardNumber,Double amount,String refNumber ) throws Exception {

        String error_response = "Transaction_Refused";
        String refMDP, body = null;
        Document doc;

         body="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:deb=\"http://ws.wso2.org/dataservice/debitAccountPresentmentIPN\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <deb:_postdebitaccountpresentmentipn>\n" +
                "        <deb:cardNumber>"+cardNumber+"</deb:cardNumber>\n" +
                "         <deb:amount>"+amount+"</deb:amount>\n" +
                "         <deb:refNumber>"+refNumber+"</deb:refNumber>\n" +
                "      </deb:_postdebitaccountpresentmentipn>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";

        String soapAction = "_postdebitaccountpresentmentipn";
        String res = Request.SendRequest(env.getProperty("MDPBaseURL"), soapAction, body);
        Logging.host("Host response -------------" + res);
        doc = XMLParser.ReadXML(res);
        String innerXml = doc.getElementsByTagName("response").item(0).getTextContent();
        doc = XMLParser.ReadXML(innerXml);


        if (res.contains("refNumber")) {


            refMDP= "  <rev:transactionType>debit-presentment</rev:transactionType>\n" +
                    "  <rev:refNumber>"+doc.getElementsByTagName("refNumber").item(0).getTextContent()+"</rev:refNumber>\n" +
                    "  <rev:stan>"+doc.getElementsByTagName("stan").item(0).getTextContent()+"</rev:stan>\n" +
                    "  <rev:authorizationCode>"+doc.getElementsByTagName("authorizationCode").item(0).getTextContent()+"</rev:authorizationCode>\n"+
                    "  <rev:trand>"+doc.getElementsByTagName("transactionDateTime").item(0).getTextContent()+"</rev:trand>";


            return refMDP;
        } else {
            return error_response;
        }

        }


    private String Meeza_Credit_IPN_Transaction(String cardNumber,Double amount,String refNumber ) throws Exception{

        String error_response = "Transaction_Refused";
        String refMDP, body = null;
        Document doc;

        body="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:deb=\"http://ws.wso2.org/dataservice/debitAccountPresentmentIPN\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <deb:_postcreditaccountpresentmentipn>\n" +
                "        <deb:cardNumber>"+cardNumber+"</deb:cardNumber>\n" +
                "         <deb:amount>"+amount+"</deb:amount>\n" +
                "         <deb:refNumber>"+refNumber+"</deb:refNumber>\n" +
                "      </deb:_postcreditaccountpresentmentipn>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";

        String soapAction = "_postcreditaccountpresentmentipn";
        String res = Request.SendRequest(env.getProperty("MDPBaseURL"), soapAction, body);
        Logging.host("Host response -------------" + res);
        doc = XMLParser.ReadXML(res);
        String innerXml = doc.getElementsByTagName("response").item(0).getTextContent();
        doc = XMLParser.ReadXML(innerXml);


        if (res.contains("refNumber")) {

            refMDP= "  <rev:transactionType>credit-presentment</rev:transactionType>\n" +
                    "  <rev:refNumber>"+doc.getElementsByTagName("refNumber").item(0).getTextContent()+"</rev:refNumber>\n" +
                    "  <rev:stan>"+doc.getElementsByTagName("stan").item(0).getTextContent()+"</rev:stan>\n" +
                    "  <rev:authorizationCode>"+doc.getElementsByTagName("authorizationCode").item(0).getTextContent()+"</rev:authorizationCode>\n"+
                    "  <rev:trand>"+doc.getElementsByTagName("transactionDateTime").item(0).getTextContent()+"</rev:trand>";


            return refMDP;
        } else {
            return error_response;
        }

    }


    private String Meeza_Reverse_IPN_Transaction(String unique_body,String account,Double amount ) throws Exception{

        String error_response = "Transaction_Refused";
        String refMDP,body = null;
        Document doc;

        body = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:rev=\"http://ws.wso2.org/dataservice/ReversalAccountPresentmentIPN\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <rev:_postreversalaccountpresentmentipn>\n" +
                "         <rev:cardNumber>"+account+"</rev:cardNumber>\n" +
                "         <rev:amount>"+amount+"</rev:amount>\n" +
                unique_body.replaceAll("trand","transactionDateTime")+

                "      </rev:_postreversalaccountpresentmentipn>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";


        String soapAction = "postreversalaccountpresentmentipn";
        String res = Request.SendRequest(env.getProperty("MDPBaseURL"), soapAction, body);
        Logging.host("Host response -------------" + res);
        doc = XMLParser.ReadXML(res);
        String innerXml = doc.getElementsByTagName("response").item(0).getTextContent();
        doc = XMLParser.ReadXML(innerXml);


        if (res.contains("refNumber")) {

            refMDP = doc.getElementsByTagName("refNumber").item(0).getTextContent();

            return refMDP;
        } else {
            return error_response;
        }

    }


    public  void Fraud_FT(String transactionId,String requestId,String timestamp,String CIF,String debitAccount, String balance,String DebitMobileNumber,String orgValue,String CreditMobileNumber,String creditAccountId,String DebitorName) throws ParseException {

        String FraudBody="";

        DateFormat inputFormat =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        DateFormat dateoutputFormat =
                new SimpleDateFormat("yyyyMMdd");
        DateFormat timeoutputFormat =
                new SimpleDateFormat("HH:mm:ss");
        String inputdate = timestamp;
        String inputtime = timestamp;
        Date date = inputFormat.parse(inputdate);
        Date time = inputFormat.parse(inputtime);
        inputdate = dateoutputFormat.format(date);
        inputtime = timeoutputFormat.format(time);
        try {
            //fraudBody
            FraudBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <dat:AFDOMESTICFTCONTRACT>\n" +
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
                    "\t\t\t<dat:smh_resp_req>1</dat:smh_resp_req>\n" +
                    "\t\t\t<dat:smh_sdd_ind>1</dat:smh_sdd_ind>\n" +
                    "\t\t\t<dat:smh_dest>SFME</dat:smh_dest>\n" +
                    "\t\t\t<dat:smh_multi_org_name>GLOBAL</dat:smh_multi_org_name>\n" +
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
                    "\t\t\t<dat:channeltype>IPN</dat:channeltype>\n" +
                    "\t\t\t<dat:DRVDT>" + inputdate + "</dat:DRVDT>\n" +
                    "\t\t\t<dat:rqo_tran_time>" + inputtime + "</dat:rqo_tran_time>\n" +
                    "\t\t\t<dat:rqo_tran_date_alt>" + inputdate + "</dat:rqo_tran_date_alt>\n" +
                    "\t\t\t<dat:rqo_tran_time_alt>" + inputtime + "</dat:rqo_tran_time_alt>\n" +
                    "\t\t\t<dat:CUSTNO>IPN-" + CIF + "</dat:CUSTNO>\n" +
                    "\t\t\t<dat:DBACCOUNT>" + debitAccount + "</dat:DBACCOUNT>\n" +
                    "\t\t\t<dat:DRCCY>EGP</dat:DRCCY>\n" +
                    "\t\t\t<dat:CODBRANCH>11029</dat:CODBRANCH>\n" +
                    "\t\t\t<dat:AvailableBal>" + balance + "</dat:AvailableBal>\n" +
                    "\t\t\t<dat:username>" + DebitMobileNumber + "</dat:username>\n" +
                    "\t\t\t<dat:hqo_entity_use_ob_userid>H</dat:hqo_entity_use_ob_userid>\n" +
                    "\t\t\t<dat:hqo_device_id>IPN</dat:hqo_device_id>\n" +
                    "\t\t\t<dat:hob_ip_address>84.36.99.114</dat:hob_ip_address>\n" +
                    "\t\t\t<dat:hob_ip_cntry_code>84.36.99.114</dat:hob_ip_cntry_code>\n" +
                    "\t\t\t<dat:hob_match_ind>Y</dat:hob_match_ind>\n" +
                    "\t\t\t<dat:hob_website_name>IPN.adcb.com.eg</dat:hob_website_name>\n" +
                    "\t\t\t<dat:hob_webpage_code>NO</dat:hob_webpage_code>\n" +
                    "\t\t\t<dat:DRAMT>" + orgValue + "</dat:DRAMT>\n" +
                    "\t\t\t<dat:CRAMT>" + orgValue + "</dat:CRAMT>\n" +
                    "\t\t\t<dat:CRCCY1>EGP</dat:CRCCY1>\n" +
                    "\t\t\t<dat:transactiontype>Y</dat:transactiontype>\n" +
                    "\t\t\t<dat:BenfID>" + CreditMobileNumber + "</dat:BenfID>\n" +
                    "\t\t\t<dat:CRACCOUNT>" + creditAccountId + "</dat:CRACCOUNT>\n" +
                    "\t\t\t<dat:tpp_bank_cntry_code>EG</dat:tpp_bank_cntry_code>\n" +
                    "\t\t\t<dat:tpp_bank_city>GIZA</dat:tpp_bank_city>\n" +
                    "\t\t\t<dat:CRCCY>EGP</dat:CRCCY>\n" +
                    "\t\t\t<dat:XREF>2</dat:XREF>\n" +
                    "\t\t\t<dat:INTRMKS>ADCB P2P - SendMony</dat:INTRMKS>\n" +
                    "\t\t\t<dat:tpp_name>IPN-" + DebitorName + "</dat:tpp_name>\n" +
                    "\t\t\t<dat:tpp_bank_name>ADCB</dat:tpp_bank_name>\n"+
                    "      </dat:AFDOMESTICFTCONTRACT>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope>";
            String response=Request.SendRequest(env.getProperty("IPNBaseURL"), "AFDOMESTICFTCONTRACT", FraudBody);
            Logging.host("TransactionID:"+transactionId+"||RequestID:"+requestId+"||Request{"+FraudBody+"} ||Response{"+response+"}");

        }
        catch(Exception ex){
            Logging.host(FraudBody +"++"+ ex.getMessage());

        }

    }



}















