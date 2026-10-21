package spacemarine.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

// Используется и в ответе, и в запросе: оба поля строковые, разбирать типы не нужно
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"name", "parentLegion"})
public class ChapterDto {

    private String name;
    private String parentLegion;

    public ChapterDto() {
    }

    public ChapterDto(String name, String parentLegion) {
        this.name = name;
        this.parentLegion = parentLegion;
    }

    public String getName() {
        return name;
    }

    public String getParentLegion() {
        return parentLegion;
    }
}
