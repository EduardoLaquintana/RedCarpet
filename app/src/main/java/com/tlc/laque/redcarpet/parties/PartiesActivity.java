package com.tlc.laque.redcarpet.parties;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.MessagePattern;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.IntegerRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.tlc.laque.redcarpet.MainActivity;
import com.tlc.laque.redcarpet.R;
import com.tlc.laque.redcarpet.chat.ChatActivity;
import com.tlc.laque.redcarpet.database.DataBaseRead;
import com.tlc.laque.redcarpet.database.DataBaseWrite;
import com.tlc.laque.redcarpet.users.ListUsers;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;

public class PartiesActivity extends MainActivity {
    Switch switchAttending;
    TextView edLocation;
    TextView edName;
    TextView edInfo;
    TextView edStTime;
    TextView edFiTime;
    Button buttonChat;
    Button buttonAttending;
    LinearLayout layoutAdmin;
    LinearLayout layoutUser;
    LinearLayout layoutUserAttended;
    String idParty;
    Party p;
    DataBaseRead dbR;
    boolean  attending = false;
    private FirebaseStorage mFirebaseStorage;

    private DatabaseReference mDatabase;
    private DataBaseWrite dbW;
    private ImageView v;
    private String pathParty;
    private float ratingValue;
    private RatingBar ratingBarOrg;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout contentFrameLayout = (FrameLayout) findViewById(R.id.content_frame); //Remember this is the FrameLayout area within your activity_main.xml
       contentFrameLayout.removeAllViews();
        getLayoutInflater().inflate(R.layout.activity_parties, contentFrameLayout);
        dbW = new DataBaseWrite();
         dbR = new DataBaseRead();


        Bundle extras = getIntent().getExtras();
        idParty = extras.getString("idParty");
        pathParty = extras.getString("pathParty");

        RatingBar rating = (RatingBar)findViewById(R.id.rating);            // Rating bar ...
        ratingBarOrg = (RatingBar)findViewById(R.id.ratingBarOrganizer);  //Rating bar to show the rate of the organizer


