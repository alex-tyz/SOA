package spacemarine.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@XmlRootElement(name = "error")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"code", "message", "timestamp", "details"})
public class ErrorDto {

    private int code;
    private String message;
    private String timestamp;

    @XmlElementWrapper(name = "details")
    @XmlElement(name = "detail")
    private List<String> details;

    public ErrorDto() {
    }

    public ErrorDto(int code, String message, List<String> details) {
        this.code = code;
        this.message = message;
        this.timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        this.details = details == null || details.isEmpty() ? null : details;
    }
}
