package co.edu.unbosque.reservasbackend.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class TestController {

    @GetMapping("/api/test")
    public String test() {
        return "Conexion Angular Spring correcta";
    }
}