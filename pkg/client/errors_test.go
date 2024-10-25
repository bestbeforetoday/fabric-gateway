// Copyright IBM Corp. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package client

import (
	"fmt"
	"testing"

	"github.com/hyperledger/fabric-protos-go-apiv2/gateway"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/protoadapt"
)

func newGrpcStatus(code codes.Code, message string, details ...*gateway.ErrorDetail) (*status.Status, error) {
	result := status.New(code, message)
	if len(details) == 0 {
		return result, nil
	}

	var v1Details []protoadapt.MessageV1
	for _, detail := range details {
		v1Details = append(v1Details, protoadapt.MessageV1Of(detail))
	}

	resultWithDetails, err := result.WithDetails(v1Details...)
	if err != nil {
		return nil, err
	}

	return resultWithDetails, nil
}

func TestErrors(t *testing.T) {
	t.Run("String", func(t *testing.T) {
		for typeName, newInstance := range map[string](func(*TransactionError) fmt.Stringer){
			"EndorseError": func(txErr *TransactionError) fmt.Stringer {
				return &EndorseError{txErr}
			},
			"SubmitError": func(txErr *TransactionError) fmt.Stringer {
				return &SubmitError{txErr}
			},
			"CommitStatusError": func(txErr *TransactionError) fmt.Stringer {
				return &CommitStatusError{txErr}
			},
		} {
			t.Run(typeName, func(t *testing.T) {
				detail := &gateway.ErrorDetail{
					Address: "DETAIL_ADDRESS",
					MspId:   "DETAIL_MSP_ID",
					Message: "DETAIL_MESSAGE",
				}
				grpcStatus, err := newGrpcStatus(codes.Aborted, "STATUS_MESSAGE", detail)
				require.NoError(t, err)

				transactionErr := newTransactionError(grpcStatus.Err(), "TRANSACTION_ID")
				actualErr := newInstance(transactionErr)

				actual := actualErr.String()

				assert.Contains(t, actual, grpcStatus.Err().Error())
				assert.Contains(t, actual, typeName)
				assert.Contains(t, actual, detail.Address)
				assert.Contains(t, actual, detail.MspId)
				assert.Contains(t, actual, detail.Message)
			})
		}
	})
}
