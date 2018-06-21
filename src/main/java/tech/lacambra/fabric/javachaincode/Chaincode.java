package tech.lacambra.fabric.javachaincode;

import org.hyperledger.fabric.protos.peer.ProposalResponsePackage;

public interface Chaincode {

    Response init(ChaincodeStub stub);

    Response invoke(ChaincodeStub stub);

}
