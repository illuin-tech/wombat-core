package tech.illuin.wombat.core.asset.profile.inference;

import tech.illuin.wombat.core.asset.profile.ServerProvider;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Tells the provider and the instance type of cluster nodes from what Kubernetes exposes of them: the nodes'
 * {@code spec.providerID} and labels, or the host name of a managed cluster's API server.
 */
public final class CloudPlacement
{
    public static final String INSTANCE_TYPE_LABEL = "node.kubernetes.io/instance-type";
    public static final String LEGACY_INSTANCE_TYPE_LABEL = "beta.kubernetes.io/instance-type";

    /** {@code spec.providerID} schemes; {@code openstack} is not one, OVHcloud sharing it with every OpenStack cloud. */
    private static final Map<String, ServerProvider> PROVIDER_ID_SCHEMES = Map.of(
        "aws", ServerProvider.aws,
        "gce", ServerProvider.gcp,
        "azure", ServerProvider.azure,
        "scaleway", ServerProvider.scaleway
    );

    /** Label prefixes the managed offers set on their nodes. */
    private static final Map<String, ServerProvider> LABEL_PREFIXES = Map.of(
        "eks.amazonaws.com/", ServerProvider.aws,
        "cloud.google.com/gke-", ServerProvider.gcp,
        "kubernetes.azure.com/", ServerProvider.azure,
        "node.k8s.ovh/", ServerProvider.ovhcloud,
        "k8s.ovh.net/", ServerProvider.ovhcloud,
        "k8s.scaleway.com/", ServerProvider.scaleway
    );

    /** Domains of the managed API servers. */
    private static final Map<String, ServerProvider> API_SERVER_DOMAINS = Map.of(
        ".eks.amazonaws.com", ServerProvider.aws,
        ".gke.goog", ServerProvider.gcp,
        ".azmk8s.io", ServerProvider.azure,
        ".k8s.ovh.net", ServerProvider.ovhcloud,
        ".scw.cloud", ServerProvider.scaleway
    );

    private CloudPlacement() {}

    /**
     * @return the provider of a node, from the scheme of its {@code spec.providerID} or else its provider-specific labels
     */
    public static Optional<ServerProvider> provider(String providerId, Map<String, String> labels)
    {
        if (providerId != null && providerId.contains("://"))
        {
            ServerProvider fromScheme = PROVIDER_ID_SCHEMES.get(providerId.substring(0, providerId.indexOf("://")).toLowerCase(Locale.ROOT));
            if (fromScheme != null)
                return Optional.of(fromScheme);
        }

        return labels.keySet().stream()
            .flatMap(key -> LABEL_PREFIXES.entrySet().stream().filter(prefix -> key.startsWith(prefix.getKey())))
            .map(Map.Entry::getValue)
            .findFirst();
    }

    /**
     * @return the instance type of a node, from its well-known label
     */
    public static Optional<String> instanceType(Map<String, String> labels)
    {
        return firstLabel(labels, INSTANCE_TYPE_LABEL, LEGACY_INSTANCE_TYPE_LABEL);
    }

    /**
     * @return the provider of a managed cluster, from the host name of its API server, e.g.
     *     {@code ABC.gr7.eu-west-3.eks.amazonaws.com} or {@code my-cluster.hcp.francecentral.azmk8s.io}
     */
    public static Optional<ServerProvider> apiServerProvider(String host)
    {
        if (host == null)
            return Optional.empty();

        String lowerHost = host.toLowerCase(Locale.ROOT);
        return API_SERVER_DOMAINS.entrySet().stream()
            .filter(domain -> lowerHost.endsWith(domain.getKey()))
            .map(Map.Entry::getValue)
            .findFirst();
    }

    private static Optional<String> firstLabel(Map<String, String> labels, String... keys)
    {
        return Stream.of(keys)
            .map(labels::get)
            .filter(value -> value != null && !value.isBlank())
            .findFirst();
    }

}
