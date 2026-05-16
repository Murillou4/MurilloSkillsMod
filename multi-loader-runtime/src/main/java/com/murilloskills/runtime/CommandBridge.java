package com.murilloskills.runtime;

import com.murilloskills.core.config.SkillType;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

final class CommandBridge {
    private CommandBridge() {
    }

    static void register(Object dispatcher, MurilloSkillsRuntime runtime) {
        if (dispatcher == null) {
            return;
        }
        try {
            Class<?> builderType = Class.forName("com.mojang.brigadier.builder.LiteralArgumentBuilder");
            Class<?> commandType = Class.forName("com.mojang.brigadier.Command");
            Method literal = builderType.getMethod("literal", String.class);
            Method then = builderType.getMethod("then", Class.forName("com.mojang.brigadier.builder.ArgumentBuilder"));
            Method executes = builderType.getMethod("executes", commandType);

            Object root = literal.invoke(null, "murilloskills");
            executes.invoke(root, command(commandType, new CommandAction() {
                public int run(Object source) {
                    return runtime.stats(source);
                }
            }));

            then.invoke(root, literalCommand(literal, executes, "stats", command(commandType, new CommandAction() {
                public int run(Object source) {
                    return runtime.stats(source);
                }
            })));

            then.invoke(root, skillCommand(literal, then, executes, commandType, "select", new SkillAction() {
                public int run(Object source, SkillType skill) {
                    return runtime.selectSkill(source, skill);
                }
            }));
            then.invoke(root, skillCommand(literal, then, executes, commandType, "paragon", new SkillAction() {
                public int run(Object source, SkillType skill) {
                    return runtime.activateParagon(source, skill);
                }
            }));
            then.invoke(root, skillCommand(literal, then, executes, commandType, "prestige", new SkillAction() {
                public int run(Object source, SkillType skill) {
                    return runtime.prestige(source, skill);
                }
            }));
            then.invoke(root, skillCommand(literal, then, executes, commandType, "ability", new SkillAction() {
                public int run(Object source, SkillType skill) {
                    return runtime.triggerAbility(source, skill);
                }
            }));
            then.invoke(root, xpCommand(literal, then, executes, commandType, runtime));
            then.invoke(root, literalCommand(literal, executes, "reset", command(commandType, new CommandAction() {
                public int run(Object source) {
                    return runtime.reset(source);
                }
            })));

            dispatcher.getClass().getMethod("register", builderType).invoke(dispatcher, root);
            runtime.log("registered /murilloskills");
        } catch (Throwable ex) {
            runtime.log("command registration skipped: " + ex.getClass().getSimpleName() + " " + ex.getMessage());
        }
    }

    private static Object xpCommand(Method literal, Method then, Method executes, Class<?> commandType, final MurilloSkillsRuntime runtime) throws Exception {
        Object xp = literal.invoke(null, "xp");
        for (final SkillType skill : SkillType.values()) {
            Object skillNode = literal.invoke(null, skill.name().toLowerCase());
            for (final int amount : new int[] {100, 1000, 10000}) {
                then.invoke(skillNode, literalCommand(literal, executes, String.valueOf(amount), command(commandType, new CommandAction() {
                    public int run(Object source) {
                        return runtime.addXp(source, skill, amount);
                    }
                })));
            }
            then.invoke(xp, skillNode);
        }
        return xp;
    }

    private static Object skillCommand(Method literal, Method then, Method executes, Class<?> commandType, String name, final SkillAction action) throws Exception {
        Object root = literal.invoke(null, name);
        for (final SkillType skill : SkillType.values()) {
            then.invoke(root, literalCommand(literal, executes, skill.name().toLowerCase(), command(commandType, new CommandAction() {
                public int run(Object source) {
                    return action.run(source, skill);
                }
            })));
        }
        return root;
    }

    private static Object literalCommand(Method literal, Method executes, String name, Object command) throws Exception {
        Object node = literal.invoke(null, name);
        executes.invoke(node, command);
        return node;
    }

    private static Object command(Class<?> commandType, final CommandAction action) {
        return Proxy.newProxyInstance(commandType.getClassLoader(), new Class<?>[] {commandType}, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("run".equals(method.getName())) {
                    Object source = args != null && args.length > 0 ? ReflectionSupport.invokeAny(args[0], "getSource") : null;
                    return Integer.valueOf(action.run(source));
                }
                return defaultValue(method.getReturnType());
            }
        });
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return Boolean.FALSE;
        }
        if (type == int.class || type == short.class || type == byte.class) {
            return Integer.valueOf(0);
        }
        if (type == long.class) {
            return Long.valueOf(0L);
        }
        if (type == float.class) {
            return Float.valueOf(0.0F);
        }
        if (type == double.class) {
            return Double.valueOf(0.0D);
        }
        return null;
    }

    private interface CommandAction {
        int run(Object source);
    }

    private interface SkillAction {
        int run(Object source, SkillType skill);
    }
}
