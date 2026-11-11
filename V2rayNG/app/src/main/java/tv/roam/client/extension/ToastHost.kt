package tv.roam.client.extension

/**
 * Message severity. Kept in the business layer so that UI implementations can
 * be replaced without touching callers of `Context.toast*`.
 */
enum class ToastType {
    NORMAL, SUCCESS, ERROR, INFO
}

data class ToastMessage(
    val message: CharSequence,
    val type: ToastType = ToastType.NORMAL,
    val long: Boolean = false,
)

/**
 * Optional UI host for in-app messages. The TV implementation registers a host
 * that renders messages inside the 64dp status band instead of a floating
 * snackbar. When no host is registered, callers fall back to a system Toast.
 */
interface ToastHost {
    /** Returns true when the message was accepted for display. */
    fun show(message: CharSequence, type: ToastType, long: Boolean): Boolean
}

object ToastHostRegistry {
    @Volatile
    var host: ToastHost? = null
}
