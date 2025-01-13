package com.nttdata.yanki.purse.util;

import com.nttdata.yanki.purse.model.entity.PurseEntity;
import com.nttdata.yanki.purse.model.request.PurseRequest;
import com.nttdata.yanki.purse.model.response.PurseResponse;

import java.util.UUID;

public class PurseMapper {
    public static PurseResponse toPurseResponse(PurseEntity purseEntity) {
        PurseResponse purseResponse = new PurseResponse();
        purseResponse.setId(purseEntity.getId());
        purseResponse.setEmail(purseEntity.getEmail());
        purseResponse.setDocumentNumber(purseEntity.getDocumentNumber());
        purseResponse.setDocumentType(purseEntity.getDocumentType());
        purseResponse.setPhoneNumber(purseEntity.getPhoneNumber());
        purseResponse.setImeiPhone(purseEntity.getImeiPhone());
        purseResponse.setAssociateDebitCard(purseEntity.isAssociateDebitCard());
        purseResponse.setBalance(purseEntity.getBalance());
        purseResponse.setDebitCardNumber(purseEntity.getDebitCardNumber());
        purseResponse.setPayments(purseEntity.getPayments());
        return purseResponse;
    }
    public static PurseEntity toPurseEntity(PurseRequest purseRequest) {
        PurseEntity purseEntity = new PurseEntity();
        purseEntity.setId(UUID.randomUUID().toString());
        purseEntity.setEmail(purseRequest.getEmail());
        purseEntity.setAssociateDebitCard(purseRequest.isAssociateDebitCard());
        if (!purseRequest.isAssociateDebitCard()) {
            purseEntity.setBalance(0);

        }
        purseEntity.setDocumentType(purseRequest.getDocumentType());
        purseEntity.setDocumentNumber(purseRequest.getDocumentNumber());
        purseEntity.setImeiPhone(purseRequest.getImeiPhone());
        purseEntity.setDebitCardNumber(purseRequest.getDebitCardNumber());
        purseEntity.setPhoneNumber(purseRequest.getPhoneNumber());
        return purseEntity;
    }
}
