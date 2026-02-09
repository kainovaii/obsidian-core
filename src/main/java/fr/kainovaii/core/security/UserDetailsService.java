package fr.kainovaii.core.security;

import java.lang.annotation.*;
import java.util.Collection;

public interface UserDetailsService
{
    UserDetails loadByUsername(String username);

    UserDetails loadById(Object id);
}
