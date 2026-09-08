package com.smartqueue.controller;

import com.smartqueue.model.Office;
import com.smartqueue.model.ServiceType;
import com.smartqueue.model.SlotCapacity;
import com.smartqueue.repository.OfficeRepository;
import com.smartqueue.repository.ServiceTimeLogRepository;
import com.smartqueue.repository.ServiceTypeRepository;
import com.smartqueue.repository.SlotCapacityRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final OfficeRepository officeRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final SlotCapacityRepository slotCapacityRepository;
    private final ServiceTimeLogRepository serviceTimeLogRepository;

    public AdminController(OfficeRepository officeRepository,
                           ServiceTypeRepository serviceTypeRepository,
                           SlotCapacityRepository slotCapacityRepository,
                           ServiceTimeLogRepository serviceTimeLogRepository) {
        this.officeRepository = officeRepository;
        this.serviceTypeRepository = serviceTypeRepository;
        this.slotCapacityRepository = slotCapacityRepository;
        this.serviceTimeLogRepository = serviceTimeLogRepository;
    }

    @GetMapping("/offices")
    public ResponseEntity<List<Office>> listOffices() {
        return ResponseEntity.ok(officeRepository.findAll());
    }

    @GetMapping("/offices/{officeId}/service-types")
    public ResponseEntity<List<ServiceType>> listServiceTypes(@PathVariable UUID officeId) {
        return ResponseEntity.ok(serviceTypeRepository.findByOfficeIdAndActiveTrue(officeId));
    }

    @GetMapping("/offices/{officeId}/slots")
    public ResponseEntity<List<SlotCapacity>> listSlots(
            @PathVariable UUID officeId,
            @RequestParam(required = false) LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(slotCapacityRepository.findByOfficeIdAndSessionDate(officeId, queryDate));
    }

    @PutMapping("/slots/{slotId}/override")
    public ResponseEntity<SlotCapacity> overrideSlotCapacity(
            @PathVariable UUID slotId,
            @RequestBody Map<String, Integer> body) {
        SlotCapacity slot = slotCapacityRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + slotId));

        if (body.containsKey("manualOverrideLimit")) {
            slot.setManualOverrideLimit(body.get("manualOverrideLimit"));
            slot = slotCapacityRepository.save(slot);
        }
        return ResponseEntity.ok(slot);
    }

    @GetMapping("/offices/{officeId}/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics(@PathVariable UUID officeId) {
        List<ServiceType> services = serviceTypeRepository.findByOfficeIdAndActiveTrue(officeId);
        List<SlotCapacity> slots = slotCapacityRepository.findByOfficeIdAndSessionDate(officeId, LocalDate.now());

        int totalIssuedToday = slots.stream().mapToInt(SlotCapacity::getIssuedCount).sum();
        int totalActiveWaiting = slots.stream().mapToInt(SlotCapacity::getActiveWaitingCount).sum();

        return ResponseEntity.ok(Map.of(
                "officeId", officeId,
                "totalActiveServiceTypes", services.size(),
                "totalIssuedToday", totalIssuedToday,
                "totalActiveWaiting", totalActiveWaiting,
                "slotsToday", slots
        ));
    }
}
