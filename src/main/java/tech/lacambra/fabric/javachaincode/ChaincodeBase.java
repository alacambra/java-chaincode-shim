package tech.lacambra.fabric.javachaincode;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.net.JksOptions;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;
import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeID;
import org.hyperledger.fabric.protos.peer.ChaincodeShim;
import org.hyperledger.fabric.protos.peer.ChaincodeSupportGrpc;

import java.util.logging.Logger;

public abstract class ChaincodeBase implements Chaincode {

    private static final Logger logger = Logger.getLogger(ChaincodeBase.class.getName());

    private final Vertx vertx;
    private boolean tlsEnabled;

    public ChaincodeBase(Vertx vertx) {
        this.vertx = vertx;
    }

    public ManagedChannel newPeerClientConnection() {
        VertxChannelBuilder builder = VertxChannelBuilder
                .forAddress(vertx, "192.168.99.100", 7051);

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


        ManagedChannel channel = newPeerClientConnection();

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
        ChatStream chatStream = new ChatStream();
        ChaincodeSupportGrpc.ChaincodeSupportStub stub = ChaincodeSupportGrpc.newStub(channel);
        StreamObserver<ChaincodeShim.ChaincodeMessage> streamObserver = stub.register(chatStream);
        chatStream.setSender(streamObserver);


        // Send the ChaincodeID during register.
        ChaincodeID chaincodeID = ChaincodeID.newBuilder()
                .setName("test")
                .build();

        ChaincodeShim.ChaincodeMessage registrationMessage = ChaincodeShim.ChaincodeMessage.newBuilder()
                .setPayload(chaincodeID.toByteString())
                .setType(ChaincodeShim.ChaincodeMessage.Type.REGISTER)
                .build();

        logger.info(String.format("Registering as '%s' ... sending %s", chaincodeID.getName(), ChaincodeShim.ChaincodeMessage.Type.REGISTER));
        // Register on the stream
//        chatStream.sendMessage(registrationMessage);
//        System.out.println(channel.shutdown().getState(false));
//        vertx.close(System.out::println);
    }

    public static void main(String[] args) {
//
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
