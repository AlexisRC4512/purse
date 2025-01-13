package com.nttdata.yanki.purse.model.entity;

import com.nttdata.yanki.purse.model.enums.TypeDocument;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;


@Document(collection = "purse")
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Getter
@Setter
public class PurseEntity {
    @Id
    private String id;
    private TypeDocument documentType;
    private int documentNumber;
    private String email;
    private String phoneNumber;
    private int imeiPhone;
    private boolean associateDebitCard;
    private double balance;
    private String debitCardNumber;
    private List<Payment> payments;

}
