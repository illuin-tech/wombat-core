package tech.illuin.wombat.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import tech.illuin.wombat.persistence.model.DatapointEntity;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

@ApplicationScoped
public class DatapointRepository implements PanacheRepositoryBase<DatapointEntity, Long>
{

    public Optional<DatapointEntity> findByInstantAndType(long instantMs, String type)
    {
        return find("instantMs = ?1 AND type = ?2", instantMs, type).firstResultOptional();
    }

    @Transactional
    public void upsert(long instantMs, String type, UnaryOperator<String> payloadUpdater)
    {
        Optional<DatapointEntity> existing = findByInstantAndType(instantMs, type);
        String newPayload = payloadUpdater.apply(existing.map(e -> e.payload).orElse(null));
        if (existing.isPresent())
        {
            existing.get().payload = newPayload;
        }
        else {
            DatapointEntity entity = new DatapointEntity();
            entity.instantMs = instantMs;
            entity.type = type;
            entity.payload = newPayload;
            persist(entity);
        }
    }

    public List<DatapointEntity> findByTypeAndRange(String type, long startMs, long endMs)
    {
        return find("type = ?1 AND instantMs >= ?2 AND instantMs <= ?3", type, startMs, endMs).list();
    }
}
