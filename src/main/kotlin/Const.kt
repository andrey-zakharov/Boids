package me.zakharov

object Const {
    const val FLOAT_SIZE = 4
    const val FLOAT2_SIZE = FLOAT_SIZE * 2
    const val FLOAT3_SIZE = FLOAT_SIZE * 3
    const val INT_SIZE = 4
    const val BOOL_SIZE = 1
}

 inline fun< reified T> getTypeSize() = when {
    T::class == Int::class ||
    T::class == Float::class -> 4
    T::class == Boolean::class -> 1 // actually it should be 1 bit
    else -> throw NotImplementedError(T::class.toString())
}