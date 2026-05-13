package com.nitrodropnative.transfer

enum class TransferState {
    Idle,
    Waiting,
    Connecting,
    Metadata,
    Transferring,
    Paused,
    Verifying,
    Completed,
    Cancelled,
    Failed
}
