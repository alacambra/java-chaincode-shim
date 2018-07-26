package tech.lacambra.fabric.javachaincode;

import io.vertx.core.Vertx;
import io.vertx.ext.shell.ShellService;
import io.vertx.ext.shell.ShellServiceOptions;
import io.vertx.ext.shell.command.CommandBuilder;
import io.vertx.ext.shell.command.CommandProcess;
import io.vertx.ext.shell.command.CommandRegistry;
import io.vertx.ext.shell.term.TelnetTermOptions;

import java.util.function.Consumer;

public class ConsoleCtrl {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 4000;

    private final Vertx vertx;
    private final CommandRegistry registry;

    public ConsoleCtrl(Vertx vertx) {
        this.vertx = vertx;
        registry = CommandRegistry.getShared(vertx);

    }

    public ConsoleCtrl addCommand(String name, Consumer<CommandProcess> supplier) {
        CommandBuilder builder = CommandBuilder.command(name);
        builder.processHandler(process -> {
            supplier.accept(process);
            process.end();
        });

        registry.registerCommand(builder.build(vertx));
        return this;

    }

    public ConsoleCtrl addCommand(String name, Runnable command) {
        CommandBuilder builder = CommandBuilder.command(name);
        builder.processHandler(process -> {
            command.run();
            process.end();
        });

        registry.registerCommand(builder.build(vertx));
        return this;
    }

    public void startService() {
        startService(DEFAULT_HOST, DEFAULT_PORT);
    }

    public void startService(String host, int port) {
        ShellService service = ShellService.create(vertx,
                new ShellServiceOptions().setTelnetOptions(
                        new TelnetTermOptions().
                                setHost(host).
                                setPort(port)
                )
        );
        service.start();
    }
}



