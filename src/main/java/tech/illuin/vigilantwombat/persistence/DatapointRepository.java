package tech.illuin.vigilantwombat.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import tech.illuin.vigilantwombat.persistence.model.DatapointEntity;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

@ApplicationScoped
public class DatapointRepository implements PanacheRepositoryBase<DatapointEntity, Long> {

    @Transactional
    void onStart(@Observes StartupEvent event) {
        new File("data/db").mkdirs();
        getEntityManager().createNativeQuery("""
            CREATE TABLE IF NOT EXISTS datapoint (
                id        INTEGER PRIMARY KEY AUTOINCREMENT,
                instantMs INTEGER NOT NULL,
                type      TEXT    NOT NULL,
                payload   TEXT    NOT NULL
            )
        """).executeUpdate();
        getEntityManager().createNativeQuery(
            "CREATE INDEX IF NOT EXISTS idx_datapoint ON datapoint (instantMs, type)"
        ).executeUpdate();
    }

    public Optional<DatapointEntity> findByInstantAndType(long instantMs, String type) {
        return find("instantMs = ?1 AND type = ?2", instantMs, type).firstResultOptional();
    }

    @Transactional
    public void upsert(long instantMs, String type, UnaryOperator<String> payloadUpdater) {
        Optional<DatapointEntity> existing = findByInstantAndType(instantMs, type);
        String newPayload = payloadUpdater.apply(existing.map(e -> e.payload).orElse(null));
        if (existing.isPresent()) {
            existing.get().payload = newPayload;
        } else {
            DatapointEntity entity = new DatapointEntity();
            entity.instantMs = instantMs;
            entity.type = type;
            entity.payload = newPayload;
            persist(entity);
        }
    }

    public List<DatapointEntity> findByTypeAndRange(String type, long startMs, long endMs) {
        return find("type = ?1 AND instantMs >= ?2 AND instantMs <= ?3", type, startMs, endMs).list();
    }
}
