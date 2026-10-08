package co.edu.unbosque.reservasbackend.sql.service;

import co.edu.unbosque.reservasbackend.sql.entity.Cliente;
import co.edu.unbosque.reservasbackend.sql.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    public Cliente crearCliente(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    public List<Cliente> obtenerTodos() {
        return clienteRepository.findAll();
    }
}
