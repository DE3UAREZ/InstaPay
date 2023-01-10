package eg.com.adcb.IPN.repositories;
import eg.com.adcb.IPN.models.Errors;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;

public interface ErrorsRepository extends CrudRepository<Errors, Long> {
    @Query(value = "SELECT e.ErrorDesc FROM Errors e WHERE e.ReturnedDesc =:ReqMsg")
    String getErrorDes(String ReqMsg);

    @Query(value = "SELECT e.ErrorCode FROM Errors e WHERE e.ReturnedDesc =:ReqMsg")
    String getErrorCode(String ReqMsg);
}
