package com.codeboy.qianghongbao;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.List;

/**
 * <p>Created by 李文龙(LeonLee) on 15/2/17 下午10:25.</p>
 * <p><a href="mailto:codeboy2013@163.com">Email:codeboy2013@163.com</a></p>
 *
 * 抢红包外挂服务
 */
public class QiangHongBaoService extends AccessibilityService {

    static final String TAG = "QiangHongBao";

    static final String CHANGE_STATE = "com.codeboy.qianghongbao.CHANGE_STATE";
    static final int NOTIFICATION_ID = 0x1988;

    static final String CHAI_HONGBAO = "拆红包";
    static final String LIN_HONGBAO = "领取红包";
    static final String LOOK_HONGBAO = "查看红包";

    /** 微信的包名*/
    static final String WECHAT_PACKAGENAME = "com.tencent.mm";
    /** 红包消息的关键字*/
    static final String HONGBAO_TEXT_KEY = "[微信红包]";

    Notification notification;
    RemoteViews notificationView;

    boolean isListen = true;
    Handler handler = new Handler();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        final int eventType = event.getEventType();

        //Log.d(TAG, "事件---->" + event);

        if (!isListen) {
            return;
        }

        //通知栏事件
        if(eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            List<CharSequence> texts = event.getText();
            if(!texts.isEmpty()) {
                for(CharSequence t : texts) {
                    String text = String.valueOf(t);
                    if(text.contains(HONGBAO_TEXT_KEY)) {
                        openNotify(event);
                        break;
                    }
                }
            }
        } else if(eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            openHongBao(event);
        }
    }

    /*@Override
    protected boolean onKeyEvent(KeyEvent event) {
        //return super.onKeyEvent(event);
        return true;
    }*/

    @Override
    public void onInterrupt() {
        Toast.makeText(this, "中断抢红包服务", Toast.LENGTH_SHORT).show();

        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);

    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Toast.makeText(this, "连接抢红包服务", Toast.LENGTH_SHORT).show();

        updateNotificaton();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String action = intent.getAction();
        Log.d(TAG, "onStartCommand ---> " + action);
        if (action.equals(CHANGE_STATE)) {
            isListen = !isListen;
        }
        updateNotificaton();

        return super.onStartCommand(intent, flags, startId);
    }




    private void updateNotificaton() {

        if (notificationView == null) {
            notificationView = new RemoteViews(getPackageName(),
                    R.layout.service_notification);
        }


        Intent changeStateIntent = new Intent();
        changeStateIntent.setClass(this, QiangHongBaoService.class);
        changeStateIntent.setAction(CHANGE_STATE);
        PendingIntent changePendingIntent = PendingIntent.getService(this, 0, changeStateIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (isListen) {
            notificationView.setTextViewText(R.id.changeStateBtn, "暂停");
        } else {
            notificationView.setTextViewText(R.id.changeStateBtn, "继续");
        }
        notificationView.setOnClickPendingIntent(R.id.changeStateBtn, changePendingIntent);

        if (notification == null) {
            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            notification = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContent(notificationView)
                    .setAutoCancel(false)
                    .setContentIntent(pendingIntent)
                    .build();
        }


        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, notification);
    }

    private void sendNotifyEvent(){
        AccessibilityManager manager= (AccessibilityManager)getSystemService(ACCESSIBILITY_SERVICE);
        if (!manager.isEnabled()) {
            return;
        }
        AccessibilityEvent event=AccessibilityEvent.obtain(AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED);
        event.setPackageName(WECHAT_PACKAGENAME);
        event.setClassName(Notification.class.getName());
        CharSequence tickerText = HONGBAO_TEXT_KEY;
        event.getText().add(tickerText);
        manager.sendAccessibilityEvent(event);
    }

    /** 打开通知栏消息*/
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void openNotify(AccessibilityEvent event) {
        if(event.getParcelableData() == null || !(event.getParcelableData() instanceof Notification)) {
            return;
        }
        //以下是精华，将微信的通知栏消息打开
        Notification notification = (Notification) event.getParcelableData();
        PendingIntent pendingIntent = notification.contentIntent;
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void openHongBao(AccessibilityEvent event) {
        Log.d(TAG, "事件----> LuckyMoneyReceiveUI classname=" + event.getClassName());
        if("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI".equals(event.getClassName())) {
            //点中了红包，下一步就是去拆红包
            Log.d(TAG, "事件----> LuckyMoneyReceiveUI" + event);
            checkKey1();
        } else if("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI".equals(event.getClassName())) {
            //拆完红包后看详细的纪录界面
            //nonething
        } else if("com.tencent.mm.ui.LauncherUI".equals(event.getClassName())) {
            //在聊天界面,去点中红包
            Log.d(TAG, "事件----> LauncherUI" + event);
            checkKey2();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void checkKey1() {

        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if(nodeInfo == null) {
            Log.w(TAG, "rootWindow为空");
            return;
        }

        //Log.d(TAG, "checkKey1----> nodeInfo" + nodeInfo);

//        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(CHAI_HONGBAO);
//        for(AccessibilityNodeInfo n : list) {
//            n.getWindowId();
//            n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//        }

        List<AccessibilityNodeInfo> list2 = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/b43");
        for(AccessibilityNodeInfo n : list2) {
            Log.i(TAG, "-->微信红包by id:" + n.getText());
            n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            break;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void checkKey2() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if(nodeInfo == null) {
            Log.w(TAG, "rootWindow为空");
            return;
        }
//        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(LIN_HONGBAO);
//        List<AccessibilityNodeInfo> list2 = nodeInfo.findAccessibilityNodeInfosByText(LOOK_HONGBAO);
//        list.addAll(list2);

        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/e4");
        Log.i(TAG, "-->该聊天中红包数量:" + list.size());
        //最新的红包领起
        for(int i = list.size() - 1; i >= 0; i --) {
            AccessibilityNodeInfo parent = list.get(i).getParent();
            Log.i(TAG, "-->查看/领取红包:" + parent);
            Log.i(TAG, "-->查看/领取红包window id:" + list.get(i).getWindowId());
            if(parent != null) {
                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                break;
            }
        }






    }

}
