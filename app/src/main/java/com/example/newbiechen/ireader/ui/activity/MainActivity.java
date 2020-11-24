package com.example.newbiechen.ireader.ui.activity;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.newbiechen.ireader.R;
import com.example.newbiechen.ireader.ui.base.BaseTabActivity;
import com.example.newbiechen.ireader.ui.fragment.BookShelfFragment;
import com.example.newbiechen.ireader.ui.fragment.CommunityFragment;
import com.example.newbiechen.ireader.ui.fragment.FindFragment;
import com.example.newbiechen.ireader.utils.Constant;
import com.example.newbiechen.ireader.utils.FileUtils;
import com.example.newbiechen.ireader.utils.PermissionsChecker;
import com.example.newbiechen.ireader.utils.SharedPreUtils;
import com.example.newbiechen.ireader.ui.dialog.SexChooseDialog;
import com.example.newbiechen.ireader.utils.ToastUtils;
import com.pgyersdk.crash.PgyCrashManager;
import com.pgyersdk.update.DownloadFileListener;
import com.pgyersdk.update.PgyUpdateManager;
import com.pgyersdk.update.UpdateManagerListener;
import com.pgyersdk.update.javabean.AppBean;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.OkHttpClient;

public class MainActivity extends BaseTabActivity{
    /*************Constant**********/
    private static final int WAIT_INTERVAL = 2000;
    private static final int PERMISSIONS_REQUEST_STORAGE = 1;
    private static final int PERMISSIONS_FILE_LOAD = 2;

    static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    /***************Object*********************/
    private final ArrayList<Fragment> mFragmentList = new ArrayList<>();
    private PermissionsChecker mPermissionsChecker;
    /*****************Params*********************/
    private boolean isPrepareFinish = false;

    @Override
    protected int getContentId() {
        return R.layout.activity_base_tab;
    }

    /**************init method***********************/
    @Override
    protected void setUpToolbar(Toolbar toolbar) {
        super.setUpToolbar(toolbar);
        toolbar.setLogo(R.mipmap.logo);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setTitle("");
    }

    @Override
    protected List<Fragment> createTabFragments() {
        initFragment();
        return mFragmentList;
    }

    private void initFragment(){
        Fragment bookShelfFragment = new BookShelfFragment();
        Fragment communityFragment = new CommunityFragment();
        Fragment discoveryFragment = new FindFragment();
        mFragmentList.add(bookShelfFragment);
        mFragmentList.add(communityFragment);
        mFragmentList.add(discoveryFragment);
    }

    @Override
    protected List<String> createTabTitles() {
        String [] titles = getResources().getStringArray(R.array.nb_fragment_title);
        return Arrays.asList(titles);
    }

    @Override
    protected void initWidget() {
        super.initWidget();
        //性别选择框
        showSexChooseDialog();
    }

