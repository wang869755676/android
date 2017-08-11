package com.aiseminar.EasyPR;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.apache.cordova.camera.CordovaUri;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;


/**
 * Created by my on 2017/7/7.
 */

public class Recognition extends CordovaPlugin {

    private CallbackContext currentCallbackContext;

    private PlateRecognizer mPlateRecognizer;
    private String plate;
    public static final int TAKE_PIC_SEC = 0;
    protected final static String[] permissions = { Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE };
    private static final int CAMERA = 100;

    private int  takeType=5;
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        // Log.e("initialize","============================");
        mPlateRecognizer = new PlateRecognizer(webView.getContext());
    }

    @Override
    public boolean execute(String action, CordovaArgs args,
                           CallbackContext callbackContext) throws JSONException {
        // save the current callback context
        currentCallbackContext = callbackContext;
        if (action.equals("pay")) {
            return pay(args);
        }
        return true;
    }


    private boolean pay(CordovaArgs args) {
        try {
            JSONObject orderInfoArgs = args.getJSONObject(0);
            //String subject = orderInfoArgs.getString("subject");
           // int type = orderInfoArgs.getInt("type");

           callTakePicture(takeType);
           /* if (subject != null && !subject.equals("")) {
                if (type == 6) {
                    plate = mPlateRecognizer.recognizes(subject);
                } else {
                    plate = mPlateRecognizer.recognize(subject);
                }

                if (plate != null && !plate.equals("")) {
                    currentCallbackContext.success(plate);
                } else {
                    currentCallbackContext.error("识别失败");
                }
            } else {
                currentCallbackContext.error("参数不正确:" + subject);
            }*/
        } catch (JSONException e1) {
            e1.printStackTrace();
            currentCallbackContext.error("参数不正确");
        }


        return true;
    }
    public void callTakePicture(int takeType) {
        boolean saveAlbumPermission = PermissionHelper.hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        boolean takePicturePermission = PermissionHelper.hasPermission(this, Manifest.permission.CAMERA);

        // CB-10120: The CAMERA permission does not need to be requested unless it is declared
        // in AndroidManifest.xml. This plugin does not declare it, but others may and so we must
        // check the package info to determine if the permission is present.

        if (!takePicturePermission) {
            takePicturePermission = true;
            try {
                PackageManager packageManager = this.cordova.getActivity().getPackageManager();
                String[] permissionsInPackage = packageManager.getPackageInfo(this.cordova.getActivity().getPackageName(), PackageManager.GET_PERMISSIONS).requestedPermissions;
                if (permissionsInPackage != null) {
                    for (String permission : permissionsInPackage) {
                        if (permission.equals(Manifest.permission.CAMERA)) {
                            takePicturePermission = false;
                            break;
                        }
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                // We are requesting the info for our package, so this should
                // never be caught
            }
        }

        if (takePicturePermission && saveAlbumPermission) {
            takePicture(takeType);
        } else if (saveAlbumPermission && !takePicturePermission) {
            PermissionHelper.requestPermission(this, TAKE_PIC_SEC, Manifest.permission.CAMERA);
        } else if (!saveAlbumPermission && takePicturePermission) {
            PermissionHelper.requestPermission(this, TAKE_PIC_SEC, Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            PermissionHelper.requestPermissions(this, TAKE_PIC_SEC, permissions);
        }
    }

    public void takePicture(int takeType)
    {
        if (this.cordova != null) {
            // Let's check to make sure the camera is actually installed. (Legacy Nexus 7 code)
            Intent intent=new Intent(this.cordova.getActivity(),CameraActivity.class);
            intent.putExtra("type",takeType);
            PackageManager mPm = this.cordova.getActivity().getPackageManager();
            if(intent.resolveActivity(mPm) != null)
            {
                this.cordova.startActivityForResult((CordovaPlugin) this, intent,CAMERA);
            }

        }
    }
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
             if(requestCode==CAMERA && resultCode==Activity.RESULT_OK&& intent!=null){
                 if(intent.getIntExtra("type",2)==1){
                     this.currentCallbackContext.success(intent.getStringExtra("result"));
                 }else{
                     this.currentCallbackContext.error(intent.getStringExtra("result"));
                 }

             }

    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                this.currentCallbackContext.error("您决绝了相应的权限，无法操作");
                return;
            }
        }
        switch (requestCode) {
            case TAKE_PIC_SEC:
                takePicture(takeType);
                break;

        }
    }
}
