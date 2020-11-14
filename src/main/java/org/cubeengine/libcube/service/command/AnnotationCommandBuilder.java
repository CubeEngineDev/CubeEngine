/*
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cubeengine.libcube.service.command;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Greedy;
import org.cubeengine.libcube.service.command.annotation.Label;
import org.cubeengine.libcube.service.command.annotation.Named;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.command.annotation.ParameterPermission;
import org.cubeengine.libcube.service.command.annotation.Parser;
import org.cubeengine.libcube.service.command.annotation.ParserFor;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.command.annotation.Using;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.command.Command.Builder;
import org.spongepowered.api.command.Command.Parameterized;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.manager.CommandMapping;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.Parameter.Key;
import org.spongepowered.api.command.parameter.Parameter.Value;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;
import org.spongepowered.api.command.parameter.managed.ValueParser;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.plugin.PluginContainer;

import static org.spongepowered.api.command.Command.builder;


/*
TODO handle @Alias with spaces
TODO @Restricted msg translation
 */
@Singleton
public class AnnotationCommandBuilder
{

    private I18n i18n;

    @Inject
    public AnnotationCommandBuilder(I18n i18n)
    {
        this.i18n = i18n;
    }

    public Map<CommandMapping, Parameterized> registerModuleCommands(Injector injector, RegisterCommandEvent<Parameterized> event,
                                                                     PluginContainer plugin, Object module, List<Field> commands)
    {
        Map<CommandMapping, org.spongepowered.api.command.Command.Parameterized> moduleCommands = new HashMap<>();
        for (Field command : commands)
        {
            try
            {
                command.setAccessible(true);
                this.registerCommands(injector, event, plugin, command.get(module), moduleCommands);
            }
            catch (IllegalAccessException e)
            {
                throw new IllegalStateException(e);
            }
        }
        return moduleCommands;
    }

    public void registerCommands(Injector injector, RegisterCommandEvent<Parameterized> event, PluginContainer plugin,
                                 Object holder, Map<CommandMapping, org.spongepowered.api.command.Command.Parameterized> moduleCommands)
    {

        final Command holderAnnotation = holder.getClass().getAnnotation(Command.class);
        this.createParsers(injector, holder);
        if (holderAnnotation != null)
        {
            final Builder builder = builder();
            final String name = this.getCommandName(null, holder, holderAnnotation);

            builder.setShortDescription(Component.text(holderAnnotation.desc()));

            //        builder.setExecutionRequirements()?
            //        builder.setExtendedDescription()!
            final Optional<CommandExecutor> dispatcherExecutor = this.createChildCommands(event, injector, plugin, holder, builder, getBasePerm(plugin), "command", name);

            final HelpExecutor helpExecutor = new HelpExecutor(i18n);
            builder.setExecutor(new DispatcherExecutor(helpExecutor, dispatcherExecutor.orElse(null)));
            builder.child(builder().setExecutor(helpExecutor).build(), "?");

            final Parameterized build = builder.build();
            helpExecutor.init(build, null,
                              String.join(".", Arrays.asList(getBasePerm(plugin), "command", name)));

            final CommandMapping mapping = event.register(plugin, build, name, holderAnnotation.alias());
            moduleCommands.put(mapping, build);
        }
        else
        {
            if (holder instanceof DispatcherCommand)
            {
                throw new IllegalStateException(
                    "Base command needs a Command annotation! " + holder.getClass().getSimpleName());
            }
            final Set<Method> methods = this.getMethods(holder.getClass()).stream().filter(
                m -> m.isAnnotationPresent(Command.class)).collect(Collectors.toSet());
            for (Method method : methods)
            {
                final Command methodAnnotation = method.getAnnotation(Command.class);
                String name = this.getCommandName(method, holder, methodAnnotation);
                try
                {
                    final Parameterized build = this.buildCommand(injector, holder, method, methodAnnotation, getBasePerm(plugin), "command", name);
                    final CommandMapping mapping = event.register(plugin, build, name, methodAnnotation.alias());
                    moduleCommands.put(mapping, build);
                }
                catch (Exception e)
                {
                    throw new IllegalStateException("Failed to register command " + name, e);
                }
            }
        }
    }

    public String getBasePerm(PluginContainer plugin)
    {
        final String id = plugin.getMetadata().getId();
        if (id.startsWith("cubeengine-"))
        {
            return "cubeengine." + id.substring(11);
        }
        return id;
    }

