package com.nttdata.yanki.purse.service;

import com.nttdata.yanki.purse.exception.PurseNotFoundException;
import com.nttdata.yanki.purse.model.entity.PurseEntity;
import com.nttdata.yanki.purse.model.request.PaymentRequest;
import com.nttdata.yanki.purse.model.request.PurseRequest;
import com.nttdata.yanki.purse.model.response.PurseResponse;
import com.nttdata.yanki.purse.repository.PurserRepository;
import com.nttdata.yanki.purse.service.impl.PurseServiceImpl;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class PurseServiceImplTest {

    @Mock
    private PurserRepository purserRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private CatalogCacheService catalogCacheService;

    @InjectMocks
    private PurseServiceImpl purseServiceImpl;

    private PurseRequest purseRequest;
    private PurseEntity purseEntity;
    private PaymentRequest paymentRequest;


    @BeforeEach
    void setUp() {
        purseRequest = new PurseRequest();
        purseRequest.setDebitCardNumber("1234567890");
        purseRequest.setAssociateDebitCard(true);
        purseRequest.setDocumentNumber(123456);
        purseRequest.setEmail("test@example.com");
        purseRequest.setPhoneNumber("123456789");
        purseRequest.setImeiPhone(123456789);

        purseEntity = new PurseEntity();
        purseEntity.setDebitCardNumber("1234567890");
        paymentRequest = new PaymentRequest();
        paymentRequest.setAmount(100.0);
    }

    @Test
    void insertShouldReturnErrorWhenDebitCardNumberIsInUse() {
        String authorizationHeader = "Bearer your_jwt_token";

        when(purserRepository.findByDebitCardNumber(purseRequest.getDebitCardNumber()))
                .thenReturn(Mono.just(purseEntity));
        Maybe<PurseResponse> result = purseServiceImpl.insert(purseRequest, authorizationHeader);
        TestObserver<PurseResponse> testObserver = result.test();
        testObserver.assertError(PurseNotFoundException.class);
    }

    @Test
    void insertShouldCreatePurseWhenDebitCardNumberNotInUse() {
        String authorizationHeader = "Bearer your_jwt_token";

        when(purserRepository.findByDebitCardNumber(purseRequest.getDebitCardNumber()))
                .thenReturn(Mono.empty());
        when(purserRepository.save(any(PurseEntity.class)))
                .thenReturn(Mono.just(purseEntity));
        Maybe<PurseResponse> result = purseServiceImpl.insert(purseRequest, authorizationHeader);
        TestObserver<PurseResponse> testObserver = result.test();
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(purseResponse -> purseResponse.getDebitCardNumber().equals(purseRequest.getDebitCardNumber()));

        verify(kafkaTemplate).send(eq("debit-card-topic-create"), anyString());
    }

    @Test
    void insertShouldCreatePurseWithoutKafkaMessageWhenAssociateDebitCardIsFalse() {
        String authorizationHeader = "Bearer your_jwt_token";
        purseRequest.setAssociateDebitCard(false);

        when(purserRepository.findByDebitCardNumber(purseRequest.getDebitCardNumber()))
                .thenReturn(Mono.empty());
        when(purserRepository.save(any(PurseEntity.class)))
                .thenReturn(Mono.just(purseEntity));
        Maybe<PurseResponse> result = purseServiceImpl.insert(purseRequest, authorizationHeader);
        TestObserver<PurseResponse> testObserver = result.test();
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(purseResponse -> purseResponse.getDebitCardNumber().equals(purseRequest.getDebitCardNumber()));

        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void findAllShouldReturnAllPurses() {
        when(purserRepository.findAll()).thenReturn(Flux.just(purseEntity));
        Flowable<PurseResponse> result = purseServiceImpl.findAll();
        TestSubscriber<PurseResponse> testSubscriber = result.test();
        testSubscriber.assertComplete();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(purseResponse -> purseResponse.getDebitCardNumber().equals(purseEntity.getDebitCardNumber()));
    }
    @Test
    void findByIdShouldReturnPurseWhenPurseExists() {
        String purseId = "1";
        purseEntity = new PurseEntity();
        purseEntity.setDebitCardNumber("1234567890");
        when(catalogCacheService.getCatalog(purseId)).thenReturn(Mono.empty());
        when(purserRepository.findById(purseId)).thenReturn(Mono.just(purseEntity));
        when(catalogCacheService.setCatalog(purseId, purseEntity)).thenReturn(Mono.just(true));
        Maybe<PurseResponse> result = purseServiceImpl.findById(purseId);
        TestObserver<PurseResponse> testObserver = result.test();
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(purseResponse -> purseResponse.getDebitCardNumber().equals(purseEntity.getDebitCardNumber()));
    }

    @Test
    void findByIdShouldReturnErrorWhenPurseDoesNotExist() {
        String purseId = "1";
        when(catalogCacheService.getCatalog(purseId)).thenReturn(Mono.empty());
        when(purserRepository.findById(purseId)).thenReturn(Mono.empty());
        Maybe<PurseResponse> result = purseServiceImpl.findById(purseId);
        TestObserver<PurseResponse> testObserver = result.test();
        testObserver.assertComplete();
        testObserver.assertNoValues();
    }
    @Test
    void updatePurseShouldUpdatePurseWhenPurseExists() {
        String purseId = "1";
        when(purserRepository.findById(purseId)).thenReturn(Mono.just(purseEntity));
        when(purserRepository.save(any(PurseEntity.class))).thenReturn(Mono.just(purseEntity));
        Maybe<PurseResponse> result = purseServiceImpl.updatePurse(purseId, purseRequest);
        TestObserver<PurseResponse> testObserver = result.test();
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(purseResponse -> purseResponse.getDebitCardNumber().equals(purseRequest.getDebitCardNumber()));
    }

    @Test
    void updatePurseShouldReturnErrorWhenPurseDoesNotExist() {
        String purseId = "1";
        when(purserRepository.findById(purseId)).thenReturn(Mono.empty());
        Maybe<PurseResponse> result = purseServiceImpl.updatePurse(purseId, purseRequest);
        TestObserver<PurseResponse> testObserver = result.test();
        testObserver.assertError(PurseNotFoundException.class);
    }




}