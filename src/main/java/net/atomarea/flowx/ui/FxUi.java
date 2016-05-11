package net.atomarea.flowx.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.makeramen.roundedimageview.RoundedImageView;

import net.atomarea.flowx.R;
import net.atomarea.flowx.entities.Conversation;
import net.atomarea.flowx.entities.Message;
import net.atomarea.flowx.entities.Transferable;
import net.atomarea.flowx.services.XmppConnectionService;
import net.atomarea.flowx.utils.UIHelper;
import net.atomarea.flowx.xmpp.chatstate.ChatState;

import java.util.ArrayList;

import github.ankushsachdeva.emojicon.EmojiconTextView;

/**
 * Created by Tom on 10.05.2016.
 */
public class FxUi extends FxXmppActivity implements XmppConnectionService.OnConversationUpdate {

    private static final String TAG = "FlowX (UI Main)";

    /*
     Prefixes:

     m -> Main
     d -> Data, used in some states
     */

    public static FxUi App;

    private Toolbar mToolbar;
    private Handler mHandler;
    private ScrollView mScroll;
    private LinearLayout mLayout;
    private LinearLayout mParent;

    private ImageView mFxLogo;
    private RelativeLayout mFxLogoParent;

    private boolean backendConnected;

    private State mFxState;

    private Conversation dConversation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fx_base_layout); // load layout from xml (base layout)

        Log.i(TAG, "=== [ FlowX Main UI ] ===");

        App = this; // static context <3

        mFxState = State.STARTUP; // startup...

        backendConnected = false; // backend isn't connected at startup

        mToolbar = (Toolbar) findViewById(R.id.fx_toolbar); // find toolbar and
        mToolbar.setTitleTextColor(Color.WHITE); // set toolbar options and
        setSupportActionBar(mToolbar); // reset toolbar & attach to activity

        mHandler = new Handler(); // initialize handler for delaying requests

        mScroll = (ScrollView) findViewById(R.id.fx_main_scroll); // find other necessary view's
        mLayout = (LinearLayout) findViewById(R.id.fx_main_layout);

        mParent = (LinearLayout) findViewById(R.id.fx_parent);

        mFxLogo = (ImageView) findViewById(R.id.fx_logo);
        mFxLogoParent = (RelativeLayout) findViewById(R.id.fx_logo_parent);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false); // prevent "back" arrow from showing
            getSupportActionBar().setTitle(R.string.app_name); // real title: FlowX
        }

        mParent.setAlpha(0f); // set alpha value on main layout

        mFxLogo.setScaleX(0); // scale logo to 0, not visible
        mFxLogo.setScaleY(0);

        mFxLogo.animate().scaleX(1).scaleY(1).setStartDelay(100).setDuration(200).setInterpolator(new DecelerateInterpolator()).start(); // start logo animation -> 1x1 in scale, visible

        // [[ wait for backend... @ onBackendConnected() ]]
    }

    @Override
    protected void refreshUiReal() {
        Log.i(TAG, "backend requested ui refresh");
        if (!backendConnected)
            return; // if the backend isn't connected yet, this function can't run
        // refreshFxUi(); // should happen later only if needed, not needed yet
        // [[ TODO: !! DETECT CHANGES AND APPLY ONLY IF NEEDED ]]
        refreshFxUi(mFxState, false);
    }

    @Override
    void onBackendConnected() {
        Log.i(TAG, "backend connected");
        backendConnected = true; // backend is now connected

        if (mFxState == State.STARTUP) // after startup
            refreshFxUi(State.RECENT_CONVERSATIONS, false); // first screen: recent message, populate before animation

        mHandler.postDelayed(new Runnable() { // delay request for animation
            @Override
            public void run() {
                mFxLogo.animate().scaleX(0).scaleY(0).setDuration(200).setInterpolator(new AccelerateInterpolator()).start();
                mParent.animate().alpha(1f).setDuration(200).start(); // properly animate the logo and the main layout to show nicely
            }
        }, 400);
        mHandler.postDelayed(new Runnable() { // delay until above animations have finished
            @Override
            public void run() {
                mFxLogoParent.setVisibility(View.GONE); // remove the "logo" layout from the shown view tree to allow inputs
            }
        }, 600);
    }

    public void refreshFxUi(State toState, boolean animate) {
        Log.i(TAG, "refresh fxui state");

        //State fromState = mFxState;

        boolean change = toState != mFxState; // changed?

        if (change && animate) {
            // [[ TODO: ANIMATION CODE HERE ]]
        }

        // [[ TODO: "SOFTER" WAY TO REFRESH ONLY ]]

        if (change) mFxState = toState;

        mLayout.removeAllViews(); // bye views, won't need you anymore

        if (State.RECENT_CONVERSATIONS == mFxState) { // cause we're showing a loading screen (or something like this), we can load everything into the ram... or at least generate everything and let android manage it properly
            ArrayList<Conversation> tConversationList = new ArrayList<>();
            xmppConnectionService.populateWithOrderedConversations(tConversationList); // load all recent conversations

            for (Conversation tmpConversation : tConversationList) { // yay, let's fill up the ram =D
                final Conversation tConversation = tmpConversation; // #finalize

                View tRow = getLayoutInflater().inflate(R.layout.fx_row_recent_conversations, mLayout, false); // create the layout

                EmojiconTextView tTvName = (EmojiconTextView) tRow.findViewById(R.id.fx_row_recent_conversations_name); // find places to fill
                EmojiconTextView tTvLastMessage = (EmojiconTextView) tRow.findViewById(R.id.fx_row_recent_conversations_last_message);
                TextView tTvTimestamp = (TextView) tRow.findViewById(R.id.fx_row_recent_conversations_timestamp);
                RoundedImageView tIvPicture = (RoundedImageView) tRow.findViewById(R.id.fx_row_recent_conversations_picture);

                if (Conversation.MODE_SINGLE == tConversation.getMode() || useSubjectToIdentifyConference())
                    tTvName.setText(tConversation.getName()); // set conversation title or
                else
                    tTvName.setText(tConversation.getJid().toBareJid().toString()); // name of user

                if (ChatState.COMPOSING.equals(tConversation.getIncomingChatState()))
                    tTvLastMessage.setText(R.string.contact_is_typing); // contact is typing or
                else
                    tTvLastMessage.setText(tConversation.getLatestMessage().getBody()); // last message

                FxUiHelper.loadAvatar(tConversation, tIvPicture, 66); // load the avatar from backend, 66dp width

                tTvTimestamp.setText(UIHelper.readableTimeDifference(this, tConversation.getLatestMessage().getTimeSent())); // create and set timestamp

                tRow.findViewById(R.id.fx_row_recent_conversations_container).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // [[ TODO: OPEN CHAT PANE WITH ANIMATION ]]
                        dConversation = tConversation; // set "current" conversation
                        refreshFxUi(State.SINGLE_CONVERSATION, true);
                    }
                });

                mLayout.addView(tRow); // add the row to the view tree
            }
        } else if (State.SINGLE_CONVERSATION == mFxState) { // show a conversation, yay this will become complicated :/
            ArrayList<Message> tMessages = new ArrayList<>();
            dConversation.populateWithMessages(tMessages);

            for (int i = 0; i < tMessages.size(); i++) {
                if (tMessages.size() - 30 > i)
                    continue; // show the last 30 messages... more coming soon

                final Message tMessage = tMessages.get(i); // #finalie

                boolean _Error = false;

                String _FileSize = null;
                String _Info = null;

                if (tMessage.getType() == Message.TYPE_IMAGE || tMessage.getType() == Message.TYPE_FILE || tMessage.getTransferable() != null) { // we have a image or a file, so do something with this
                    if (tMessage.getFileParams().size > (1024 * 1024))
                        _FileSize = tMessage.getFileParams().size / (1024 * 1024) + " MB";
                    else if (tMessage.getFileParams().size > 0)
                        _FileSize = tMessage.getFileParams().size / 1024 + " KB";
                    if (tMessage.getTransferable() != null && tMessage.getTransferable().getStatus() == Transferable.STATUS_FAILED)
                        _Error = true; // something wen't wrong in the backend, let's tell the user
                }

                if (tMessage.getMergedStatus() == Message.STATUS_WAITING)
                    _Info = getResources().getString(R.string.waiting);
                if (tMessage.getMergedStatus() == Message.STATUS_UNSEND) {
                    if (tMessage.getTransferable() != null)
                        _Info = getResources().getString(R.string.sending_file, tMessage.getTransferable().getProgress());
                    else _Info = getResources().getString(R.string.sending);
                }
                if (tMessage.getMergedStatus() == Message.STATUS_OFFERED)
                    _Info = getResources().getString(R.string.offering);
                if (tMessage.getMergedStatus() == Message.STATUS_SEND_RECEIVED) {
                    _Info = "RECEIVED"; // [[ TODO: INDICATOR ]]
                }
                if (tMessage.getMergedStatus() == Message.STATUS_SEND_DISPLAYED) {
                    _Info = "DISPLAYED"; // [[ TODO: INDICATOR ]]
                }
                if (tMessage.getMergedStatus() == Message.STATUS_SEND_FAILED) {
                    _Info = getResources().getString(R.string.send_failed);
                    _Error = true;
                }
                if (_Info == null) _Info = UIHelper.getMessageDisplayName(tMessage);

                View tRow = null;

                switch (tMessage.getType()) {
                    case Message.TYPE_TEXT:
                        if (FxUiHelper.isMessageReceived(tMessage))
                            tRow = getLayoutInflater().inflate(R.layout.fx_msg_recv_text, mLayout, false);
                        else
                            tRow = getLayoutInflater().inflate(R.layout.fx_msg_sent_text, mLayout, false);
                        ((EmojiconTextView) tRow.findViewById(R.id.message_text)).setText(tMessage.getBody());
                        break;
                    case Message.TYPE_IMAGE:
                        tRow = getLayoutInflater().inflate(R.layout.fx_msg_sent_image, mLayout, false);
                        loadBitmap(tMessage, (ImageView) tRow.findViewById(R.id.message_image));
                        break;
                    case Message.TYPE_FILE:
                        if (tMessage.getFileParams().width > 0) { // is it an image?
                            tRow = getLayoutInflater().inflate(R.layout.fx_msg_recv_image, mLayout, false);
                            loadBitmap(tMessage, (ImageView) tRow.findViewById(R.id.message_image));
                        }
                        break;
                }

                if (tRow == null) continue; //hm nothing was inflated, why we should go on...

                String _Time = UIHelper.readableTimeDifference(this, tMessage.getMergedTimeSent());

                EmojiconTextView tMessageInfo = (EmojiconTextView) tRow.findViewById(R.id.message_information);

                if (tMessage.getMergedStatus() <= Message.STATUS_RECEIVED) {
                    if (_FileSize != null && _Info != null)
                        tMessageInfo.setText(_Time + " \u00B7 " + _FileSize + " \u00B7 " + _Info);
                    else if (_FileSize == null && _Info != null)
                        tMessageInfo.setText(_Time + " \u00B7 " + _Info);
                    else if (_FileSize != null)
                        tMessageInfo.setText(_Time + " \u00B7 " + _FileSize);
                    else tMessageInfo.setText(_Time);
                } else {
                    if (_FileSize != null && _Info != null)
                        tMessageInfo.setText(_FileSize + " \u00B7 " + _Info);
                    else if (_FileSize == null && _Info != null)
                        tMessageInfo.setText(_Info + " \u00B7 " + _Time);
                    else if (_FileSize != null)
                        tMessageInfo.setText(_FileSize + " \u00B7 " + _Time);
                    else tMessageInfo.setText(_Time);
                }

                mLayout.addView(tRow);
            }
        }

        if (change && animate) {
            // [[ TODO: ANIMATION CODE HERE ]]
        }
    }

    @Override
    public void onBackPressed() {
        if (mFxState == State.RECENT_CONVERSATIONS) super.onBackPressed();
        else {
            refreshFxUi(State.RECENT_CONVERSATIONS, true);
        }
    }

    @Override
    public void onConversationUpdate() {
        refreshUi();
    }

    public enum State {
        STARTUP, RECENT_CONVERSATIONS, SINGLE_CONVERSATION, CONTACTS, GROUPS
    }
}
