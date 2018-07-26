package tech.lacambra.fabric.javachaincode;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.vertx.core.Vertx;
import io.vertx.core.net.JksOptions;
import io.vertx.grpc.VertxChannelBuilder;
import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeID;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage;

import java.util.logging.Logger;

public abstract class ChaincodeBase implements Chaincode {

    private static final Logger logger = Logger.getLogger(ChaincodeBase.class.getName());

    private final Vertx vertx;
    private final ConsoleCtrl consoleCtrl;
    private boolean tlsEnabled;
    private ChatStream chatStream;
    private ManagedChannel channel;

    public ChaincodeBase(Vertx vertx) {
        this.vertx = vertx;
        consoleCtrl = new ConsoleCtrl(vertx);
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
        vertx.setPeriodic(500, tid -> {

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

        consoleCtrl.addCommand("r", this::sendMessage);

        consoleCtrl.addCommand("restart", () -> {
            channel.shutdown();
            start();
        });

        consoleCtrl.addCommand("state", (process) -> {
            ConnectivityState state = channel.getState(false);
            process.write(state.name());
        });

        consoleCtrl.startService();
    }

    public static void main(String[] args) {

        Vertx vertx = Vertx.vertx().exceptionHandler(System.out::println);
        ChaincodeBase chaincodeBase = new ChaincodeBase(vertx) {
            @Override
            public Response init(AsyncChaincodeStub stub) {
                return null;
            }

            @Override
            public Response invoke(AsyncChaincodeStub stub) {
                return null;
            }
        };
        chaincodeBase.start("");
    }

}
