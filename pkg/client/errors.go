// Copyright IBM Corp. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package client

import (
	"fmt"
	"reflect"
	"strings"

	"github.com/hyperledger/fabric-protos-go-apiv2/gateway"
	"github.com/hyperledger/fabric-protos-go-apiv2/peer"
	"google.golang.org/grpc/status"
)

type grpcError struct {
	error
}

func (e *grpcError) GRPCStatus() *status.Status {
	return status.Convert(e.error)
}

func (e *grpcError) Unwrap() error {
	return e.error
}

func newTransactionError(err error, transactionID string) *TransactionError {
	if err == nil {
		return nil
	}

	return &TransactionError{
		grpcError:     &grpcError{err},
		TransactionID: transactionID,
	}
}

func (e *grpcError) Details() []*gateway.ErrorDetail {
	var results []*gateway.ErrorDetail

	for _, detail := range e.GRPCStatus().Details() {
		switch detail := detail.(type) {
		case *gateway.ErrorDetail:
			results = append(results, detail)
		}
	}

	return results
}

// TransactionError represents an error invoking a transaction. This is a gRPC [status] error.
type TransactionError struct {
	*grpcError
	TransactionID string
}

func (e *TransactionError) string(subType error) string {
	var result strings.Builder
	result.WriteString(reflect.TypeOf(subType).Elem().Name())
	result.WriteString(": ")
	result.WriteString(e.Error())
	result.WriteString("\nTransactionID: ")
	result.WriteString(e.TransactionID)

	details := e.Details()
	if len(details) == 0 {
		return result.String()
	}

	result.WriteString("\nDetails:")
	for _, detail := range details {
		result.WriteString("\n - Address: ")
		result.WriteString(detail.Address)
		result.WriteString("; MspId: ")
		result.WriteString(detail.MspId)
		result.WriteString("; Message: ")
		result.WriteString(detail.Message)
	}

	return result.String()
}

// EndorseError represents a failure endorsing a transaction proposal.
type EndorseError struct {
	*TransactionError
}

// String representation of the error, including associated error details
func (e *EndorseError) String() string {
	return e.string(e)
}

// SubmitError represents a failure submitting an endorsed transaction to the orderer.
type SubmitError struct {
	*TransactionError
}

// String representation of the error, including associated error details
func (e *SubmitError) String() string {
	return e.string(e)
}

// CommitStatusError represents a failure obtaining the commit status of a transaction.
type CommitStatusError struct {
	*TransactionError
}

// String representation of the error, including associated error details
func (e *CommitStatusError) String() string {
	return e.string(e)
}

func newCommitError(transactionID string, code peer.TxValidationCode) error {
	return &CommitError{
		message:       fmt.Sprintf("transaction %s failed to commit with status code %d (%s)", transactionID, int32(code), peer.TxValidationCode_name[int32(code)]),
		TransactionID: transactionID,
		Code:          code,
	}
}

// CommitError represents a transaction that fails to commit successfully.
type CommitError struct {
	message       string
	TransactionID string
	Code          peer.TxValidationCode
}

func (e *CommitError) Error() string {
	return e.message
}
