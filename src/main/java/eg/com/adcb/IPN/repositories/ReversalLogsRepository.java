package eg.com.adcb.IPN.repositories;

import eg.com.adcb.IPN.models.ReversalLog;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface ReversalLogsRepository extends CrudRepository<ReversalLog, Long> {
    @Query(value = "select  l.response from ReversalLogs l where l.requestId=:reqId order by l.date desc ")
    String [] getFt(String reqId);



}
