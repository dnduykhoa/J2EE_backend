package j2ee_backend.nhom05.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    public void broadcastProductUpdate(Long productId, String status, Integer stockQuantity) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        data.put("status", status);
        data.put("stockQuantity", stockQuantity);

        sendToAll("product-update", data);
    }

    public void broadcastVariantUpdate(Long productId, Long variantId, Boolean isActive, Integer stockQuantity) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        data.put("variantId", variantId);
        data.put("isActive", isActive);
        data.put("stockQuantity", stockQuantity);

        sendToAll("variant-update", data);
    }

    private void sendToAll(String eventName, Object data) {
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }
}
