package com.nttdata.yanki.purse.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.nttdata.yanki.purse.exception.PaymentDataException;
import com.nttdata.yanki.purse.exception.PurseNotFoundException;
import com.nttdata.yanki.purse.model.entity.Payment;
import com.nttdata.yanki.purse.model.entity.PurseEntity;
import com.nttdata.yanki.purse.model.events.CompleteEvent;
import com.nttdata.yanki.purse.model.events.PaymentEvent;
import com.nttdata.yanki.purse.model.events.PaymentStatusEvent;
import com.nttdata.yanki.purse.model.events.TransactionEvent;
import com.nttdata.yanki.purse.model.request.PaymentRequest;
import com.nttdata.yanki.purse.model.request.PurseRequest;
import com.nttdata.yanki.purse.model.response.PaymentResponse;
import com.nttdata.yanki.purse.model.response.PurseResponse;
import com.nttdata.yanki.purse.repository.PurserRepository;
import com.nttdata.yanki.purse.service.CatalogCacheService;
import com.nttdata.yanki.purse.service.PurserService;
import com.nttdata.yanki.purse.util.PaymentConverter;
import com.nttdata.yanki.purse.util.PurseMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.json.JSONObject;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.adapter.rxjava.RxJava3Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.nttdata.yanki.purse.util.constants.ConstantPurse.*;


@Log4j2
@RequiredArgsConstructor
@Service
public class PurseServiceImpl implements PurserService {
    private final PurserRepository purserRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final CatalogCacheService catalogCacheService;

