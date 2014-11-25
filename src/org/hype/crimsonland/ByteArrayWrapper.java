package org.hype.crimsonland;

import java.io.Serializable;
import java.util.Arrays;

public final class ByteArrayWrapper implements Serializable {
    private static final long serialVersionUID = 6548787978684L;
    private final byte[][] data;

    public ByteArrayWrapper(byte[][] data) {
        if (data == null) {
            throw new NullPointerException();
        }
        this.data = data;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ByteArrayWrapper)) {
            return false;
        }
        return Arrays.deepEquals(data, ((ByteArrayWrapper) other).data);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(data);
    }

    @Override
    public String toString() {
        return "ByteArrayWrapper{" +
                "data=" + Arrays.deepToString(data) +
                '}';
    }
}