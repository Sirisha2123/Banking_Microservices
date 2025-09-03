package com.banking.customer.controller;

import com.banking.customer.entity.CustomerEntity;
import com.banking.customer.service.CustomerService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService service;

    @Autowired
    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @PostMapping
    public CustomerEntity create(@RequestBody CustomerEntity customer) {
        return service.create(customer);
    }

    @PostMapping("/{id}/kyc")
    public void submitKyc(@PathVariable Long id, @RequestBody String docs) {
        service.submitKyc(id, docs);
    }

    @PutMapping("/{id}/approve-kyc")
    public void approveKyc(@PathVariable Long id) {
        service.approveKyc(id);
    }

    @GetMapping("/{id}")
    public CustomerEntity get(@PathVariable Long id) {
        return service.getById(id);
    }
    
    @GetMapping(produces = "application/json")
    public Flux<CustomerEntity> getAllCustomers() {
        return service.getAllCustomers();
    }

    @PostMapping(consumes = "application/json", produces = "application/json")
    public Mono<CustomerEntity> createCustomer(@RequestBody CustomerEntity customer) {
        return service.saveCustomer(customer);
    }
}