package com.nttdata.yanki.purse.service;

import com.nttdata.yanki.purse.model.request.PaymentRequest;
import com.nttdata.yanki.purse.model.request.PurseRequest;
import com.nttdata.yanki.purse.model.response.PaymentResponse;
import com.nttdata.yanki.purse.model.response.PurseResponse;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;

public interface PurserService {
    Maybe<PurseResponse> insert (PurseRequest purseRequest);
    Flowable<PurseResponse> findAll ();
    Maybe<PurseResponse> findById (String purseId);
    Maybe<PurseResponse> updatePurse (String purseId,PurseRequest purseRequest);
    Completable deletePurse(String purseId);
    Maybe<PaymentResponse> payByCreditId(String idPurse,String numberPhone , PaymentRequest paymentRequest);

}
