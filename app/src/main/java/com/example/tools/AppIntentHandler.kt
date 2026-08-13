package com.example.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.ActionIntent
import com.example.data.ParsedIntent
import com.example.data.ToolResult

/**
 * Handles launching Android App Intents based on parsed AI intent outputs.
 * Specifically implements direct calling (ACTION_CALL) and system dialing (ACTION_DIAL)
 * with intelligent contact resolution.
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
            ActionIntent.MAKE_CALL -> makeDirectPhoneCall(
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
     * Makes a direct phone call using ACTION_CALL if CALL_PHONE permission is granted,
     * or falls back to system dialer (ACTION_DIAL) with contact resolution.
     *
     * @param contactOrNumber Phone number or contact name provided by user speech.
     * @return ToolResult indicating whether call intent was launched successfully.
     */
    fun makeDirectPhoneCall(contactOrNumber: String): ToolResult {
        val targetInput = contactOrNumber.trim()
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        return try {
            val resolvedNumber = resolvePhoneNumber(targetInput)
            val finalTarget = when {
                resolvedNumber.isNotBlank() -> resolvedNumber
                targetInput.isNotBlank() -> targetInput
                else -> ""
            }

            if (finalTarget.isBlank()) {
                // Open general dialer if no target specified
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
                return ToolResult(
                    success = true,
                    message = "Opened system dialer.",
                    actionExecuted = "ACTION_DIAL Launched"
                )
            }

            val digitsOnly = finalTarget.replace(Regex("[^0-9+]"), "")
            val uri = Uri.parse("tel:${if (digitsOnly.isNotBlank()) digitsOnly else Uri.encode(finalTarget)}")

            if (hasCallPermission && digitsOnly.length >= 3) {
                // Directly initiate phone call
                val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(callIntent)

                val successMsg = "Calling '$targetInput' ($digitsOnly) directly..."
                MarcoLogger.logIntentSuccess(Intent.ACTION_CALL, successMsg)

                ToolResult(
                    success = true,
                    message = successMsg,
                    actionExecuted = "Direct Call Initiated (ACTION_CALL)",
                    details = mapOf(
                        "contactName" to targetInput,
                        "phoneNumber" to digitsOnly,
                        "intentAction" to Intent.ACTION_CALL
                    )
                )
            } else {
                // Open dialer with prefilled number
                val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)

                val successMsg = "Opened dialer for '$targetInput' (${if (digitsOnly.isNotBlank()) digitsOnly else finalTarget})."
                MarcoLogger.logIntentSuccess(Intent.ACTION_DIAL, successMsg)

                ToolResult(
                    success = true,
                    message = successMsg,
                    actionExecuted = "Dialer Opened (ACTION_DIAL)",
                    details = mapOf(
                        "contactName" to targetInput,
                        "phoneNumber" to digitsOnly,
                        "intentAction" to Intent.ACTION_DIAL
                    )
                )
            }
        } catch (e: Exception) {
            val errorMsg = "Could not initiate call: ${e.localizedMessage}"
            MarcoLogger.logIntentError(Intent.ACTION_CALL, errorMsg, e)
            Log.e("AppIntentHandler", "Failed to launch call intent: ${e.message}", e)
            ToolResult(
                success = false,
                message = errorMsg,
                actionExecuted = "Call Failed",
                details = mapOf("error" to (e.localizedMessage ?: "Unknown error"))
            )
        }
    }

    /**
     * Resolves a phone number from an input string by querying device contacts.
     * If input is already numeric/phone format (e.g., "+1234567890", "9876543210"), returns it directly.
     */
    fun resolvePhoneNumber(input: String): String {
        if (input.isBlank()) return ""

        val cleanedInput = input.trim()
        val digitsCount = cleanedInput.count { it.isDigit() }
        val isRawNumber = cleanedInput.matches(Regex("^[+0-9()\\s\\-]+$")) && digitsCount >= 3
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

            // Try exact and partial name match
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$cleanedInput%")

            var resultNumber = ""

            contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (cursor.moveToFirst() && numberIndex != -1) {
                    resultNumber = cursor.getString(numberIndex) ?: ""
                }
            }

            if (resultNumber.isNotBlank()) {
                return resultNumber
            }

            // Fallback: Query all phone numbers and filter in Kotlin for case-insensitive match
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex) ?: ""
                    val number = cursor.getString(numberIndex) ?: ""
                    if (name.contains(cleanedInput, ignoreCase = true) || cleanedInput.contains(name, ignoreCase = true)) {
                        if (number.isNotBlank()) {
                            return@use number
                        }
                    }
                }
            }

            ""
        } catch (e: SecurityException) {
            Log.w("AppIntentHandler", "READ_CONTACTS permission not granted: ${e.message}")
            ""
        } catch (e: Exception) {
            Log.e("AppIntentHandler", "Error resolving contact name: ${e.message}")
            ""
        }
    }
}

