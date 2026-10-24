package com.campus.parking.identity.service;

import com.campus.parking.identity.dto.StudentDto;

public interface IdentityModuleApi {
    String authenticate(String username, String password);
    Long verifyToken(String token);
    StudentDto getStudentById(Long studentId);
    StudentDto getStudentByUserId(Long userId);
    StudentDto getStudentByCode(String studentCode);
}