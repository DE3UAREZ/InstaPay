package eg.com.adcb.IPN.repositories;

import eg.com.adcb.IPN.models.RejectedLog;
import org.springframework.data.repository.CrudRepository;

public interface RejectedLogsRepository extends CrudRepository<RejectedLog, Long> {
}
