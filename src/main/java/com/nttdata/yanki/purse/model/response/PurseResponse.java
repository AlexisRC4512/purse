package com.nttdata.yanki.purse.model.response;

import com.nttdata.yanki.purse.model.entity.Payment;
import com.nttdata.yanki.purse.model.enums.TypeDocument;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@Builder
@NoArgsConstructor
@Getter
@Setter
public class PurseResponse {
    private String id;
    private TypeDocument documentType;
    private Integer documentNumber;
    private String email;
    private String phoneNumber;
    private int imeiPhone;
    private boolean associateDebitCard;
    private double balance;
    private String debitCardNumber;
    private List<Payment> payments;
}
