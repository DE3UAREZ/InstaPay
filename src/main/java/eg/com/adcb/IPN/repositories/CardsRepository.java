package eg.com.adcb.IPN.repositories;

import eg.com.adcb.IPN.models.Card;
import org.springframework.data.repository.CrudRepository;

public interface CardsRepository<M, L extends Number> extends CrudRepository<Card, Long> {
}
