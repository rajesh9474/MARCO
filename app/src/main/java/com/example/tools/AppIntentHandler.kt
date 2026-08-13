package com.example.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.example.data.ActionIntent
import com.example.data.ParsedIntent
import com.example.data.ToolResult

/**
 * Handles launching Android App Intents based on parsed AI intent outputs.
 * Specifically implements ACTION_DIAL intent handling with contact names or phone numbers.
 */
class AppIntentHandler(private val context: Context) {

    init {
        MarcoLogger.init(context)
    }

    /**
     * Executes intent handling for a ParsedIntent provided by the AI engine.
     */
    fun handleParsedIntent(parsedIntent: ParsedIntent): ToolResult {
        return when (parsedIntent.intent) {
            ActionIntent.MAKE_CALL -> openDialerWithContactOrNumber(
                contactOrNumber = parsedIntent.contactName ?: parsedIntent.searchQuery ?: ""
            )
            else -> {
                val errMsg = "Unsupported intent type for AppIntentHandler: ${parsedIntent.intent}"
                MarcoLogger.logIntentWarning(parsedIntent.intent.name, errMsg)
                ToolResult(
                    success = false,
                    message = errMsg,
                    actionExecuted = "Intent Handling Skipped"
                )
            }
        }
    }

    /**
     * Opens the system phone dialer using Android's ACTION_DIAL intent with a specific contact name or phone number.
     *
     * @param contactOrNumber Phone number or contact name provided by AI parsed intent.
     * @return ToolResult indicating whether the dialer intent was launched successfully.
     */
    fun openDialerWithContactOrNumber(contactOrNumber: String): ToolResult {
        val targetInput = contactOrNumber.trim()

        return try {
            val phoneNumber = resolvePhoneNumber(targetInput)
            val uri = when {
                phoneNumber.isNotBlank() -> Uri.parse("tel:$phoneNumber")
                targetInput.isNotBlank() -> Uri.parse("tel:${Uri.encode(targetInput)}")
                else -> Uri.parse("tel:")
            }

            val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(dialIntent)

            val displayTarget = when {
                phoneNumber.isNotBlank() -> phoneNumber
                targetInput.isNotBlank() -> targetInput
                else -> "dialer"
            }

            val successMsg = "Opened system dialer for '$displayTarget'."
            MarcoLogger.logIntentSuccess(Intent.ACTION_DIAL, successMsg)

            ToolResult(
                success = true,
                message = successMsg,
                actionExecuted = "ACTION_DIAL Intent Executed",
                details = mapOf(
                    "rawInput" to targetInput,
                    "resolvedNumber" to phoneNumber,
                    "intentAction" to Intent.ACTION_DIAL
                )
            )
        } catch (e: Exception) {
            val errorMsg = "Could not open dialer: ${e.localizedMessage}"
            MarcoLogger.logIntentError(Intent.ACTION_DIAL, errorMsg, e)
            Log.e("AppIntentHandler", "Failed to launch ACTION_DIAL intent: ${e.message}", e)
            ToolResult(
                success = false,
                message = errorMsg,
                actionExecuted = "ACTION_DIAL Intent Failed",
                details = mapOf("error" to (e.localizedMessage ?: "Unknown error"))
            )
        }
    }

    /**
     * Resolves a phone number from an input string.
     * If input is already numeric/phone format (e.g., "+1234567890", "9876543210"), returns it directly.
     * Otherwise queries ContactsContract.CommonDataKinds.Phone to find a matching contact number.
     */
    fun resolvePhoneNumber(input: String): String {
        if (input.isBlank()) return ""

        val cleanedInput = input.trim()
        val isRawNumber = cleanedInput.matches(Regex("^[+0-9()\\s\\-]+$")) && cleanedInput.count { it.isDigit() } >= 3
        if (isRawNumber) {
            return cleanedInput
        }

        return try {
            val contentResolver = context.contentResolver
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$cleanedInput%")

            contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (cursor.moveToFirst() && numberIndex != -1) {
                    cursor.getString(numberIndex) ?: ""
                } else {
                    ""
                }
            } ?: ""
        } catch (e: SecurityException) {
            Log.w("AppIntentHandler", "READ_CONTACTS permission not granted: ${e.message}")
            ""
        } catch (e: Exception) {
            Log.e("AppIntentHandler", "Error resolving contact name: ${e.message}")
            ""
        }
    }
}
