package spacemarine.repository;

import java.util.List;

public record PageResult<T>(List<T> items, long totalElements) {
}
