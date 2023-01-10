package eg.com.adcb.IPN.repositories;

import eg.com.adcb.IPN.models.Login;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;

public interface LoginsRepository extends CrudRepository<Login, Long> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM Logins l WHERE l.customerReference = :customerReference")
    int deleteByCustomerReference(@Param("customerReference") String customerReference);

    @Query(value = "FROM Logins l WHERE l.approvalToken = :approvalToken")
    Login getLoginByToken(String approvalToken);

    @Query(value = "FROM Logins l WHERE l.mobileNumber = :mobileNumber")
    Login getLoginByMobileNumber(String mobileNumber);

    @Query(value = "FROM Logins l WHERE l.customerReference = :customerReference")
    Login getLoginByCustomerReference(String customerReference);

    @Query(value = "SELECT l.requestId FROM Logins l WHERE l.requestId = :rid")
    String getLoginByRequestID(String rid);

    @Query(value = "SELECT l.accountid FROM Logins l WHERE l.mobileNumber = :mobileNumber")
    String getLastAuthAccountId(String mobileNumber);

    @Query(value = "SELECT l.approvalToken FROM Logins l WHERE l.requestId = :reqId")
    String getToken(String reqId);

    @Query(value = "SELECT l.mobileNumber FROM Logins l WHERE l.mobileNumber = :mn")
    String checkMobileNumber(String mn);

}
