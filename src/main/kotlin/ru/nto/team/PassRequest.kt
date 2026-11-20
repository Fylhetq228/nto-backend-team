package ru.nto.team

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "pass_request")
class PassRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    var host: Staff = Staff(),
    @Column(nullable = false, length = 150)
    var fullName: String = "",
    @Column(nullable = false, length = 12)
    var phone: String = "",
    @Column(length = 150)
    var organization: String? = null,
    @Column(nullable = false, length = 200)
    var purpose: String = "",
    @Column(nullable = false)
    var validFrom: LocalDateTime = LocalDateTime.MIN,
    @Column(nullable = false)
    var validTo: LocalDateTime = LocalDateTime.MIN,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: RequestStatus = RequestStatus.PENDING,
    @Column(length = 500)
    var comment: String? = null,
    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.MIN,
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.MIN,
)
