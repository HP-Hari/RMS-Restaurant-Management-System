package com.rms.operations.controller;

import com.rms.operations.model.Table;
import com.rms.operations.repository.TableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tables")
@CrossOrigin(origins = "*")
public class TableController {

    @Autowired
    private TableRepository tableRepository;

    @GetMapping
    public List<Table> getAllTables() {
        return tableRepository.findAll();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Table> updateTable(@PathVariable String id, @RequestBody Table tableDetails) {
        Optional<Table> optionalTable = tableRepository.findById(id);
        if (optionalTable.isPresent()) {
            Table table = optionalTable.get();
            if (tableDetails.getIs_occupied() != null) {
                table.setIs_occupied(tableDetails.getIs_occupied());
            }
            return ResponseEntity.ok(tableRepository.save(table));
        }
        return ResponseEntity.notFound().build();
    }
}
