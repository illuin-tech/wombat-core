package tech.illuin.wombat.core.asset.profile;

/**
 * Where an evaluated profile comes from: {@link #DECLARED} by the user, or else what the module inferring it read it
 * from, each module defining its own origins (e.g. as an enum implementing this interface).
 * <p>
 * Origins are told apart by their {@link #name()}, which is how they are serialized.
 */
public interface ProfileOrigin
{
    /** Declared by the user. */
    ProfileOrigin DECLARED = Declared.DECLARED;

    String name();

    enum Declared implements ProfileOrigin
    {
        DECLARED
    }
}
