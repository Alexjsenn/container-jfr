package es.andrewazor.containertest.commands;

import java.util.Set;

import dagger.Module;
import dagger.Provides;
import es.andrewazor.containertest.commands.internal.CommandsInternalModule;
import es.andrewazor.containertest.commands.internal.ConnectionListenerModule;
import es.andrewazor.containertest.tui.ClientWriter;

@Module(includes = { CommandsInternalModule.class, ConnectionListenerModule.class })
public class CommandsModule {
    @Provides CommandRegistry provideCommandRegistry(ClientWriter cw, Set<Command> commands) {
        return new CommandRegistry(cw, commands);
    }
}