package fr.kainovaii.obsidian.core.web.route;

import fr.kainovaii.obsidian.core.security.role.HasRole;
import fr.kainovaii.obsidian.core.security.role.RoleChecker;
import fr.kainovaii.obsidian.core.web.route.methods.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;

import static spark.Spark.*;

public class RouteLoader
{
    private static final Logger logger = LoggerFactory.getLogger(RouteLoader.class);

    public static void registerRoutes(List<Object> controllers)
    {
        for (Object controller : controllers) {
            registerControllerRoutes(controller);
        }
    }

    private static void registerControllerRoutes(Object controller)
    {
        for (Method method : controller.getClass().getDeclaredMethods()) {
            registerGetRoute(controller, method);
            registerPostRoute(controller, method);
            registerPutRoute(controller, method);
            registerPatchRoute(controller, method);
            registerDeleteRoute(controller, method);
            registerOptionsRoute(controller, method);
            registerHeadRoute(controller, method);
        }
    }

    private static void registerGetRoute(Object controller, Method method)
    {
        if (!method.isAnnotationPresent(GET.class)) return;

        GET annotation = method.getAnnotation(GET.class);
        String path = annotation.value();
        String name = annotation.name();

        Route.registerNamedRoute(name, path);
        registerRoleIfPresent(method, path);
        get(path, RouteHandler.create(controller, method));

        logger.debug("Registered GET route: {} -> {}", name, path);
    }

    private static void registerPostRoute(Object controller, Method method)
    {
        if (!method.isAnnotationPresent(POST.class)) return;

        POST annotation = method.getAnnotation(POST.class);
        String path = annotation.value();
        String name = annotation.name();

        Route.registerNamedRoute(name, path);
        registerRoleIfPresent(method, path);
        post(path, RouteHandler.create(controller, method));

        logger.debug("Registered POST route: {} -> {}", name, path);
    }

    private static void registerPutRoute(Object controller, Method method)
    {
        if (!method.isAnnotationPresent(PUT.class)) return;

        PUT annotation = method.getAnnotation(PUT.class);
        String path = annotation.value();
        String name = annotation.name();

        Route.registerNamedRoute(name, path);
        registerRoleIfPresent(method, path);
        put(path, RouteHandler.create(controller, method));

        logger.debug("Registered PUT route: {} -> {}", name, path);
    }

    private static void registerPatchRoute(Object controller, Method method)
    {
        if (!method.isAnnotationPresent(PATCH.class)) return;

        PATCH annotation = method.getAnnotation(PATCH.class);
        String path = annotation.value();
        String name = annotation.name();

        Route.registerNamedRoute(name, path);
        registerRoleIfPresent(method, path);
        patch(path, RouteHandler.create(controller, method));

        logger.debug("Registered PATCH route: {} -> {}", name, path);
    }

    private static void registerDeleteRoute(Object controller, Method method)
    {
        if (!method.isAnnotationPresent(DELETE.class)) return;

        DELETE annotation = method.getAnnotation(DELETE.class);
        String path = annotation.value();
        String name = annotation.name();

        Route.registerNamedRoute(name, path);
        registerRoleIfPresent(method, path);
        delete(path, RouteHandler.create(controller, method));

        logger.debug("Registered DELETE route: {} -> {}", name, path);
    }

    private static void registerOptionsRoute(Object controller, Method method)
    {
        if (!method.isAnnotationPresent(OPTIONS.class)) return;

        OPTIONS annotation = method.getAnnotation(OPTIONS.class);
        String path = annotation.value();
        String name = annotation.name();

        Route.registerNamedRoute(name, path);
        registerRoleIfPresent(method, path);
        options(path, RouteHandler.create(controller, method));

        logger.debug("Registered OPTIONS route: {} -> {}", name, path);
    }

    private static void registerHeadRoute(Object controller, Method method)
    {
        if (!method.isAnnotationPresent(HEAD.class)) return;

        HEAD annotation = method.getAnnotation(HEAD.class);
        String path = annotation.value();
        String name = annotation.name();

        Route.registerNamedRoute(name, path);
        registerRoleIfPresent(method, path);
        head(path, RouteHandler.create(controller, method));

        logger.debug("Registered HEAD route: {} -> {}", name, path);
    }

    private static void registerRoleIfPresent(Method method, String path)
    {
        if (method.isAnnotationPresent(HasRole.class)) {
            HasRole roleAnnotation = method.getAnnotation(HasRole.class);
            RoleChecker.registerPathWithRole(path, roleAnnotation.value());
        }
    }
}