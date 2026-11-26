package tech.illuin.wombat.core.asset.profile;

import java.util.List;

/**
 * A server profile inferred rather than declared, with what it was read from and what it had to assume.
 */
public interface InferredProfile
{
    ProfileOrigin origin();

    /**
     * @return what the inference had to assume or leave out, worth telling whoever reads the evaluation
     */
    List<String> warnings();
}
