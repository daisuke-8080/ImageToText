package com.example.imagetotext;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;


public class Result extends AppCompatActivity implements View.OnClickListener {

    private String postedResultString;
    private CharSequence putResultCharSeq;

    private Button buttonHome;
    private Button buttonCopy;

    private SubsamplingScaleImageView imageSelected;
    private EditText editTextResult;

    private static Uri postedUri;
    private static Bitmap resultBmp;


    private OnBackPressedCallback BackButtonCallBack = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            Intent intentMain = new Intent(getApplication(), MainActivity.class);
            startActivity(intentMain);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        getOnBackPressedDispatcher().addCallback(this, BackButtonCallBack);

        Intent intentResult = getIntent();
        postedResultString = intentResult.getStringExtra("resultString");
        postedUri = intentResult.getParcelableExtra("resultUri");


        putResultCharSeq = postedResultString;
        editTextResult = findViewById(R.id.editTextResult);
        editTextResult.setText(putResultCharSeq);

        imageSelected = findViewById(R.id.imagePost);

        resultBmp = MainActivity.correctBitmapOrientation(postedUri);

        imageSelected.setImage(ImageSource.bitmap(resultBmp));

        buttonHome = findViewById(R.id.buttonHome);
        buttonHome.setOnClickListener(this);

        buttonCopy = findViewById(R.id.buttonCopy);
        buttonCopy.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonHome:
                Intent intentHome = new Intent(getApplication(), MainActivity.class);
                startActivity(intentHome);
                break;

            case R.id.buttonCopy:
                Toast copyToast = Toast.makeText(ImagetoText.getContext(),"クリップボードにコピーされました",Toast.LENGTH_SHORT);
                String copyString = editTextResult.getText().toString();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Result String", copyString);
                clipboard.setPrimaryClip(clip);
                copyToast.show();
                break;
        }
    }
}