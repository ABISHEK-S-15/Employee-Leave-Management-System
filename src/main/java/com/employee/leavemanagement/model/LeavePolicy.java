package com.employee.leavemanagement.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leave_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeavePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer annualQuota;

    @Column(nullable = false)
    private Integer sickQuota;

    @Column(nullable = false)
    private Integer casualQuota;

    @Column(nullable = false)
    private Integer carryForwardLimit;

    @Column(nullable = false)
    private Integer maxConsecutiveDays;

    @Column(nullable = false)
    private Boolean skipWeekends;
}
