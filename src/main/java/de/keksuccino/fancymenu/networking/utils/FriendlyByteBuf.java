package de.keksuccino.fancymenu.networking.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ByteProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;

public class FriendlyByteBuf extends ByteBuf {

    private final ByteBuf source;

    public FriendlyByteBuf(ByteBuf byteBuf) {
        source = byteBuf;
    }

    public FriendlyByteBuf() {
        this(Unpooled.buffer());
    }

    public ByteBuf getUnderlyingByteBuf() {
        return source;
    }

    public <T extends Enum<T>> T readEnum(Class<T> clazz) {
        return clazz.getEnumConstants()[readVarInt()];
    }

    public FriendlyByteBuf writeEnum(Enum<?> enum_) {
        return this.writeVarInt(enum_.ordinal());
    }

    public FriendlyByteBuf writeByteArray(byte[] bs) {
        writeVarInt(bs.length);
        writeBytes(bs);
        return this;
    }

    public byte[] readByteArray() {
        return readByteArray(readableBytes());
    }

    public byte[] readByteArray(int i) {
        int j = readVarInt();
        if (j > i) {
            throw new DecoderException("ByteArray with size " + j + " is bigger than allowed " + i);
        } else {
            byte[] bs = new byte[j];
            readBytes(bs);
            return bs;
        }
    }

