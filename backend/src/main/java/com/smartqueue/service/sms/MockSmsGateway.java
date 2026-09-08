package com.smartqueue.service.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MockSmsGateway implements SmsGatewayProvider {

    private static final Logger log = LoggerFactory.getLogger(MockSmsGateway.class);

    public record SentSmsRecord(String recipient, String message, OffsetDateTime sentAt) {}

    private final List<SentSmsRecord> messageLog = Collections.synchronizedList(new ArrayList<>());

    @Override
    public boolean sendSms(String recipientPhone, String messageText) {
        SentSmsRecord record = new SentSmsRecord(recipientPhone, messageText, OffsetDateTime.now());
        messageLog.add(record);
        log.info("[MOCK SMS GATEWAY] >>> Recipient: {}, Content: '{}'", recipientPhone, messageText);
        return true;
    }

    public List<SentSmsRecord> getMessageLog() {
        return new ArrayList<>(messageLog);
    }

    public void clearLog() {
        messageLog.clear();
    }
}
