# Non-Wallet Transfer Functionality

This document provides an overview of the non-wallet transfer functionality in the LulPay application.

## Overview

Non-wallet transfers allow users to send money to recipients who don't have a LulPay wallet. This feature enables users to transfer funds to individuals who may not be registered on the platform, expanding the reach and utility of the service.

## Key Components

1. **NonWalletTransferRequest**: DTO for handling non-wallet transfer requests.
2. **NonWalletTransferResponse**: DTO for handling non-wallet transfer responses.
3. **NonWalletRecipientDetail**: Entity for storing recipient details.
4. **DisbursementStage**: Entity for tracking the status of disbursements.
5. **NonWalletTransferService**: Service for processing non-wallet transfers.
6. **NonWalletTransferController**: Controller for handling API endpoints.

## Database Schema

The functionality relies on the following database tables:

- `disbursement_stages`: Tracks the various stages of a non-wallet transfer.
- `non_wallet_recipient_details`: Stores information about recipients who don't have a wallet.
- `transaction_history`: Records all transactions, including non-wallet transfers.
- `transaction_audit_log`: Maintains an audit trail of all transaction-related actions.

## API Endpoints

### Initiate Non-Wallet Transfer

```
POST /api/v1/transfers/non-wallet
```

**Request Body:**
```json
{
  "senderWalletTypeId": 1,
  "amount": 10000,
  "pin": "1234",
  "recipientFullName": "John Doe",
  "idDocumentType": "National ID",
  "idNumber": "CM12345678",
  "phoneNumber": "+256712345678",
  "email": "john.doe@example.com",
  "country": "Uganda",
  "state": "Central",
  "city": "Kampala",
  "relationship": "Friend",
  "description": "Birthday gift",
  "idempotencyKey": "unique-key-123"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Non-wallet transfer successful",
  "data": {
    "status": "success",
    "transactionId": 123456789,
    "senderWalletId": 1,
    "amount": 10000,
    "fee": 100,
    "totalAmount": 10100,
    "currency": "UGX",
    "description": "Birthday gift",
    "timestamp": "2023-06-15T10:30:45",
    "senderWalletBalanceAfter": 89900,
    "recipientName": "John Doe",
    "recipientPhoneNumber": "+256712345678",
    "disbursementStageId": 2,
    "disbursementStageName": "Processing"
  }
}
```

### Get Non-Wallet Transfer Status

```
GET /api/v1/transfers/non-wallet/{transactionId}
```

**Response:**
```json
{
  "success": true,
  "message": "Non-wallet transfer status retrieved successfully",
  "data": {
    "status": "completed",
    "transactionId": 123456789,
    "senderWalletId": 1,
    "amount": 10000,
    "fee": 100,
    "totalAmount": 10100,
    "currency": "UGX",
    "description": "Birthday gift",
    "timestamp": "2023-06-15T10:30:45",
    "senderWalletBalanceAfter": 89900,
    "recipientName": "John Doe",
    "recipientPhoneNumber": "+256712345678",
    "disbursementStageId": 4,
    "disbursementStageName": "Completed"
  }
}
```

## Transaction Flow

1. **Initiation**: User initiates a non-wallet transfer by providing recipient details and transfer amount.
2. **Validation**: System validates the request, checks for sufficient funds, and verifies the user's PIN.
3. **Processing**: System deducts funds from the sender's wallet, records the transaction, and stores recipient details.
4. **Notification**: System sends notifications to the sender and an SMS to the recipient.
5. **Completion**: Transfer is marked as completed, and the recipient can collect the funds.

## Disbursement Stages

1. **Initiated**: Transfer has been initiated but not yet processed.
2. **Processing**: Transfer is being processed.
3. **Pending Pickup**: Funds are ready for pickup by the recipient.
4. **Completed**: Transfer has been completed and funds have been received.
5. **Failed**: Transfer has failed.
6. **Cancelled**: Transfer has been cancelled.

## Error Handling

The system handles various error scenarios, including:

- Insufficient funds
- Invalid PIN
- Invalid recipient details
- System errors

Each error is mapped to a specific error code and HTTP status code for consistent error reporting.

## Security Considerations

- All transfers require PIN verification.
- Sensitive recipient information is stored securely.
- Idempotency keys prevent duplicate transfers.
- Audit logs track all transfer-related actions.

## Future Enhancements

1. **Batch Processing**: Support for batch processing of non-wallet transfers.
2. **Scheduled Transfers**: Allow users to schedule transfers for future dates.
3. **Recurring Transfers**: Enable recurring transfers to the same recipient.
4. **Multi-Currency Support**: Support for transfers in multiple currencies.
5. **Enhanced Notifications**: More detailed notifications with transfer status updates. 