package de.tellerstatttonne.backend.holiday;

import java.time.LocalDate;

public record Holiday(LocalDate date, String name, String region) {}
