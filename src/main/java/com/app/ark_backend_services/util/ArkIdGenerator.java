package com.app.ark_backend_services.util;

import java.security.SecureRandom;

public final class ArkIdGenerator {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int NANOID_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private ArkIdGenerator() {
    }

    public static String generateOrgId() {
        return "ARK-ORG-" + nanoid();
    }

    public static String generateBranchId() {
        return "ARK-BR-" + nanoid();
    }

    public static String generateStudentId() {
        return "ARK-STU-" + nanoid();
    }

    public static String generateFacultyId() {
        return "ARK-FAC-" + nanoid();
    }

    public static String generateExamId() {
        return "ARK-EXM-" + nanoid();
    }

    public static String generateEnrollmentId() {
        return "ARK-ENR-" + nanoid();
    }

    public static String generateAssignmentId() {
        return "ARK-ASN-" + nanoid();
    }

    private static String nanoid() {
        StringBuilder sb = new StringBuilder(NANOID_LENGTH);
        for (int i = 0; i < NANOID_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}
