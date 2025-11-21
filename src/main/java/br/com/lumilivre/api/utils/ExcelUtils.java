package br.com.lumilivre.api.utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelUtils {

    private static final Logger log = LoggerFactory.getLogger(ExcelUtils.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ExcelUtils() {
    }

    public static String getString(Cell cell) {
        if (isCellEmpty(cell))
            return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> formatNumericString(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> processFormula(cell);
            default -> "";
        };
    }

    public static Integer getInteger(Cell cell) {
        if (isCellEmpty(cell))
            return null;

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) Math.round(cell.getNumericCellValue());
            } else if (cell.getCellType() == CellType.STRING) {
                String val = cell.getStringCellValue().trim();
                return val.isBlank() ? null : (int) Math.round(Double.parseDouble(val));
            }
        } catch (NumberFormatException e) {
            log.warn("Erro ao converter célula {} para Integer: {}", getCellAddress(cell), e.getMessage());
        }
        return null;
    }

    public static Long getLong(Cell cell) {
        if (isCellEmpty(cell))
            return null;

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (long) cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String val = cell.getStringCellValue().trim();
                return val.isBlank() ? null : Long.parseLong(val.replaceAll("\\D", ""));
            }
        } catch (NumberFormatException e) {
            log.warn("Erro ao converter célula {} para Long: {}", getCellAddress(cell), e.getMessage());
        }
        return null;
    }

    public static LocalDate getLocalDate(Cell cell) {
        if (isCellEmpty(cell))
            return null;

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getDateCellValue().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
            } else if (cell.getCellType() == CellType.STRING) {
                String val = cell.getStringCellValue().trim();
                return val.isBlank() ? null : LocalDate.parse(val, DATE_FORMATTER);
            }
        } catch (Exception e) {
            log.warn("Erro ao converter data na célula {}: {}", getCellAddress(cell), e.getMessage());
        }
        return null;
    }

    public static <T extends Enum<T>> T getEnum(Cell cell, Class<T> enumType, T defaultValue) {
        String value = getString(cell);
        if (value.isBlank())
            return defaultValue;

        try {
            String normalized = value.trim().toUpperCase().replace(" ", "_");
            return Enum.valueOf(enumType, normalized);
        } catch (IllegalArgumentException e) {
            log.warn("Valor '{}' inválido para Enum {} na célula {}", value, enumType.getSimpleName(),
                    getCellAddress(cell));
            return defaultValue;
        }
    }

    private static boolean isCellEmpty(Cell cell) {
        return cell == null || cell.getCellType() == CellType.BLANK;
    }

    private static String formatNumericString(double val) {
        long asLong = (long) val;
        return (val == asLong) ? String.valueOf(asLong) : String.valueOf(val);
    }

    private static String processFormula(Cell cell) {
        try {
            return cell.getStringCellValue();
        } catch (IllegalStateException e) {
            return String.valueOf(cell.getNumericCellValue());
        }
    }

    private static String getCellAddress(Cell cell) {
        return (cell != null) ? cell.getAddress().formatAsString() : "N/A";
    }
}