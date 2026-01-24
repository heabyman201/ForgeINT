#include <jni.h>
#include <string>
#include <android/log.h>
#include <vector>
#include <cstring>


#include "llama.h"

#define TAG "LocalEngine_Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static llama_model *g_model = nullptr;
static llama_context *g_context = nullptr;
static llama_sampler *g_sampler = nullptr;

// --- 1. LOAD MODEL ---
extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_forgeint_presentation_LocalInferenceEngine_loadModel(
        JNIEnv *env,
        jobject /* this */,
        jstring modelPathStr) {

    const char *modelPath = env->GetStringUTFChars(modelPathStr, nullptr);

    if (g_model) {
        llama_free_model(g_model);
        g_model = nullptr;
    }
    if (g_context) {
        llama_free(g_context);
        g_context = nullptr;
    }

    llama_backend_init();

    auto model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;

    // CPU only for Watch

    g_model = llama_load_model_from_file(modelPath, model_params);

    if (!g_model) {
        LOGE("Failed to load model from %s", modelPath);
        env->ReleaseStringUTFChars(modelPathStr, modelPath);
        return false;
    }

    auto ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 1024;
    ctx_params.n_threads = 2; // Strict limit for Exynos W920
    ctx_params.n_threads_batch = 2;


    g_context = llama_new_context_with_model(g_model, ctx_params);
    g_sampler = llama_sampler_init_greedy();

    LOGI("Model loaded successfully: %s", modelPath);
    env->ReleaseStringUTFChars(modelPathStr, modelPath);
    return true;
}

// --- 2. GENERATE RESPONSE ---
extern "C" JNIEXPORT jstring JNICALL
Java_com_example_forgeint_presentation_LocalInferenceEngine_generateResponse(
        JNIEnv *env,
        jobject /* this */,
        jstring promptStr) {

    if (!g_model || !g_context) {
        return env->NewStringUTF("Error: Neural Core not initialized.");
    }

    const char *prompt = env->GetStringUTFChars(promptStr, nullptr);
    std::string result_text = "";

    // Get Vocabulary
    const llama_vocab *vocab = llama_model_get_vocab(g_model);

    // Tokenize
    int n_prompt = -llama_tokenize(vocab, prompt, strlen(prompt), NULL, 0, true, true);
    std::vector<llama_token> prompt_tokens(n_prompt);

    if (llama_tokenize(vocab, prompt, strlen(prompt), prompt_tokens.data(), prompt_tokens.size(), true, true) < 0) {
        return env->NewStringUTF("Error: Tokenization failed");
    }

    // Prepare Batch
    llama_batch batch = llama_batch_get_one(prompt_tokens.data(), prompt_tokens.size());

    // Decode Prompt
    if (llama_decode(g_context, batch) != 0) {
        return env->NewStringUTF("Error: Prompt decoding failed");
    }

    // Generate Loop
    int n_predict = 128; // Battery saver limit
    for (int i = 0; i < n_predict; i++) {
        llama_token new_token_id = llama_sampler_sample(g_sampler, g_context, -1);

        if (llama_token_is_eog(vocab, new_token_id)) {
            break;
        }

        char buf[256];
        int n = llama_token_to_piece(vocab, new_token_id, buf, sizeof(buf), 0, true);

        if (n < 0) {
            // Error handling
        } else {
            std::string piece(buf, n);
            result_text += piece;
        }

        batch = llama_batch_get_one(&new_token_id, 1);

        if (llama_decode(g_context, batch) != 0) {
            break;
        }
    }

    env->ReleaseStringUTFChars(promptStr, prompt);
    return env->NewStringUTF(result_text.c_str());
}

// --- 3. UNLOAD MODEL ---
extern "C" JNIEXPORT void JNICALL
Java_com_example_forgeint_presentation_LocalInferenceEngine_unloadModel(
        JNIEnv *env,
        jobject /* this */) {

    if (g_sampler) {
        llama_sampler_free(g_sampler);
        g_sampler = nullptr;
    }
    if (g_context) {
        llama_free(g_context);
        g_context = nullptr;
    }
    if (g_model) {
        llama_free_model(g_model);
        g_model = nullptr;
    }

    LOGI("Neural Core unloaded.");
}