package eg.com.adcb.IPN.repositories;

import eg.com.adcb.IPN.models.mfields;
import org.springframework.data.jpa.repository.Query;

public interface mfieldsRepository extends CardsRepository<mfields,Long> {

    @Query(value = "SELECT m.field_req FROM mfields m WHERE m.field_name =:fname AND m.field_req=:frequest")
    String checkMandatoryFields(String fname,String frequest);


}
