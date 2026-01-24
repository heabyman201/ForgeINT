#include <jni.h>
#include <string>
#include <vector>
#include <cstring>

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
    
    // We will handle the "nn[0-9]+" artifact removal INLINE during this loop
    // to avoid a second pass and regex overhead.
    // State for artifact detection: 
    // 0: normal, 1: found 'n', 2: found 'n' (so "nn"), 3: found digit (inside pattern)
    int artifactState = 0; 
    std::string potentialArtifact; // Buffer to hold "nn..." in case it's not a full match

    for (size_t i = startPos; i < line.length(); ++i) {
        char c = line[i];

        if (escape) {
            // If we were parsing a potential artifact, flush it because escape sequence breaks it
            if (artifactState > 0) {
                 cleanContent += potentialArtifact;
                 artifactState = 0;
                 potentialArtifact.clear();
            }

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
             // If we have a pending artifact buffer, flush it (unlikely to end with "nn123" at quote, but safe to flush)
             if (artifactState > 0) {
                 cleanContent += potentialArtifact;
             }
             break;
        } else {
            // Artifact Removal Logic: "nn[0-9]+"
            // State machine to detect and drop "nn" followed by digits
            
            if (artifactState == 0) {
                if (c == 'n') {
                    artifactState = 1;
                    potentialArtifact += c;
                } else {
                    cleanContent += c;
                }
            } else if (artifactState == 1) { // Found "n"
                if (c == 'n') {
                    artifactState = 2;
                    potentialArtifact += c;
                } else {
                    // It was just one 'n' followed by something else. Flush and append current.
                    cleanContent += potentialArtifact;
                    cleanContent += c;
                    artifactState = 0;
                    potentialArtifact.clear();
                }
            } else if (artifactState == 2) { // Found "nn"
                if (c >= '0' && c <= '9') {
                    artifactState = 3; // Entered digit phase
                    potentialArtifact += c;
                } else {
                    // "nn" followed by non-digit. Not an artifact. Flush.
                    cleanContent += potentialArtifact;
                    cleanContent += c;
                    artifactState = 0;
                    potentialArtifact.clear();
                }
            } else if (artifactState == 3) { // Found "nn[0-9]..."
                if (c >= '0' && c <= '9') {
                    potentialArtifact += c;
                    // Continue consuming digits
                } else {
                    // End of digits. We successfully matched "nn[0-9]+". 
                    // DROP the potentialArtifact (do nothing with it).
                    // Reset state and process the current character 'c' normally.
                    artifactState = 0;
                    potentialArtifact.clear();
                    
                    // Re-process 'c' effectively (it's not part of the artifact)
                    // We know 'c' is not '\', '"', or 'n' (handled by outer logic? No wait.)
                    // We are in the 'else' block of escape and quote checks.
                    
                    // Wait, we need to handle if 'c' starts a NEW artifact?
                    // "nn123n..." -> 'n' could be start of new.
                    if (c == 'n') {
                         artifactState = 1;
                         potentialArtifact += c;
                    } else {
                         cleanContent += c;
                    }
                }
            }
        }
    }

    env->ReleaseStringUTFChars(rawData, utfChars);

    if (cleanContent.empty()) return NULL;
    return env->NewStringUTF(cleanContent.c_str());
}