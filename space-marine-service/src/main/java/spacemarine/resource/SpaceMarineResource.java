package spacemarine.resource;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import spacemarine.dto.AchievementListDto;
import spacemarine.dto.SpaceMarineDto;
import spacemarine.dto.SpaceMarineListDto;
import spacemarine.dto.SpaceMarinePageDto;
import spacemarine.dto.SpaceMarineRequestDto;
import spacemarine.entity.SpaceMarine;
import spacemarine.exception.ApiException;
import spacemarine.mapper.SpaceMarineMapper;
import spacemarine.query.SpaceMarineQuery;
import spacemarine.repository.PageResult;
import spacemarine.service.SpaceMarineService;
import spacemarine.validation.ListQueryParser;
import spacemarine.validation.PathParams;
import spacemarine.validation.SpaceMarineValidator;

import java.util.List;

@Path("/space-marines")
@RequestScoped
public class SpaceMarineResource {

    @Inject
    SpaceMarineService service;

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public SpaceMarinePageDto list(@Context UriInfo uriInfo) {
        SpaceMarineQuery query = ListQueryParser.parse(uriInfo.getQueryParameters());
        PageResult<SpaceMarine> result = service.list(query);
        int totalPages = (int) ((result.totalElements() + query.size() - 1) / query.size());
        return new SpaceMarinePageDto(
                SpaceMarineMapper.toDtoList(result.items()),
                query.page(),
                query.size(),
                result.totalElements(),
                totalPages);
    }

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response create(SpaceMarineRequestDto body, @Context UriInfo uriInfo) {
        SpaceMarine created = service.create(SpaceMarineValidator.validate(body));
        String location = uriInfo.getBaseUri().getPath() + "space-marines/" + created.getId();
        return Response.status(Response.Status.CREATED)
                .header("Location", location)
                .entity(SpaceMarineMapper.toDto(created))
                .build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_XML)
    public SpaceMarineDto getById(@PathParam("id") String rawId) {
        return SpaceMarineMapper.toDto(service.get(PathParams.parseId(rawId)));
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public SpaceMarineDto update(@PathParam("id") String rawId, SpaceMarineRequestDto body) {
        int id = PathParams.parseId(rawId);
        SpaceMarine candidate = SpaceMarineValidator.validate(body);
        return SpaceMarineMapper.toDto(service.update(id, candidate));
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String rawId) {
        service.delete(PathParams.parseId(rawId));
        return Response.noContent().build();
    }

    @DELETE
    @Path("/by-weapon-type/{weaponType}")
    @Produces(MediaType.APPLICATION_XML)
    public SpaceMarineDto deleteAnyByWeaponType(@PathParam("weaponType") String rawWeapon) {
        return SpaceMarineMapper.toDto(service.deleteAnyByWeapon(PathParams.parseWeapon(rawWeapon)));
    }

    @GET
    @Path("/by-achievements")
    @Produces(MediaType.APPLICATION_XML)
    public SpaceMarineListDto findByAchievements(@QueryParam("substring") String substring) {
        if (substring == null) {
            throw ApiException.invalidQuery(List.of("substring: параметр обязателен"));
        }
        if (substring.isEmpty()) {
            throw ApiException.invalidQuery(List.of("substring: строка не может быть пустой"));
        }
        return new SpaceMarineListDto(SpaceMarineMapper.toDtoList(service.findByAchievements(substring)));
    }

    @GET
    @Path("/achievements/unique")
    @Produces(MediaType.APPLICATION_XML)
    public AchievementListDto uniqueAchievements() {
        return new AchievementListDto(service.uniqueAchievements());
    }
}
