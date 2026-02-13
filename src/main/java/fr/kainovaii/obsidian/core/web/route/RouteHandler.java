package fr.kainovaii.obsidian.core.web.route;

import fr.kainovaii.obsidian.core.security.csrf.CsrfProtect;
import fr.kainovaii.obsidian.core.security.csrf.CsrfProtection;
import fr.kainovaii.obsidian.core.security.role.RoleChecker;
import fr.kainovaii.obsidian.core.web.di.Container;
import fr.kainovaii.obsidian.core.web.error.ErrorHandler;
import fr.kainovaii.obsidian.core.web.middleware.After;
import fr.kainovaii.obsidian.core.web.middleware.Before;
import fr.kainovaii.obsidian.core.web.middleware.MiddlewareManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class RouteHandler
{
    private static final Logger logger = LoggerFactory.getLogger(RouteHandler.class);

    public static spark.Route create(Object controller, Method method)
    {
        return (req, res) -> {
            try {
                RoleChecker.checkAccess(req, res);

                executeBeforeMiddleware(method, req, res);

                validateCsrf(controller, method, req, res);

                Object result = invokeMethod(controller, method, req, res);

                executeAfterMiddleware(method, req, res);

                return result;

            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                return ErrorHandler.handle(cause, req, res);
            } catch (Exception e) {
                return ErrorHandler.handle(e, req, res);
            }
        };
    }

    private static void executeBeforeMiddleware(Method method, Request req, Response res) throws Exception
    {
        if (method.isAnnotationPresent(Before.class)) {
            Before beforeAnnotation = method.getAnnotation(Before.class);
            MiddlewareManager.executeBefore(beforeAnnotation.value(), req, res);
        }
    }

    private static void validateCsrf(Object controller, Method method, Request req, Response res)
    {
        if (method.isAnnotationPresent(CsrfProtect.class))
        {
            if (!CsrfProtection.validate(req)) {
                logger.warn("CSRF validation failed for {}.{}",
                        controller.getClass().getSimpleName(),
                        method.getName());

                if (req.session(false) != null) {
                    req.session().attribute("flash_error", "Invalid security token. Please try again.");
                }

                res.status(403);
                throw new SecurityException("CSRF token validation failed");
            }
        }
    }

    private static Object invokeMethod(Object controller, Method method, Request req, Response res) throws Exception
    {
        method.setAccessible(true);
        Object[] args = resolveMethodParameters(method, req, res);
        return method.invoke(controller, args);
    }

    private static Object[] resolveMethodParameters(Method method, Request req, Response res)
    {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

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

        return args;
    }

    private static void executeAfterMiddleware(Method method, Request req, Response res) throws Exception
    {
        if (method.isAnnotationPresent(After.class)) {
            After afterAnnotation = method.getAnnotation(After.class);
            MiddlewareManager.executeAfter(afterAnnotation.value(), req, res);
        }
    }
}