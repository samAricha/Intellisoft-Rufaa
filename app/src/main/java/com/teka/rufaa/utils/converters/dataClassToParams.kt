package com.teka.rufaa.utils.converters

import kotlin.reflect.full.memberProperties

fun <T : Any> T.toParams(): MutableList<Pair<String, String>> {
    return this::class.memberProperties.map { property ->
        val value = property.getter.call(this)
        property.name to (value?.toString() ?: "")
    }.toMutableList()
}


class PayloadHolder {
    private val params = mutableListOf<Pair<String, String>>()

    fun add(key: String, value: Any?) = apply {
        params.add(key to (value?.toString() ?: ""))
    }

    fun toParams(): MutableList<Pair<String, String>> = params

    companion object {
        fun build(block: PayloadHolder.() -> Unit): PayloadHolder {
            return PayloadHolder().apply(block)
        }
    }
}

fun apiPayload(block: PayloadHolder.() -> Unit): MutableList<Pair<String, String>> {
    return PayloadHolder.build(block).toParams()
}
