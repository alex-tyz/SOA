package spacemarine.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

@XmlRootElement(name = "spaceMarinePage")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"spaceMarines", "page", "size", "totalElements", "totalPages"})
public class SpaceMarinePageDto {

    @XmlElementWrapper(name = "spaceMarines")
    @XmlElement(name = "spaceMarine")
    private List<SpaceMarineDto> spaceMarines;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public SpaceMarinePageDto() {
    }

    public SpaceMarinePageDto(List<SpaceMarineDto> spaceMarines, int page, int size, long totalElements,
                              int totalPages) {
        this.spaceMarines = spaceMarines;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }
}
