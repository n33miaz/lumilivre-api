package br.com.lumilivre.api.utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;

public class ExcelUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Retorna o valor da célula como String
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
                    // remove o ".0" se for número inteiro (ex: matrícula, CPF, CEP etc)
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

    // Retorna o valor da célula como Integer (se aplicável)
    public static Integer getInteger(Cell cell) {
        if (cell == null)
            return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            } else {
                String s = getString(cell);
                return s.isBlank() ? null : Integer.parseInt(s);
            }
        } catch (Exception e) {
            return null;
        }
    }

    // Retorna o valor da célula como LocalDate, tratando texto ou data do Excel
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
            // ignora erro de parse
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
            // Converte o valor da planilha (ex: "BROCHURA") para o Enum correspondente
            return Enum.valueOf(enumType, value.trim().toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            // Se o valor não corresponder a nenhuma constante do Enum, retorna o padrão
            return defaultValue;
        }
    }
}