    public void createParsers(Injector injector, Object holder)
    {
        final Using using = holder.getClass().getAnnotation(Using.class);
        if (using != null)
        {
            for (Class<?> parser : using.value())
            {
                final Object parserInstance = injector.getInstance(parser);
                final Class<?> parsedType = parser.getAnnotation(ParserFor.class).value();
                ParameterRegistry.register(parsedType, parserInstance);
            }
        }
    }

    private Optional<CommandExecutor> createChildCommands(RegisterCommandEvent<Parameterized> event, Injector injector, PluginContainer plugin, Object holder, Builder dispatcher, String... permNodes)
    {
        final String basePermNode = String.join(".", permNodes);
        dispatcher.setPermission(basePermNode + ".use");
        Optional<CommandExecutor> dispatcherExecutor = Optional.empty();
        final Set<Method> methods = this.getMethods(holder.getClass()).stream().filter(
            m -> m.isAnnotationPresent(Command.class)).collect(Collectors.toSet());
        for (Method method : methods)
        {
            final Command methodAnnotation = method.getAnnotation(Command.class);

            if (methodAnnotation.dispatcher())
            {
                String name = this.getCommandName(method, holder, methodAnnotation);
                final String[] newPermNodes = Arrays.copyOf(permNodes, permNodes.length + 1);
                newPermNodes[permNodes.length] = name;
                final Parameterized build = buildCommand(injector, holder, method, methodAnnotation, newPermNodes);

                dispatcher.parameters(build.parameters());
                for (org.spongepowered.api.command.parameter.managed.Flag flag : build.flags())
                {
                    dispatcher.flag(flag);
                }
                // TODO execution requirements - but they do not apply for child commands does it work?
                final Predicate<CommandCause> requirements = build.getExecutionRequirements();
                // TODO would get overwritten by other requirements... maybe wrap executor instead?
                dispatcher.setExecutionRequirements(requirements);

                dispatcherExecutor = build.getExecutor();
            }
            else
            {
                this.createChildCommand(event, injector, plugin, holder, dispatcher, method, methodAnnotation, permNodes);
            }

        }

        if (holder instanceof DispatcherCommand)
        {
            for (Object subHolder : ((DispatcherCommand)holder).getSubCommands())
            {
                this.createParsers(injector, subHolder);
                final Command subHolderAnnotation = subHolder.getClass().getAnnotation(Command.class);
                if (subHolderAnnotation != null)
                {
                    final Builder builder = builder();
                    final String name = this.getCommandName(null, subHolder, subHolderAnnotation);
                    builder.setShortDescription(Component.text(subHolderAnnotation.desc()));
                    //        builder.setExecutionRequirements()?
                    //        builder.setExtendedDescription()!
                    final String[] newPermNodes = Arrays.copyOf(permNodes, permNodes.length + 1);
                    newPermNodes[permNodes.length] = name;
                    final Optional<CommandExecutor> subDispatcherExecutor = this.createChildCommands(event, injector, plugin, subHolder, builder, newPermNodes);
                    final HelpExecutor helpExecutor = new HelpExecutor(i18n);
                    builder.setExecutor(new DispatcherExecutor(helpExecutor, subDispatcherExecutor.orElse(null)));
                    final List<String> alias = new ArrayList<>();
                    alias.add(name);
                    alias.addAll(Arrays.asList(subHolderAnnotation.alias()));

                    final Parameterized helpChild = builder().setExecutor(helpExecutor).build();
                    builder.child(helpChild, "?");

                    final Parameterized build = builder.build();
                    helpExecutor.init(build, null, String.join(".", permNodes));
                    dispatcher.child(build, alias);

                    final Alias aliasAnnotation = subHolder.getClass().getAnnotation(Alias.class);
                    if (aliasAnnotation != null)
                    {
                        event.register(plugin, build, aliasAnnotation.value(), aliasAnnotation.alias());
                    }
                }
                else
                {
                    this.createChildCommands(event, injector, plugin, subHolder, dispatcher, permNodes);
                }
            }
        }
        return dispatcherExecutor;
    }

    private void createChildCommand(RegisterCommandEvent<Parameterized> event, Injector injector, PluginContainer plugin, Object holder, Builder dispatcher, Method method, Command methodAnnotation, String... permNodes)
    {
        String name = this.getCommandName(method, holder, methodAnnotation);
        final String[] newPermNodes = Arrays.copyOf(permNodes, permNodes.length + 1);
        newPermNodes[permNodes.length] = name;
        final Parameterized build = buildCommand(injector, holder, method, methodAnnotation, newPermNodes);
        final List<String> alias = new ArrayList<>();
        alias.add(name);
        alias.addAll(Arrays.asList(methodAnnotation.alias()));
        dispatcher.child(build, alias);

        final Alias aliasAnnotation = method.getAnnotation(Alias.class);
        if (aliasAnnotation != null)
        {
            event.register(plugin, build, aliasAnnotation.value(), aliasAnnotation.alias());
        }

    }

