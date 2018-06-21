package tech.lacambra.fabric.javachaincode;

import io.grpc.ManagedChannel;
import io.vertx.core.Vertx;
import io.vertx.core.net.JksOptions;
import io.vertx.grpc.VertxChannelBuilder;
import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeID;
import org.hyperledger.fabric.protos.peer.ChaincodeShim;
import org.hyperledger.fabric.protos.peer.ChaincodeSupportGrpc;

public abstract class ChaincodeBase implements Chaincode {

    private final Vertx vertx;
    private boolean tlsEnabled;
    private MsgQueueHandler msgQueueHandler;

    public ChaincodeBase() {
        vertx = Vertx.vertx();
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
        chatWithPeer(newPeerClientConnection());
    }

    public void chatWithPeer(ManagedChannel channel) {
        ChatStream responseObserver = new ChatStream();
        ChaincodeSupportGrpc.ChaincodeSupportStub stub = ChaincodeSupportGrpc.newStub(channel);
        io.grpc.stub.StreamObserver<ChaincodeShim.ChaincodeMessage> streamObserver = stub.register(responseObserver);
        msgQueueHandler = new MsgQueueHandler(streamObserver);


        // Send the ChaincodeID during register.
        ChaincodeID chaincodeID = ChaincodeID.newBuilder()
                .setName("")
                .build();

        ChaincodeShim.ChaincodeMessage registrationMessage = ChaincodeShim.ChaincodeMessage.newBuilder()
                .setPayload(chaincodeID.toByteString())
                .setType(ChaincodeShim.ChaincodeMessage.Type.REGISTER)
                .build();

        // Register on the stream
        msgQueueHandler.queueMsg(registrationMessage);

        while (true) {
            try {
                responseObserver.receive();
            } catch (Exception e) {
                System.err.println(e);
                break;
            }
        }
    }

    public static void main(String[] args) {
        new ChaincodeBase() {
            @Override
            public Response init(ChaincodeStub stub) {
                return null;
            }

            @Override
            public Response invoke(ChaincodeStub stub) {
                return null;
            }
        }.start("");
    }

}
