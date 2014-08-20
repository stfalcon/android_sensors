package com.example.sasha.connection;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.example.sasha.ConnectActivity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author alwx
 * @version 1.0
 */
abstract public class MessageHandler extends Handler {
  private static final String DEBUG_TAG = MessageHandler.class.getName();
  private ConnectActivity mActivity;

  public MessageHandler(ConnectActivity activity) {
    mActivity = activity;
  }

  @Override
  public void handleMessage(Message msg) {
    final String message = msg.getData().getString(Communication.MESSAGE);
    try {
      final JSONObject jsonObject = new JSONObject(message);
      final String type = jsonObject.getString(Communication.MESSAGE_TYPE);

      mActivity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          onMessage(type, jsonObject);
        }
      });
    } catch (JSONException e) {
      Log.e(DEBUG_TAG, "Invalid message format: " + e);
    }
  }

  abstract public void onMessage(String type, JSONObject message);
}
