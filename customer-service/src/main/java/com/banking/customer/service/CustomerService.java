package com.banking.customer.service;

import com.banking.customer.entity.CustomerEntity;
import com.banking.customer.repository.CustomerRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private final CustomerRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public CustomerService(CustomerRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }
    


    public Flux<CustomerEntity> getAllCustomers() {
        return Flux.fromIterable(repository.findAll());
    }

    public Mono<CustomerEntity> saveCustomer(CustomerEntity customer) {
        return Mono.just(repository.save(customer));
    }

    public CustomerEntity create(CustomerEntity customer) {
        customer.setKycStatus("PENDING");
        CustomerEntity saved = repository.save(customer);
        kafkaTemplate.send("kyc-events", saved.toString() + ":" + customer.getPhone() + ":" + customer.getEmail());
        return saved;
    }

    public void submitKyc(Long id, String docs) {
        CustomerEntity customer = repository.findById(id).orElseThrow();
        customer.setKycDocs(docs);
        customer.setKycStatus("PENDING");
        repository.save(customer);
        kafkaTemplate.send("kyc-events", customer.toString() + ":" + customer.getPhone() + ":" + customer.getEmail());
    }

    public void approveKyc(Long id) {
        CustomerEntity customer = repository.findById(id).orElseThrow();
        customer.setKycStatus("APPROVED");
        repository.save(customer);
        kafkaTemplate.send("notification-events", "KYC_APPROVED:" + "KYC approved for customer " + id + ":" + customer.getPhone() + ":" + customer.getEmail());
    }

    public CustomerEntity getById(Long id) {
        return repository.findById(id).orElseThrow();
    }
    
    
}