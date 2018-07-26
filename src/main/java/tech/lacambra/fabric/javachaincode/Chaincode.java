package tech.lacambra.fabric.javachaincode;

public interface Chaincode {

    Response init(AsyncChaincodeStub stub);

    Response invoke(AsyncChaincodeStub stub);

}
