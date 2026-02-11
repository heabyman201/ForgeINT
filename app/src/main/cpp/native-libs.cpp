#include <jni.h>
#include <string>
#include <string_view>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_forgeint_presentation_GeminiViewModel_parseStreamChunkNative(
        JNIEnv* env,
        jobject /* this */,
        jstring rawData) {

    const char* utfChars = env->GetStringUTFChars(rawData, NULL);
    if (utfChars == nullptr) return nullptr;

    // Use string_view to avoid copying the entire rawData into a std::string
    std::string_view line(utfChars);

    // 1. Basic Protocol Filtering
    if (line.find("data: ") == std::string_view::npos || line.find("[DONE]") != std::string_view::npos) {
        env->ReleaseStringUTFChars(rawData, utfChars);
        return nullptr;
    }

    // 2. Extract Content Field Surgically
    // Look for "content":"
    constexpr char key[] = "\"content\":\"";
    size_t startPos = line.find(key);
    
    if (startPos == std::string_view::npos) {
        env->ReleaseStringUTFChars(rawData, utfChars);
        return NULL;
    }

    startPos += (sizeof(key) - 1); // Length of "\"content\":\""
    
    // Pre-allocate assuming worst case (content length = remaining line length)
    std::string cleanContent;
    cleanContent.reserve(line.length() - startPos);

    bool escape = false;
    // Lightweight artifact removal: drop "nn" followed by digits.
    // pendingN: 0 none, 1 saw 'n', 2 saw "nn".
    int pendingN = 0;
    bool inArtifactDigits = false;

    for (size_t i = startPos; i < line.length(); ++i) {
        char c = line[i];
    reprocess_char:

        if (escape) {
            if (pendingN == 1) {
                cleanContent += 'n';
            } else if (pendingN == 2) {
                cleanContent += 'n';
                cleanContent += 'n';
            }
            pendingN = 0;
            inArtifactDigits = false;

            switch (c) {
                case 'n': cleanContent += '\n'; break;
                case 't': cleanContent += '\t'; break;
                case 'r': break; // Skip carriage return
                case '"': cleanContent += '"'; break;
                case '\\': cleanContent += '\\'; break;
                default: cleanContent += c; break;
            }
            escape = false;
        } else if (c == '\\') {
            escape = true;
        } else if (c == '"') {
             // End of JSON string value
             if (pendingN == 1) {
                 cleanContent += 'n';
             } else if (pendingN == 2) {
                 cleanContent += 'n';
                 cleanContent += 'n';
             }
             break;
        } else if (inArtifactDigits) {
            // Drop digits until we hit a non-digit, then continue processing.
            if (c < '0' || c > '9') {
                inArtifactDigits = false;
                goto reprocess_char;
            }
        } else {
            // Artifact removal: detect "nn" followed by digits.
            if (pendingN == 0) {
                if (c == 'n') {
                    pendingN = 1;
                } else {
                    cleanContent += c;
                }
            } else if (pendingN == 1) {
                if (c == 'n') {
                    pendingN = 2;
                } else {
                    cleanContent += 'n';
                    pendingN = 0;
                    goto reprocess_char;
                }
            } else { // pendingN == 2
                if (c >= '0' && c <= '9') {
                    inArtifactDigits = true;
                    pendingN = 0;
                } else {
                    cleanContent += 'n';
                    cleanContent += 'n';
                    pendingN = 0;
                    goto reprocess_char;
                }
            }
        }
    }

    env->ReleaseStringUTFChars(rawData, utfChars);

    if (cleanContent.empty()) return NULL;
    return env->NewStringUTF(cleanContent.c_str());
}
