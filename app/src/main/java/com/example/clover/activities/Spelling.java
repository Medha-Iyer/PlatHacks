package com.example.clover.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.clover.R;
import com.example.clover.pojo.GameItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.time.chrono.HijrahDate;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;

public class Spelling extends AppCompatActivity implements View.OnClickListener {

    BottomNavigationView navView;
    ImageView hearWordBtn;
    TextView viewWord, correctView;
    EditText userWord;
    Button checkWordBtn, checkAgainBtn, nextWordBtn;

    private TextToSpeech mTTS;

    //to store words
    private String currentWord;
    private ArrayList<String> wordList = new ArrayList<String>();
    private ArrayList<GameItem> completedList = new ArrayList<GameItem>();

    private int score = 0;

    //show word for a few seconds
    private Handler mHandler = new Handler();

    //to get the right age from list
    private int age;
    private String userId;
    private Scanner scanner;

    FirebaseFirestore fStore;
    FirebaseAuth fAuth;
    DocumentReference documentReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spelling);

        viewWord = findViewById(R.id.show_word);
        correctView = findViewById(R.id.correct_text);
        userWord = findViewById(R.id.input_word);

        checkWordBtn = findViewById(R.id.check_word);
        checkWordBtn.setOnClickListener(this);

        checkAgainBtn = findViewById(R.id.check_again);
        checkAgainBtn.setOnClickListener(this);

        nextWordBtn = findViewById(R.id.next_word);
        nextWordBtn.setOnClickListener(this);

        reset();

        //speak word if click on speaker button
        hearWordBtn = findViewById(R.id.speak_word);
        hearWordBtn.setOnClickListener(this);
        // declare if text to speech is being used
        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTTS.setLanguage(Locale.getDefault());

                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    } else {
                        hearWordBtn.setEnabled(true);
                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        });

        //call from firebase
        fStore = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance();
        userId = fAuth.getCurrentUser().getUid();
        documentReference = fStore.collection("users").document(userId);

        //getting the correct list based on age
        //loads age from firebase
        readData(new Spelling.FirebaseCallback() {
            @Override
            public void onCallback(int a) {
                //adds all the words from text file into an arraylist so they can be chosen randomly in the game.
                if(age>=7){
                    scanner = new Scanner(getResources().openRawResource(R.raw.words2));
                }else{
                    scanner = new Scanner(getResources().openRawResource(R.raw.words1));
                }
                while (scanner.hasNextLine()) {
                    wordList.add(scanner.nextLine());
                }
                scanner.close();

                //show word for 5 seconds before disappearing
                setUpWord();
            }
        });

        //perform item selected listener
        navView = findViewById(R.id.nav_bar);
        navView.setSelectedItemId(R.id.home); //TODO set the navigation bar to nothing highlighted
        navView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.camera:
                    startActivity(new Intent(getApplicationContext(), Camera.class));
                    overridePendingTransition(0, 0);
                    return true;
                case R.id.library:
                    startActivity(new Intent(getApplicationContext(), Library.class));
                    overridePendingTransition(0, 0);
                    return true;
                case R.id.home:
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                case R.id.profile:
                    startActivity(new Intent(getApplicationContext(), Profile.class));
                    overridePendingTransition(0, 0);
                    return true;
                case R.id.settings:
                    startActivity(new Intent(getApplicationContext(), Settings.class));
                    overridePendingTransition(0, 0);
                    return true;
            }
            return false;
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.speak_word:
                Settings.speak(mTTS, currentWord);
                break;
            case R.id.check_word:
                checkIfCorrect(0);
                break;
            case R.id.check_again:
                checkIfCorrect(1);
                break;
            case R.id.next_word:
                reset();
                setUpWord();
                break;
        }
    }

    private void setUpWord(){
        //show word for 5 seconds before disappearing
        viewWord.setText(randomLine(wordList));
        Settings.speak(mTTS, currentWord);
        mHandler.postDelayed(mShowLoadingRunnable, 2000);
    }

    //after showing word for 2 seconds
    private Runnable mShowLoadingRunnable = new Runnable() {
        @Override
        public void run() {
            viewWord.setText("Loading...");
            userWord.setVisibility(View.VISIBLE);
            checkWordBtn.setVisibility(View.VISIBLE);
        }
    };

    //check if word is correct
    private void checkIfCorrect(int code){

        String result = userWord.getText().toString();
        if(result.equals("")){
            Toast.makeText(this, "Please type in the word...", Toast.LENGTH_SHORT).show();
            return;
        }
        viewWord.setText(currentWord);

        if(result.equalsIgnoreCase(currentWord)){
            correctView.setText("Correct!");
            correctView.setTextColor(getResources().getColor(R.color.darkGreen));

            if(code==0) {
                viewWord.setBackground(getResources().getDrawable(R.drawable.rounded_light_green));
                score++;
                completedList.get(0).setItemIcon(R.drawable.check);
            }

            checkAgainBtn.setVisibility(View.GONE);
            nextWordBtn.setVisibility(View.VISIBLE);


            if(completedList.size()==2){
                sendToSpellingResults();
            }

        }else{

            correctView.setText("Incorrect!");
            correctView.setTextColor(getResources().getColor(R.color.darkRed));

            viewWord.setBackground(getResources().getDrawable(R.drawable.rounded_light_red));
            checkAgainBtn.setVisibility(View.VISIBLE);

            if(code==0){
                completedList.get(0).setItemIcon(R.drawable.x);
            }
        }

        checkWordBtn.setVisibility(View.GONE);
        correctView.setVisibility(View.VISIBLE);
    }

    private void sendToSpellingResults(){
        Intent i = new Intent(Spelling.this, SpellingResults.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("spelling list", completedList);
        i.putExtras(bundle);
        startActivity(i);
        overridePendingTransition(0,0);
    }

    //for the speaker function
    @Override
    protected void onDestroy() {
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }

        super.onDestroy();
    }

    //get random word from wordList
    public String randomLine(ArrayList<String> list) {
        currentWord = list.get(new Random().nextInt(list.size()));

        //add to beginning of list
        completedList.add(0, new GameItem(currentWord));
        wordList.remove(currentWord);
        return currentWord;
    }

    //get the right list depending on age
    private void readData(final Spelling.FirebaseCallback f){
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                if (e != null) {
                    Toast.makeText(Spelling.this, "Error while loading!", Toast.LENGTH_SHORT).show();
                    Log.d("read data", e.toString());
                    return;
                }

                if (documentSnapshot.exists()) {
                    age = Integer.parseInt(documentSnapshot.getString("age"));
                    f.onCallback(age);
                }
            }
        });
    }

    //allows access of variable age outside of the snapshotlistener
    private interface FirebaseCallback{
        void onCallback(int age);
    }

    private void reset(){
        correctView.setVisibility(View.GONE);
        userWord.setVisibility(View.GONE);
        userWord.setText("");
        checkWordBtn.setVisibility(View.GONE);
        checkAgainBtn.setVisibility(View.GONE);
        nextWordBtn.setVisibility(View.GONE);
        viewWord.setBackground(getResources().getDrawable(R.drawable.rounded_nav));
        viewWord.setText("Loading word...");
    }
}
