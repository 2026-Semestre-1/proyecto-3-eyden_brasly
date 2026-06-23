/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package commands;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 *
 * @author eyden
 */
public class CommandRegistry {
    private final Map<String, Command> commands;

    public CommandRegistry() {
        this.commands = new LinkedHashMap<>();
        registerDefaults();
    }

    public void register(Command command) {
        commands.put(command.getName(), command);
    }

    public Optional<Command> find(String name) {
        return Optional.ofNullable(commands.get(name));
    }

    public Collection<Command> getCommands() {
        return commands.values();
    }

    private void registerDefaults() {
        register(new FormatCommand());
        register(new UserAddCommand());
        register(new GroupAddCommand());
        register(new PasswdCommand());
        register(new SuCommand());
        register(new WhoamiCommand());
        register(new InfoFSCommand());
        register(new ClearCommand());
        register(new HelpCommand());
        register(new ExitCommand());
    }
}
