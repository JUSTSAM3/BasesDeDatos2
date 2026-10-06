package co.edu.unbosque.reservasbackend.repository.mongo;

import co.edu.unbosque.reservasbackend.model.mongo.Resena;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResenaRepository extends MongoRepository<Resena, String> {

    Optional<Resena> findByIdReserva(Integer idReserva);

    List<Resena> findByIdCliente(Integer idCliente);

    List<Resena> findByIdEspacio(Integer idEspacio);
}