    @CircuitBreaker(name = "purse", fallbackMethod = "fallbackCreatePurse")
    @Override
    public Maybe<PurseResponse> insert(PurseRequest purseRequest ,String authorizationHeader) {
        return purserRepository.findByDebitCardNumber(purseRequest.getDebitCardNumber())
                .flatMap(purseEntity -> {
                    if (purseEntity.getDebitCardNumber() != null) {
                        return Mono.error(new PurseNotFoundException("The purse with debit card number is in use: " + purseRequest.getDebitCardNumber()));
                    }
                    return Mono.just(purseEntity);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    PurseEntity newPurseEntity = PurseMapper.toPurseEntity(purseRequest);
                    if (newPurseEntity.isAssociateDebitCard()) {
                        String message = createKafkaMessage(newPurseEntity,authorizationHeader);
                        kafkaTemplate.send("debit-card-topic-create", message);
                        log.info("Message sent to Kafka: {}", message);
                    }
                    return purserRepository.save(newPurseEntity);
                }))
                .map(PurseMapper::toPurseResponse)
                .as(RxJava3Adapter::monoToMaybe);
    }

    private String createKafkaMessage(PurseEntity purseEntity, String authorizationHeader) {
        return "{\"dni\": \"" + purseEntity.getDocumentNumber() + "\", " +
                "\"cardNumber\": \"" + purseEntity.getDebitCardNumber() + "\", " +
                "\"authorization\": \"" + authorizationHeader + "\"}";
    }


    @CircuitBreaker(name = "purse", fallbackMethod = "fallbackGetAllPurse")
    @Override
    public Flowable<PurseResponse> findAll() {
        return RxJava3Adapter.fluxToFlowable(purserRepository.findAll().map(PurseMapper::toPurseResponse)
                .doOnError(e -> log.error("Error fetching purse : {}",e))
                .onErrorMap(e -> new Exception("Error fetching purse", e)));
    }
    @CircuitBreaker(name = "purse", fallbackMethod = "fallbackGetPurseById")
    @Override
    public Maybe<PurseResponse> findById(String purseId) {
        return RxJava3Adapter.monoToMaybe(
                catalogCacheService.getCatalog(purseId)
                        .doOnNext(value -> log.debug("Valor obtenido de Redis para la clave {}: {}", purseId, value))
                        .switchIfEmpty(purserRepository.findById(purseId)
                                .flatMap(purseEntity -> catalogCacheService.setCatalog(purseId, purseEntity)
                                        .doOnSuccess(success -> log.debug("Valor guardado en Redis para la clave {}: {}", purseId, purseEntity))
                                        .thenReturn(purseEntity)))
                        .map(purseEntity -> PurseMapper.toPurseResponse((PurseEntity) purseEntity))
        );
    }
    @CircuitBreaker(name = "purse", fallbackMethod = "fallbackUpdatePurse")
    @Override
    public Maybe<PurseResponse> updatePurse(String purseId, PurseRequest purseRequest) {

        return RxJava3Adapter.monoToMaybe( purserRepository.findById(purseId)
                .switchIfEmpty(Mono.error(new PurseNotFoundException("No purse found with " + purseId)))
                .flatMap(existingPurse -> {
                    catalogCacheService.setCatalog(purseId,existingPurse);
                    PurseEntity updatedPurse = PurseMapper.toPurseEntity(purseRequest);
                    updatedPurse.setId(existingPurse.getId());
                    return purserRepository.save(updatedPurse);
                })
                .map(PurseMapper::toPurseResponse)
        );
    }
    @CircuitBreaker(name = "purse", fallbackMethod = "fallbackDeleteById")
    @Override
    public Completable deletePurse(String purseId) {
        return RxJava3Adapter.monoToCompletable(purserRepository.deleteById(purseId).then(
                catalogCacheService.deleteCatalog(purseId)
                )
                .switchIfEmpty(Mono.error(new PurseNotFoundException("No purse found with" + purseId)))
                .doOnError(e -> log.error("Error delete purse with id: {}", purseId, e))
                .onErrorMap(e -> new Exception("Error delete purse by id", e)));
    }
    @CircuitBreaker(name = "purse", fallbackMethod = "fallbackPayment")
    @Override
    public Maybe<PaymentResponse> payByCreditId(String idPurse, String numberPhone, PaymentRequest paymentRequest,String authorizationHeader) {
        return RxJava3Adapter.monoToMaybe(
                purserRepository.findByPhoneNumber(numberPhone)
                        .flatMap(purseEntity -> purserRepository.findById(idPurse)
                                .flatMap(purseById -> checkBalance(purseById, paymentRequest)
                                        .then(Mono.defer(() -> {
                                            List<PurseEntity> purseEntityList = Arrays.asList(purseEntity, purseById);
                                            List<String> listOfDebitCardNumbers = getDebitCardNumbers(purseEntityList);
                                            return processPayment(purseById, purseEntity, paymentRequest, listOfDebitCardNumbers ,authorizationHeader);
                                        }))))
                        .doOnError(e -> log.error("Error processing payment: {}", e.getMessage()))
        );
    }

    @KafkaListener(id = "myConsumer", topics = "purse-balance-topic", groupId = "springboot-group-1", autoStartup = "true")
    public void listen(String message) {
        log.info(KAFKA_MESSAGE, message);
        JSONObject jsonObject = new JSONObject(message);
        String cardNumber = jsonObject.getString("cardNumber");
        if (jsonObject.has("balance")) {
            double balance = jsonObject.getDouble("balance");
            updatePurseBalance(cardNumber, balance);
        } else if (jsonObject.has("error")) {
            handleCompensation(cardNumber);
        }
    }
    @KafkaListener(id = "myConsumer1", topics = "debit-card-topic-pay-read", groupId = "springboot-group-1", autoStartup = "true")
    public void listenMessagePay(String message) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            log.info(KAFKA_MESSAGE, message);
            PaymentStatusEvent paymentStatusEvent = objectMapper.readValue(message, PaymentStatusEvent.class);
            processPaymentStatusEvent(paymentStatusEvent);
        } catch (Exception e) {
            log.error("Error processing message: {}", message, e);
        }
    }

    @KafkaListener(id = "myConsumer2", topics = "bootcoin-pay-yanki", groupId = "springboot-group-1", autoStartup = "true")
    public void listenYanki(String message) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Gson gson = new Gson();
        TransactionEvent transactionEvent = objectMapper.readValue(message, TransactionEvent.class);
        log.info(KAFKA_MESSAGE, message);
        completeTransaction(transactionEvent);
        CompleteEvent completeEvent = new CompleteEvent();
        completeEvent.setIdPay(transactionEvent.getIdPay());
        completeEvent.setIdPurseBuy(transactionEvent.getIdPurseBuy());
        completeEvent.setIdPurseSeller(transactionEvent.getIdPurseSeller());
        completeEvent.setDate(transactionEvent.getDate());
        completeEvent.setAmount(transactionEvent.getAmount());
        completeEvent.setIdTransaction(transactionEvent.getIdTransaction());

        purserRepository.findByPhoneNumber(transactionEvent.getNumberPhoneSeller())
                .flatMap(purseEntity -> {
                    completeEvent.setState("Success");
                    purseEntity.setBalance(purseEntity.getBalance() + transactionEvent.getAmount());
                    Payment payment = new Payment();
                    payment.setState(completeEvent.getState());
                    payment.setAmount(completeEvent.getAmount());
                    payment.setDescription("Bootcoin");
                    payment.setId(completeEvent.getIdPay());
                    payment.setDate(completeEvent.getDate());
                    addPayment(purseEntity, payment);
                    log.info("Updated purse entity: {}", purseEntity);
                    String completeEventJson = gson.toJson(completeEvent);
                    log.info("Sending message to Kafka: {}", completeEventJson);
                    kafkaTemplate.send("bootcoin-pay-yanki-complete", completeEventJson);
                    return purserRepository.save(purseEntity);
                })
                .doOnError(e -> {
                    log.error("Error saving purse: {}", e.getMessage());
                    sendErrorEvent(transactionEvent, gson);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("Error processing message: {}", transactionEvent);
                    sendErrorEvent(transactionEvent, gson);
                    return Mono.empty();
                }))
                .subscribe();
    }
    private void sendErrorEvent(TransactionEvent transactionEvent, Gson gson) {
        CompleteEvent errorEvent = new CompleteEvent();
        errorEvent.setIdPay(transactionEvent.getIdPay());
        errorEvent.setIdPurseBuy(transactionEvent.getIdPurseBuy());
        errorEvent.setIdPurseSeller(transactionEvent.getIdPurseSeller());
        errorEvent.setDate(transactionEvent.getDate());
        errorEvent.setAmount(0.0);
        errorEvent.setState(ERROR_MESSAGE);
        errorEvent.setIdTransaction(transactionEvent.getIdTransaction());
        kafkaTemplate.send("bootcoin-pay-yanki-complete", gson.toJson(errorEvent));
    }
    private void completeTransaction(TransactionEvent transactionEvent) {
        purserRepository.findByPhoneNumber(transactionEvent.getNumberPhoneSeller()).flatMap(purseEntity -> {
            purseEntity.setBalance(purseEntity.getBalance() + transactionEvent.getAmount());
            Payment payment = new Payment();
            payment.setId(UUID.randomUUID().toString());
            payment.setAmount(transactionEvent.getAmount());
            payment.setState(COMPLETE);
            payment.setDate(new Date());
            payment.setDescription("Bootcoin payment");
            addPayment(purseEntity,payment);

            return purserRepository.save(purseEntity);
        });
    }
    private void updatePurseBalance(String cardNumber, double balance) {
        purserRepository.findByDebitCardNumber(cardNumber)
                .flatMap(purse -> {
                    purse.setBalance(balance);
                    return purserRepository.save(purse);
                })
                .doOnError(e -> log.error("Error updating balance for card number: {}", cardNumber, e))
                .subscribe();
    }

    private void handleCompensation(String cardNumber) {
        purserRepository.findByDebitCardNumber(cardNumber)
                .flatMap(purse -> {
                    purse.setBalance(0);
                    return purserRepository.save(purse);
                })
                .doOnError(e -> log.error("Error handling compensation for card number: {}", cardNumber, e))
                .subscribe();
    }
    private void processPaymentStatusEvent(PaymentStatusEvent paymentStatusEvent) {
        Flux.fromIterable(paymentStatusEvent.getDebitCardNumbers())
                .flatMap(debitCardNumber -> processDebitCardNumber(debitCardNumber, paymentStatusEvent))
                .doOnError(e -> log.error("Error processing payment status event: {}", paymentStatusEvent, e))
                .subscribe();
    }


    private Mono<PurseEntity> processDebitCardNumber(String debitCardNumber, PaymentStatusEvent paymentStatusEvent) {
        return purserRepository.findByDebitCardNumber(debitCardNumber)
                .flatMap(purseEntity -> processPurseEntity(purseEntity, paymentStatusEvent));
    }

    private Mono<PurseEntity> processPurseEntity(PurseEntity purseEntity, PaymentStatusEvent paymentStatusEvent) {
        List<Payment> filteredPayments = filterPayments(purseEntity, paymentStatusEvent);
        if (filteredPayments.isEmpty()) {
            return Mono.error(new RuntimeException("No payments found for the provided transaction IDs"));
        }
        return Flux.fromIterable(filteredPayments)
                .flatMap(payment -> processPayment(purseEntity, payment, paymentStatusEvent))
                .last()
                .flatMap(purserRepository::save);
    }

    private List<Payment> filterPayments(PurseEntity purseEntity, PaymentStatusEvent paymentStatusEvent) {
        return purseEntity.getPayments().stream()
                .filter(payment -> paymentStatusEvent.getListTransactionId().contains(payment.getId()))
                .collect(Collectors.toList());
    }

    private Mono<PurseEntity> processPayment(PurseEntity purseEntity, Payment payment, PaymentStatusEvent paymentStatusEvent) {
        if ("Complete".equalsIgnoreCase(paymentStatusEvent.getState())) {
            payment.setState(COMPLETE);
            adjustBalanceForCompleteState(purseEntity, payment);
        } else if ("Error".equalsIgnoreCase(paymentStatusEvent.getState())) {
            payment.setState(ERROR_MESSAGE);
            adjustBalanceForErrorState(purseEntity, payment);
        }
        return Mono.just(purseEntity);
    }

    private void adjustBalanceForCompleteState(PurseEntity purseEntity, Payment payment) {
        if (purseEntity.getId().equals(payment.getId())) {
            purseEntity.setBalance(purseEntity.getBalance() - payment.getAmount());
        }
        if (purseEntity.getId().equals(payment.getId())) {
            purseEntity.setBalance(purseEntity.getBalance() + payment.getAmount());
        }
    }

    private void adjustBalanceForErrorState(PurseEntity purseEntity, Payment payment) {
        if (purseEntity.getId().equals(payment.getId())) {
            purseEntity.setBalance(purseEntity.getBalance() + payment.getAmount());
        }
        if (purseEntity.getId().equals(payment.getId())) {
            purseEntity.setBalance(purseEntity.getBalance() - payment.getAmount());
        }
    }

    private PaymentEvent createPaymentEvent( PaymentRequest paymentRequest,List<String> listDebitCardNumber,String idPay,List<String>listPayments,String authorizationHeader) {
        PaymentEvent paymentEvent = new PaymentEvent();
        paymentEvent.setIdPay(idPay);
        paymentEvent.setEventType("PAYMENT");
        paymentEvent.setAmount(paymentRequest.getAmount());
        paymentEvent.setListTransactionId(listPayments);
        paymentEvent.setDebitCardNumbers(listDebitCardNumber);
        paymentEvent.setAuthorizationHeader(authorizationHeader);
        return paymentEvent;
    }
    private void addPayment(PurseEntity purseEntity, Payment payment) {
        List<Payment> payments = Optional.ofNullable(purseEntity.getPayments())
                .orElseGet(() -> {
                    List<Payment> newPayments = new ArrayList<>();
                    purseEntity.setPayments(newPayments);
                    return newPayments;
                });
        payments.add(payment);
    }
    private Mono<Void> checkBalance(PurseEntity purseById, PaymentRequest paymentRequest) {
        if (purseById.getBalance() < paymentRequest.getAmount()) {
            return Mono.error(new PaymentDataException("Payment amount exceeds outstanding balance"));
        }
        return Mono.empty();
    }

    private List<String> getDebitCardNumbers(List<PurseEntity> purseEntityList) {
        List<String> listOfDebitCardNumbers = new ArrayList<>();
        for (PurseEntity purse : purseEntityList) {
            if (purse.isAssociateDebitCard()) {
                listOfDebitCardNumbers.add(purse.getDebitCardNumber());
            }
        }
        return listOfDebitCardNumbers;
    }

    private Mono<PaymentResponse> processPayment(PurseEntity purseById, PurseEntity purseEntity, PaymentRequest paymentRequest
            , List<String> listOfDebitCardNumbers ,String authorizationHeader) {
        String idAument = UUID.randomUUID().toString();
        String idDescount = UUID.randomUUID().toString();
        AtomicReference<String> statePayDeposit = new AtomicReference<>("INITIAL");
        AtomicReference<String> statePayAument = new AtomicReference<>("INITIAL");

        List<String>listPaymentsID = new ArrayList<>();
        listPaymentsID.add(idAument);
        listPaymentsID.add(idDescount);
        if (!listOfDebitCardNumbers.isEmpty()) {
            Gson gson = new Gson();
            PaymentEvent paymentEvent = createPaymentEvent(paymentRequest, listOfDebitCardNumbers,idDescount,listPaymentsID,authorizationHeader);
            String jsonPaymentEvent = gson.toJson(paymentEvent);
            kafkaTemplate.send("debit-card-topic-pay-write", jsonPaymentEvent);
            log.info("Message sent to Kafka: {}", jsonPaymentEvent);
        }
        purseById.setBalance(purseById.getBalance() - paymentRequest.getAmount());
        return purserRepository.save(purseById)
                .then(purserRepository.findById(purseEntity.getId())
                        .flatMap(purseEntityUpdated -> {
                            if (purseEntityUpdated.isAssociateDebitCard()) {
                                statePayDeposit.set(COMPLETE);
                            }
                            if (purseById.isAssociateDebitCard()) {
                                statePayAument.set(COMPLETE);
                            }
                            Payment paymentDescont = new Payment(idDescount, paymentRequest.getAmount(), new Date(), "you pay: " + paymentRequest.getAmount(), statePayDeposit.get());
                            Payment paymentAument = new Payment(idAument, paymentRequest.getAmount(), new Date(), "you receive: " + paymentRequest.getAmount(), statePayAument.get());
                            addPayment(purseById, paymentDescont);
                            addPayment(purseEntityUpdated, paymentAument);
                            purseEntityUpdated.setBalance(purseEntityUpdated.getBalance() + paymentRequest.getAmount());
                            return purserRepository.save(purseEntityUpdated)
                                    .flatMap(savedPurse -> {
                                        return catalogCacheService.setCatalog(purseEntityUpdated.getId(), savedPurse)
                                                .then(catalogCacheService.setCatalog(purseById.getId(), purseById))
                                                .thenReturn(PaymentConverter.toPaymentResponse(paymentDescont));
                                    });
                        }));
    }


    public Maybe<PurseResponse> fallbackCreatePurse(Exception exception) {
        log.error("Fallback method for CreatePurse", exception);
        return RxJava3Adapter.monoToMaybe(Mono.error(new Exception("Fallback method for CreatePurse")));
    }

    public Flowable<PurseResponse> fallbackGetAllPurse(Exception exception) {
        log.error("Fallback method for GetAllPurse", exception);
        return RxJava3Adapter.fluxToFlowable(Flux.error(new Exception("Fallback method for GetAllPurse")));
    }

    public Maybe<PurseResponse> fallbackGetPurseById(Exception exception) {
        log.error("Fallback method for GetPurseById", exception);
        return RxJava3Adapter.monoToMaybe(Mono.error(new Exception("Fallback method for GetPurseById")));
    }

    public Completable fallbackDeleteById(Exception exception) {
        log.error("Fallback method for deleteById", exception);
         return RxJava3Adapter.monoToCompletable(Mono.error(new Exception("Fallback method for deleteById")));
    }

    public Maybe<PurseResponse> fallbackUpdatePurse(Exception exception) {
        log.error("Fallback method for UpdatePurse", exception);
         return RxJava3Adapter.monoToMaybe(Mono.error(new Exception("Fallback method for UpdatePurse")));
    }


    public Maybe<PaymentResponse> fallbackPayment(Exception exception) {
        log.error("Fallback method for paymentPayment", exception);
        return RxJava3Adapter.monoToMaybe(Mono.error(new Exception("Fallback method for paymentPayment")));
    }


}
