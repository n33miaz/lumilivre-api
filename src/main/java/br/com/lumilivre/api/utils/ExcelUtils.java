package br.com.lumilivre.api.utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Logger log = LoggerFactory.getLogger(ExcelUtils.class);

    public static String getString(Cell cell) {
        if (cell == null)
            return "";
        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();
                case NUMERIC:
                    double val = cell.getNumericCellValue();
                    long asLong = (long) val;
                    return (val == asLong) ? String.valueOf(asLong) : String.valueOf(val);
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    return cell.getRichStringCellValue().getString().trim();
                default:
                    return cell.toString().trim();
            }
        } catch (Exception e) {
            return "";
        }
    }

    public static Integer getInteger(Cell cell) {
        if (cell == null) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) Math.round(cell.getNumericCellValue());
            } else if (cell.getCellType() == CellType.STRING) {
                String s = cell.getStringCellValue().trim();
                if (s.isBlank()) {
                    return null;
                }
                try {
                    return (int) Math.round(Double.parseDouble(s));
                } catch (NumberFormatException e) {
                    log.warn("Não foi possível converter a string '{}' para número na célula {}", s, cell.getAddress());
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("Erro inesperado ao ler célula inteira em {}", cell.getAddress(), e);
        }
        return null;
    }

    public static LocalDate getLocalDate(Cell cell) {
        if (cell == null)
            return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getDateCellValue()
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
            } else if (cell.getCellType() == CellType.STRING) {
                String valor = cell.getStringCellValue().trim();
                if (valor.isBlank())
                    return null;
                return LocalDate.parse(valor, FORMATTER);
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static Long getLong(Cell cell) {
        if (cell == null)
            return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (long) cell.getNumericCellValue();
            } else {
                String s = getString(cell);
                return s.isBlank() ? null : Long.parseLong(s);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static <T extends Enum<T>> T getEnum(Cell cell, Class<T> enumType, T defaultValue) {
        String value = getString(cell);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}
