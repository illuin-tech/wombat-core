package tech.illuin.wombat.module.conformance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.time.Duration;

/**
 * The Boavizta and Ecologits sidecars, started once for every conformance test. Testcontainers removes them when the JVM
 * exits.
 */
final class ConformanceSidecars
{
    /* The images the app ships (compose/, wombat-k8s), pinned by digest: boaviztapi 2.4.1 and ecologits-api 0.0.2 */
    private static final String BOAVIZTA_IMAGE = "ghcr.io/boavizta/boaviztapi@sha256:bc9b2756bc7fdea3d632d03931fd0a955dd2bbbe90003e0ca61891a7f2a3e4e8";
    private static final String ECOLOGITS_IMAGE = "ghcr.io/mlco2/ecologits-api@sha256:a6475ffdce34c69a590cd5cc5576a74a3f846c594edd1672c1dfb962bd4f57e3";
    private static final int BOAVIZTA_PORT = 5000;
    private static final int ECOLOGITS_PORT = 80;
    private static final GenericContainer<?> boavizta = container(BOAVIZTA_IMAGE, BOAVIZTA_PORT, "/v1/utils/version");
    private static final GenericContainer<?> ecologits = container(ECOLOGITS_IMAGE, ECOLOGITS_PORT, "/v1beta/providers");

    private static final Logger logger = LoggerFactory.getLogger(ConformanceSidecars.class);

    static
    {
        boavizta.start();
        ecologits.start();
        logger.info("Conformance sidecars: boaviztapi {}, ecologits {}", boaviztaVersion(), ecologitsVersion());
    }

    private ConformanceSidecars() {}

    /** The base URI the app is configured with, e.g. {@code https://api.boavizta.org/v1/}. */
    static URI boaviztaUri()
    {
        return uri(boavizta, BOAVIZTA_PORT, "/v1/");
    }

    static URI ecologitsUri()
    {
        return uri(ecologits, ECOLOGITS_PORT, "/");
    }

    static String boaviztaVersion()
    {
        return exec(boavizta, "pip show boaviztapi | sed -n 's/^Version: //p'");
    }

    /* The API image's own tag says nothing of the library it wraps, whose version sets the methodology */
    static String ecologitsVersion()
    {
        return exec(ecologits, "ls -d /app/.venv/lib/python*/site-packages/ecologits-*.dist-info | sed 's/.*ecologits-//; s/.dist-info//'");
    }

    private static GenericContainer<?> container(String image, int port, String readinessPath)
    {
        return new GenericContainer<>(DockerImageName.parse(image))
            .withExposedPorts(port)
            .waitingFor(Wait.forHttp(readinessPath).forPort(port).withStartupTimeout(Duration.ofMinutes(2)))
        ;
    }

    private static URI uri(GenericContainer<?> container, int port, String path)
    {
        return URI.create("http://" + container.getHost() + ":" + container.getMappedPort(port) + path);
    }

    private static String exec(GenericContainer<?> container, String command)
    {
        try {
            Container.ExecResult result = container.execInContainer("sh", "-c", command);
            String version = result.getStdout().strip();
            return version.isEmpty() ? "unknown" : version;
        }
        catch (Exception e)
        {
            logger.warn("Could not read the version in {}", container.getDockerImageName(), e);
            return "unknown";
        }
    }
}
