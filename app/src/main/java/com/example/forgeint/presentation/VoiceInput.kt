package com.example.forgeint.presentation


import android.app.Activity


import android.app.RemoteInput


import android.content.Intent


import android.speech.RecognizerIntent


import android.view.inputmethod.EditorInfo


import androidx.activity.compose.rememberLauncherForActivityResult


import androidx.activity.result.contract.ActivityResultContracts


import androidx.compose.runtime.Composable


import androidx.compose.runtime.remember


import androidx.wear.input.RemoteInputIntentHelper





@Composable


fun rememberVoiceLauncher(onInputReceived: (String) -> Unit): () -> Unit {


    val launcher = rememberLauncherForActivityResult(


        ActivityResultContracts.StartActivityForResult()


    ) { result ->


        if (result.resultCode == Activity.RESULT_OK) {


            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)


            val text = results?.get(0)


            text?.let { onInputReceived(it) }


        }


    }





    return remember {


        {


            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)


            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)


            launcher.launch(intent)


        }


    }


}





@Composable


fun rememberTextInputLauncher(onInputReceived: (String) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val results = RemoteInput.getResultsFromIntent(result.data)
            val text = results?.getCharSequence("result_text")?.toString()
            text?.let { onInputReceived(it) }
        }
    }

    return remember {
        {
            val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
            val remoteInputs = listOf(
                RemoteInput.Builder("result_text")
                    .setLabel("Type your message")
                    .setAllowFreeFormInput(true)
                    .build()
            )
            RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)

            // Optional: Hint to open keyboard directly if supported by OS version
//            intent.putExtra(EditorInfo.EXT, EditorInfo.IME_ACTION_SEND)

            launcher.launch(intent)
        }
    }
}