    private String getCommandName(Method method, Object holder, Command cmd)
    {
        String name = cmd.name();
        if (!name.isEmpty())
        {
            return name;
        }
        if (method == null)
        {
            throw new IllegalStateException("Command needs a name: " + holder.getClass().getSimpleName());
        }
        return method.getName();
    }

    public Set<Method> getMethods(Class<?> holder)
    {
        HashSet<Method> methods = new LinkedHashSet<>(Arrays.asList(holder.getMethods()));
        for (Method method : methods)
        {
            method.setAccessible(true);
        }
        methods.addAll(Arrays.asList(holder.getDeclaredMethods()));
        return methods;
    }

    public static class Requirements implements Predicate<CommandCause>
    {
        private final List<Predicate<CommandCause>> requirements = new ArrayList<>();
        private String permission;

        @Override
        public boolean test(CommandCause commandCause)
        {
            for (Predicate<CommandCause> requirement : this.requirements)
            {
                if (!requirement.test(commandCause))
                {
                    return false;
                }
            }
            return true;
        }

        public void add(Predicate<CommandCause> predicate)
        {
            this.requirements.add(predicate);
        }

        public void add(Restricted restricted)
        {
            if (restricted != null)
            {
                final Class<?> restrictedTo = restricted.value();
                final String msg = restricted.msg().isEmpty() ? "Command is restricted to " + restrictedTo.getSimpleName() : restricted.msg();

                this.add(commandCause -> {
                    if (!restrictedTo.isAssignableFrom(commandCause.getSubject().getClass()))
                    {
                        commandCause.getAudience().sendMessage(Identity.nil(), Component.text(msg));
                    }
                    return true;
                });
            }
        }

        public void addPermission(String permission)
        {
            this.permission = permission;
            this.requirements.add(c -> c.hasPermission(permission));
        }

        public String getPermission()
        {
            return permission;
        }
    }

    private Parameterized buildCommand(Injector injector, Object holder, Method method, Command annotation, String... permNodes)
    {
        try
        {
            final Builder builder = builder();

            final Annotation[][] annotationsList = method.getParameterAnnotations();
            final Type[] types = method.getParameterTypes();
            final java.lang.reflect.Parameter[] parameters = method.getParameters();
            final List<ContextExtractor<?>> extractors = new ArrayList<>();
            final Requirements requirements = new Requirements();
            List<Value.Builder> params = new ArrayList<>();
            Map<Named, Value.Builder> namedParameter = new LinkedHashMap<>();
            List<org.spongepowered.api.command.parameter.managed.Flag> flags = new ArrayList<>();
            for (int i = 0; i < types.length; i++)
            {
                final Type type = types[i];
                final Annotation[] annotations = annotationsList[i];
                final java.lang.reflect.Parameter parameter = parameters[i];
                extractors.add(
                    this.buildParameter(i, params, namedParameter, flags, parameter, type, annotations, types.length - 1, requirements, injector, permNodes));
            }
            buildParams(builder, params, namedParameter, flags);
            requirements.addPermission(String.join(".", permNodes) + ".use");
            requirements.add(method.getAnnotation(Restricted.class));
            builder.setExecutionRequirements(requirements);
            builder.setShortDescription(Component.text(annotation.desc()));
//        builder.setExtendedDescription()
            final CubeEngineCommand executor = new CubeEngineCommand(holder, method, extractors, injector);
            builder.setExecutor(executor);
            final HelpExecutor helpExecutor = new HelpExecutor(i18n);
            builder.child(builder().setExecutor(helpExecutor).build(), "?");
            final Parameterized build = builder.build();
            helpExecutor.init(build, executor, String.join(".", permNodes));
            return build;
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Exception while building command for\n" + method, e);
        }
    }

    private void buildParams(Builder builder, List<Parameter.Value.Builder> params, Map<Named, Parameter.Value.Builder> namedParams, List<org.spongepowered.api.command.parameter.managed.Flag> flags)
    {

        for (int i = 0; i < params.size(); i++)
        {
            final Value.Builder param = params.get(i);
            if (i == params.size() - 1)
            {
                param.terminal();
            }
            builder.parameter(param.build());
        }
        for (org.spongepowered.api.command.parameter.managed.Flag flag : flags)
        {
            builder.flag(flag);
        }
        for (Entry<Named, Parameter.Value.Builder> namedParam : namedParams.entrySet())
        {
            {
                // Flag experiment
//                builder.flag(org.spongepowered.api.command.parameter.managed.Flag.of(namedParam.getValue(), namedParam.getKey().value()));
            }
            {
                // Sequence
                final Value<Boolean> literal = Parameter.literal(Boolean.class, true, namedParam.getKey().value()).setKey(namedParam.getKey().value()[0]).build();
                final Parameter named = Parameter.seqBuilder(literal).then(namedParam.getValue().build()).optional().terminal().build();
                builder.parameter(named);
            }



        }
    }

