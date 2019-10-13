package com.SMAStudios.Call_Recording_program;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.SMAStudios.Call_Recording_program.adapter.DatabaseAdapter;
import com.SMAStudios.Call_Recording_program.controller.MyFileManager;
import com.SMAStudios.Call_Recording_program.model.RecordModel;
import com.SMAStudios.Call_Recording_program.utils.MyConstants;
import com.SMAStudios.Call_Recording_program.utils.MyDateUtils;
import com.SMAStudios.Call_Recording_program.utils.PreferUtils;
import com.SMAStudios.Call_Recording_program.utils.Utilities;
import com.google.android.gms.ads.MobileAds;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by Anh Son on 6/16/2016.
 */
public class PlayerActivity extends AppCompatActivity implements MyConstants,
        View.OnClickListener,
        MediaPlayer.OnPreparedListener,MediaPlayer.OnCompletionListener {
    private Context mContext;
    private RecordModel mRecord;

    private ImageView mPhotoContact;
    private TextView mNameContact;
    private TextView mDate;
    private ImageView mStatusImage;
    private ImageView mPlayOrPause;
    private SeekBar mSeekbar;
    private EditText mNoteEdt;
    private TextView mNoteTxt;
    private TextView mEndtime;
    private TextView mStartTime;
    private ImageView mSaveNoteBtn;
    //controller bar
    private LinearLayout mCall;
    private LinearLayout mAddContact;
    private LinearLayout mMessage;

    private MediaPlayer mMediaPlayer;
    private double timeElapsed = 0;
    private Runnable mUpdateSeekBarTime;
    private Handler mDurationHandler;
    private ExecutorService mThreadPoolExecutor;
    private Future mLongRunningTaskFuture;

    //Fix issue ic_pause or play
    private boolean isPause = false;
    //Improve performance set contact name
    private boolean isNeedUpdateContactName;

    private int activity = 0;
    private int FROM_RECORD_TYPE = 10000;
    private int position;
    private int id_original = -1;
    private DatabaseAdapter mDatabase;

    private boolean isEditMode;
    //passcode screen
    private View mPasscodeScreen;
    private PasscodeScreen mPasscode;

    // For admob
    private AdView adView;
LinearLayout update_name;
    // For admob

    static final int PICK_CONTACT=1;
    int PERMISSION_REQUEST_CONTACT=200;
    AdView mAview;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_player);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.player_title));
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                CallRecorderApp.isNeedShowPasscode = false;
            }
        });
        Bundle data = getIntent().getExtras();
        if(data != null ){
            mRecord = data.getParcelable(KEY_SEND_RECORD_TO_PLAYER);
            activity = data.getInt(KEY_ACTIVITY);
            FROM_RECORD_TYPE = data.getInt(KEY_RECORD_TYPE_PLAY);
            position = data.getInt("play_position");
        }
        mDatabase = DatabaseAdapter.getInstance(mContext);
        initUI();
        intPasscodeUI();
        mThreadPoolExecutor = Executors.newSingleThreadExecutor();
        setDataForPlayer(mRecord,true);
        initAdsAdmob();


        update_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(PlayerActivity.this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {

                        if (ActivityCompat.shouldShowRequestPermissionRationale(PlayerActivity.this,
                                Manifest.permission.READ_CONTACTS)) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(PlayerActivity.this);
                            builder.setTitle("Contacts access needed");
                            builder.setPositiveButton(android.R.string.ok, null);
                            builder.setMessage("please confirm Contacts access");//TODO put real question
                            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @TargetApi(Build.VERSION_CODES.M)
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    requestPermissions(
                                            new String[]
                                                    {Manifest.permission.READ_CONTACTS}
                                            , PERMISSION_REQUEST_CONTACT);
                                }
                            });
                            builder.show();
                            // Show an expanation to the user *asynchronously* -- don't block
                            // this thread waiting for the user's response! After the user
                            // sees the explanation, try again to request the permission.

                        } else {

                            // No explanation needed, we can request the permission.

                            ActivityCompat.requestPermissions(PlayerActivity.this,
                                    new String[]{Manifest.permission.READ_CONTACTS},
                                    PERMISSION_REQUEST_CONTACT);

                            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                            // app-defined int constant. The callback method gets the
                            // result of the request.
                        }
                    }else{
                        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                        startActivityForResult(intent, PICK_CONTACT);
                    }
                }
                else{
                    Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                    startActivityForResult(intent, PICK_CONTACT);
                }

            }
        });

        AdRequest adRequest = new AdRequest.Builder().build();

        mAview.loadAd(adRequest);
        mAview.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                mAview.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
            }
        });



    }

    private void initUI() {
        mPhotoContact = (ImageView) findViewById(R.id.player_img_avatar);
        mNameContact = (TextView) findViewById(R.id.player_txt_namecontacts);
        mDate = (TextView) findViewById(R.id.player_txt_datetime);
        mStatusImage = (ImageView) findViewById(R.id.player_img_status);
        mNoteEdt = (EditText) findViewById(R.id.player_edt_note);
        mNoteTxt = (TextView) findViewById(R.id.player_txt_note);
        mSeekbar = (SeekBar) findViewById(R.id.player_seekbar);
        mEndtime = (TextView) findViewById(R.id.player_txt_endtime);
        mStartTime = (TextView) findViewById(R.id.player_txt_starttime);
        mPlayOrPause = (ImageView) findViewById(R.id.player_img_play);
        mSaveNoteBtn = (ImageView) findViewById(R.id.player_btn_save_note);

        update_name=findViewById(R.id.update_name);
        mCall = (LinearLayout) findViewById(R.id.player_controller_ll_call);
        mAddContact = (LinearLayout) findViewById(R.id.player_controller_ll_contact);
        mMessage = (LinearLayout) findViewById(R.id.player_controller_ll_message);

        mCall.setOnClickListener(this);
        mAddContact.setOnClickListener(this);
        mMessage.setOnClickListener(this);

        mSaveNoteBtn.setOnClickListener(this);
        mAview=findViewById(R.id.mAview);
        mNoteTxt.setMovementMethod(new ScrollingMovementMethod());
        // Hide edit note when play record from Search
        if(FROM_RECORD_TYPE == FROM_SEARCH) mSaveNoteBtn.setVisibility(View.GONE);

    }

    private void initAdsAdmob(){
        // Look up the AdView as a resource and load a request.
        adView = (AdView) findViewById(R.id.adView_main);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(MyConstants.TEST_DEVICE_ID)
                .build();
        adView.loadAd(adRequest);
    }
    private void resumeAds(){
        adView.resume();
    }
    private void pauseAds(){
        adView.pause();
    }


    private void setDataForPlayer(RecordModel record , boolean isNeedLoadHistoryContact) {
        if(record == null){
            if(DEBUG) Log.e(TAG, "setDataForPlayer : record == null");
            return;
        }
        String phone = record.getPhoneNumber();
        mNameContact.setText(MyFileManager.getContactName(mContext, phone));
        try {
            Bitmap bm = MyFileManager.loadContactPhoto(mContext, Integer
                    .parseInt(MyFileManager.getContactIdFromPhoneNumber(
                            mContext, phone)));
            if (bm != null)
                mPhotoContact.setImageBitmap(bm);
        } catch (NumberFormatException e) {
            // TODO: handle exception
            if (DEBUG)
                Log.e(TAG,
                        "Exception RecorderHistoryFragment: Can not convert this string to number : ");
        }
        mDate.setText(MyDateUtils.formatDateFromMilliseconds(record.getDate())
                + ", "
                + MyDateUtils.getTimeFromMilliseconds(mContext, record.getDate()));
        int duration = Utilities.getDurationAudioFile(mContext, record.getPath());
        if(duration > 0){
            mEndtime.setText(Utilities.formatDurationAudioFile(
                    duration, true));
        }else {
            Utilities.showToast(mContext, getString(R.string.file_is_not_exist));
        }

        int statusCall = record.getStatus();
        if (statusCall == INCOMING_CALL_STARTED) {
            mStatusImage.setImageResource(R.drawable.ic_incoming);
        } else if (statusCall == OUTGOING_CALL_STARTED) {
            mStatusImage.setImageResource(R.drawable.ic_outgoing);
        }

        mNoteEdt.setText(record.getNote());
        mNoteTxt.setText(record.getNote());

        // For mediaplayer
        // For play audio file
        mMediaPlayer = MediaPlayer.create(mContext,
                Uri.fromFile(new File(record.getPath())));
        if(mMediaPlayer == null) {
            return;
        }
//		mSeekbar.setMax((int) mMediaPlayer.getDuration());
        mDurationHandler = new Handler();
        mUpdateSeekBarTime = new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                if(mMediaPlayer != null)
                    timeElapsed = mMediaPlayer.getCurrentPosition();

                // set seekbar progress
                mSeekbar.setProgress((int) timeElapsed);
                // repeat yourself that again in 100 miliseconds
                mDurationHandler.postDelayed(this, 100);
                mStartTime
                        .setText(String.format(Locale.getDefault(),
                                "%02d:%02d",
                                TimeUnit.MILLISECONDS
                                        .toMinutes((long) timeElapsed),
                                TimeUnit.MILLISECONDS
                                        .toSeconds((long) timeElapsed)
                                        - TimeUnit.MINUTES
                                        .toSeconds(TimeUnit.MILLISECONDS
                                                .toMinutes((long) timeElapsed))));

            }

        };
        mLongRunningTaskFuture = mThreadPoolExecutor.submit(mUpdateSeekBarTime);
        try {
//			mMediaPlayer.prepare();
//			mMediaPlayer.start();
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mPlayOrPause.setImageResource(R.drawable.ic_pause);
            timeElapsed = mMediaPlayer.getCurrentPosition();
            mSeekbar.setProgress((int) timeElapsed);
            mDurationHandler.postDelayed(mUpdateSeekBarTime, 100);
        } catch (IllegalStateException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }catch (NullPointerException e) {
            // TODO: handle exception
            e.printStackTrace();
        }

        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer arg0) {
                // TODO Auto-generated method stub
                mPlayOrPause.setImageResource(R.drawable.ic_play);
            }
        });

        mPlayOrPause.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                    mPlayOrPause.setImageResource(R.drawable.ic_play);
                } else {
                    try {
                        mMediaPlayer.start();
                        mPlayOrPause.setImageResource(R.drawable.ic_pause);
                    } catch (IllegalStateException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }
            }
        });

        // For Contact History
