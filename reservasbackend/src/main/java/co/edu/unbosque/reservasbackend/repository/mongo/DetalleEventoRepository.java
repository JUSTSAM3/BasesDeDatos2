package co.edu.unbosque.reservasbackend.repository.mongo;

import co.edu.unbosque.reservasbackend.model.mongo.DetalleEvento;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DetalleEventoRepository extends MongoRepository<DetalleEvento, String> {

    Optional<DetalleEvento> findByIdReserva(Integer idReserva);

    List<DetalleEvento> findByIdCliente(Integer idCliente);

    List<DetalleEvento> findByTipoEvento(String tipoEvento);
}