    public int readVarInt() {
        int i = 0;
        int j = 0;

        byte b;
        do {
            b = readByte();
            i |= (b & 127) << j++ * 7;
            if (j > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((b & 128) == 128);

        return i;
    }

    public long readVarLong() {
        byte b;
        long l = 0L;
        int i = 0;
        do {
            b = this.readByte();
            l |= (long) (b & 0x7F) << i++ * 7;
            if (i <= 10) continue;
            throw new RuntimeException("VarLong too big");
        } while ((b & 0x80) == 128);
        return l;
    }

    public FriendlyByteBuf writeUUID(UUID uUID) {
        writeLong(uUID.getMostSignificantBits());
        writeLong(uUID.getLeastSignificantBits());
        return this;
    }

    public UUID readUUID() {
        return new UUID(readLong(), readLong());
    }

    public FriendlyByteBuf writeVarInt(int i) {
        while ((i & -128) != 0) {
            writeByte(i & 127 | 128);
            i >>>= 7;
        }

        writeByte(i);
        return this;
    }

    public FriendlyByteBuf writeVarLong(long l) {
        while (true) {
            if ((l & 0xFFFFFFFFFFFFFF80L) == 0L) {
                this.writeByte((int) l);
                return this;
            }
            this.writeByte((int) (l & 0x7FL) | 0x80);
            l >>>= 7;
        }
    }

    public String readUtf(int i) {
        int j = readVarInt();
        if (j > i * 4) {
            throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + j + " > " + i * 4 + ')');
        } else if (j < 0) {
            throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
        } else {
            String string = toString(readerIndex(), j, StandardCharsets.UTF_8);
            readerIndex(readerIndex() + j);
            if (string.length() > i) {
                throw new DecoderException("The received string length is longer than maximum allowed (" + j + " > " + i + ')');
            } else {
                return string;
            }
        }
    }

    public FriendlyByteBuf writeUtf(String string, int i) {
        byte[] bs = string.getBytes(StandardCharsets.UTF_8);
        if (bs.length > i) {
            throw new EncoderException("String too big (was " + bs.length + " bytes encoded, max " + i + ')');
        } else {
            writeVarInt(bs.length);
            writeBytes(bs);
            return this;
        }
    }

    public String readUtf() {
        return readUtf(32767);
    }

    public FriendlyByteBuf writeUtf(String string) {
        return writeUtf(string, 32767);
    }

    public <T, C extends Collection<T>> C readCollection(IntFunction<C> collectionFactory, Reader<T> reader) {
        int size = this.readVarInt();
        C collection = collectionFactory.apply(size);

        for(int j = 0; j < size; ++j) {
            collection.add(reader.apply(this));
        }

        return collection;
    }

    public <T> void writeCollection(Collection<T> collection, Writer<T> writer) {
        this.writeVarInt(collection.size());
        for (T entry : collection) {
            writer.accept(this, entry);
        }
    }

    public <T> List<T> readList(Reader<T> reader) {
        return this.readCollection(Lists::newArrayListWithCapacity, reader);
    }

    public <K, V, M extends Map<K, V>> M readMap(IntFunction<M> mapFactory, Reader<K> keyReader, Reader<V> valueReader) {
        int i = this.readVarInt();
        M m = mapFactory.apply(i);

        for(int j = 0; j < i; ++j) {
            K k = keyReader.apply(this);
            V v = valueReader.apply(this);
            m.put(k, v);
        }

        return m;
    }

    public <K, V> Map<K, V> readMap(Reader<K> keyReader, Reader<V> valueReader) {
        return this.readMap(Maps::newHashMapWithExpectedSize, keyReader, valueReader);
    }

    public <K, V> void writeMap(Map<K, V> map, Writer<K> keyReader, Writer<V> valueReader) {
        this.writeVarInt(map.size());
        map.forEach((key, value) -> {
            keyReader.accept(this, key);
            valueReader.accept(this, value);
        });
    }

    public <T> void writeOptional(Optional<T> optional, Writer<T> writer) {
        if (optional.isPresent()) {
            this.writeBoolean(true);
            writer.accept(this, optional.get());
        } else {
            this.writeBoolean(false);
        }
    }

    public <T> Optional<T> readOptional(Reader<T> reader) {
        return this.readBoolean() ? Optional.of(reader.apply(this)) : Optional.empty();
    }

    @Override
    public int capacity() {
        return source.capacity();
    }

    @Override
    public ByteBuf capacity(int i) {
        return source.capacity(i);
    }

    @Override
    public int maxCapacity() {
        return source.maxCapacity();
    }

    @Override
    public ByteBufAllocator alloc() {
        return source.alloc();
    }

    @Override
    public ByteOrder order() {
        return source.order();
    }

    @Override
    public ByteBuf order(ByteOrder byteOrder) {
        return source.order(byteOrder);
    }

    @Override
    public ByteBuf unwrap() {
        return source.unwrap();
    }

    @Override
    public boolean isDirect() {
        return source.isDirect();
    }

    @Override
    public boolean isReadOnly() {
        return source.isReadOnly();
    }

    @Override
    public ByteBuf asReadOnly() {
        return source.asReadOnly();
    }

    @Override
    public int readerIndex() {
        return source.readerIndex();
    }

    @Override
    public ByteBuf readerIndex(int i) {
        return source.readerIndex(i);
    }

    @Override
    public int writerIndex() {
        return source.writerIndex();
    }

    @Override
    public ByteBuf writerIndex(int i) {
        return source.writerIndex(i);
    }

    @Override
    public ByteBuf setIndex(int i, int j) {
        return source.setIndex(i, j);
    }

    @Override
    public int readableBytes() {
        return source.readableBytes();
    }

    @Override
    public int writableBytes() {
        return source.writableBytes();
    }

    @Override
    public int maxWritableBytes() {
        return source.maxWritableBytes();
    }

    @Override
    public boolean isReadable() {
        return source.isReadable();
    }

    @Override
    public boolean isReadable(int i) {
        return source.isReadable(i);
    }

    @Override
    public boolean isWritable() {
        return source.isWritable();
    }

    @Override
    public boolean isWritable(int i) {
        return source.isWritable(i);
    }

    @Override
    public ByteBuf clear() {
        return source.clear();
    }

    @Override
    public ByteBuf markReaderIndex() {
        return source.markReaderIndex();
    }

    @Override
    public ByteBuf resetReaderIndex() {
        return source.resetReaderIndex();
    }

    @Override
    public ByteBuf markWriterIndex() {
        return source.markWriterIndex();
    }

    @Override
    public ByteBuf resetWriterIndex() {
        return source.resetWriterIndex();
    }

    @Override
    public ByteBuf discardReadBytes() {
        return source.discardReadBytes();
    }

    @Override
    public ByteBuf discardSomeReadBytes() {
        return source.discardSomeReadBytes();
    }

    @Override
    public ByteBuf ensureWritable(int i) {
        return source.ensureWritable(i);
    }

    @Override
    public int ensureWritable(int i, boolean bl) {
        return source.ensureWritable(i, bl);
    }

    @Override
    public boolean getBoolean(int i) {
        return source.getBoolean(i);
    }

    @Override
    public byte getByte(int i) {
        return source.getByte(i);
    }

    @Override
    public short getUnsignedByte(int i) {
        return source.getUnsignedByte(i);
    }

    @Override
    public short getShort(int i) {
        return source.getShort(i);
    }

    @Override
    public short getShortLE(int i) {
        return source.getShortLE(i);
    }

    @Override
    public int getUnsignedShort(int i) {
        return source.getUnsignedShort(i);
    }

    @Override
    public int getUnsignedShortLE(int i) {
        return source.getUnsignedShortLE(i);
    }

    @Override
    public int getMedium(int i) {
        return source.getMedium(i);
    }

    @Override
    public int getMediumLE(int i) {
        return source.getMediumLE(i);
    }

    @Override
    public int getUnsignedMedium(int i) {
        return source.getUnsignedMedium(i);
    }

    @Override
    public int getUnsignedMediumLE(int i) {
        return source.getUnsignedMediumLE(i);
    }

    @Override
    public int getInt(int i) {
        return source.getInt(i);
    }

    @Override
    public int getIntLE(int i) {
        return source.getIntLE(i);
    }

    @Override
    public long getUnsignedInt(int i) {
        return source.getUnsignedInt(i);
    }

    @Override
    public long getUnsignedIntLE(int i) {
        return source.getUnsignedIntLE(i);
    }

    @Override
    public long getLong(int i) {
        return source.getLong(i);
    }

    @Override
    public long getLongLE(int i) {
        return source.getLongLE(i);
    }

    @Override
    public char getChar(int i) {
        return source.getChar(i);
    }

    @Override
    public float getFloat(int i) {
        return source.getFloat(i);
    }

    @Override
    public double getDouble(int i) {
        return source.getDouble(i);
    }

    @Override
    public ByteBuf getBytes(int i, ByteBuf byteBuf) {
        return source.getBytes(i, byteBuf);
    }

    @Override
    public ByteBuf getBytes(int i, ByteBuf byteBuf, int j) {
        return source.getBytes(i, byteBuf, j);
    }

    @Override
    public ByteBuf getBytes(int i, ByteBuf byteBuf, int j, int k) {
        return source.getBytes(i, byteBuf, j, k);
    }

    @Override
    public ByteBuf getBytes(int i, byte[] bs) {
        return source.getBytes(i, bs);
    }

    @Override
    public ByteBuf getBytes(int i, byte[] bs, int j, int k) {
        return source.getBytes(i, bs, j, k);
    }

    @Override
    public ByteBuf getBytes(int i, ByteBuffer byteBuffer) {
        return source.getBytes(i, byteBuffer);
    }

    @Override
    public ByteBuf getBytes(int i, OutputStream outputStream, int j) throws IOException {
        return source.getBytes(i, outputStream, j);
    }

    @Override
    public int getBytes(int i, GatheringByteChannel gatheringByteChannel, int j) throws IOException {
        return source.getBytes(i, gatheringByteChannel, j);
    }

    @Override
    public int getBytes(int i, FileChannel fileChannel, long l, int j) throws IOException {
        return source.getBytes(i, fileChannel, l, j);
    }

    @Override
    public CharSequence getCharSequence(int i, int j, Charset charset) {
        return source.getCharSequence(i, j, charset);
    }

    @Override
    public ByteBuf setBoolean(int i, boolean bl) {
        return source.setBoolean(i, bl);
    }

    @Override
    public ByteBuf setByte(int i, int j) {
        return source.setByte(i, j);
    }

    @Override
    public ByteBuf setShort(int i, int j) {
        return source.setShort(i, j);
    }

    @Override
    public ByteBuf setShortLE(int i, int j) {
        return source.setShortLE(i, j);
    }

    @Override
    public ByteBuf setMedium(int i, int j) {
        return source.setMedium(i, j);
    }

    @Override
    public ByteBuf setMediumLE(int i, int j) {
        return source.setMediumLE(i, j);
    }

    @Override
    public ByteBuf setInt(int i, int j) {
        return source.setInt(i, j);
    }

    @Override
    public ByteBuf setIntLE(int i, int j) {
        return source.setIntLE(i, j);
    }

    @Override
    public ByteBuf setLong(int i, long l) {
        return source.setLong(i, l);
    }

    @Override
    public ByteBuf setLongLE(int i, long l) {
        return source.setLongLE(i, l);
    }

    @Override
    public ByteBuf setChar(int i, int j) {
        return source.setChar(i, j);
    }

    @Override
    public ByteBuf setFloat(int i, float f) {
        return source.setFloat(i, f);
    }

    @Override
    public ByteBuf setDouble(int i, double d) {
        return source.setDouble(i, d);
    }

    @Override
    public ByteBuf setBytes(int i, ByteBuf byteBuf) {
        return source.setBytes(i, byteBuf);
    }

    @Override
    public ByteBuf setBytes(int i, ByteBuf byteBuf, int j) {
        return source.setBytes(i, byteBuf, j);
    }

    @Override
    public ByteBuf setBytes(int i, ByteBuf byteBuf, int j, int k) {
        return source.setBytes(i, byteBuf, j, k);
    }

    @Override
    public ByteBuf setBytes(int i, byte[] bs) {
        return source.setBytes(i, bs);
    }

    @Override
    public ByteBuf setBytes(int i, byte[] bs, int j, int k) {
        return source.setBytes(i, bs, j, k);
    }

    @Override
    public ByteBuf setBytes(int i, ByteBuffer byteBuffer) {
        return source.setBytes(i, byteBuffer);
    }

    @Override
    public int setBytes(int i, InputStream inputStream, int j) throws IOException {
        return source.setBytes(i, inputStream, j);
    }

    @Override
    public int setBytes(int i, ScatteringByteChannel scatteringByteChannel, int j) throws IOException {
        return source.setBytes(i, scatteringByteChannel, j);
    }

    @Override
    public int setBytes(int i, FileChannel fileChannel, long l, int j) throws IOException {
        return source.setBytes(i, fileChannel, l, j);
    }

    @Override
    public ByteBuf setZero(int i, int j) {
        return source.setZero(i, j);
    }

    @Override
    public int setCharSequence(int i, CharSequence charSequence, Charset charset) {
        return source.setCharSequence(i, charSequence, charset);
    }

    @Override
    public boolean readBoolean() {
        return source.readBoolean();
    }

    @Override
    public byte readByte() {
        return source.readByte();
    }

    @Override
    public short readUnsignedByte() {
        return source.readUnsignedByte();
    }

    @Override
    public short readShort() {
        return source.readShort();
    }

    @Override
    public short readShortLE() {
        return source.readShortLE();
    }

    @Override
    public int readUnsignedShort() {
        return source.readUnsignedShort();
    }

    @Override
    public int readUnsignedShortLE() {
        return source.readUnsignedShortLE();
    }

    @Override
    public int readMedium() {
        return source.readMedium();
    }

    @Override
    public int readMediumLE() {
        return source.readMediumLE();
    }

    @Override
    public int readUnsignedMedium() {
        return source.readUnsignedMedium();
    }

    @Override
    public int readUnsignedMediumLE() {
        return source.readUnsignedMediumLE();
    }

    @Override
    public int readInt() {
        return source.readInt();
    }

    @Override
    public int readIntLE() {
        return source.readIntLE();
    }

    @Override
    public long readUnsignedInt() {
        return source.readUnsignedInt();
    }

    @Override
    public long readUnsignedIntLE() {
        return source.readUnsignedIntLE();
    }

    @Override
    public long readLong() {
        return source.readLong();
    }

    @Override
    public long readLongLE() {
        return source.readLongLE();
    }

    @Override
    public char readChar() {
        return source.readChar();
    }

    @Override
    public float readFloat() {
        return source.readFloat();
    }

    @Override
    public double readDouble() {
        return source.readDouble();
    }

    @Override
    public ByteBuf readBytes(int i) {
        return source.readBytes(i);
    }

    @Override
    public ByteBuf readSlice(int i) {
        return source.readSlice(i);
    }

    @Override
    public ByteBuf readRetainedSlice(int i) {
        return source.readRetainedSlice(i);
    }

    @Override
    public ByteBuf readBytes(ByteBuf byteBuf) {
        return source.readBytes(byteBuf);
    }

    @Override
    public ByteBuf readBytes(ByteBuf byteBuf, int i) {
        return source.readBytes(byteBuf, i);
    }

    @Override
    public ByteBuf readBytes(ByteBuf byteBuf, int i, int j) {
        return source.readBytes(byteBuf, i, j);
    }

    @Override
    public ByteBuf readBytes(byte[] bs) {
        return source.readBytes(bs);
    }

    @Override
    public ByteBuf readBytes(byte[] bs, int i, int j) {
        return source.readBytes(bs, i, j);
    }

    @Override
    public ByteBuf readBytes(ByteBuffer byteBuffer) {
        return source.readBytes(byteBuffer);
    }

    @Override
    public ByteBuf readBytes(OutputStream outputStream, int i) throws IOException {
        return source.readBytes(outputStream, i);
    }

    @Override
    public int readBytes(GatheringByteChannel gatheringByteChannel, int i) throws IOException {
        return source.readBytes(gatheringByteChannel, i);
    }

    @Override
    public CharSequence readCharSequence(int i, Charset charset) {
        return source.readCharSequence(i, charset);
    }

    @Override
    public int readBytes(FileChannel fileChannel, long l, int i) throws IOException {
        return source.readBytes(fileChannel, l, i);
    }

    @Override
    public ByteBuf skipBytes(int i) {
        return source.skipBytes(i);
    }

    @Override
    public ByteBuf writeBoolean(boolean bl) {
        return source.writeBoolean(bl);
    }

    @Override
    public ByteBuf writeByte(int i) {
        return source.writeByte(i);
    }

    @Override
    public ByteBuf writeShort(int i) {
        return source.writeShort(i);
    }

    @Override
    public ByteBuf writeShortLE(int i) {
        return source.writeShortLE(i);
    }

    @Override
    public ByteBuf writeMedium(int i) {
        return source.writeMedium(i);
    }

    @Override
    public ByteBuf writeMediumLE(int i) {
        return source.writeMediumLE(i);
    }

    @Override
    public ByteBuf writeInt(int i) {
        return source.writeInt(i);
    }

    @Override
    public ByteBuf writeIntLE(int i) {
        return source.writeIntLE(i);
    }

    @Override
    public ByteBuf writeLong(long l) {
        return source.writeLong(l);
    }

    @Override
    public ByteBuf writeLongLE(long l) {
        return source.writeLongLE(l);
    }

    @Override
    public ByteBuf writeChar(int i) {
        return source.writeChar(i);
    }

    @Override
    public ByteBuf writeFloat(float f) {
        return source.writeFloat(f);
    }

    @Override
    public ByteBuf writeDouble(double d) {
        return source.writeDouble(d);
    }

    @Override
    public ByteBuf writeBytes(ByteBuf byteBuf) {
        return source.writeBytes(byteBuf);
    }

    @Override
    public ByteBuf writeBytes(ByteBuf byteBuf, int i) {
        return source.writeBytes(byteBuf, i);
    }

    @Override
    public ByteBuf writeBytes(ByteBuf byteBuf, int i, int j) {
        return source.writeBytes(byteBuf, i, j);
    }

    @Override
    public ByteBuf writeBytes(byte[] bs) {
        return source.writeBytes(bs);
    }

    @Override
    public ByteBuf writeBytes(byte[] bs, int i, int j) {
        return source.writeBytes(bs, i, j);
    }

    @Override
    public ByteBuf writeBytes(ByteBuffer byteBuffer) {
        return source.writeBytes(byteBuffer);
    }

    @Override
    public int writeBytes(InputStream inputStream, int i) throws IOException {
        return source.writeBytes(inputStream, i);
    }

    @Override
    public int writeBytes(ScatteringByteChannel scatteringByteChannel, int i) throws IOException {
        return source.writeBytes(scatteringByteChannel, i);
    }

    @Override
    public int writeBytes(FileChannel fileChannel, long l, int i) throws IOException {
        return source.writeBytes(fileChannel, l, i);
    }

    @Override
    public ByteBuf writeZero(int i) {
        return source.writeZero(i);
    }

    @Override
    public int writeCharSequence(CharSequence charSequence, Charset charset) {
        return source.writeCharSequence(charSequence, charset);
    }

    @Override
    public int indexOf(int i, int j, byte b) {
        return source.indexOf(i, j, b);
    }

    @Override
    public int bytesBefore(byte b) {
        return source.bytesBefore(b);
    }

    @Override
    public int bytesBefore(int i, byte b) {
        return source.bytesBefore(i, b);
    }

    @Override
    public int bytesBefore(int i, int j, byte b) {
        return source.bytesBefore(i, j, b);
    }

    @Override
    public int forEachByte(ByteProcessor byteProcessor) {
        return source.forEachByte(byteProcessor);
    }

    @Override
    public int forEachByte(int i, int j, ByteProcessor byteProcessor) {
        return source.forEachByte(i, j, byteProcessor);
    }

    @Override
    public int forEachByteDesc(ByteProcessor byteProcessor) {
        return source.forEachByteDesc(byteProcessor);
    }

    @Override
    public int forEachByteDesc(int i, int j, ByteProcessor byteProcessor) {
        return source.forEachByteDesc(i, j, byteProcessor);
    }

    @Override
    public ByteBuf copy() {
        return source.copy();
    }

    @Override
    public ByteBuf copy(int i, int j) {
        return source.copy(i, j);
    }

    @Override
    public ByteBuf slice() {
        return source.slice();
    }

    @Override
    public ByteBuf retainedSlice() {
        return source.retainedSlice();
    }

    @Override
    public ByteBuf slice(int i, int j) {
        return source.slice(i, j);
    }

    @Override
    public ByteBuf retainedSlice(int i, int j) {
        return source.retainedSlice(i, j);
    }

    @Override
    public ByteBuf duplicate() {
        return source.duplicate();
    }

    @Override
    public ByteBuf retainedDuplicate() {
        return source.retainedDuplicate();
    }

    @Override
    public int nioBufferCount() {
        return source.nioBufferCount();
    }

    @Override
    public ByteBuffer nioBuffer() {
        return source.nioBuffer();
    }

    @Override
    public ByteBuffer nioBuffer(int i, int j) {
        return source.nioBuffer(i, j);
    }

    @Override
    public ByteBuffer internalNioBuffer(int i, int j) {
        return source.internalNioBuffer(i, j);
    }

    @Override
    public ByteBuffer[] nioBuffers() {
        return source.nioBuffers();
    }

    @Override
    public ByteBuffer[] nioBuffers(int i, int j) {
        return source.nioBuffers(i, j);
    }

    @Override
    public boolean hasArray() {
        return source.hasArray();
    }

    @Override
    public byte[] array() {
        return source.array();
    }

    @Override
    public int arrayOffset() {
        return source.arrayOffset();
    }

    @Override
    public boolean hasMemoryAddress() {
        return source.hasMemoryAddress();
    }

    @Override
    public long memoryAddress() {
        return source.memoryAddress();
    }

    @Override
    public String toString(Charset charset) {
        return source.toString(charset);
    }

    @Override
    public String toString(int i, int j, Charset charset) {
        return source.toString(i, j, charset);
    }

    @Override
    public int hashCode() {
        return source.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        return source.equals(object);
    }

    @Override
    public int compareTo(ByteBuf byteBuf) {
        return source.compareTo(byteBuf);
    }

    @Override
    public String toString() {
        return source.toString();
    }

    @Override
    public ByteBuf retain(int i) {
        return source.retain(i);
    }

    @Override
    public ByteBuf retain() {
        return source.retain();
    }

    @Override
    public ByteBuf touch() {
        return source.touch();
    }

    @Override
    public ByteBuf touch(Object object) {
        return source.touch(object);
    }

    @Override
    public int refCnt() {
        return source.refCnt();
    }

    @Override
    public boolean release() {
        return source.release();
    }

    @Override
    public boolean release(int i) {
        return source.release(i);
    }

    @FunctionalInterface
    public interface Writer<T> extends BiConsumer<FriendlyByteBuf, T> {
        default Writer<Optional<T>> asOptional() {
            return (buf, optional) -> buf.writeOptional(optional, this);
        }
    }

    @FunctionalInterface
    public interface Reader<T> extends Function<FriendlyByteBuf, T> {
        default Reader<Optional<T>> asOptional() {
            return (buf) -> buf.readOptional(this);
        }
    }

}