        rating.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {

            @Override
            public void onRatingChanged(RatingBar arg0, float rateValue, boolean arg2) {
                // TODO Auto-generated method stub
                Log.d("Rating", "your selected value is :"+rateValue);
                ratingValue =  rateValue;
            }
        });
        setVariable();
        getInformatinDataBase();
        switchAttending.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if(!p.isPartyFinished()){
                    dbW.registerUser(dbR.getUserId(),idParty);}
                } else {
                    dbW.cancelRegisterUser(dbR.getUserId(),idParty, p.getName());
                }
            }
        });
    }

    //Initialize the Variable
    private void setVariable(){                                 //SetVariable
        edLocation = findViewById(R.id.textViewLocation);
        edInfo = findViewById(R.id.textViewInformation);
        edStTime = findViewById(R.id.textViewTimeStart);
        edFiTime = findViewById(R.id.textViewTimeFinish);
        switchAttending = findViewById(R.id.switchAttendingUs);
        buttonAttending = findViewById(R.id.buttonAttendindParty);
        buttonChat = findViewById(R.id.buttonChatParty);
        layoutAdmin = findViewById(R.id.layoutAdminParty);
        layoutUser = findViewById(R.id.layoutUserParty);
        layoutUserAttended = findViewById(R.id.layoutUserPartyAttended);


    }

    //Get information Party from the DataBase
    private void getInformatinDataBase(){
        mDatabase = FirebaseDatabase.getInstance().getReference(pathParty);
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue() == null){}                    //If user exist show the old informations
                else {
                    try {
                        getInfoParty(dataSnapshot);
                        getInfoOrganizer();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }

    public void getInfoOrganizer(){
        mDatabase = FirebaseDatabase.getInstance().getReference("users/"+p.getOrganizer());
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue() == null){}                    //If user exist show the old informations
                else {
                    float newR = Float.valueOf(dataSnapshot.child("rating").getValue().toString());
                    int i = Integer.parseInt(dataSnapshot.child("numberVote").getValue().toString());
                    ratingBarOrg.setRating(newR/i*5);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    public void  getInfoParty(DataSnapshot dataSnapshot) throws ParseException, MalformedURLException {
        p = new Party();
        p = dbR.getParty(dataSnapshot);
        if(dataSnapshot.child("userAttending").child(dbR.getUserId()).exists()){
            attending = true;
        }
         else {
            attending = false;
        }
        setTextView();

    }
    //Set the TextView after read the DataBase
    private void setTextView() throws MalformedURLException {

        if(pathParty.toLowerCase().contains("administration")){
            layoutUserAttended.setVisibility(View.GONE);
            layoutUser.setVisibility(View.GONE);
            switchAttending.setVisibility(View.GONE);

        }
        else {
            if (p.isPartyFinished()) {
                layoutAdmin.setVisibility(View.GONE);
                layoutUser.setVisibility(View.GONE);
            } else {
                layoutAdmin.setVisibility(View.GONE);
                layoutUserAttended.setVisibility(View.GONE);
            }
        }
        edLocation.setText(p.getLocation());
        edStTime.setText(p.getTimeStart());
        edFiTime.setText(p.getTimeFinish());
        edInfo.setText(p.getInfo());
        setTitle(p.getName());

        if(attending){
            switchAttending.setChecked(true);
        }
        else{
            switchAttending.setChecked(false);
        }
        if(p.isPartyStarted() == true || p.isPartyFinished() == true){
            switchAttending.setClickable(false);
        }
        setImage();
    }

    //Buttons Listener for all the button in the activity
    public void buttonPartyClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.buttonMaps:
                viewOnMap(p.getLocation());
                break;
            case R.id.buttonAttendindParty:
                intent = new Intent(PartiesActivity.this, ListUsers.class);
                intent.putExtra("idParty", idParty );
                startActivity(intent);
                break;
            case R.id.buttonChatParty:
                intent = new Intent(PartiesActivity.this, ChatActivity.class);
                intent.putExtra("nameChat", p.getName());
                intent.putExtra("path", "Parties/"+idParty+ "/chatParty");
                startActivity(intent);
                break;
            case R.id.buttonAcceptParty:
                acceptPArty();
                break;
            case R.id.buttonRefureParty:
                refuseParty();
                break;
            case R.id.buttonVoteParty:
                voteParty();
                break;


        }
    }

    //if request.auth != null
    private void setImage() throws MalformedURLException {
        v = findViewById(R.id.imageViewParty);
        Picasso.with(PartiesActivity.this).load(p.getUrl()).fit().centerCrop()
                .placeholder(R.drawable.progress_animation )
                .error(R.drawable.error_download)
                .into(v);


    }

    //Open Google Maps to show the adress of the Party
    private void viewOnMap(String adress) {
        //String r = adress.replaceAll("\\s+","");
        String map = "http://maps.google.co.in/maps?q=" + adress;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(map));
        startActivity(intent);
    }

    //For Administration Part Accept or Refuse the PARTY
    private void acceptPArty(){
        DataBaseWrite dw = new DataBaseWrite();
        dw.deleteAttendingPArty(p.getKey());
        dw.saveNewParty(p);
        Intent intent = new Intent(PartiesActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void refuseParty(){
        DataBaseWrite dw = new DataBaseWrite();
        dw.deleteAttendingPArty(p.getKey());
        Intent intent = new Intent(PartiesActivity.this, MainActivity.class);
        startActivity(intent);
        finish();

        //StorageReference photoRef = mFirebaseStorage.getReferenceFromUrl(p.getUrl());
    }

    private void voteParty(){
        mDatabase = FirebaseDatabase.getInstance().getReference("users/"+p.getOrganizer());
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue() == null){}                    //If user exist show the old informations
                else {
                    DataBaseWrite dw = new DataBaseWrite();
                    dw.voteParty(p, p.getOrganizer(),dataSnapshot.child("rating").getValue().toString(), ratingValue, dataSnapshot.child("numberVote").getValue().toString());
                    Toast.makeText(PartiesActivity.this, "Thank you! you are improving RedCarpet!", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

}
