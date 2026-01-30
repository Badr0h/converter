package com.converter.backend.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data // includes getter + setter + toString + equals + hashCode
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subscriptions")
public class Subscription {
    @Id
    @GeneratedValue(strategy =GenerationType.IDENTITY)
    private Long id ; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user ;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = true)
    private Plan plan; // <--- lien vers le plan, nullable for free tier

    @Enumerated(EnumType.STRING)
    @Column(name = "status",columnDefinition = "ENUM('ACTIVE', 'CANCELLED', 'EXPIRED','PENDING')DEFAULT 'PENDING'")
    private Status status =Status.PENDING ;

    @Column(name = "start_date" ,nullable = false)
    private LocalDate  startDate;

    @Column(name = "end_date")
    private LocalDate  endDate;

    @Column(name = "created_at",columnDefinition = "Timestamp DEFAULT CURRENT_TIMESTAMP", insertable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration" , nullable = false)
    private SubscriptionDuration duration ; 

    @Column(name = "max_conversions_per_month")
    private Integer maxConversionsPerMonth; // Number of conversions allowed per month

    @Column(name = "is_trial")
    private Boolean isTrial = false;

    @Column(name = "trial_end_date")
    private LocalDate trialEndDate;

    @Column(name = "auto_renew")
    private Boolean autoRenew = true;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    public enum Status{
        ACTIVE,
        CANCELLED,
        EXPIRED,
        PENDING
    }

    public enum SubscriptionDuration {
    // Les valeurs (constantes) doivent appeler le constructeur
    ONE_MONTH(1),
    THREE_MONTHS(3),
    TWELVE_MONTHS(12);

    private final int months; // 1. Champ pour stocker la valeur

    // 2. Constructeur pour initialiser le champ
    SubscriptionDuration(int months) {
        this.months = months;
    }

    // 3. Getter pour récupérer la valeur numérique
    public int getMonths() {
        return months;
    }
}
}
