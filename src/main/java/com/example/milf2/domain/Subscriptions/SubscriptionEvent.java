package com.example.milf2.domain.Subscriptions;


import com.example.milf2.domain.Subscriptions.EventTypes;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionEvent {

    private EventTypes eventType;
    private Object body;
}
