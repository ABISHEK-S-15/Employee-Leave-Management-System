package com.employee.leavemanagement.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leave_balances", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "year"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "year", nullable = false)
    private Integer year;


    private Integer annualQuota;
    private Integer sickQuota;
    private Integer casualQuota;


    private Integer annualUsed;
    private Integer sickUsed;
    private Integer casualUsed;


    private Integer carriedForward;
}
