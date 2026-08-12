package tech.illuin.wombat.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import tech.illuin.wombat.persistence.model.ModelMetricEntity;

@ApplicationScoped
public class ModelMetricRepository implements PanacheRepositoryBase<ModelMetricEntity, Long>
{
    @Transactional
    public void save(ModelMetricEntity entity)
    {
        persist(entity);
    }

    public long sumOutputTokens(long startMs, long endMs, String profileId)
    {
        Query query = getEntityManager().createNativeQuery(
            "SELECT COALESCE(SUM(outputTokens), 0) FROM model_metrics"
            + " WHERE instantMs >= :start AND instantMs <= :end"
            + " AND json_extract(data, '$.profileId') = :profileId");
        query.setParameter("start", startMs);
        query.setParameter("end", endMs);
        query.setParameter("profileId", profileId);
        return ((Number) query.getSingleResult()).longValue();
    }
}
