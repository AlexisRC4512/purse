package com.nttdata.yanki.purse.model.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * Represents a payment in the system.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    private String id;
    /**
     * Amount of the payment.
     */
    private double amount;

    /**
     * Date of the payment.
     */
    private Date date;

    /**
     * Description of the payment.
     */
    private String description;

    private String state;

}