    private ContextExtractor<?> buildParameter(int index, List<Parameter.Value.Builder> params, Map<Named, Parameter.Value.Builder> namedParameter,
                                               List<org.spongepowered.api.command.parameter.managed.Flag> flags,
                                               java.lang.reflect.Parameter parameter, Type type,
                                               Annotation[] annotations, int last, Requirements requirements, Injector injector, String[] permNodes)
    {
        final String name = parameter.getName();
        // TODO search param annotation for name

        return buildParameter(index, params, namedParameter, flags, type, annotations, last, name, false, requirements, injector, permNodes);
    }

    private ContextExtractor<?> buildParameter(int index, List<Parameter.Value.Builder> params, Map<Named, Parameter.Value.Builder> namedParameter,
                                               List<org.spongepowered.api.command.parameter.managed.Flag> flags,
                                               Type type, Annotation[] annotations, int last, String name,
                                               boolean forceOptional, Requirements requirements, Injector injector, String[] permNodes)
    {

        if (type == CommandCause.class)
        {
            return COMMAND_CAUSE;
        }
        if (type == CommandContext.class)
        {
            return COMMAND_CONTEXT;
        }
        if (index == 0)
        {
            if (type == ServerPlayer.class)
            {
                // First Parameter and ServerPlayer == restricted ServerPlayer command
                requirements.add(AnnotationCommandBuilder::playerRestricted);
                return COMMAND_PLAYER;
            }
            if (type == Audience.class)
            {
                return COMMAND_AUDIENCE;
            }
        }

        final Flag flagAnnotation = getAnnotated(annotations, Flag.class);
        final Parser parserAnnotation = getAnnotated(annotations, Parser.class);
        final ParameterPermission permAnnotation = getAnnotated(annotations, ParameterPermission.class);

        Class<?> rawType = (Class<?>)(type instanceof ParameterizedType ? ((ParameterizedType)type).getRawType() : type);
        if (flagAnnotation != null)
        {
            return buildFlagParameter(flags, name, flagAnnotation, permAnnotation, rawType, permNodes);
        }
        final Parameter.Value.Builder<?> parameterBuilder;
        if (rawType == Optional.class)
        {
            return this.buildParameter(index, params, namedParameter, flags, ((ParameterizedType)type).getActualTypeArguments()[0],
                                       annotations, last, name, true, requirements, injector, permNodes);
        }
        else if (rawType.isArray())
        {
            throw new IllegalStateException("Not implemented yet");
        }
        else if (rawType == List.class)
        {
            throw new IllegalStateException("Not implemented yet");
        }
        else if (rawType == Set.class)
        {
            throw new IllegalStateException("Not implemented yet");
        }
        else
        {
            Greedy greedyAnnotation = getAnnotated(annotations, Greedy.class);
            final Class<?> parserType = parserAnnotation != null && parserAnnotation.parser()
                != ValueParser.class ? parserAnnotation.parser() : rawType;
            final ValueParser parser = ParameterRegistry.getParser(injector, parserType, index == last, greedyAnnotation != null);
            if (parser != null)
            {
                parameterBuilder = Parameter.builder(rawType).parser(parser);
            }
            else
            {
                throw new IllegalArgumentException("Could not build Parameter for type: " + TypeToken.get(type));
            }

            final Class<?> completerType = parserAnnotation != null && parserAnnotation.completer()
                != ValueCompleter.class ? parserAnnotation.completer() : rawType;
            final ValueCompleter completer = ParameterRegistry.getCompleter(injector, completerType);
            if (completer != null)
            {
                parameterBuilder.setSuggestions(completer);
            }
            else if (rawType == String.class)
            {
                parameterBuilder.setSuggestions((context, currentInput) -> Collections.emptyList());
            }
        }

        parameterBuilder.setKey(name);
        final Key<?> key = Parameter.key(name, TypeToken.get(rawType));

        Default defaultAnnotation = getAnnotated(annotations, Default.class);
        Named namedAnnotation = getAnnotated(annotations, Named.class);

        boolean optional = defaultAnnotation != null || namedAnnotation != null || isOptional(annotations);

        final DefaultParameterProvider defaultParameterProvider;
        if (defaultAnnotation != null)
        {
            Class<?> clazz = defaultAnnotation.value();
            if (clazz == DefaultParameterProvider.class)
            {
                clazz = rawType;
            }
            defaultParameterProvider = ParameterRegistry.getDefaultProvider(injector, clazz);
            if (namedAnnotation == null)
            {
                parameterBuilder.optional().orDefault(defaultParameterProvider);
            }
        }
        else
        {
            defaultParameterProvider = null;
            if (optional && namedAnnotation == null)
            {
                parameterBuilder.optional();
            }
        }

        Label labelAnnotation = getAnnotated(annotations, Label.class);
        if (labelAnnotation != null)
        {
            parameterBuilder.setUsage(k -> labelAnnotation.value());
        }

        if (permAnnotation != null)
        {
            parameterBuilder.setRequiredPermission(String.join(".", permNodes) + "." + name);
        }

        if (namedAnnotation != null)
        {
            if (namedAnnotation.value().length == 0)
            {
                throw new IllegalArgumentException("Named parameter must have at least one name");
            }
            namedParameter.put(namedAnnotation, parameterBuilder);
        }
        else
        {
            params.add(parameterBuilder);
        }

        if (forceOptional)
        {
            return c -> c.getOne(key);
        }
        else if (optional)
        {
            if (namedAnnotation != null && defaultParameterProvider != null)
            {
                return c -> ((Optional)c.getOne(key)).orElse(defaultParameterProvider.apply(c.getCause()));
            }
            return c -> c.getOne(key).orElse(null);
        }
        else
        {
            return c -> c.requireOne(key);
        }
    }

