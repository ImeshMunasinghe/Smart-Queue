package com.smartqueue.service.sms;

public interface SmsGatewayProvider {
    boolean sendSms(String recipientPhone, String messageText);
}
