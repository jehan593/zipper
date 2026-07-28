package com.zipper.app.ui.components

sealed class OperationStatus {
    data object Idle : OperationStatus()
    data class InProgress(val done: Int, val total: Int) : OperationStatus()
    data object Success : OperationStatus()
    data class Failure(val message: String) : OperationStatus()
}
