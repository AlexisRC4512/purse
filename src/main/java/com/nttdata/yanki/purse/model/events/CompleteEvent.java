package com.nttdata.yanki.purse.model.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CompleteEvent {
    private String idPay;
    private Double amount;
    private String idTransaction;
    private String idPurseBuy;
    private String idPurseSeller;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MMM dd, yyyy, h:mm:ss a", locale = "en")
    private Date date;
    private String state;

}
