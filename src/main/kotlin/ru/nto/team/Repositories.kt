package ru.nto.team

import org.springframework.data.jpa.repository.JpaRepository

interface StaffRepository : JpaRepository<Staff, Long> {
    fun findByCode(code: String): Staff?
}

interface PassRequestRepository : JpaRepository<PassRequest, Long> {
    fun findAllByHostIdOrderByValidFromAscIdAsc(hostId: Long): List<PassRequest>
    fun findAllByHostIdAndStatusOrderByValidFromAscIdAsc(hostId: Long, status: RequestStatus): List<PassRequest>
    fun findByIdAndHostId(id: Long, hostId: Long): PassRequest?
    fun findAllByHostIdAndPhoneAndStatusIn(
        hostId: Long,
        phone: String,
        statuses: Collection<RequestStatus>,
    ): List<PassRequest>
}
