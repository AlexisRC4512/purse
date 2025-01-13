package com.nttdata.yanki.purse.model.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nttdata.yanki.purse.model.enums.TypeDocument;
import com.nttdata.yanki.purse.util.PurseTypeDeserializer;
import lombok.Getter;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@Getter
public class PurseRequest {
    @JsonDeserialize(using = PurseTypeDeserializer.class)
    private TypeDocument documentType;
    private Integer documentNumber;
    private String email;
    private String phoneNumber;
    private int imeiPhone;
    private boolean associateDebitCard;
    private String debitCardNumber;
    public PurseRequest( TypeDocument documentType, int documentNumber, String email, String phoneNumber, int imeiPhone, boolean associateDebitCard) {
        setDocumentType(documentType);
        setDocumentNumber(documentNumber);
        setEmail(email);
        setPhoneNumber(phoneNumber);
        setImeiPhone(imeiPhone);
        setAssociateDebitCard(associateDebitCard);
    }
    public void setDebitCardNumber(String debitCardNumber) {
        this.debitCardNumber = debitCardNumber;
    }
    public void setImeiPhone(Integer imeiPhone) {
        if (imeiPhone == null || imeiPhone <= 0) {
            throw new IllegalArgumentException("The IMEI of the phone cannot be null or negative.");
        }
        this.imeiPhone = imeiPhone;
    }

    public void setEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Email should be valid");
        }
        this.email = email;
    }

    public void setDocumentType(TypeDocument documentType) {
        if (documentType == null) {
            throw new IllegalArgumentException("Type is required");
        }
        this.documentType = documentType;

    }


    public void setAssociateDebitCard(boolean associateDebitCard) {
        this.associateDebitCard = associateDebitCard;
    }
    public void setDocumentNumber(Integer documentNumber) {
        if (documentNumber.toString().length() < 0 || documentNumber.toString().length() > 8) {
            throw new IllegalArgumentException("The ID number must not be greater than 8 or less than 0.");
        }
        this.documentNumber = documentNumber;
    }


    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber.length() < 0 || phoneNumber.length() > 9) {
            throw new IllegalArgumentException("The Phone number must not be greater than 9 or less than 0.");
        }
        this.phoneNumber = phoneNumber;
    }
}
