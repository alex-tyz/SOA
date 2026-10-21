package spacemarine;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import java.util.Map;

@ApplicationPath("/api/v1")
public class SpaceMarineApplication extends Application {

    @Override
    public Map<String, Object> getProperties() {
        return Map.of("jersey.config.server.headers.location.relative.resolution.disabled", true);
    }
}
