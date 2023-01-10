package eg.com.adcb.IPN.repositories;

import eg.com.adcb.IPN.models.AcceptedLog;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface AcceptedLogsRepository extends CrudRepository<AcceptedLog, Long> {
    @Query(value = "select  l.response from AcceptedLogs l where l.requestId=:reqId order by l.date desc ")
    String [] getFt(String reqId);

    @Query(value = "select  l.TransactionRefrence from AcceptedLogs l where l.requestId=:reqId order by l.date desc ")
    String [] getTransactionRefrence(String reqId);
}
