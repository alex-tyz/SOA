package spacemarine.dto;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.time.LocalDate;

public class LocalDateAdapter extends XmlAdapter<String, LocalDate> {

    @Override
    public LocalDate unmarshal(String value) {
        return value == null ? null : LocalDate.parse(value.trim());
    }

    @Override
    public String marshal(LocalDate value) {
        return value == null ? null : value.toString();
    }
}
