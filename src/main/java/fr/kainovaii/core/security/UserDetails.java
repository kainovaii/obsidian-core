package fr.kainovaii.core.security;

import java.util.Collection;

public interface UserDetails
{
    Object getId();
    String getUsername();
    String getPassword();
    String getRole();

    default boolean isEnabled() { return true; }
}
