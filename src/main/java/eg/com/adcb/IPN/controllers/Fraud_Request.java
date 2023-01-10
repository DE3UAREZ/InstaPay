package eg.com.adcb.IPN.controllers;

import eg.com.adcb.IPN.consts.Logging;
import eg.com.adcb.IPN.utils.Request;
import org.springframework.core.env.Environment;

public class Fraud_Request {

    private static Environment env;


    public static void Fraud_FT(String timestamp,String CIF,String debitAccount, String balance,String DebitMobileNumber,String orgValue,String CreditMobileNumber,String creditAccountId,String ref,String DebitorName){

        String FraudBody="";
try {
    //fraudBody
      FraudBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://ws.wso2.org/dataservice\">\n" +
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
            "\t\t\t<dat:DRVDT>" + timestamp + "</dat:DRVDT>\n" +
            "\t\t\t<dat:rqo_tran_time>" + timestamp + "</dat:rqo_tran_time>\n" +
            "\t\t\t<dat:rqo_tran_date_alt>" + timestamp + "</dat:rqo_tran_date_alt>\n" +
            "\t\t\t<dat:rqo_tran_time_alt>" + timestamp + "</dat:rqo_tran_time_alt>\n" +
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
            "\t\t\t\n" +
            "\t\t\t<dat:CRCCY>EGP</dat:CRCCY>\n" +
            "\t\t\t<dat:XREF>" + ref + "</dat:XREF>\n" +
            "\t\t\t<dat:INTRMKS>ADCB P2P - SendMony</dat:INTRMKS>\n" +
            "\t\t\t<dat:tpp_name>IPN-" + DebitorName + "</dat:tpp_name>\n" +
            "      </dat:AFINTERNALFTCONTRACT>\n" +
            "   </soapenv:Body>\n" +
            "</soapenv:Envelope>";
    Request.SendRequest(env.getProperty("IPNBaseURL"), "AFINTERNALFTCONTRACT", FraudBody);

}
catch(Exception ex){
   Logging.warn(FraudBody +"++"+ ex.getStackTrace());

}

    }



}
