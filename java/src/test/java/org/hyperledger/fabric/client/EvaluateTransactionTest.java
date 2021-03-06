/*
 * Copyright 2019 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.client;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.hyperledger.fabric.client.identity.X509Identity;
import org.hyperledger.fabric.gateway.ProposedTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

public final class EvaluateTransactionTest {
    private static final TestUtils utils = TestUtils.getInstance();

    private GatewayMocker mocker;
    private GatewayServiceStub stub;
    private Gateway gateway;
    private Network network;

    @BeforeEach
    void beforeEach() {
        mocker = new GatewayMocker();
        stub = mocker.getServiceStubSpy();

        gateway = mocker.getGatewayBuilder().connect();
        network = gateway.getNetwork("NETWORK");
    }

    @AfterEach
    void afterEach() {
        gateway.close();
        mocker.close();
    }

    @Test
    void returns_gateway_response() throws ContractException {
        doReturn(utils.newResult("MY_RESULT"))
                .when(stub).evaluate(any());

        Contract contract = network.getContract("CHAINCODE_ID");
        byte[] actual = contract.evaluateTransaction("TRANSACTION_NAME");

        assertThat(actual).asString(StandardCharsets.UTF_8).isEqualTo("MY_RESULT");
    }

    @Test
    void sends_chaincode_ID() throws ContractException, InvalidProtocolBufferException {
        Contract contract = network.getContract("MY_CHAINCODE_ID");
        contract.evaluateTransaction("TRANSACTION_NAME");

        ProposedTransaction request = mocker.captureEvaluate();
        String actual = mocker.getChaincodeSpec(request).getChaincodeId().getName();

        assertThat(actual).isEqualTo("MY_CHAINCODE_ID");
    }

    @Test
    void sends_transaction_name_for_default_contract() throws ContractException, InvalidProtocolBufferException {
        Contract contract = network.getContract("CHAINCODE_ID");
        contract.evaluateTransaction("MY_TRANSACTION_NAME");

        ProposedTransaction request = mocker.captureEvaluate();
        List<String> chaincodeArgs = mocker.getChaincodeSpec(request).getInput().getArgsList().stream()
                .map(ByteString::toStringUtf8)
                .collect(Collectors.toList());

        assertThat(chaincodeArgs).first().isEqualTo("MY_TRANSACTION_NAME");
    }

    @Test
    void sends_transaction_name_for_specified_contract() throws ContractException, InvalidProtocolBufferException {
        Contract contract = network.getContract("CHAINCODE_ID", "MY_CONTRACT");
        contract.evaluateTransaction("MY_TRANSACTION_NAME");

        ProposedTransaction request = mocker.captureEvaluate();
        List<String> chaincodeArgs = mocker.getChaincodeSpec(request).getInput().getArgsList().stream()
                .map(ByteString::toStringUtf8)
                .collect(Collectors.toList());

        assertThat(chaincodeArgs).first().isEqualTo("MY_CONTRACT:MY_TRANSACTION_NAME");
    }

    @Test
    void sends_transaction_arguments() throws ContractException, InvalidProtocolBufferException {
        Contract contract = network.getContract("CHAINCODE_ID");
        contract.evaluateTransaction("TRANSACTION_NAME", "one", "two", "three");

        ProposedTransaction request = mocker.captureEvaluate();
        List<String> chaincodeArgs = mocker.getChaincodeSpec(request).getInput().getArgsList().stream()
                .skip(1)
                .map(ByteString::toStringUtf8)
                .collect(Collectors.toList());

        assertThat(chaincodeArgs).containsExactly("one", "two", "three");
    }

    @Test
    void uses_signer() throws ContractException {
        Signer signer = (digest) -> "MY_SIGNATURE".getBytes(StandardCharsets.UTF_8);
        try (Gateway gateway = mocker.getGatewayBuilder().signer(signer).connect()) {
            network = gateway.getNetwork("NETWORK");

            Contract contract = network.getContract("CHAINCODE_ID");
            contract.evaluateTransaction("TRANSACTION_NAME");

            ProposedTransaction request = mocker.captureEvaluate();
            String signature = request.getProposal().getSignature().toStringUtf8();

            assertThat(signature).isEqualTo("MY_SIGNATURE");
        }
    }

    @Test
    void throws_on_connection_error() {
        doThrow(new StatusRuntimeException(Status.UNAVAILABLE)).when(stub).evaluate(any());

        Contract contract = network.getContract("CHAINCODE_ID");

        assertThatThrownBy(() -> contract.evaluateTransaction("TRANSACTION_NAME"))
                .isInstanceOf(StatusRuntimeException.class);
    }

    @Test
    void uses_identity() throws ContractException, InvalidProtocolBufferException {
        Identity identity = new X509Identity("MY_MSP_ID", utils.getCredentials().getCertificate());
        try (Gateway gateway = mocker.getGatewayBuilder().identity(identity).connect()) {
            network = gateway.getNetwork("NETWORK");

            Contract contract = network.getContract("CHAINCODE_ID");
            contract.evaluateTransaction("TRANSACTION_NAME");

            ProposedTransaction request = mocker.captureEvaluate();
            ByteString serializedIdentity = mocker.getSignatureHeader(request).getCreator();

            byte[] expected = GatewayUtils.serializeIdentity(identity);
            assertThat(serializedIdentity.toByteArray()).isEqualTo(expected);
        }
    }

    @Test
    void sends_network_name() throws ContractException, InvalidProtocolBufferException {
        network = gateway.getNetwork("MY_NETWORK");

        Contract contract = network.getContract("CHAINCODE_ID");
        contract.evaluateTransaction("TRANSACTION_NAME");

        ProposedTransaction request = mocker.captureEvaluate();
        String networkName = mocker.getChannelHeader(request).getChannelId();

        assertThat(networkName).isEqualTo("MY_NETWORK");
    }
}
