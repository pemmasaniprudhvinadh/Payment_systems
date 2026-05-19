package com.example.loan.service;

import com.example.loan.model.Loan;
import com.example.loan.repository.LoanRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class LoanService {
    private final LoanRepository repository;

    public LoanService(LoanRepository repository) {
        this.repository = repository;
    }

    public Loan createLoan(String userId, Double amount) {
        String id = UUID.randomUUID().toString();
        Loan loan = new Loan(id, userId, amount, "PENDING");
        return repository.save(loan);
    }

    public List<Loan> listLoans() { return repository.findAll(); }

    public Optional<Loan> getLoan(String id) { return repository.findById(id); }

    public Loan updateLoan(String id, Double amount, String status) {
        return repository.findById(id).map(l -> {
            l.setAmount(amount);
            l.setStatus(status);
            return repository.save(l);
        }).orElseThrow(() -> new RuntimeException("Loan not found: " + id));
    }

    public void deleteLoan(String id) { repository.deleteById(id); }
}
