package kam.kamsTweaks.utils.events;

import org.bukkit.Bukkit;
import org.bukkit.event.*;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import kam.kamsTweaks.Logger;

public class SafeEventManager {

    public static void register(Object target, Plugin plugin) {
        for (Method method : target.getClass().getDeclaredMethods()) {
            SafeEventHandler annotation = method.getAnnotation(SafeEventHandler.class);
            if (annotation == null) continue;

            if (method.getParameterCount() != 1 || !Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
                plugin.getLogger().warning("Invalid event handler: " + method.getName());
                continue;
            }

            Class<? extends Event> eventClass = method.getParameterTypes()[0].asSubclass(Event.class);
            EventPriority priority = annotation.priority();
            boolean ignoreCancelled = annotation.ignoreCancelled();

            method.setAccessible(true);

            Bukkit.getPluginManager().registerEvent(
                eventClass,
                new Listener() {},
                priority,
                    (listener, event) -> {
                        if (ignoreCancelled && event instanceof Cancellable && ((Cancellable) event).isCancelled()) {
                            return;
                        }

                        try {
                            method.invoke(target, event);
                        } catch (Exception e) {
                            StringBuilder stackTrace = new StringBuilder();
                            for (StackTraceElement element : e.getStackTrace()) {
                                stackTrace.append(element.toString()).append("\n");
                            }
                            Logger.error("Error while handling event in method: " + method.getName() + " of class: " + target.getClass().getName() + ".\nException: " + e + "\nCause: " + e.getCause() + "\nStack trace: " + stackTrace);
                        }
                    },
                plugin,
                true
            );
        }
    }
}
