package com.wind.jpushkit;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.lang.reflect.Method;

import cn.jpush.android.api.JPushInterface;

/**
 * Created By wind
 * on 2019-11-07
 */
public class JPush {
    public static final String TAG="JPush";
    public static final int CODE_SET_ALIAS=1000;
    public static final int CODE_DEL_ALIAS=1001;
    public static final String PREF_KEY_ALIAS="pref_key_alias";
    public static final String PREF_FILE_NAME="jpush";


    /**消息Id**/
    private static final String KEY_MSGID = "msg_id";
    /**该通知的下发通道**/
    private static final String KEY_WHICH_PUSH_SDK = "rom_type";
    /**通知标题**/
    private static final String KEY_TITLE = "n_title";
    /**通知内容**/
    private static final String KEY_CONTENT = "n_content";
    /**通知附加字段**/
    private static final String KEY_EXTRAS = "n_extras";

    /**
     * 是否来自厂商推送通道
     * @param intent
     * @return
     */
    public static boolean fromManufacturerChannelPush(Intent intent){
        String data=resolveIntent(intent);
        boolean channelPush=false;
        if (!TextUtils.isEmpty(data)){
            channelPush=true;
        }
        return channelPush;

    }

    private static String resolveIntent(Intent intent){

        if (intent==null){
            return null;
        }
        //获取华为平台附带的jpush信息
        String data=null;
        if (intent.getData() != null) {
            data = intent.getData().toString();
        }

        //获取fcm、oppo、vivo、华硕、小米平台附带的jpush信息
        if(TextUtils.isEmpty(data) && intent.getExtras() != null){
            data = intent.getExtras().getString("JMessageExtra");
        }
        Log.d(TAG,"JPUSH msg content is " +data);
        return data;

    }


    /**
     * 处理点击事件，当前启动配置的Activity都是使用
     * Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
     * 方式启动，只需要在onCreat中调用此方法进行处理
     *
     * {"route":{"path":"mu://app/user","data":{"uid":"24458854"}}}
     *
     */
    public static void handleOpenClick(Intent intent, Context context) {


        try {
            if (fromManufacturerChannelPush(intent)) {
                JSONObject jsonObject = new JSONObject(resolveIntent(intent));
                String msgId = jsonObject.optString(KEY_MSGID);
                byte whichPushSDK = (byte) jsonObject.optInt(KEY_WHICH_PUSH_SDK);
                JPushInterface.reportNotificationOpened(context, msgId, whichPushSDK);
               // Log.d(TAG," JPushInterface.reportNotificationOpened msgId："+msgId+" whichPushSDK："+whichPushSDK);

                String extras = jsonObject.optString(KEY_EXTRAS);

                //{"route":"{\"path\":\"mu://app/user\",\"data\":{\"uid\":\"24458854\"}}"}


                JSONObject extrasJSONObject = new JSONObject(extras);

                JSONObject routeJSONObject=null;
                if (extrasJSONObject.has("data")){
                    JSONObject dataJSONObject= getJSONObject(extrasJSONObject,"data");
                    if (dataJSONObject!=null){
                        routeJSONObject = getJSONObject(dataJSONObject,"route");
                    }
                }else {
                    routeJSONObject = getJSONObject(extrasJSONObject,"route");
                    if (routeJSONObject==null){
                        String routeStr=extrasJSONObject.getString("route");
                        if (!TextUtils.isEmpty(routeStr)){
                            routeJSONObject = new JSONObject(routeStr);
                        }
                    }
                }

                if (routeJSONObject!=null){
                    String path = routeJSONObject.getString("path");
                    String pathArgs = routeJSONObject.getString("data");
                    String msg="path:"+path +"  data:"+pathArgs;
                    Log.d(TAG,msg);

                    Class routerClass=Class.forName("com.wind.base.Router");
                    Class routeBeanClass=Class.forName("com.wind.base.bean.RouteBean");

                    Method setPathMethod=routeBeanClass.getMethod("setPath",String.class);
                    Method setDataMethod=routeBeanClass.getMethod("setData",String.class);

                    Method routeMethod=routerClass.getMethod("route",Context.class,routeBeanClass);

                    Object routeBean=routeBeanClass.newInstance();

                    setPathMethod.invoke(routeBean,path);
                    setDataMethod.invoke(routeBean,pathArgs);

                    routeMethod.invoke(null,context,routeBean);
                }



            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private static JSONObject getJSONObject(JSONObject jsonObject,String key){
        JSONObject keyJSONObject=null;
        try {
            keyJSONObject=jsonObject.getJSONObject(key);

        }catch (Exception e){
            e.printStackTrace();
        }

        return keyJSONObject;

    }
}