/*        if(isNeedLoadHistoryContact){
            mLoadListContactHistory = new LoadListContactHistory();
            mLoadListContactHistory.execute(phone);
        }*/
        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
                mMediaPlayer.seekTo(getSeekToProgress());
                mDurationHandler.postDelayed(mUpdateSeekBarTime, 100);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
                mDurationHandler.removeCallbacks(mUpdateSeekBarTime);
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                // TODO Auto-generated method stub
                if (fromUser) {
                    mSeekbar.setProgress(progress);
                }
            }
        });
    }

    private int getSeekToProgress() {
        return (int) ((double) mSeekbar.getProgress()
                * (1 / (double) mSeekbar.getMax()) * (double) mMediaPlayer
                .getDuration());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isNeedUpdateContactName && mNameContact != null) {
            mNameContact.setText(MyFileManager.getContactName(mContext, mRecord
                    .getPhoneNumber().trim()));
            isNeedUpdateContactName = false;
        }
        if (isPause && mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mPlayOrPause.setImageResource(R.drawable.ic_pause);
            }else {
                mPlayOrPause.setImageResource(R.drawable.ic_play);
            }
            isPause = false;
        }
        if(CallRecorderApp.isNeedShowPasscode && PreferUtils.getbooleanPreferences(this, MyConstants.KEY_PRIVATE_MODE)
                && !PreferUtils.getbooleanPreferences(mContext, MyConstants.KEY_IS_LOGINED)){
            mPasscode.showPasscode();
        }
        resumeAds();
    }
    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        isPause = true;
        pauseAds();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mMediaPlayer != null && mMediaPlayer.isPlaying()){
            mMediaPlayer.pause();
        }
        CallRecorderApp.isNeedShowPasscode = true;
        PreferUtils.savebooleanPreferences(mContext,KEY_IS_LOGINED, false);
    }

    @Override
    protected void onDestroy() {
        if (mMediaPlayer != null) {
            mLongRunningTaskFuture.cancel(true);
            mDurationHandler.removeCallbacks(mUpdateSeekBarTime);
            mMediaPlayer.release();
            mMediaPlayer = null;

        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        CallRecorderApp.isNeedShowPasscode = false;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.player_controller_ll_call:
                Intent intentCall = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"
                        + mRecord.getPhoneNumber().trim()));
                startActivity(intentCall);
                break;
            case R.id.player_controller_ll_message:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms",
                        mRecord.getPhoneNumber().trim(), null)));
                break;
            case R.id.player_controller_ll_contact:
                if(Utilities.isSaveContact(mContext, mRecord.getPhoneNumber().trim())){
                    Intent contactIntent = new Intent(Intent.ACTION_VIEW);
                    Uri uri = Uri.withAppendedPath(
                            ContactsContract.Contacts.CONTENT_URI,
                            String.valueOf(MyFileManager
                                    .getContactIdFromPhoneNumber(mContext,
                                            mRecord.getPhoneNumber().trim())));
                    contactIntent.setData(uri);
                    startActivity(contactIntent);
                }else {
                    Intent intentAddContact = new Intent(Intent.ACTION_INSERT);
                    intentAddContact.setType(ContactsContract.Contacts.CONTENT_TYPE);
                    intentAddContact.putExtra(
                            ContactsContract.Intents.Insert.PHONE, mRecord
                                    .getPhoneNumber().trim());
                    startActivity(intentAddContact);
                }
                isNeedUpdateContactName = true;
                break;
            case R.id.player_btn_save_note:
                if(!isEditMode){
                    mNoteEdt.setVisibility(View.VISIBLE);
                    mNoteTxt.setVisibility(View.GONE);
                    mSaveNoteBtn.setBackgroundResource(R.drawable.ic_save_enable);
                    mNoteEdt.requestFocus();
                    Utilities.showOrHideKeyboard(this,mNoteEdt,true);
                    isEditMode = true;
                }else{
                    String note = mNoteEdt.getText().toString();
                    int modeNote = Integer.parseInt(PreferUtils.getStringPreferences(mContext, KEY_ACTION_WHEN_NOTE));
                    long index = -1;
                    if(FROM_RECORD_TYPE == FROM_RECORDING){
                        index = mDatabase.updateNote(DatabaseAdapter.TABLE_INBOX, mRecord.getId(), note);
                        mDatabase.updateSearchNote(mRecord.getId(), note);
                        switch (modeNote) {
                            case AUTOMATIC_SAVE_RECORDING:
                                RecordModel newrecord = mRecord;
                                newrecord.setNote(note);
                                long rowId = mDatabase.addRecord(newrecord,
                                        DatabaseAdapter.TABLE_SAVE_RECORD);
                                if(rowId > 0){
                                    mDatabase.deleteRecord(mRecord, DatabaseAdapter.TABLE_INBOX);
//                                  mDatabase.deleteSearchItemIndex(mRecord);
                                    Utilities.showOrHideKeyboard(mContext, mNoteEdt, false);
                                    sendBroadcastToUpdateList(UPDATE_SAVED_BROADCAST);

                                }
                                break;
                            case ASK_WHAT_TO_DO:
                                Utilities.showOrHideKeyboard(mContext, mNoteEdt, false);
                                showAskWhatTodoAfterNoteDialog(note);
                                break;
                            default:
                                Utilities.showOrHideKeyboard(mContext, mNoteEdt, false);
                                sendBroadcastToUpdateList(UPDATE_NOTE_BROADCAST);
                                break;
                        }
                    }else if (FROM_RECORD_TYPE == FROM_FAVORITE) {
                        index = mDatabase.updateNote(DatabaseAdapter.TABLE_SAVE_RECORD, mRecord.getId(), note);
                        long updateNoteInt = mDatabase.updateSearchNote(mRecord.getId(), note);
                        Log.d(TAG, "updateNoteInt = "+updateNoteInt);
                        Utilities.showOrHideKeyboard(mContext, mNoteEdt, false);
                        sendBroadcastToUpdateList(UPDATE_NOTE_BROADCAST);
                    }
                    if(index > 0 ){
                        Utilities.showToast(mContext, getString(R.string.player_save_success_notice));
                    } else {
                        Utilities.showToast(mContext, getString(R.string.player_save_unsuccess_notice));
                    }
                    mNoteEdt.setVisibility(View.GONE);
                    mNoteTxt.setVisibility(View.VISIBLE);
                    mNoteTxt.setText(mNoteEdt.getText().toString());
                    mSaveNoteBtn.setBackgroundResource(R.drawable.ic_edit);
                    isEditMode = false;
                }

                break;
            default:
                break;
        }
    }


    @Override
    public void onCompletion(MediaPlayer mp) {
        mDurationHandler.removeCallbacks(mUpdateSeekBarTime);
        mp.release();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mSeekbar.setMax(mp.getDuration());
        mp.start();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_history:
                Intent historyIntent = new Intent(this, ContactHistoryActivity.class);
                historyIntent.putExtra(KEY_RECORD_TYPE_PLAY,FROM_RECORD_TYPE);
                historyIntent.putExtra("phonenumber_key",mRecord.getPhoneNumber());
                startActivity(historyIntent);
                CallRecorderApp.isNeedShowPasscode = false;
                return true;
            case R.id.menu_share:
                Uri uri = Uri.fromFile(new File(mRecord.getPath()));
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("audio/*");
                share.putExtra(Intent.EXTRA_STREAM, uri);
                startActivity(Intent.createChooser(share,getString(R.string.share_this_record_dialog_title)));
                CallRecorderApp.isNeedShowPasscode = false;
                return true;
            case R.id.menu_detail:
                showDialogDetail();
                return true;
            default:
                return false;
        }
    }
    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        switch (reqCode) {
            case (PICK_CONTACT) :
                if (resultCode == Activity.RESULT_OK) {

                    Uri contactData = data.getData();
                    Cursor c =  managedQuery(contactData, null, null, null, null);
                    if (c.moveToFirst()) {


                        String id =c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));

                        String hasPhone =c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

                        if (hasPhone.equalsIgnoreCase("1")) {
                            Cursor phones = getContentResolver().query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ id,
                                    null, null);
                            phones.moveToFirst();
                            String cNumber = phones.getString(phones.getColumnIndex("data1"));
                            Log.v("wwwww",cNumber);
                            if(mDatabase.updateName(mRecord.getId(),cNumber)==1);
                            {String name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                                mNameContact.setText(name);

                                Intent updateListSaved = new Intent();
                                updateListSaved.setAction(MyConstants.ACTION_BROADCAST_INBOX_INTENT_UPDATE_LIST_RECORD_NAME);
                                updateListSaved.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                                updateListSaved.putExtra("play_position",position);
                                updateListSaved.putExtra("player_update_name",cNumber);
                                sendBroadcast(updateListSaved);
                            }

                        }
                    }
                }
                break;
        }
    }

    /**
     * Dialog ask what to do after note call was added
     */
    private void showAskWhatTodoAfterNoteDialog(final String noteChange){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.save_recording_note_title);
        builder.setMessage(R.string.ask_save_this_recording_dialog);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.string_yes, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                RecordModel newrecord = mRecord;
                newrecord.setNote(noteChange);
                long rowId = mDatabase.addRecord(newrecord,
                        DatabaseAdapter.TABLE_SAVE_RECORD);
                if(rowId > 0){
                    mDatabase.deleteRecord(mRecord, DatabaseAdapter.TABLE_INBOX);
//                  mDatabase.deleteSearchItemIndex(mRecord);
                    sendBroadcastToUpdateList(UPDATE_SAVED_BROADCAST);
                }
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.string_no, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                dialog.dismiss();
                sendBroadcastToUpdateList(UPDATE_NOTE_BROADCAST);
            }
        });

        builder.show();
    }
    /**
     * Send broadcast to refresh listview
     */
    private void sendBroadcastToUpdateList(int type){
        Log.d(TAG,"FROM_RECORD_TYPE = "+FROM_RECORD_TYPE);
        switch (type){
            case UPDATE_NOTE_BROADCAST:
                Intent intent = new Intent();
                if(FROM_RECORD_TYPE == FROM_RECORDING){
                    intent.setAction(MyConstants.ACTION_BROADCAST_INBOX_INTENT_UPDATE_NOTE);
                } else {
                    intent.setAction(MyConstants.ACTION_BROADCAST_SAVED_INTENT_UPDATE_NOTE);
                }
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                intent.putExtra("player_update_note",mNoteEdt.getText().toString().trim());
                intent.putExtra("play_position",position);
                sendBroadcast(intent);
                Log.d(TAG,"ACTION = "+intent.getAction());
                break;
            case UPDATE_SAVED_BROADCAST:
                if(FROM_RECORD_TYPE == FROM_RECORDING){
                    Intent updateListInbox = new Intent();
                    updateListInbox.setAction(MyConstants.ACTION_BROADCAST_INBOX_INTENT_UPDATE_LIST_RECORD);
                    updateListInbox.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                    updateListInbox.putExtra("play_position",position);
                    sendBroadcast(updateListInbox);
                    Intent updateListSaved = new Intent();
                    updateListSaved.setAction(MyConstants.ACTION_BROADCAST_SAVED_INTENT_UPDATE_LIST_RECORD);
                    updateListSaved.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                    updateListSaved.putExtra("play_position",position);
                    sendBroadcast(updateListSaved);
                }

                break;
            default:
                break;
        }


    }
    private void showDialogDetail(){
        View view = View.inflate(this, R.layout.dialog_detail_record, null);

        TextView phonenumberDetail = (TextView) view.findViewById(R.id.dialog_detail_txt_phonenumber);
        TextView namecontact =(TextView) view.findViewById(R.id.dialog_detail_txt_namecontact);
        TextView time = (TextView) view.findViewById(R.id.dialog_detail_txt_time);
        ImageView avatar = (ImageView) view.findViewById(R.id.dialog_detail_img_avatar);
        ImageView status = (ImageView) view.findViewById(R.id.dialog_detail_img_status);
        TextView filetype = (TextView) view.findViewById(R.id.dialog_detail_txt_filetype);
        TextView size = (TextView) view.findViewById(R.id.dialog_detail_txt_size);
        TextView duration = (TextView) view.findViewById(R.id.dialog_detail_txt_duration);
        TextView path = (TextView) view.findViewById(R.id.dialog_detail_txt_path);

        namecontact.setText(MyFileManager.getContactName(mContext, mRecord.getPhoneNumber()));
        time.setText(MyDateUtils.formatDateFromMilliseconds(mRecord.getDate())
                + ", "
                + MyDateUtils.getTimeFromMilliseconds(mContext, mRecord.getDate()));
        String phonenumber = mRecord.getPhoneNumber();
        try {
            Bitmap bm = MyFileManager.loadContactPhoto(mContext, Integer.parseInt(
                    MyFileManager.getContactIdFromPhoneNumber(mContext, phonenumber)));
            if(bm != null ) avatar.setImageBitmap(bm);
        } catch (NumberFormatException e) {
            // TODO: handle exception
            if(DEBUG) Log.e(TAG, "Exception RecorderHistoryFragment: Can not convert this string to number : ");
        }
        try {

            int statusCall = mRecord.getStatus();
            if(statusCall == INCOMING_CALL_STARTED){
                status.setImageResource(R.drawable.ic_incoming);
            }else if (statusCall == OUTGOING_CALL_STARTED) {
                status.setImageResource(R.drawable.ic_outgoing);
            }
        } catch (NumberFormatException e) {
            // TODO: handle exception
            if(DEBUG) Log.e(TAG, "Exception RecorderHistoryFragment: Can not convert this string to number : ");
        }
        String pathFile = mRecord.getPath();
        path.setText(pathFile);
        size.setText(Utilities.humanReadableByteCount(new File(mRecord.getPath()).length(), true));
        filetype.setText(pathFile.substring(pathFile.length()-3));
        phonenumberDetail.setText(phonenumber);
        duration.setText(Utilities.formatDurationAudioFile(mRecord.getDuration(), false));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view)
                .setPositiveButton(R.string.string_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                }).show();
    }

    private void intPasscodeUI(){
        mPasscodeScreen = findViewById(R.id.player_passcode);
        Button number_0 = (Button) findViewById(R.id.numpad_0);
        Button number_1 = (Button) findViewById(R.id.numpad_1);
        Button number_2 = (Button) findViewById(R.id.numpad_2);
        Button number_3 = (Button) findViewById(R.id.numpad_3);
        Button number_4 = (Button) findViewById(R.id.numpad_4);
        Button number_5 = (Button) findViewById(R.id.numpad_5);
        Button number_6 = (Button) findViewById(R.id.numpad_6);
        Button number_7 = (Button) findViewById(R.id.numpad_7);
        Button number_8 = (Button) findViewById(R.id.numpad_8);
        Button number_9 = (Button) findViewById(R.id.numpad_9);
        ImageButton number_erase = (ImageButton) findViewById(R.id.button_erase);

        EditText pinfield_1 = (EditText) findViewById(R.id.pin_field_1);
        EditText pinfield_2 = (EditText) findViewById(R.id.pin_field_2);
        EditText pinfield_3 = (EditText) findViewById(R.id.pin_field_3);
        EditText pinfield_4 = (EditText) findViewById(R.id.pin_field_4);
        TextView resetPassword = (TextView) findViewById(R.id.passcode_txt_reset_password);
        mPasscode = new PasscodeScreen();
        mPasscode.initLayout(this,mPasscodeScreen,number_0, number_1, number_2, number_3,
                number_4, number_5, number_6, number_7, number_8, number_9,
                number_erase,pinfield_1,pinfield_2,pinfield_3,pinfield_4,resetPassword);
    }
}
