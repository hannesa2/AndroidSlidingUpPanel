package com.sothree.slidinguppanel.canvasSaveProxy

import android.graphics.Canvas
import android.util.Log
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

@Deprecated("")
internal class LegacyCanvasSaveProxy(private var canvas: Canvas) : CanvasSaveProxy {
    private val saveMethod: Method
    private val clipSaveFlag: Int

    init {
        Log.d(TAG, "New LegacyCanvasSaveProxy")
        saveMethod = findSaveMethod()
        clipSaveFlag = clipSaveFlagValue
    }

    override fun save(): Int {
        return invokeSave()
    }

    override fun isFor(canvas: Canvas): Boolean {
        return canvas === this.canvas
    }

    private val clipSaveFlagValue: Int
        get() {
            val constantField: Field
            return try {
                constantField = Canvas::class.java.getDeclaredField(FIELD_NAME)
                constantField[null] as Int
            } catch (e: NoSuchFieldException) {
                throw IllegalStateException("Failed to get value of $FIELD_NAME - NoSuchFieldException", e)
            } catch (e: IllegalAccessException) {
                throw IllegalStateException("Failed to get value of $FIELD_NAME - IllegalAccessException", e)
            }
        }

    private fun findSaveMethod(): Method {
        return try {
            Canvas::class.java.getMethod(METHOD_NAME, Int::class.javaPrimitiveType)
        } catch (e: NoSuchMethodException) {
            throw IllegalStateException("Canvas does not contain a method with signature save(int)")
        }
    }

    private fun invokeSave(): Int {
        return try {
            saveMethod.invoke(canvas, clipSaveFlag) as Int
        } catch (e: IllegalAccessException) {
            throw IllegalStateException("Failed to execute save(int) - IllegalAccessException", e)
        } catch (e: InvocationTargetException) {
            throw IllegalStateException("Failed to execute save(int) - InvocationTargetException", e)
        }
    }

    companion object {
        private val TAG = CanvasSaveProxy::class.java.simpleName
        private const val METHOD_NAME = "save"
        private const val FIELD_NAME = "CLIP_SAVE_FLAG"
    }

}
