package com.example.tacademy.kaokatest;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import com.kakao.auth.ApiResponseCallback;
import com.kakao.auth.AuthService;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.Session;
import com.kakao.auth.network.response.AccessTokenInfoResponse;
import com.kakao.kakaolink.KakaoLink;
import com.kakao.kakaolink.KakaoTalkLinkMessageBuilder;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.LogoutResponseCallback;
import com.kakao.usermgmt.callback.MeResponseCallback;
import com.kakao.usermgmt.response.model.UserProfile;
import com.kakao.util.KakaoParameterException;
import com.kakao.util.exception.KakaoException;
import com.kakao.util.helper.log.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {
    private SessionCallback callback;

    class SessionCallback implements ISessionCallback {

        @Override
        public void onSessionOpened() {
            Log.i("KAKA", "onSessionOpened() call");
            redirectSignupActivity();
        }

        @Override
        public void onSessionOpenFailed(KakaoException exception) {
            if (exception != null) {
                Log.i("KAKA", "onSessionOpenFailed() call :" + exception.getMessage());
                Logger.e(exception);
            }
        }
    }

    // 액티비티가 생성되면 세션에 아답터를 등록하고 체킹에 들어간다.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 카카오 링크
        try {
            kakaoLink = kakaoLink.getKakaoLink(this.getApplicationContext());
        } catch (KakaoParameterException e) {
            e.printStackTrace();
        }

        //해시키 구하기
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }

        // 카카오 로그인에 대한 세션 체킹을 위한 아답터 생성
        callback = new SessionCallback();
        // 세션 객체에 등록
        Session.getCurrentSession().addCallback(callback);
        // 세션 체킹
        Session.getCurrentSession().checkAndImplicitOpen();
    }

    // 액티비티가 소멸되면 세션에 등록된 아답터를 제거한다.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Session.getCurrentSession().removeCallback(callback);
    }

    // 로그인 수행 수 돌아왔을 때 호출된다 (데이터를 가지고)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("KAKA", "requestCode:" + requestCode);
        Log.i("KAKA", "resultCode:" + resultCode);
        if (data != null)
            Log.i("KAKA", "Intent:" + data.toString());

        if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    // 간단하게 로그인 하는 액티비티로 이동
    protected void redirectSignupActivity() {
        Log.i("KAKA", "redirectSignupActivity()");
        /*
        final Intent intent = new Intent(this, SampleSignupActivity.class);
        startActivity(intent);
        finish();*/
        requestMe();
        requestAccessTokenInfo();
    }

    // 프로필 정보 가져오기
    private void requestMe() {
        UserManagement.requestMe(new MeResponseCallback() {
            @Override
            public void onFailure(ErrorResult errorResult) {
                String message = "failed to get user info. msg=" + errorResult;
                Logger.d(message);

                //redirectLoginActivity();
            }

            @Override
            public void onSessionClosed(ErrorResult errorResult) {
                // redirectLoginActivity();
            }

            @Override
            public void onSuccess(UserProfile userProfile) {
                Logger.d("UserProfile : " + userProfile);
                Log.i("KAKA", "UserProfile:" + userProfile);
                // redirectMainActivity();
            }

            @Override
            public void onNotSignedUp() {
                //  showSignup();
            }
        });
    }

    private void requestAccessTokenInfo() {
        AuthService.requestAccessTokenInfo(new ApiResponseCallback<AccessTokenInfoResponse>() {
            @Override
            public void onSessionClosed(ErrorResult errorResult) {
                // redirectLoginActivity(self);
            }

            @Override
            public void onNotSignedUp() {
                // not happened
            }

            @Override
            public void onFailure(ErrorResult errorResult) {
                Logger.e("failed to get access token info. msg=" + errorResult);
            }

            @Override
            public void onSuccess(AccessTokenInfoResponse accessTokenInfoResponse) {
                long userId = accessTokenInfoResponse.getUserId();
                Logger.d("this access token is for userId=" + userId);

                long expiresInMilis = accessTokenInfoResponse.getExpiresInMillis();
                Logger.d("this access token expires after " + expiresInMilis + " milliseconds.");
            }
        });
    }

    KakaoLink kakaoLink;
    public void onKakaoLink(View view) {
        try {
            KakaoTalkLinkMessageBuilder kakaoTalkLinkMessageBuilder = kakaoLink.createKakaoTalkLinkMessageBuilder();
            String text = "첫메시지!!123!@$ASDasd";
            kakaoTalkLinkMessageBuilder.addText(text).build();
            kakaoLink.sendMessage(kakaoTalkLinkMessageBuilder, this);
        } catch (KakaoParameterException e) {
            e.printStackTrace();
        }
    }

    // 세션이 열렸을 때만 활성화 하고 -> 마이 메뉴쪽 하단에 배치
    // 자동로그인상태인지도 저장해놔야 sns종류에 따라서 로그아웃 띄워야됨
    public void onClickLogout(View view) {
        UserManagement.requestLogout(new LogoutResponseCallback() {
            @Override
            public void onCompleteLogout() {
                //redirectLoginActivity();
                Log.i("Logout","logout!");
                // 저장된 로그인 정보도 모두 삭제
            }
        });
    }
}