package com.nttdata.yanki.purse.util;


import com.nttdata.yanki.purse.model.entity.Payment;
import com.nttdata.yanki.purse.model.response.PaymentResponse;
import reactor.core.publisher.Flux;
import java.util.Date;
import java.util.List;

public class PaymentConverter {
    public static PaymentResponse toPaymentResponse(Payment payment) {
        PaymentResponse paymentResponse = new PaymentResponse(payment.getId(),payment.getAmount(), new Date(), payment.getState());
        return paymentResponse;
    }
    public static Flux<PaymentResponse> toListPaymentResponse(List<PaymentResponse>paymentResponseList) {
        Flux<PaymentResponse>newPaymentResponseList = Flux.just((PaymentResponse) paymentResponseList);
        return newPaymentResponseList;
    }
}
