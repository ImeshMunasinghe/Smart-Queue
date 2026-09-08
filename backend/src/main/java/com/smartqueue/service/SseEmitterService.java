package com.smartqueue.service;

import com.smartqueue.model.TokenStateTransition;
import com.smartqueue.repository.TokenStateTransitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseEmitterService {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterService.class);
    private final Map<UUID, List<SseEmitter>> tokenEmitters = new ConcurrentHashMap<>();
    private final TokenStateTransitionRepository transitionRepository;

    public SseEmitterService(TokenStateTransitionRepository transitionRepository) {
        this.transitionRepository = transitionRepository;
    }

    public SseEmitter subscribe(UUID tokenId, Long lastEventId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30 min timeout

        tokenEmitters.computeIfAbsent(tokenId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(tokenId, emitter));
        emitter.onTimeout(() -> removeEmitter(tokenId, emitter));
        emitter.onError(e -> removeEmitter(tokenId, emitter));

        // Replay missed events if client reconnected with Last-Event-ID
        if (lastEventId != null && lastEventId > 0) {
            List<TokenStateTransition> missed = transitionRepository
                    .findByTokenIdAndIdGreaterThanOrderByCreatedAtAsc(tokenId, lastEventId);
            for (TokenStateTransition transition : missed) {
                try {
                    emitter.send(SseEmitter.event()
                            .id(String.valueOf(transition.getId()))
                            .name("state_transition")
                            .data(transition));
                } catch (IOException e) {
                    removeEmitter(tokenId, emitter);
                    break;
                }
            }
        }

        // Send initial connection event
        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("tokenId", tokenId)));
        } catch (IOException ignored) {}

        return emitter;
    }

    public void broadcastTokenUpdate(UUID tokenId, String eventType, Object data, Long eventId) {
        List<SseEmitter> emitters = tokenEmitters.get(tokenId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                        .name(eventType)
                        .data(data);
                if (eventId != null) {
                    event.id(String.valueOf(eventId));
                }
                emitter.send(event);
            } catch (Exception e) {
                removeEmitter(tokenId, emitter);
            }
        }
    }

    @Scheduled(fixedRate = 15000)
    public void sendHeartbeats() {
        tokenEmitters.forEach((tokenId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("keep-alive"));
                } catch (Exception e) {
                    removeEmitter(tokenId, emitter);
                }
            }
        });
    }

    private void removeEmitter(UUID tokenId, SseEmitter emitter) {
        List<SseEmitter> list = tokenEmitters.get(tokenId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                tokenEmitters.remove(tokenId);
            }
        }
    }
}
