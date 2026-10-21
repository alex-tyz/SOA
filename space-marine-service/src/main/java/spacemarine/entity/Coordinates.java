package spacemarine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Coordinates {

    @Column(name = "coordinates_x", nullable = false)
    private Integer x;

    @Column(name = "coordinates_y", nullable = false)
    private Float y;

    public Coordinates() {
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Float getY() {
        return y;
    }

    public void setY(Float y) {
        this.y = y;
    }
}
