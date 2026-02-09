package fr.kainovaii.core.web.controller;

import fr.kainovaii.core.security.HasRole;
import fr.kainovaii.core.security.RoleChecker;
import fr.kainovaii.core.web.di.Container;
import fr.kainovaii.core.web.route.Route;
import fr.kainovaii.core.web.route.methods.DELETE;
import fr.kainovaii.core.web.route.methods.GET;
import fr.kainovaii.core.web.route.methods.POST;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

import static spark.Spark.*;

public class ControllerLoader
{
    private static final Logger logger = LoggerFactory.getLogger(ControllerLoader.class);

    public static void loadControllers()
    {
        before("/*", RoleChecker::checkAccess);

        Reflections reflections = new Reflections("fr.kainovaii.spark.app.controllers");
        Set<Class<?>> controllerClasses = reflections.getTypesAnnotatedWith(Controller.class);

        List<Object> controllers = controllerClasses.stream()
                .map(cls -> {
                    try {
                        return cls.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        logger.error("Failed to instantiate controller: {}", cls.getName(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        registerRoutes(controllers);
        logger.info("Loaded {} controllers", controllers.size());
    }

    private static void registerRoutes(List<Object> controllers)
    {
        for (Object controller : controllers) {
            for (Method method : controller.getClass().getDeclaredMethods())
            {
                if (method.isAnnotationPresent(GET.class))
                {
                    GET getAnnotation = method.getAnnotation(GET.class);
                    String path = getAnnotation.value();
                    String name = getAnnotation.name();
                    Route.registerNamedRoute(name, path);
                    registerRoleIfPresent(method, path);
                    get(path, createRoute(controller, method));
                    logger.debug("Registered GET route: {} -> {}", name, path);
                }
                if (method.isAnnotationPresent(POST.class))
                {
                    POST postAnnotation = method.getAnnotation(POST.class);
                    String path = postAnnotation.value();
                    String name = postAnnotation.name();
                    Route.registerNamedRoute(name, path);
                    registerRoleIfPresent(method, path);
                    post(path, createRoute(controller, method));
                    logger.debug("Registered POST route: {} -> {}", name, path);
                }
                if (method.isAnnotationPresent(DELETE.class))
                {
                    DELETE deleteAnnotation = method.getAnnotation(DELETE.class);
                    String path = deleteAnnotation.value();
                    String name = deleteAnnotation.name();
                    Route.registerNamedRoute(name, path);
                    registerRoleIfPresent(method, path);
                    delete(path, createRoute(controller, method));
                    logger.debug("Registered DELETE route: {} -> {}", name, path);
                }
            }
        }
    }

    private static void registerRoleIfPresent(Method method, String path)
    {
        if (method.isAnnotationPresent(HasRole.class)) {
            HasRole roleAnnotation = method.getAnnotation(HasRole.class);
            RoleChecker.registerPathWithRole(path, roleAnnotation.value());
        }
    }

    private static spark.Route createRoute(Object controller, Method method)
    {
        return (req, res) -> {
            try {
                RoleChecker.checkAccess(req, res);
                method.setAccessible(true);

                // Prepare method parameters
                Parameter[] parameters = method.getParameters();
                Object[] args = new Object[parameters.length];

                // Dependency injection
                for (int i = 0; i < parameters.length; i++) {
                    Class<?> paramType = parameters[i].getType();

                    if (paramType == Request.class) {
                        args[i] = req;
                    } else if (paramType == Response.class) {
                        args[i] = res;
                    } else {
                        args[i] = Container.resolve(paramType);
                    }
                }

                // Invoke controller method
                return method.invoke(controller, args);

            } catch (InvocationTargetException e) {
                // Unwrap the real exception thrown by the controller method
                Throwable cause = e.getCause();
                logger.error("Error in route {}.{}: {}",
                        controller.getClass().getSimpleName(),
                        method.getName(),
                        cause.getMessage(),
                        cause);

                // Handle user-facing error with flash message
                if (req.session() != null) {
                    req.session().attribute("flash_error", "An error occurred. Please try again.");
                }

                // Redirect to error page or return error response
                if (res.type() == null || res.type().contains("html")) {
                    res.redirect("/error");
                    return null;
                } else {
                    res.status(500);
                    res.type("application/json");
                    return "{\"error\":\"Internal server error\"}";
                }

            } catch (Exception e) {
                // Handle framework-level errors (reflection, DI, etc.)
                logger.error("Framework error in route {}.{}: {}",
                        controller.getClass().getSimpleName(),
                        method.getName(),
                        e.getMessage(),
                        e);

                res.status(500);
                res.type("application/json");
                return "{\"error\":\"Internal server error\"}";
            }
        };
    }
}