    private void showSexChooseDialog(){
        String sex = SharedPreUtils.getInstance()
                .getString(Constant.SHARED_SEX);
        if (sex.equals("")){
            mVp.postDelayed(()-> {
                Dialog dialog = new SexChooseDialog(this);
                dialog.show();
            },500);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main,menu);
        MenuItem item = menu.findItem(R.id.action_settings);
        item.setTitle("检查新版本（"+getVersionName(this)+"）");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Class<?> activityCls = null;
        switch (id) {
            case R.id.action_search:
                activityCls = SearchActivity.class;
                break;
//            case R.id.action_login:
//                break;
//            case R.id.action_my_message:
//                break;
            case R.id.action_download:
                activityCls = DownloadActivity.class;
                break;
//            case R.id.action_sync_bookshelf:
//                break;
            case R.id.action_scan_local_book:

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M){

                    if (mPermissionsChecker == null){
                        mPermissionsChecker = new PermissionsChecker(this);
                    }

                    //获取读取和写入SD卡的权限
                    if (mPermissionsChecker.lacksPermissions(PERMISSIONS)){
                        //请求权限
                        ActivityCompat.requestPermissions(this, PERMISSIONS,PERMISSIONS_REQUEST_STORAGE);
                        return super.onOptionsItemSelected(item);
                    }
                }

                activityCls = FileSystemActivity.class;
                break;
//            case R.id.action_wifi_book:
//                break;
//            case R.id.action_feedback:
//                break;
//            case R.id.action_night_mode:
//                break;
            case R.id.action_settings:
                //检查新版本
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M){

                    if (mPermissionsChecker == null){
                        mPermissionsChecker = new PermissionsChecker(this);
                    }

                    //获取读取和写入SD卡的权限
                    if (mPermissionsChecker.lacksPermissions(PERMISSIONS)){
                        //请求权限
                        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSIONS_FILE_LOAD);
                        return super.onOptionsItemSelected(item);
                    }
                }
                checkUpdate();
                break;
            default:
                break;
        }
        if (activityCls != null){
            Intent intent = new Intent(this, activityCls);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkUpdate(){
        new PgyUpdateManager.Builder()
                .setForced(true)                //设置是否强制提示更新
                .setUserCanRetry(false)         //失败后是否提示重新下载
                .setDeleteHistroyApk(false)     // 检查更新前是否删除本地历史 Apk， 默认为true
                .register();

//        new PgyUpdateManager.Builder()
//                .setForced(true)                //设置是否强制提示更新,非自定义回调更新接口此方法有用
//                .setUserCanRetry(true)          //失败后是否提示重新下载，非自定义下载 apk 回调此方法有用
//                .setDeleteHistroyApk(true)      // 检查更新前是否删除本地历史 Apk， 默认为true
//                .setUpdateManagerListener(new UpdateManagerListener() {
//                    @Override
//                    public void onNoUpdateAvailable() {
//                        //没有更新是回调此方法
//                        Log.d("xmg", "there is no new version");
//                        ToastUtils.show("当前为最新版");
//                    }
//                    @Override
//                    public void onUpdateAvailable(AppBean appBean) {
//                        //有更新回调此方法
//                        Log.d("xmg", "there is new version can update."
//                                + " new versionCode is " + appBean.getVersionCode());
//                        //调用以下方法，DownloadFileListener 才有效；
//                        //如果完全使用自己的下载方法，不需要设置DownloadFileListener
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                PgyUpdateManager.downLoadApk(appBean.getDownloadURL());
//                            }
//                        });
//                    }
//
//                    @Override
//                    public void checkUpdateFailed(Exception e) {
//                        //更新检测失败回调
//                        //更新拒绝（应用被下架，过期，不在安装有效期，下载次数用尽）以及无网络情况会调用此接口
//                        Log.e("xmg", "check update failed ", e);
//                    }
//                })
//                //注意 ：
//                //下载方法调用 PgyUpdateManager.downLoadApk(appBean.getDownloadURL()); 此回调才有效
//                //此方法是方便用户自己实现下载进度和状态的 UI 提供的回调
//                //想要使用蒲公英的默认下载进度的UI则不设置此方法
//                .setDownloadFileListener(new DownloadFileListener() {
//                    @Override
//                    public void downloadFailed() {
//                        //下载失败
////                        Log.e("pgyer", "download apk failed");
//                    }
//
//                    @Override
//                    public void downloadSuccessful(File file) {
////                        Log.e("pgyer", "download apk success");
//                        // 使用蒲公英提供的安装方法提示用户 安装apk
//                        PgyUpdateManager.installApk(file);
//                        Log.i("xmg", "file path :"+file.getAbsolutePath());
////                        installApk(file);
////                        install(file);
//                    }
//
//                    @Override
//                    public void onProgressUpdate(Integer... integers) {
////                        Log.e("pgyer", "update download apk progress" + integers);
//                    }})
//                .register();
    }

    private void install(File file){
        //源文件路径
        final String FROMPATH = file.getAbsolutePath();
        //目标文件路径
        final String TOPATH = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download";

        String newFilePath = FileUtils.copyFile(FROMPATH, TOPATH);
        if(!TextUtils.isEmpty(newFilePath)){
            Log.d("xmg", "文件拷贝成功  newFilePath="+newFilePath);
            installApk(new File(newFilePath));
        }else{
            Log.d("xmg", "文件拷贝失败");
        }
    }

    private void installApk(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.d("xmg", "版本大于 N ，开始使用 fileProvider 进行安装");

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(this, "com.max.app.ireader.fileProvider", file);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        } else {
            Log.d("xmg", "正常进行安装");

            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        }
        startActivity(intent);
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        if (menu != null && menu instanceof MenuBuilder){
            try {
                Method method = menu.getClass().
                        getDeclaredMethod("setOptionalIconsVisible",Boolean.TYPE);
                method.setAccessible(true);
                method.invoke(menu,true);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return super.onPreparePanel(featureId, view, menu);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSIONS_REQUEST_STORAGE:
                // 如果取消权限，则返回的值为0
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //跳转到 FileSystemActivity
                    Intent intent = new Intent(this, FileSystemActivity.class);
                    startActivity(intent);

                } else {
                    ToastUtils.show("用户拒绝开启读写权限");
                }
                break;
            case PERMISSIONS_FILE_LOAD:
                if (grantResults.length>0&&grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkUpdate();
                } else {
                    ToastUtils.show("权限拒绝");
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if(!isPrepareFinish){
            mVp.postDelayed(
                    () -> isPrepareFinish = false,WAIT_INTERVAL
            );
            isPrepareFinish = true;
            Toast.makeText(this, "再按一次退出", Toast.LENGTH_SHORT).show();
        }
        else {
            super.onBackPressed();
        }
    }

    public static String getVersionName(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
