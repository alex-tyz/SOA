package spacemarine.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"x", "y"})
public class CoordinatesDto {

    private Integer x;
    private Integer y;

    public CoordinatesDto() {
    }

    public CoordinatesDto(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }
}