    private ContextExtractor<Object> buildFlagParameter(List<org.spongepowered.api.command.parameter.managed.Flag> flags, String name, Flag flagAnnotation,
                                                        ParameterPermission permAnnotation, Class<?> rawType, String[] permNodes)
    {
        if (rawType == Boolean.class || rawType == boolean.class)
        {
            String longName = flagAnnotation.longName();
            if (longName.isEmpty())
            {
                longName = name;
            }
            String shortName = flagAnnotation.value();
            if (shortName.isEmpty())
            {
                shortName = longName.substring(0, 1);
            }

            final org.spongepowered.api.command.parameter.managed.Flag.Builder builder = org.spongepowered.api.command.parameter.managed.Flag.builder().aliases(shortName, longName);
            if (permAnnotation != null)
            {
                builder.setPermission(String.join(".", permNodes) + "." + name);
            }

            final org.spongepowered.api.command.parameter.managed.Flag flag = builder.build();
            flags.add(flag);
            return (c -> c.hasFlag(flag));
        }
        throw new IllegalArgumentException("@Flag parameter must be a boolean");
    }

    private static boolean playerRestricted(CommandCause cause)
    {
        final boolean isPlayer = cause.getAudience() instanceof ServerPlayer;
        if (!isPlayer)
        {
            // TODO show error message?
            // TODO translate
//            throw new IllegalStateException("This command is restricted to players in game");
        }
        return isPlayer;
    }

    private <T extends Annotation> T getAnnotated(Annotation[] annotations, Class<T> clazz)
    {
        for (Annotation annotation : annotations)
        {
            if (clazz.isAssignableFrom(annotation.getClass()))
            {
                return clazz.cast(annotation);
            }
        }
        return null;
    }

    private boolean isOptional(Annotation[] annotations)
    {
        for (Annotation annotation : annotations)
        {
            if (annotation instanceof Option)
            {
                return true;
            }
        }
        return false;
    }

    public void injectCommands(Injector injector, Object module, List<Field> fields)
    {
        for (Field field : fields)
        {
            Object command = injector.getInstance(field.getType());
            try
            {
                field.setAccessible(true);
                field.set(module, command);
            }
            catch (IllegalAccessException e)
            {
                throw new IllegalStateException(e);
            }
        }
    }

    public interface ContextExtractor<T> extends Function<CommandContext, T>
    {
    }

    private static final ContextExtractor<CommandCause> COMMAND_CAUSE = CommandContext::getCause;
    private static final ContextExtractor<CommandContext> COMMAND_CONTEXT = c -> c;
    private static final ContextExtractor<ServerPlayer> COMMAND_PLAYER = c -> (ServerPlayer)c.getCause().getAudience();
    private static final ContextExtractor<Audience> COMMAND_AUDIENCE = c -> c.getCause().getAudience();
}
