package tech.illuin.wombat.profile;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ServerProfileRepository implements PanacheRepositoryBase<ServerProfileEntity, String>
{
}
