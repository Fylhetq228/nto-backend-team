package ru.nto.team

import org.springframework.stereotype.Service

@Service
class StaffService(private val staff: StaffRepository) {
    fun profile(code: String): ProfileDto {
        RequestValidation.requireValidCodeFormat(code)
        val employee = staff.findByCode(code) ?: throw ApiException(401, "Unauthorized")
        return ProfileDto(employee.fullName)
    }
}
