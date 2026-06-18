package tech.illuin.wombat.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import tech.illuin.wombat.persistence.model.ContainerLocation;
import tech.illuin.wombat.persistence.model.KubernetesMetricEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

@ApplicationScoped
public class KubernetesMetricRepository implements PanacheRepositoryBase<KubernetesMetricEntity, Long>
{

    @Transactional
    public void save(KubernetesMetricEntity entity)
    {
        persist(entity);
    }

    public List<KubernetesMetricEntity> findByRange(long startMs, long endMs)
    {
        return find("instantMs >= ?1 AND instantMs <= ?2", startMs, endMs).list();
    }

    public List<KubernetesMetricEntity> findByRangeAndClusters(long startMs, long endMs, List<String> clusterIds)
    {
        if (clusterIds.isEmpty()) return findByRange(startMs, endMs);
        return find("instantMs >= ?1 AND instantMs <= ?2 AND cluster IN ?3",
            startMs, endMs, clusterIds).list();
    }

    /**
     * Mean over instants of the total CPU recorded at each instant, computed entirely in SQL.
     * Returns an empty value when no metric matches the filters.
     */
    public OptionalDouble averageCpuPerInstant(long startMs, long endMs, List<String> clusterIds)
    {
        StringBuilder sql = new StringBuilder(
            "SELECT AVG(perInstant) FROM ("
            + " SELECT SUM(cpu_nanocores) AS perInstant FROM kubernetes_metrics"
            + " WHERE instantMs >= :start AND instantMs <= :end");
        appendClusterFilter(sql, clusterIds);
        sql.append(" GROUP BY instantMs)");

        Query query = bind(getEntityManager().createNativeQuery(sql.toString()), startMs, endMs, clusterIds);
        Object result = query.getSingleResult();
        return result == null ? OptionalDouble.empty() : OptionalDouble.of(((Number) result).doubleValue());
    }

    /**
     * Per-container share of total CPU, summed and normalised in SQL. Returns an empty map
     * when there is no data or the total CPU is zero.
     */
    public Map<String, Double> containerShares(long startMs, long endMs, List<String> clusterIds)
    {
        StringBuilder sql = new StringBuilder(
            "SELECT container, SUM(cpu_nanocores) * 1.0 / SUM(SUM(cpu_nanocores)) OVER () AS share FROM kubernetes_metrics"
            + " WHERE instantMs >= :start AND instantMs <= :end");
        appendClusterFilter(sql, clusterIds);
        sql.append(" GROUP BY container");

        Query query = bind(getEntityManager().createNativeQuery(sql.toString()), startMs, endMs, clusterIds);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        Map<String, Double> shares = new LinkedHashMap<>();
        for (Object[] row : rows)
        {
            // share is NULL only when the total CPU is zero (division by zero in SQL)
            if (row[1] == null) return Map.of();
            shares.put((String) row[0], ((Number) row[1]).doubleValue());
        }
        return shares;
    }

    /**
     * Distinct (cluster, namespace) locations per container, deduplicated in SQL.
     */
    public Map<String, List<ContainerLocation>> containerLocations(long startMs, long endMs, List<String> clusterIds)
    {
        StringBuilder sql = new StringBuilder(
            "SELECT DISTINCT container, cluster, namespace FROM kubernetes_metrics"
            + " WHERE instantMs >= :start AND instantMs <= :end");
        appendClusterFilter(sql, clusterIds);
        sql.append(" ORDER BY container, cluster, namespace");

        Query query = bind(getEntityManager().createNativeQuery(sql.toString()), startMs, endMs, clusterIds);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        Map<String, List<ContainerLocation>> locations = new LinkedHashMap<>();
        for (Object[] row : rows)
        {
            locations.computeIfAbsent((String) row[0], k -> new ArrayList<>())
                .add(new ContainerLocation((String) row[1], (String) row[2]));
        }
        return locations;
    }

    private static void appendClusterFilter(StringBuilder sql, List<String> clusterIds)
    {
        if (!clusterIds.isEmpty()) sql.append(" AND cluster IN (:clusters)");
    }

    private static Query bind(Query query, long startMs, long endMs, List<String> clusterIds)
    {
        query.setParameter("start", startMs);
        query.setParameter("end", endMs);
        if (!clusterIds.isEmpty()) query.setParameter("clusters", clusterIds);
        return query;
    }
}
