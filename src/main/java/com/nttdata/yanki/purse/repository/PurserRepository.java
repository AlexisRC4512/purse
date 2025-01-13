package com.nttdata.yanki.purse.repository;

import com.nttdata.yanki.purse.model.entity.PurseEntity;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface PurserRepository extends ReactiveMongoRepository<PurseEntity,String> {
    Mono<PurseEntity> findByDebitCardNumber(String debitCardNumber);
    Mono<PurseEntity> findByPhoneNumber(String phoneNumber);
    @Query("{ 'payments.id': ?0 }")
    Mono<PurseEntity> findByPaymentId(String paymentId);
}
