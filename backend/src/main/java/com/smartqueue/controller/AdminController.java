package com.smartqueue.controller;

import com.smartqueue.model.Office;
import com.smartqueue.model.ServiceType;
import com.smartqueue.model.SlotCapacity;
import com.smartqueue.repository.OfficeRepository;
import com.smartqueue.repository.ServiceTimeLogRepository;
import com.smartqueue.repository.ServiceTypeRepository;
import com.smartqueue.repository.SlotCapacityRepository;
import com.smartqueue.repository.TokenRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final OfficeRepository officeRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final SlotCapacityRepository slotCapacityRepository;
    private final ServiceTimeLogRepository serviceTimeLogRepository;
    private final TokenRepository tokenRepository;

    public AdminController(OfficeRepository officeRepository,
                           ServiceTypeRepository serviceTypeRepository,
                           SlotCapacityRepository slotCapacityRepository,
                           ServiceTimeLogRepository serviceTimeLogRepository,
                           TokenRepository tokenRepository) {
        this.officeRepository = officeRepository;
        this.serviceTypeRepository = serviceTypeRepository;
        this.slotCapacityRepository = slotCapacityRepository;
        this.serviceTimeLogRepository = serviceTimeLogRepository;
        this.tokenRepository = tokenRepository;
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

    /**
     * GET /api/v1/admin/offices/{officeId}/analytics/daily-volume?days=7
     *
     * Returns daily token issuance counts grouped by service type for the last N days.
     * Response: [ { date, serviceTypeId, serviceTypeName, count }, ... ]
     */
    @GetMapping("/offices/{officeId}/analytics/daily-volume")
    public ResponseEntity<List<Map<String, Object>>> getDailyVolume(
            @PathVariable UUID officeId,
            @RequestParam(defaultValue = "7") int days) {

        OffsetDateTime from = LocalDate.now().minusDays(days - 1)
                .atStartOfDay().atOffset(ZoneOffset.UTC);

        List<Object[]> rows = tokenRepository.findDailyIssuanceByServiceType(officeId, from);

        // Build a serviceTypeId → name lookup map
        Map<String, String> serviceNames = new HashMap<>();
        serviceTypeRepository.findByOfficeIdAndActiveTrue(officeId)
                .forEach(st -> serviceNames.put(st.getId().toString(), st.getName()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", row[0] != null ? row[0].toString() : null);
            entry.put("serviceTypeId", row[1] != null ? row[1].toString() : null);
            entry.put("serviceTypeName",
                    row[1] != null ? serviceNames.getOrDefault(row[1].toString(), "Unknown") : "Unknown");
            entry.put("count", row[2]);
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/admin/offices/{officeId}/analytics/hourly-wait?days=7
     *
     * Returns average service duration (seconds) grouped by hour of day (0–23) for the last N days.
     * Response: [ { hour, avgDurationSeconds }, ... ]
     */
    @GetMapping("/offices/{officeId}/analytics/hourly-wait")
    public ResponseEntity<List<Map<String, Object>>> getHourlyWait(
            @PathVariable UUID officeId,
            @RequestParam(defaultValue = "7") int days) {

        OffsetDateTime from = LocalDate.now().minusDays(days - 1)
                .atStartOfDay().atOffset(ZoneOffset.UTC);

        List<Object[]> rows = serviceTimeLogRepository.findAvgDurationByHour(officeId, from);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("hour", row[0] != null ? ((Number) row[0]).intValue() : null);
            entry.put("avgDurationSeconds",
                    row[1] != null ? Math.round(((Number) row[1]).doubleValue()) : 0);
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }
}

