package com.java360.agendei;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExemploController {

    @GetMapping("/ok")
    public ResponseEntity<String> sayOk(){
        return ResponseEntity.ok("tudo certo");
    }

    @PostMapping("/echo")
    public ResponseEntity<String> echo(@RequestBody String value){
        StringBuilder sb = new StringBuilder(value);
        return ResponseEntity.ok(sb.reverse().toString());
    }


}
