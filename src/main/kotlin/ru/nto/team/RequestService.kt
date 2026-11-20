package ru.nto.team

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class RequestService(
    private val staff: StaffRepository,
    private val requests: PassRequestRepository,
    private val clock: Clock,
) {
    private fun now(): LocalDateTime = LocalDateTime.now(clock)

    private fun requireStaff(code: String): Staff {
        RequestValidation.requireValidCodeFormat(code)
        return staff.findByCode(code) ?: throw ApiException(401, "Unauthorized")
    }

    fun list(code: String, status: String?): List<RequestListDto> {
        val employee = requireStaff(code)
        val filter = RequestValidation.parseStatusFilter(status)
        val entities = if (filter == null) {
            requests.findAllByHostIdOrderByValidFromAscIdAsc(employee.id)
        } else {
            requests.findAllByHostIdAndStatusOrderByValidFromAscIdAsc(employee.id, filter)
        }
        return entities.map {
            RequestListDto(it.id, it.fullName, it.validFrom, it.validTo, it.status)
        }
    }

    fun details(code: String, id: Long): RequestDetailsDto {
        val employee = requireStaff(code)
        return requests.findByIdAndHostId(id, employee.id)?.toDetails()
            ?: throw ApiException(404, "Request not found")
    }

    @Transactional
    fun create(code: String, body: CreateRequestBody): RequestDetailsDto {
        val fullName = body.fullName?.trim()
        val phone = body.phone?.trim()
        val organization = body.organization?.trim()
        val purpose = body.purpose?.trim()
        val validFromRaw = body.validFrom?.trim()
        val validToRaw = body.validTo?.trim()

        val employee = requireStaff(code)
        val current = now()

        val errors = LinkedHashMap<String, String>()
        if (fullName == null || fullName.length !in 5..150) errors["fullName"] = "Invalid full name"
        if (phone == null || !RequestValidation.phonePattern.matches(phone)) errors["phone"] = "Invalid phone"
        if (organization != null && organization.length > 150) errors["organization"] = "Invalid organization"
        if (purpose == null || purpose.length !in 5..200) errors["purpose"] = "Invalid purpose"

        val validFrom = validFromRaw?.let { RequestValidation.parseDateTime(it) }
        if (validFromRaw == null || validFrom == null) {
            errors["validFrom"] = "Invalid validFrom"
        } else if (validFrom.isBefore(current)) {
            errors["validFrom"] = "validFrom must not be in the past"
        }

        val validTo = validToRaw?.let { RequestValidation.parseDateTime(it) }
        if (validToRaw == null || validTo == null) {
            errors["validTo"] = "Invalid validTo"
        }

        if (validFrom != null && validTo != null &&
            (!validTo.isAfter(validFrom) || validFrom.toLocalDate() != validTo.toLocalDate())
        ) {
            errors["validTo"] = "validTo must be after validFrom on the same day"
        }

        if (errors.isNotEmpty()) throw ApiException(400, mapOf("errors" to errors))

        val duplicates = requests.findAllByHostIdAndPhoneAndStatusIn(
            employee.id,
            phone!!,
            listOf(RequestStatus.PENDING, RequestStatus.APPROVED),
        )
        val overlaps = duplicates.any {
            validFrom!!.isBefore(it.validTo) && it.validFrom.isBefore(validTo!!)
        }
        if (overlaps) throw ApiException(409, "Duplicate request")

        val stamp = current.truncatedTo(ChronoUnit.SECONDS)
        val entity = PassRequest(
            host = employee,
            fullName = fullName!!,
            phone = phone,
            organization = organization?.ifEmpty { null },
            purpose = purpose!!,
            validFrom = validFrom!!,
            validTo = validTo!!,
            status = RequestStatus.PENDING,
            comment = null,
            createdAt = stamp,
            updatedAt = stamp,
        )
        return requests.save(entity).toDetails()
    }

    @Transactional
    fun cancel(code: String, id: Long): RequestDetailsDto {
        val employee = requireStaff(code)
        val entity = requests.findByIdAndHostId(id, employee.id)
            ?: throw ApiException(404, "Request not found")
        if (entity.status != RequestStatus.PENDING) throw ApiException(409, "Invalid status transition")
        entity.status = RequestStatus.CANCELLED
        entity.updatedAt = now().truncatedTo(ChronoUnit.SECONDS)
        return requests.save(entity).toDetails()
    }

    @Transactional
    fun decide(id: Long, body: DecisionBody): RequestDetailsDto {
        val entity = requests.findById(id).orElseThrow { ApiException(404, "Request not found") }

        val statusRaw = body.status?.trim()
        val commentRaw = body.comment?.trim()

        val errors = LinkedHashMap<String, String>()
        val target: RequestStatus? = when (statusRaw) {
            "APPROVED" -> RequestStatus.APPROVED
            "REJECTED" -> RequestStatus.REJECTED
            else -> {
                errors["status"] = "Invalid status"
                null
            }
        }
        if (target == RequestStatus.REJECTED && (commentRaw == null || commentRaw.length !in 1..500)) {
            errors["comment"] = "Invalid comment"
        }
        if (errors.isNotEmpty()) throw ApiException(400, mapOf("errors" to errors))

        if (entity.status != RequestStatus.PENDING) throw ApiException(409, "Invalid status transition")

        entity.status = target!!
        entity.comment = if (target == RequestStatus.REJECTED) commentRaw else null
        entity.updatedAt = now().truncatedTo(ChronoUnit.SECONDS)
        return requests.save(entity).toDetails()
    }

    private fun PassRequest.toDetails() = RequestDetailsDto(
        id = id,
        fullName = fullName,
        phone = phone,
        organization = organization,
        purpose = purpose,
        validFrom = validFrom,
        validTo = validTo,
        status = status,
        comment = comment,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
