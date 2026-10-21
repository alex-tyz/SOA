package spacemarine.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"x", "y"})
public class CoordinatesDto {

    private Integer x;
    private Float y;

    public CoordinatesDto() {
    }

    public CoordinatesDto(Integer x, Float y) {
        this.x = x;
        this.y = y;
    }
}
