package com.shopsmart.rag;

/** Small helpers for embedding vectors. */
public final class Vectors {
    private Vectors() {}

    /**
     * Returns exactly {@code dim} values, L2-normalised. If the provider returned a longer vector (it ignored the
     * requested dimension) the vector is truncated - valid for Matryoshka-trained models such as gemini-embedding-001.
     * A shorter vector is a configuration error.
     */
    public static float[] fit(float[] v, int dim) {
        if (v == null || v.length < dim) {
            throw new IllegalArgumentException("Embedding model returned " + (v == null ? 0 : v.length)
                    + " dimensions but " + dim + " are configured (EMBEDDING_DIMENSION)");
        }
        float[] out = java.util.Arrays.copyOf(v, dim);
        double norm = 0;
        for (float x : out) norm += (double) x * x;
        norm = Math.sqrt(norm);
        if (norm > 0) for (int i = 0; i < dim; i++) out[i] = (float) (out[i] / norm);
        return out;
    }

    /** pgvector text format: [0.1,0.2,...] (used with a ?::vector cast). */
    public static String literal(float[] v) {
        StringBuilder sb = new StringBuilder(v.length * 10).append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }
}
