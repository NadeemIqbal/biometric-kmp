package io.github.nadeemiqbal.biometric

import com.sun.jna.Callback
import com.sun.jna.CallbackReference
import com.sun.jna.Function
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import com.sun.jna.Structure

/**
 * Minimal JNA shim over the Objective-C runtime, used only by the macOS backend.
 *
 * It resolves classes/selectors and forwards messages through `objc_msgSend`. The tricky part is
 * [globalBlock], which hand-builds an Objective-C "global block" literal so that
 * `LAContext.evaluatePolicy:localizedReason:reply:` can call back into JVM code. The block ABI is
 * stable and documented (clang `Block-ABI-Apple`), but this path cannot be exercised without a real
 * Touch ID prompt, so it is verified on-device rather than in CI.
 *
 * arm64 and x86_64 both pass these (non-struct, non-float) arguments in integer registers, so a
 * single `objc_msgSend` works on both; no `_stret` variant is needed here.
 */
internal object ObjC {

    private val objc: NativeLibrary = NativeLibrary.getInstance("objc")
    private val msgSend: Function = objc.getFunction("objc_msgSend")
    private val getClass: Function = objc.getFunction("objc_getClass")
    private val selRegisterName: Function = objc.getFunction("sel_registerName")

    /** Address of `_NSConcreteGlobalBlock`, the isa every global block points at. */
    private val nsConcreteGlobalBlock: Pointer? =
        runCatching { NativeLibrary.getInstance("System").getGlobalVariableAddress("_NSConcreteGlobalBlock") }
            .getOrNull()

    init {
        // Loading the framework registers LAContext and the LAError constants with the runtime.
        runCatching {
            NativeLibrary.getInstance(
                "/System/Library/Frameworks/LocalAuthentication.framework/LocalAuthentication",
            )
        }
    }

    fun objClass(name: String): Pointer? = getClass.invokePointer(arrayOf<Any?>(name))

    private fun sel(name: String): Pointer? = selRegisterName.invokePointer(arrayOf<Any?>(name))

    fun sendPtr(receiver: Pointer?, selector: String, vararg args: Any?): Pointer? =
        msgSend.invokePointer(arrayOf(receiver, sel(selector), *args))

    /** Send a message whose Objective-C return type is `BOOL`; reads only the low byte. */
    fun sendBool(receiver: Pointer?, selector: String, vararg args: Any?): Boolean =
        (msgSend.invokeInt(arrayOf(receiver, sel(selector), *args)) and 0xFF) != 0

    fun sendLong(receiver: Pointer?, selector: String, vararg args: Any?): Long =
        msgSend.invokeLong(arrayOf(receiver, sel(selector), *args))

    fun sendVoid(receiver: Pointer?, selector: String, vararg args: Any?) {
        msgSend.invokeVoid(arrayOf(receiver, sel(selector), *args))
    }

    /** Build an NSString from [value] via `+[NSString stringWithUTF8String:]`. */
    fun nsString(value: String): Pointer? =
        sendPtr(objClass("NSString"), "stringWithUTF8String:", value)

    /** Read an NSString back to a Kotlin string via `-[NSString UTF8String]`. */
    fun nsStringToKotlin(nsString: Pointer?): String? {
        if (nsString == null) return null
        return sendPtr(nsString, "UTF8String")?.getString(0)
    }

    /**
     * Reply callback shape for `evaluatePolicy:...:reply:` -> `^(BOOL success, NSError *error)`.
     * The first argument is the block pointer itself (ignored here).
     */
    fun interface ReplyCallback : Callback {
        fun invoke(block: Pointer?, success: Byte, error: Pointer?)
    }

    /**
     * Allocate a global Objective-C block wrapping [callback] and return (block, descriptor) so the
     * caller can keep both alive for as long as the block may be invoked.
     */
    fun globalBlock(callback: ReplyCallback): Block {
        val descriptor = BlockDescriptor().apply {
            reserved = 0
            size = BLOCK_SIZE
            write()
        }
        val literal = BlockLiteral().apply {
            isa = nsConcreteGlobalBlock
            flags = BLOCK_IS_GLOBAL
            reserved = 0
            invoke = CallbackReference.getFunctionPointer(callback)
            this.descriptor = descriptor.pointer
            write()
        }
        return Block(literal, descriptor, callback)
    }

    /** Keeps the native block, its descriptor, and the JVM callback reachable together. */
    class Block(
        private val literal: BlockLiteral,
        private val descriptor: BlockDescriptor,
        private val callback: ReplyCallback,
    ) {
        val pointer: Pointer get() = literal.pointer
    }

    @Structure.FieldOrder("isa", "flags", "reserved", "invoke", "descriptor")
    class BlockLiteral : Structure() {
        @JvmField var isa: Pointer? = null
        @JvmField var flags: Int = 0
        @JvmField var reserved: Int = 0
        @JvmField var invoke: Pointer? = null
        @JvmField var descriptor: Pointer? = null
    }

    @Structure.FieldOrder("reserved", "size")
    class BlockDescriptor : Structure() {
        @JvmField var reserved: Long = 0
        @JvmField var size: Long = 0
    }

    private const val BLOCK_IS_GLOBAL = 1 shl 28
    private const val BLOCK_SIZE = 32L
}
