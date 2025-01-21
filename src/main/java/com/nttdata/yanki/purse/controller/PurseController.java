package com.nttdata.yanki.purse.controller;

import com.nttdata.yanki.purse.model.request.PaymentRequest;
import com.nttdata.yanki.purse.model.request.PurseRequest;
import com.nttdata.yanki.purse.model.response.PaymentResponse;
import com.nttdata.yanki.purse.model.response.PurseResponse;
import com.nttdata.yanki.purse.service.PurserService;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/purses")
@RequiredArgsConstructor
public class PurseController {
    private final PurserService purserService;

    @PostMapping
    public ResponseEntity<Maybe<PurseResponse>> createPurse(@RequestBody PurseRequest purseRequest
            , @RequestHeader("Authorization") String authorizationHeader) {
        Maybe<PurseResponse> response = purserService.insert(purseRequest,authorizationHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Flowable<PurseResponse>> getAllPurses() {
        Flowable<PurseResponse> response = purserService.findAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Maybe<PurseResponse>> getPurseById(@PathVariable String id) {
        Maybe<PurseResponse> response = purserService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Maybe<PurseResponse>> updatePurse(@PathVariable String id, @RequestBody PurseRequest purseRequest) {
        Maybe<PurseResponse> response = purserService.updatePurse(id, purseRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePurse(@PathVariable String id) {
        purserService.deletePurse(id).subscribe();
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{idPurse}/payByCreditId")
    public Maybe<PaymentResponse> payByCreditId(@PathVariable String idPurse,
                                                @RequestParam String numberPhone,
                                                @RequestBody PaymentRequest paymentRequest,
                                                @RequestHeader("Authorization") String authorizationHeader ) {
        return purserService.payByCreditId(idPurse, numberPhone, paymentRequest ,authorizationHeader);
    }
}