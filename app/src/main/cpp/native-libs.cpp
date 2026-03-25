#include <jni.h>
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <cstdlib>
#include <limits>
#include <vector>

namespace {

constexpr float kVectorNormalizeEpsilon = 1e-8f;
constexpr float kCosineNormEpsilon = 1e-10f;

inline bool isAsciiWhitespace(char c) {
    return c == ' ' || c == '\t' || c == '\n' || c == '\r';
}

size_t countCommaSeparatedFloats(const char* text) {
    if (text == nullptr) return 0;

    const char* p = text;
    size_t count = 0;
    while (*p != '\0') {
        while (isAsciiWhitespace(*p) || *p == ',') {
            ++p;
        }
        if (*p == '\0') break;

        char* endPtr = nullptr;
        std::strtof(p, &endPtr);
        if (endPtr == p) {
            while (*p != '\0' && *p != ',') ++p;
            continue;
        }
        ++count;
        p = endPtr;
        while (*p != '\0' && *p != ',') {
            if (!isAsciiWhitespace(*p)) break;
            ++p;
        }
        if (*p == ',') ++p;
    }
    return count;
}

size_t parseCommaSeparatedFloatsToBuffer(const char* text, float* out, size_t capacity) {
    if (text == nullptr || out == nullptr || capacity == 0) return 0;

    const char* p = text;
    size_t count = 0;
    while (*p != '\0' && count < capacity) {
        while (isAsciiWhitespace(*p) || *p == ',') {
            ++p;
        }
        if (*p == '\0') break;

        char* endPtr = nullptr;
        const float value = std::strtof(p, &endPtr);
        if (endPtr == p) {
            while (*p != '\0' && *p != ',') ++p;
            continue;
        }
        out[count++] = value;
        p = endPtr;
        while (*p != '\0' && *p != ',') {
            if (!isAsciiWhitespace(*p)) break;
            ++p;
        }
        if (*p == ',') ++p;
    }
    return count;
}

void normalizeInPlace(float* vec, size_t len) {
    if (vec == nullptr || len == 0) return;
    float normSq = 0.0f;
    for (size_t i = 0; i < len; ++i) {
        normSq += vec[i] * vec[i];
    }
    const float norm = std::sqrt(normSq);
    if (norm <= kVectorNormalizeEpsilon) return;
    const float inv = 1.0f / norm;
    for (size_t i = 0; i < len; ++i) {
        vec[i] *= inv;
    }
}

double cosineSimilaritySlices(const float* a, size_t aLen, const float* b, size_t bLen) {
    const size_t size = std::min(aLen, bLen);
    if (size == 0) return 0.0;

    float dot = 0.0f;
    float normA = 0.0f;
    float normB = 0.0f;
    for (size_t i = 0; i < size; ++i) {
        const float va = a[i];
        const float vb = b[i];
        dot += va * vb;
        normA += va * va;
        normB += vb * vb;
    }
    if (normA <= kCosineNormEpsilon || normB <= kCosineNormEpsilon) return 0.0;
    const float score = dot / (std::sqrt(normA) * std::sqrt(normB));
    return std::clamp(score, 0.0f, 1.0f);
}

double cosineSimilarityFloatBytesLE(const float* a, size_t aLen, const uint8_t* bytes, size_t floatCount) {
    const size_t size = std::min(aLen, floatCount);
    if (size == 0 || bytes == nullptr) return 0.0;

    float dot = 0.0f;
    float normA = 0.0f;
    float normB = 0.0f;
    for (size_t i = 0; i < size; ++i) {
        const size_t base = i * 4u;
        const uint32_t bits =
                (static_cast<uint32_t>(bytes[base])) |
                (static_cast<uint32_t>(bytes[base + 1]) << 8u) |
                (static_cast<uint32_t>(bytes[base + 2]) << 16u) |
                (static_cast<uint32_t>(bytes[base + 3]) << 24u);
        float vb = 0.0f;
        std::memcpy(&vb, &bits, sizeof(float));
        const float va = a[i];
        dot += va * vb;
        normA += va * va;
        normB += vb * vb;
    }
    if (normA <= kCosineNormEpsilon || normB <= kCosineNormEpsilon) return 0.0;
    const float score = dot / (std::sqrt(normA) * std::sqrt(normB));
    return std::clamp(score, 0.0f, 1.0f);
}

inline bool isTokenChar(unsigned char c) {
    return ((c >= 'a' && c <= 'z') ||
            (c >= 'A' && c <= 'Z') ||
            (c >= '0' && c <= '9') ||
            c == '\'');
}

} // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_forgeint_presentation_GeminiViewModel_parseStreamChunkNative(
        JNIEnv* env,
        jobject /* this */,
        jstring rawData) {

    if (rawData == nullptr) return nullptr;

    const char* utfChars = env->GetStringUTFChars(rawData, nullptr);
    if (utfChars == nullptr) return nullptr; // OOM / invalid input

    // Fast pre-filtering with C-string scans (very hot path on watch streaming).
    constexpr const char* kDataPrefix = "data: ";
    constexpr const char* kDoneToken = "[DONE]";
    constexpr const char* kContentKey = "\"content\":\"";
    constexpr size_t kContentKeyLen = 11; // strlen("\"content\":\"")
    constexpr size_t kMaxChunkChars = 2048; // hard cap to avoid runaway allocations

    const char* dataPos = std::strstr(utfChars, kDataPrefix);
    if (dataPos == nullptr || std::strstr(dataPos, kDoneToken) != nullptr) {
        env->ReleaseStringUTFChars(rawData, utfChars);
        return nullptr;
    }

    const char* contentPos = std::strstr(dataPos, kContentKey);
    if (contentPos == nullptr) {
        env->ReleaseStringUTFChars(rawData, utfChars);
        return nullptr;
    }
    const char* p = contentPos + kContentKeyLen;

    // Fixed thread-local buffer avoids per-chunk heap allocations entirely.
    thread_local char outBuf[kMaxChunkChars + 1];
    size_t outLen = 0;
    auto pushChar = [&](char ch) {
        if (outLen < kMaxChunkChars) {
            outBuf[outLen++] = ch;
        }
    };

    bool escaped = false;
    // Lightweight artifact removal: drop "nn" followed by digits.
    // pendingN: 0 none, 1 saw 'n', 2 saw "nn".
    int pendingN = 0;
    bool inArtifactDigits = false;

    auto flushPendingN = [&]() {
        if (pendingN == 1) {
            pushChar('n');
        } else if (pendingN == 2) {
            pushChar('n');
            pushChar('n');
        }
        pendingN = 0;
        inArtifactDigits = false;
    };

    while (*p != '\0' && outLen < kMaxChunkChars) {
        const char c = *p++;

        if (escaped) {
            flushPendingN();
            switch (c) {
                case 'n': pushChar('\n'); break;
                case 't': pushChar('\t'); break;
                case 'r': break; // Skip carriage return
                case '"': pushChar('"'); break;
                case '\\': pushChar('\\'); break;
                default: pushChar(c); break;
            }
            escaped = false;
            continue;
        }

        if (c == '\\') {
            escaped = true;
            continue;
        }
        if (c == '"') {
            // End of JSON string value.
            flushPendingN();
            break;
        }
        if (inArtifactDigits) {
            if (c >= '0' && c <= '9') {
                continue; // drop artifact digits
            }
            inArtifactDigits = false;
        }

        // Artifact removal state machine.
        if (pendingN == 0) {
            if (c == 'n') {
                pendingN = 1;
            } else {
                pushChar(c);
            }
            continue;
        }
        if (pendingN == 1) {
            if (c == 'n') {
                pendingN = 2;
            } else {
                pushChar('n');
                pendingN = 0;
                // Re-process current character in zero state.
                if (c == 'n') pendingN = 1;
                else pushChar(c);
            }
            continue;
        }
        // pendingN == 2
        if (c >= '0' && c <= '9') {
            inArtifactDigits = true;
            pendingN = 0;
        } else {
            pushChar('n');
            pushChar('n');
            pendingN = 0;
            if (pendingN == 1) {
                pushChar('n');
                pendingN = 0;
            } else if (c == 'n') {
                pendingN = 1;
            } else {
                pushChar(c);
            }
        }
    }

    env->ReleaseStringUTFChars(rawData, utfChars);

    if (outLen == 0) return nullptr;
    outBuf[outLen] = '\0';
    return env->NewStringUTF(outBuf);
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_example_forgeint_presentation_GeminiViewModel_decodeVectorNative(
        JNIEnv* env,
        jobject /* this */,
        jstring encoded) {
    if (encoded == nullptr) return env->NewFloatArray(0);

    const char* utfChars = env->GetStringUTFChars(encoded, nullptr);
    if (utfChars == nullptr) return env->NewFloatArray(0);

    const size_t valueCount = countCommaSeparatedFloats(utfChars);
    env->ReleaseStringUTFChars(encoded, utfChars);

    jfloatArray out = env->NewFloatArray(static_cast<jsize>(valueCount));
    if (out == nullptr) return nullptr;
    if (valueCount == 0) return out;

    jfloat* outPtr = env->GetFloatArrayElements(out, nullptr);
    if (outPtr == nullptr) return out;

    utfChars = env->GetStringUTFChars(encoded, nullptr);
    if (utfChars == nullptr) {
        env->ReleaseFloatArrayElements(out, outPtr, 0);
        return out;
    }
    parseCommaSeparatedFloatsToBuffer(utfChars, outPtr, valueCount);
    env->ReleaseStringUTFChars(encoded, utfChars);
    env->ReleaseFloatArrayElements(out, outPtr, 0);
    return out;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_example_forgeint_presentation_GeminiViewModel_normalizeVectorNative(
        JNIEnv* env,
        jobject /* this */,
        jfloatArray raw) {
    if (raw == nullptr) return env->NewFloatArray(0);
    const jsize len = env->GetArrayLength(raw);
    jfloatArray out = env->NewFloatArray(len);
    if (out == nullptr) return nullptr;
    if (len <= 0) return out;

    jfloat* outPtr = env->GetFloatArrayElements(out, nullptr);
    if (outPtr == nullptr) return out;
    env->GetFloatArrayRegion(raw, 0, len, outPtr);
    normalizeInPlace(outPtr, static_cast<size_t>(len));
    env->ReleaseFloatArrayElements(out, outPtr, 0);
    return out;
}

extern "C" JNIEXPORT jdouble JNICALL
Java_com_example_forgeint_presentation_GeminiViewModel_cosineSimilarityNative(
        JNIEnv* env,
        jobject /* this */,
        jfloatArray a,
        jfloatArray b) {
    if (a == nullptr || b == nullptr) return 0.0;
    const jsize lenA = env->GetArrayLength(a);
    const jsize lenB = env->GetArrayLength(b);
    const jsize size = std::min(lenA, lenB);
    if (size <= 0) return 0.0;

    auto* va = env->GetFloatArrayElements(a, nullptr);
    if (va == nullptr) return 0.0;
    auto* vb = env->GetFloatArrayElements(b, nullptr);
    if (vb == nullptr) {
        env->ReleaseFloatArrayElements(a, va, JNI_ABORT);
        return 0.0;
    }

    const double score = cosineSimilaritySlices(
            va,
            static_cast<size_t>(size),
            vb,
            static_cast<size_t>(size));
    env->ReleaseFloatArrayElements(b, vb, JNI_ABORT);
    env->ReleaseFloatArrayElements(a, va, JNI_ABORT);
    return score;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_example_forgeint_presentation_GeminiViewModel_fallbackHashEmbeddingNative(
        JNIEnv* env,
        jobject /* this */,
        jstring text,
        jint dim) {
    if (dim <= 0) return env->NewFloatArray(0);

    std::vector<float> vec(static_cast<size_t>(dim), 0.0f);

    if (text != nullptr) {
        const char* utfChars = env->GetStringUTFChars(text, nullptr);
        if (utfChars != nullptr) {
            size_t tokenIndex = 0;

            auto processTokenHash = [&](uint32_t hash) {
                const int32_t signedHash = static_cast<int32_t>(hash);
                const int64_t absHash = (signedHash == std::numeric_limits<int32_t>::min())
                                        ? static_cast<int64_t>(std::numeric_limits<int32_t>::max()) + 1
                                        : std::llabs(static_cast<long long>(signedHash));
                const int idx = static_cast<int>(absHash % dim);
                const float sign = (((hash >> 1u) & 1u) == 0u) ? 1.0f : -1.0f;
                const float weight = 1.0f + static_cast<float>(tokenIndex % 3u) * 0.1f;
                vec[static_cast<size_t>(idx)] += sign * weight;
                ++tokenIndex;
            };

            const unsigned char* p = reinterpret_cast<const unsigned char*>(utfChars);
            uint32_t tokenHash = 0u;
            bool hasToken = false;
            while (*p != '\0') {
                unsigned char c = *p++;
                if (isTokenChar(c)) {
                    if (c >= 'A' && c <= 'Z') c = static_cast<unsigned char>(c - 'A' + 'a');
                    tokenHash = (tokenHash * 31u) + static_cast<uint8_t>(c);
                    hasToken = true;
                } else if (hasToken) {
                    processTokenHash(tokenHash);
                    tokenHash = 0u;
                    hasToken = false;
                }
            }
            if (hasToken) {
                processTokenHash(tokenHash);
            }

            env->ReleaseStringUTFChars(text, utfChars);
        }
    }

    normalizeInPlace(vec.data(), static_cast<size_t>(dim));
    jfloatArray out = env->NewFloatArray(dim);
    if (out == nullptr) return nullptr;
    env->SetFloatArrayRegion(out, 0, dim, vec.data());
    return out;
}

extern "C" JNIEXPORT jdoubleArray JNICALL
Java_com_example_forgeint_presentation_GeminiViewModel_scoreMessageVectorsNative(
        JNIEnv* env,
        jobject /* this */,
        jfloatArray queryVector,
        jobjectArray vectorBlobs) {
    if (queryVector == nullptr || vectorBlobs == nullptr) {
        return env->NewDoubleArray(0);
    }

    const jsize queryLen = env->GetArrayLength(queryVector);
    const jsize itemCount = env->GetArrayLength(vectorBlobs);
    jdoubleArray out = env->NewDoubleArray(itemCount);
    if (out == nullptr) return nullptr;
    if (queryLen <= 0 || itemCount <= 0) return out;

    auto* query = env->GetFloatArrayElements(queryVector, nullptr);
    if (query == nullptr) return out;

    std::vector<jdouble> scores(static_cast<size_t>(itemCount), 0.0);
    for (jsize i = 0; i < itemCount; ++i) {
        auto* blob = static_cast<jbyteArray>(env->GetObjectArrayElement(vectorBlobs, i));
        if (blob == nullptr) continue;

        const jsize byteLen = env->GetArrayLength(blob);
        const jsize floatCount = byteLen / 4;
        if (floatCount > 0) {
            auto* bytes = static_cast<const jbyte*>(env->GetPrimitiveArrayCritical(blob, nullptr));
            if (bytes != nullptr) {
                scores[static_cast<size_t>(i)] = cosineSimilarityFloatBytesLE(
                        query,
                        static_cast<size_t>(queryLen),
                        reinterpret_cast<const uint8_t*>(bytes),
                        static_cast<size_t>(floatCount));
                env->ReleasePrimitiveArrayCritical(blob, const_cast<jbyte*>(bytes), JNI_ABORT);
            }
        }
        env->DeleteLocalRef(blob);
    }

    env->ReleaseFloatArrayElements(queryVector, query, JNI_ABORT);
    env->SetDoubleArrayRegion(out, 0, itemCount, scores.data());
    return out;
}
