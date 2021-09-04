package com.example.transcriber;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.microsoft.cognitiveservices.speech.CancellationReason;
import com.microsoft.cognitiveservices.speech.PropertyId;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;

public class MainActivity extends AppCompatActivity {

    private static final int ON_RECOGNIZED = 100;
    private static final int ON_RECOGNIZING = 101;
    private static final int ON_CANCELLED = 102;
    private static final int ON_SPEECH_END = 103;
    private static final int ON_START = 104;
    // Replace below with your own subscription key
    private static String speechSubscriptionKey = "<sub>";
    // Replace below with your own service region (e.g., "westus").
    private static String serviceRegion = "eastus";
    private static StringBuffer transcript = new StringBuffer("");
    private Handler mHandlerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request the permissions
        int requestCode = 5;
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, INTERNET}, requestCode);
    }


    public void onSpeechButtonClicked(View v) throws Exception {
        TextView recognizedTxt = (TextView) this.findViewById(R.id.recognized);
        Button button = (Button) this.findViewById(R.id.button);

        mHandlerThread = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == ON_RECOGNIZING) {
                    recognizedTxt.setText(transcript + msg.obj.toString());
                } else if (msg.what == ON_RECOGNIZED) {
                    transcript.append(msg.obj.toString());
                    recognizedTxt.setText(transcript);
                } else if (msg.what == ON_CANCELLED) {
                    recognizedTxt.setText(msg.obj.toString());
                } else if (msg.what == ON_SPEECH_END) {
                    button.setTextColor(Color.BLACK);
                } else if (msg.what == ON_START) {
                    button.setTextColor(Color.RED);
                }
            }
        };

        SpeechConfig config = SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion);
        config.setProperty(PropertyId.SpeechServiceConnection_EndSilenceTimeoutMs, "5000");

        AudioConfig audioConfig = AudioConfig.fromDefaultMicrophoneInput();
        SpeechRecognizer recognizer = new SpeechRecognizer(config, audioConfig);

        recognizer.recognizing.addEventListener((s, e) -> {
            Message message = new Message();
            message.what = ON_RECOGNIZING;
            message.obj = e.getResult().getText();
            mHandlerThread.sendMessage(message);
        });

        recognizer.recognized.addEventListener((s, e) -> {
            Message message = new Message();
            message.what = ON_RECOGNIZED;
            if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                message.obj = e.getResult().getText();
            } else if (e.getResult().getReason() == ResultReason.NoMatch) {
                message.obj = " *??* ";
            }
            mHandlerThread.sendMessage(message);
        });

        recognizer.canceled.addEventListener((s, e) -> {
            Message message = new Message();
            message.what = ON_CANCELLED;
            StringBuilder reason = new StringBuilder();
            reason.append("CANCELED: Reason=" + e.getReason());
            if (e.getReason() == CancellationReason.Error) {
                reason.append(recognizedTxt.getText() +
                        "/n CANCELED: ErrorCode=" + e.getErrorCode() +
                        "/nCANCELED: ErrorDetails=" + e.getErrorDetails() +
                        "/n CANCELED: Did you update the subscription info?");
                audioConfig.close();
            }
            message.obj = reason;
            mHandlerThread.sendMessage(message);
        });

        recognizer.speechEndDetected.addEventListener((s, e) -> {
            mHandlerThread.sendEmptyMessage(ON_SPEECH_END);
            try {
                recognizer.stopContinuousRecognitionAsync();
                audioConfig.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        mHandlerThread.sendEmptyMessage(ON_START);
        recognizer.startContinuousRecognitionAsync();
    }

}

