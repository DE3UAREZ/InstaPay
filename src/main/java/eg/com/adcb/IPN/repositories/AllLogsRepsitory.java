package eg.com.adcb.IPN.repositories;
import antlr.collections.List;
import eg.com.adcb.IPN.models.AllLogs;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import java.util.*;
import javax.transaction.Transactional;

public interface AllLogsRepsitory extends CrudRepository<AllLogs, Long> {
    @Query(value = "select  status from all_logs where requestId=:reqId order by date desc ")
    String [] getResp(String reqId);

    @Query(value = "select  request from all_logs where requestId=:reqId order by date desc ")
    String [] getReq(String reqId);

    @Query(value = "select  response from all_logs where requestId=:reqId order by date desc ")
    String [] getResDetails(String reqId);


    @Query(value = "select  requestId from all_logs where requestId=:reqId order by date desc ")
    String [] getDuplicateRequest(String reqId);

    @Query(value = "select  TransactionRefrence from all_logs where requestId=:reqId and request like %:account% and TransactionRefrence like %:rrn% order by date desc ")
    String [] getMeezaReversal(String reqId, String account, String rrn);


}
//SELECT *
//        into AllLogs
//        FROM [ADCB_IPN].[dbo].[RejectedLogs]
//
//        UNION ALL
//
//        SELECT *
//        FROM [ADCB_IPN].[dbo].[AcceptedLogs]
//
//
//        select  top 1 * from [ADCB_IPN].[dbo].AllLogs where cast(RequestId as nvarchar(max))=cast('EBC1e608143ae054e07ac58d4a2ec3ca22c22' as nvarchar(max)) order by Timestamp DESC