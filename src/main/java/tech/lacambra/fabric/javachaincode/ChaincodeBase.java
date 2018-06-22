package tech.lacambra.fabric.javachaincode;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.vertx.core.Vertx;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.shell.ShellService;
import io.vertx.ext.shell.ShellServiceOptions;
import io.vertx.ext.shell.command.CommandBuilder;
import io.vertx.ext.shell.command.CommandRegistry;
import io.vertx.ext.shell.term.TelnetTermOptions;
import io.vertx.grpc.VertxChannelBuilder;
import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeID;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage;

import java.util.logging.Logger;

public abstract class ChaincodeBase implements Chaincode {

    private static final Logger logger = Logger.getLogger(ChaincodeBase.class.getName());

    private final Vertx vertx;
    private boolean tlsEnabled;
    ChatStream chatStream;
    ManagedChannel channel;

    public ChaincodeBase(Vertx vertx) {
        this.vertx = vertx;
        setConsole();
    }

    public ManagedChannel newPeerClientConnection() {
        VertxChannelBuilder builder = VertxChannelBuilder
                .forAddress(vertx, "192.168.99.100", 7052);

        if (tlsEnabled) {
            builder.useSsl(options -> options
                    .setSsl(true)
                    .setUseAlpn(true)
                    .setTrustStoreOptions(new JksOptions()
                            .setPath("client-truststore.jks")
                            .setPassword("secret")));
        } else {
            builder.usePlaintext(true);
        }

        return builder.build();
    }

    public void start(String... args) {

        channel = newPeerClientConnection();
        vertx.setPeriodic(100, tid -> {

            ConnectivityState state = channel.getState(true);

            logger.info("[start] Channel state is " + state);

            if (state == ConnectivityState.READY) {
                chatWithPeer(channel);
                vertx.cancelTimer(tid);
            }
        });
    }

    public void chatWithPeer(ManagedChannel channel) {
        chatStream = new ChatStream(channel);
        // Send the ChaincodeID during register.
        sendMessage();
//        System.out.println(channel.shutdown().getState(false));
//        vertx.close(System.out::println);
    }

    private void sendMessage() {

        ChaincodeID chaincodeID = ChaincodeID.newBuilder()
                .setName("test")
                .build();

        ChaincodeMessage registrationMessage = ChaincodeMessage.newBuilder()
                .setPayload(chaincodeID.toByteString())
                .setType(ChaincodeMessage.Type.REGISTER)
                .build();
        // Register on the stream
        chatStream.sendMessage(registrationMessage);
    }

    private void setConsole() {

        CommandRegistry registry = CommandRegistry.getShared(vertx);

        CommandBuilder builder = CommandBuilder.command("r");
        builder.processHandler(process -> {
            sendMessage();
            process.end();
        });

        registry.registerCommand(builder.build(vertx));

        builder = CommandBuilder.command("restart");
        builder.processHandler(process -> {
            channel.shutdownNow();
            start();
            process.end();
        });

        registry.registerCommand(builder.build(vertx));

        builder = CommandBuilder.command("state");
        builder.processHandler(process -> {
            ConnectivityState state = channel.getState(false);
            process.write(state.name());
            process.end();
        });

        registry.registerCommand(builder.build(vertx));

        ShellService service = ShellService.create(vertx,
                new ShellServiceOptions().setTelnetOptions(
                        new TelnetTermOptions().
                                setHost("localhost").
                                setPort(4000)
                )
        );
        service.start();
    }

    public static void main(String[] args) {

        Vertx vertx = Vertx.vertx().exceptionHandler(System.out::println);
        ChaincodeBase chaincodeBase = new ChaincodeBase(vertx) {
            @Override
            public Response init(ChaincodeStub stub) {
                return null;
            }

            @Override
            public Response invoke(ChaincodeStub stub) {
                return null;
            }
        };
        chaincodeBase.start("");
    }

}
