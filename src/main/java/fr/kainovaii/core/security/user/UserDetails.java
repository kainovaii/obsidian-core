package fr.kainovaii.core.security.user;

public interface UserDetails
{
    Object getId();
    String getUsername();
    String getPassword();
    String getRole();

    default boolean isEnabled() { return true; }
}
