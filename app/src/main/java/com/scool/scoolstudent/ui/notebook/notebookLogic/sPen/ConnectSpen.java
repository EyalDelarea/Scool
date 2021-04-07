package com.scool.scoolstudent.ui.notebook.notebookLogic.sPen;

import android.content.Context;
import android.util.Log;

//import com.samsung.android.sdk.penremote.SpenRemote;
//import com.samsung.android.sdk.penremote.SpenUnitManager;
//
//public class ConnectSpen {
//
//    static SpenUnitManager mSpenUnitManager = null;
//
//    public static  void connect(Context context) {
//
//        SpenRemote spenRemote = SpenRemote.getInstance();
//
//        if (!spenRemote.isConnected()) {
//            spenRemote.connect(context,
//                    new SpenRemote.ConnectionResultCallback() {
//                        @Override
//                        public void onSuccess(SpenUnitManager manager) {
//                            mSpenUnitManager = manager;
//                            Log.i("SPEN", "success connecttiong");
//                        }
//
//                        @Override
//                        public void onFailure(int error) {
//                            Log.i("SPEN", "FAILED");
//                        }
//                    });
//        }
//    }
//}
//
