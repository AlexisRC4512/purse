package com.nttdata.yanki.purse.model.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEvent {
    private String idPay;
    private String eventType;
    private double amount;
    private List<String> listTransactionId;
    private List<String> debitCardNumbers;
}
