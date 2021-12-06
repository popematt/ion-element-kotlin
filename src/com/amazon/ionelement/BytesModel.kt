package com.amazon.ionelement


class ArraySlice<T>(private val array: Array<T>, private val start: Int, override val size: Int): AbstractList<T>() {
    override fun get(index: Int): T = array[index + start]
}

@ExperimentalUnsignedTypes
tailrec fun findVarIntSize(bytes: Array<UByte>, start: Int, sizeSoFar: Int = 0): Int {
    return if (bytes[start] and 0x80_u == 0x80.toUByte()) sizeSoFar + 1 else findVarIntSize(bytes, start + 1, sizeSoFar + 1)
}

@ExperimentalUnsignedTypes
sealed class BytesModel(bytes: ArraySlice<UByte>) : List<UByte> by bytes
@ExperimentalUnsignedTypes
class TypeDescriptorByte(bytes: Array<UByte>, start: Int) : BytesModel(ArraySlice(bytes, start, 1))
@ExperimentalUnsignedTypes
class IonVersionMarkerBytes(bytes: Array<UByte>, start: Int) : BytesModel(ArraySlice(bytes, start, 4))
@ExperimentalUnsignedTypes
class VarUIntBytes(bytes: Array<UByte>, start: Int) : BytesModel(ArraySlice(bytes, start, findVarIntSize(bytes, start)))
@ExperimentalUnsignedTypes
class UIntBytes(bytes: Array<UByte>, start: Int, size: Int) : BytesModel(ArraySlice(bytes, start, size))
@ExperimentalUnsignedTypes
class VarIntBytes(bytes: Array<UByte>, start: Int) : BytesModel(ArraySlice(bytes, start, findVarIntSize(bytes, start)))
@ExperimentalUnsignedTypes
class IntBytes(bytes: Array<UByte>, start: Int, size: Int) : BytesModel(ArraySlice(bytes, start, size))
@ExperimentalUnsignedTypes
class UnstructuredBytes(bytes: Array<UByte>, start: Int, size: Int) : BytesModel(ArraySlice(bytes, start, size))


// TODO: Do we need this?
@ExperimentalUnsignedTypes
typealias IonBinaryTypedBytes = List<BytesModel>
@ExperimentalUnsignedTypes
fun IonBinaryTypedBytes.byteIterator() = this.iterator().asSequence().flatten().iterator()
@ExperimentalUnsignedTypes
val IonBinaryTypedBytes.byteSize: Int get() = this.sumBy { it.size }



interface IonValueB {

    @ExperimentalUnsignedTypes
    class IonStringB(val t: TypeDescriptorByte, val len: VarUIntBytes?, val utf8Bytes: UnstructuredBytes): IonValueB
    @ExperimentalUnsignedTypes
    class IonBoolB(val t: TypeDescriptorByte): IonValueB
    @ExperimentalUnsignedTypes
    class IonListB(val t: TypeDescriptorByte, val len: VarUIntBytes?, val elements: List<IonValueB>): IonValueB

}
