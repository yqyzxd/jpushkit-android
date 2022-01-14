package com.wind.jpushkit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.alibaba.android.arouter.launcher.ARouter;
import com.alibaba.fastjson.JSON;
import com.wind.router.RouteAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Iterator;

import cn.jpush.android.api.JPushMessage;
import cn.jpush.android.api.NotificationMessage;
import cn.jpush.android.service.JPushMessageReceiver;

/**
 * Created By wind
 * on 2019-11-07
 */
public class JPushNotificationReceive extends JPushMessageReceiver {


    /**
     * 华为的厂商通道，经过实测，不支持极光的回调方法，所以只能在用户点击通知以后才能拿到，JPushMessageReceiver子类下的处理方法不会被触发
     * <p>
     * 小米的厂商消息送达时，JPushMessageReceiver子类下的处理方法会被触发，但是不推荐使用，其他部分厂商有不触发的情况，所以要综合考虑
     *
     * @param context
     * @param notificationMessage
     */
    @Override
    public void onNotifyMessageOpened(Context context, NotificationMessage notificationMessage) {
        // super.onNotifyMessageOpened(context, notificationMessage);
        boolean enabled=false;
        if (enabled){
            return;
        }
        String extras = notificationMessage.notificationExtras;
        System.out.println("JPushNotificationReceive jpush extras:" + extras);
        if ("{}".equals(extras)) {
            super.onNotifyMessageOpened(context, notificationMessage);
        } else {
            //检测是否已经登录
            SharedPreferences sp = context.getSharedPreferences("config", Context.MODE_PRIVATE);

            String token = sp.getString("pref_key_user_token", "");
            if (TextUtils.isEmpty(token)) {
                ARouter.getInstance().build("/login/start_activity")
                        .withFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .navigation();
            } else {
                try {
                    JSONObject extrasJSONObject = new JSONObject(extras);
                    JSONObject routeJSONObject = extrasJSONObject.getJSONObject("data").getJSONObject("route");
                    String path = routeJSONObject.getString("path");
                    String args = routeJSONObject.getString("data");
                    if (!TextUtils.isEmpty(path)) {
                        if (RouteAdapter.isH5(path)) {
                            navigateToH5(context, path, args);
                        } else if ("mu://app/home".equals(path)) {
                            getMainParams(0, "tab_home");
                        } else if ("mu://app/dynamics".equals(path)) {
                            getMainParams(0, "tab_dynamic");
                        } else if ("mu://app/message".equals(path) || "mu://app/message/chat".equals(path)) {
                            getMainParams(0, "tab_msg");

                        } else if ("mu://app/speed_dating".equals(path)) {
                            getMainParams(0, "tab_match");

                        } else if ("mu://app/live/list".equals(path)) {
                            getMainParams(0, "tab_live");

                        } else if ("mu://app/activity/detail".equals(path)) {

                            try {
                                JSONObject jsonObject = new JSONObject(args);
                                int id = jsonObject.getInt("id");
                                String event_url = jsonObject.getString("event_url");
                                String share_url = jsonObject.getString("share_url");
                                String title = jsonObject.getString("title");
                                String content = jsonObject.getString("content");
                                boolean share = jsonObject.getInt("is_share") == 1;
                                String shareImage = jsonObject.getString("share_image");

                                H5Param param = new H5Param();
                                param.setTargetUrl(event_url);
                                param.setTitle(title);
                                if (share) {
                                    ShareInfo shareInfo = new ShareInfo();
                                    shareInfo.setContent(content);
                                    shareInfo.setIconUrl(shareImage);
                                    shareInfo.setTitle(title);
                                    shareInfo.setShareUrl(share_url);
                                    param.setShareInfo(shareInfo);
                                    param.setRightBtnName("分享");
                                }


                                ARouter.getInstance().build("/base/h5_activity")
                                        .withString("extra_key_json", object2Json(param))
                                        .navigation();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        } else {
                            String aroutePath = RouteAdapter.get(path);
                            if (!TextUtils.isEmpty(path)) {
                                if (TextUtils.isEmpty(args) || "{}".equals(args)) {
                                    try {
                                        //添加自身uid
                                        JSONObject jsonObject = new JSONObject();
                                        jsonObject.put("uid", getUid(context));
                                        args = jsonObject.toString();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                }
                                //执行跳转
                                ARouter.getInstance().build(aroutePath)
                                        .withString("extra_key_json", args)
                                        .navigation();

                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static String object2Json(Object obj) {
        try {
            return JSON.toJSONString(obj);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";

    }

    public String getUid(Context context) {
        SharedPreferences sp = context.getSharedPreferences("sp_file_user", Context.MODE_PRIVATE);
        String userJson = sp.getString("pref_key_loginuserinfo", "");
        String uid = "";
        if (!TextUtils.isEmpty(userJson)) {
            try {
                JSONObject jsonObject = new JSONObject(userJson);
                uid = jsonObject.getJSONObject("user").getString("uid");
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
        return uid;
    }

    private void navigateToH5(Context context, String path, String data) {
        SharedPreferences sp = context.getSharedPreferences("sp_file_user", Context.MODE_PRIVATE);
        String userJson = sp.getString("pref_key_loginuserinfo", "");
        String uid = "";
        if (!TextUtils.isEmpty(userJson)) {
            try {
                JSONObject jsonObject = new JSONObject(userJson);
                uid = jsonObject.getJSONObject("user").getString("uid");
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
        String appPath = RouteAdapter.get(path);
        String url = RouteAdapter.getH5Url(path);
        if (!"{}".equals(data)) {
            try {
                JSONObject jsonObject = new JSONObject(data);
                Iterator<String> keys = jsonObject.keys();
                StringBuilder paramsBuilder = new StringBuilder();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = jsonObject.getString(key);
                    paramsBuilder.append(key).append("=").append(value)
                            .append("&");
                }
                // String userJson=PrefsUtil.getString(context,SP_FILE_USER,PREF_KEY_LOGINUSER,"");
                // String uid= UserRepo.getInstance().get(context).getUser().getUid();
                paramsBuilder.append("uid=").append(uid);
                url = url + "?" + paramsBuilder.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else {
            if (!url.contains("?")) {
                url = url + "?uid=" + uid;
            } else {
                url = url + "&uid=" + uid;
            }

        }
        try {
            JH5Param param = JH5Param.obtain(url, "", "");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("targetUrl", param.targetUrl);
            jsonObject.put("title", param.title);
            jsonObject.put("rightBtnName", param.rightBtnName);
            ARouter.getInstance().build(appPath)
                    .withString("extra_key_json", jsonObject.toString())
                    .navigation();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public static class JH5Param {
        private String targetUrl;
        private String title;
        private String rightBtnName;

        public JH5Param() {

        }

        public JH5Param(String targetUrl, String title, String rightBtnName) {
            this.targetUrl = targetUrl;
            this.title = title;
            this.rightBtnName = rightBtnName;
        }

        public static JH5Param obtain(String url, String title, String rightBtnName) {
            return new JH5Param(url, title, rightBtnName);
        }
    }


    public void getMainParams(int cate, String tabName) {
        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append("{").append("\"").append("tab_name").append("\"")
                .append(":\"").append(tabName).append("\",")
                .append("\"").append("cate").append("\"").append(":")
                .append(cate).append("}");

        ARouter.getInstance().build("/app/main_activity")
                .withString("extra_key_json", sBuilder.toString())
                .navigation();


    }

    @Override
    public void onAliasOperatorResult(Context context, JPushMessage jPushMessage) {
        super.onAliasOperatorResult(context, jPushMessage);
        int sequence = jPushMessage.getSequence();
        System.out.println("jpush onAliasOperatorResult:" + sequence);
        if (sequence == JPush.CODE_SET_ALIAS) {
            String alias = jPushMessage.getAlias();
            int code = jPushMessage.getErrorCode();
            /**
             * 极光于 2020/03/10 对「别名设置」的上限进行限制，最多允许绑定 10 个设备。如需更高上限，请联系商务，详情请阅读公告。
             *错误码：6017
             */
            System.out.println("jpush Alias:" + jPushMessage.getAlias() + " code:" + code);
            if (!TextUtils.isEmpty(alias) && code == 0) {
                SharedPreferences sp = context.getSharedPreferences(JPush.PREF_FILE_NAME, Context.MODE_PRIVATE);
                sp.edit().putString(JPush.PREF_KEY_ALIAS + "_" + alias, alias).apply();
            }
        }

    }


    @Override
    public void onRegister(Context context, String registrationId) {
        super.onRegister(context, registrationId);
        //System.out.println("jpush registrationId："+registrationId);
    }
    private boolean mCalled;
    @Override
    public void onConnected(Context context, boolean isConnected) {
        super.onConnected(context, isConnected);
        //System.out.println("jpush isConnected:" + isConnected);
        /*if (isConnected&& !mCalled) {
            try {
                mCalled=true;
                SharedPreferences sp = context.getSharedPreferences("config", Context.MODE_PRIVATE);
                String uid = sp.getString("pref_key_user_uid", "");
                if (!TextUtils.isEmpty(uid)) {
                    Class pushClass = Class.forName("com.wind.base.task.JPushTask");
                    Method method = pushClass.getMethod("setAlias", Context.class, String.class);
                    method.invoke(null,context, uid);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }*/

    }

    public static class H5Param implements Serializable {

        private String targetUrl;
        private String title;
        private String rightBtnName;

        private ShareInfo shareInfo;

        public H5Param() {
        }

        public String getTargetUrl() {
            return targetUrl;
        }

        public void setTargetUrl(String targetUrl) {
            this.targetUrl = targetUrl;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getRightBtnName() {
            return rightBtnName;
        }

        public void setRightBtnName(String rightBtnName) {
            this.rightBtnName = rightBtnName;
        }

        public ShareInfo getShareInfo() {
            return shareInfo;
        }

        public void setShareInfo(ShareInfo shareInfo) {
            this.shareInfo = shareInfo;
        }
    }

    public static class ShareInfo implements Serializable {

        private String title;

        private String content;

        private String iconUrl;

        private String shareUrl;   // 分享链接，需要拼接商品id和拼团id

        public ShareInfo() {

        }


        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getIconUrl() {
            return iconUrl;
        }

        public void setIconUrl(String iconUrl) {
            this.iconUrl = iconUrl;
        }

        public String getShareUrl() {
            return shareUrl;
        }

        public void setShareUrl(String shareUrl) {
            this.shareUrl = shareUrl;
        }
    }

}
