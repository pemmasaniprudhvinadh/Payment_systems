package com.example.loan.controller;

import com.example.loan.service.LoanService;
import com.example.loan.model.Loan;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    public ResponseEntity<?> createLoan(@RequestBody Map<String, Object> body) {
        String userId = (String) body.get("userId");
        Double amount = Double.valueOf(body.get("amount").toString());
        com.example.loan.model.Loan loan = loanService.createLoan(userId, amount);
        return ResponseEntity.ok(loan);
    }

    @GetMapping
    public ResponseEntity<?> listLoans() {
        return ResponseEntity.ok(loanService.listLoans());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getLoan(@PathVariable String id) {
        return loanService.getLoan(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateLoan(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            Double amount = body.get("amount") == null ? null : Double.valueOf(body.get("amount").toString());
            String status = (String) body.get("status");
            com.example.loan.model.Loan updated = loanService.updateLoan(id, amount, status);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLoan(@PathVariable String id) {
        loanService.deleteLoan(id);
        return ResponseEntity.noContent().build();
    }
}
