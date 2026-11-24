package com.nova.phoneassistant;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;

public class VoiceCaptureActivity extends Activity {
    private static final int REQUEST_SPEECH = 7101;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar");
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث بالأمر الذي تريد من NOVA تنفيذه");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            startActivityForResult(intent, REQUEST_SPEECH);
        } catch (Exception e) {
            Toast.makeText(this, "ميزة التعرف على الصوت غير متاحة على هذا الهاتف.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SPEECH && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty() && AssistantOverlayService.activeCommandInput != null) {
                AssistantOverlayService.activeCommandInput.setText(results.get(0));
                AssistantOverlayService.activeCommandInput.setSelection(results.get(0).length());
            } else if (AssistantOverlayService.activeCommandInput == null) {
                Toast.makeText(this, "افتح لوحة NOVA أولاً ثم جرّب الميكروفون.", Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }
}
