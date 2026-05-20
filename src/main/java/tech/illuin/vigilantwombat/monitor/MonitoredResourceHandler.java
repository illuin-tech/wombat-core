package tech.illuin.vigilantwombat.monitor;

public interface MonitoredResourceHandler<M> {
    // TODO: name will be used later
    void handle(String name, M config);
}
