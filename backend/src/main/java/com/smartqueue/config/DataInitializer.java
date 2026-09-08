package com.smartqueue.config;

import com.smartqueue.model.*;
import com.smartqueue.repository.CounterRepository;
import com.smartqueue.repository.OfficeRepository;
import com.smartqueue.repository.ServiceTypeRepository;
import com.smartqueue.repository.SlotCapacityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final OfficeRepository officeRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final CounterRepository counterRepository;
    private final SlotCapacityRepository slotCapacityRepository;

    public DataInitializer(OfficeRepository officeRepository,
                           ServiceTypeRepository serviceTypeRepository,
                           CounterRepository counterRepository,
                           SlotCapacityRepository slotCapacityRepository) {
        this.officeRepository = officeRepository;
        this.serviceTypeRepository = serviceTypeRepository;
        this.counterRepository = counterRepository;
        this.slotCapacityRepository = slotCapacityRepository;
    }

    @Override
    public void run(String... args) {
        if (officeRepository.count() > 0) {
            log.info("Database already seeded. Skipping initial data load.");
            return;
        }

        log.info("Seeding Pilot Office data (Colombo Divisional Secretariat)...");

        // 1. Office
        UUID officeId = UUID.fromString("a0000000-0000-0000-0000-000000000001");
        Office office = new Office(
                officeId,
                "Colombo Divisional Secretariat (Pilot Office)",
                "DS-COLOMBO-PILOT",
                "Asia/Colombo",
                "Dam Street, Colombo 12, Sri Lanka"
        );
        officeRepository.save(office);

        // 2. Service Types
        UUID s1 = UUID.fromString("b0000000-0000-0000-0000-000000000001");
        ServiceType nicService = new ServiceType(s1, officeId, "NIC_RENEWAL", "National Identity Card (NIC) Services",
                "New issuance and renewal of NICs", 12);

        UUID s2 = UUID.fromString("b0000000-0000-0000-0000-000000000002");
        ServiceType gramaService = new ServiceType(s2, officeId, "GRAMA_CERT", "Grama Niladhari Character / Residence Certificate",
                "Verification of residence and character certificates", 8);

        UUID s3 = UUID.fromString("b0000000-0000-0000-0000-000000000003");
        ServiceType opdService = new ServiceType(s3, officeId, "OPD_CONSULT", "General OPD Medical Consultation",
                "Outpatient department preliminary clinical screening", 15);

        serviceTypeRepository.save(nicService);
        serviceTypeRepository.save(gramaService);
        serviceTypeRepository.save(opdService);

        // 3. Counters
        Counter c1 = new Counter(
                UUID.fromString("c0000000-0000-0000-0000-000000000001"),
                officeId, "C1", "Counter 1 (NIC Priority)", CounterStatus.ONLINE,
                "[\"" + s1 + "\"]"
        );

        Counter c2 = new Counter(
                UUID.fromString("c0000000-0000-0000-0000-000000000002"),
                officeId, "C2", "Counter 2 (Grama & General)", CounterStatus.ONLINE,
                "[\"" + s1 + "\", \"" + s2 + "\"]"
        );

        Counter c3 = new Counter(
                UUID.fromString("c0000000-0000-0000-0000-000000000003"),
                officeId, "C3", "Counter 3 (OPD Consultation)", CounterStatus.ONLINE,
                "[\"" + s3 + "\"]"
        );

        counterRepository.save(c1);
        counterRepository.save(c2);
        counterRepository.save(c3);

        // 4. Slot Capacities for Today
        LocalDate today = LocalDate.now();
        createSlot(officeId, s1, today, LocalTime.of(8, 30), LocalTime.of(12, 30), 40, 46);
        createSlot(officeId, s2, today, LocalTime.of(8, 30), LocalTime.of(12, 30), 50, 58);
        createSlot(officeId, s3, today, LocalTime.of(8, 0), LocalTime.of(13, 0), 60, 70);

        log.info("Pilot Office data initialization complete.");
    }

    private void createSlot(UUID officeId, UUID serviceTypeId, LocalDate date, LocalTime start, LocalTime end, int raw, int computed) {
        SlotCapacity slot = new SlotCapacity();
        slot.setId(UUID.randomUUID());
        slot.setOfficeId(officeId);
        slot.setServiceTypeId(serviceTypeId);
        slot.setSessionDate(date);
        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setRawCapacity(raw);
        slot.setComputedLimit(computed);
        slot.setIssuedCount(0);
        slot.setActiveWaitingCount(0);
        slot.setRiskThreshold(new BigDecimal("0.100"));
        slotCapacityRepository.save(slot);
    }
}
