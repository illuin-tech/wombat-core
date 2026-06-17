package tech.illuin.wombat.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import tech.illuin.wombat.persistence.model.DatapointEntity;

import java.util.List;

@ApplicationScoped
public class DatapointRepository implements PanacheRepositoryBase<DatapointEntity, Long>
{

    @Transactional
    public void save(DatapointEntity entity)
    {
        persist(entity);
    }

    public List<DatapointEntity> findByTypeAndRange(String type, long startMs, long endMs)
    {
        return find("type = ?1 AND instantMs >= ?2 AND instantMs <= ?3", type, startMs, endMs).list();
    }

    public List<DatapointEntity> findByTypeRangeAndClusters(String type, long startMs, long endMs, List<String> clusterIds)
    {
        if (clusterIds.isEmpty()) return findByTypeAndRange(type, startMs, endMs);
        return find("type = ?1 AND instantMs >= ?2 AND instantMs <= ?3 AND cluster IN ?4",
            type, startMs, endMs, clusterIds).list();
    }
}
