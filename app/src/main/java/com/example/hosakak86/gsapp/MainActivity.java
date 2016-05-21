package com.example.hosakak86.gsapp;
//起動時に実行されるアクティビティーです。１つの画面に１つのアクティビティーが必要です。
//どのアクティビティーが起動時に実行されるのかはAndroidManifestに記述されています。

        import android.app.ActionBar;
        import android.app.Activity;
        import android.content.Context;
        import android.content.Intent;
        import android.content.SharedPreferences;
        import android.graphics.Color;
        import android.net.Uri;
        import android.os.Bundle;
        import android.support.v7.app.ActionBarActivity;
        import android.support.v7.app.AppCompatActivity;
        import android.support.v7.widget.Toolbar;
        import android.text.SpannableString;
        import android.text.Spanned;
        import android.text.method.LinkMovementMethod;
        import android.text.style.ClickableSpan;
        import android.text.style.URLSpan;
        import android.view.Gravity;
        import android.view.Menu;
        import android.view.MenuItem;
        import android.view.View;
        import android.view.Window;
        import android.widget.Button;
        import android.widget.ListView;
        import android.widget.TextView;
        import android.widget.Toast;

        import com.android.volley.Response;
        import com.android.volley.VolleyError;
        import com.android.volley.toolbox.JsonObjectRequest;
        import com.facebook.LoggingBehavior;
        import com.facebook.Session;
        import com.facebook.SessionState;
        import com.facebook.Settings;
        import com.kii.cloud.storage.Kii;
        import com.kii.cloud.storage.KiiObject;
        import com.kii.cloud.storage.KiiUser;
        import com.kii.cloud.storage.callback.KiiQueryCallBack;
        import com.kii.cloud.storage.callback.KiiSocialCallBack;
        import com.kii.cloud.storage.query.KiiQuery;
        import com.kii.cloud.storage.query.KiiQueryResult;
        import com.kii.cloud.storage.social.KiiSocialConnect;
        import com.kii.cloud.storage.social.connector.KiiSocialNetworkConnector;

        import org.json.JSONArray;
        import org.json.JSONException;
        import org.json.JSONObject;

        import java.util.ArrayList;
        import java.util.HashMap;
        import java.util.List;
        import java.util.Map;
        import java.util.regex.Matcher;
        import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    //アダプタークラスです。
    private MessageRecordsAdapter mAdapter;

//    private Button button;
//    private TextView textView;

    //起動時にOSから実行される関数です。
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //setContentViewより前にWindowにActionBar表示を設定
//        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
//        setContentView(R.layout.activity_main);
//
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        //KiiCloudでのログイン状態を取得します。nullの時はログインしていない。
        KiiUser user = KiiUser.getCurrentUser();
        //自動ログインのため保存されているaccess tokenを読み出す。tokenがあればログインできる
        SharedPreferences pref = getSharedPreferences(getString(R.string.save_data_name), Context.MODE_PRIVATE);
        String token = pref.getString(getString(R.string.save_token), "");//保存されていない時は""
        //ログインしていない時はログインのactivityに遷移.SharedPreferencesが空の時もチェックしないとLogOutできない。
        if(user == null || token == "") {
            // Intent のインスタンスを取得する。getApplicationContext()でViewの自分のアクティビティーのコンテキストを取得。遷移先のアクティビティーを.classで指定
            Intent intent = new Intent(getApplicationContext(), UserActivity.class);
            // 遷移先の画面を呼び出す
            startActivity(intent);
            //戻れないようにActivityを終了します。
            finish();
        }
        //-----------------------------------------------------------------------------------------
//        Kii.initialize("4c7447d9", "a48cbb93ebc6f84059f0fbe7f2083acd", Kii.Site.JP);
//
//        Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);
//
//        Session session = Session.getActiveSession();
//        if (session == null) {
//            if (savedInstanceState != null) {
//                session = Session.restoreSession(this, null, this, savedInstanceState);
//            }
//            if (session == null) {
//                session = new Session(this);
//            }
//            Session.setActiveSession(session);
//            if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
//                session.openForRead(new Session.OpenRequest(this).setCallback(this));
//            }
//        }
//        button = (Button) findViewById(R.id.button);
//        textView = (TextView) findViewById(R.id.textView);
//        updateView(session);

        //---------------------------------------------------------------------------------------

        //メイン画面のレイアウトをセットしています。ListView
        setContentView(R.layout.activity_main);

        //アダプターを作成します。newでクラスをインスタンス化しています。
        mAdapter = new MessageRecordsAdapter(this);

        //ListViewのViewを取得
        ListView listView = (ListView) findViewById(R.id.mylist);
        //ListViewにアダプターをセット。
        listView.setAdapter(mAdapter);
        //一覧のデータを作成して表示します。
        fetch();

    }

    //ListView2で追加ここから
    //KiiCLoud対応のfetchです。
    //自分で作った関数です。一覧のデータを作成して表示します。
    private void fetch() {
        //KiiCloudの検索条件を作成。検索条件は未設定。なので全件。
        KiiQuery query = new KiiQuery();
        //ソート条件を設定。日付の降順
        query.sortByDesc("_created");
        //バケットmessagesを検索する。最大200件
        Kii.bucket("messages")
                .query(new KiiQueryCallBack<KiiObject>() {
                    //検索が完了した時
                    @Override
                    public void onQueryCompleted(int token, KiiQueryResult<KiiObject> result, Exception exception) {
                        if (exception != null) {
                            //エラー処理を書く
                            return;
                        }
                        //空のMessageRecordデータの配列を作成
                        ArrayList<MessageRecord> records = new ArrayList<MessageRecord>();
                        //検索結果をListで得る
                        List<KiiObject> objLists = result.getResult();
                        //得られたListをMessageRecordに設定する
                        for (KiiObject obj : objLists) {
                            //_id(KiiCloudのキー)を得る。空の時は""が得られる。
                            String id = obj.getString("_id", "");
                            String title = obj.getString("comment", "");
                            String url = obj.getString("imageUrl", "");
                            //MessageRecordを新しく作ります。
                            MessageRecord record = new MessageRecord(id, url, title);
                            //MessageRecordの配列に追加します。
                            records.add(record);
                        }
                        //データをアダプターにセットしています。これで表示されます。
                        mAdapter.setMessageRecords(records);
                    }
                }, query);

    }
//    Postから戻ってくるときに画面を更新したいのでfetchを実行しています。
    @Override
    protected void onStart() {
        super.onStart();
        //一覧のデータを作成して表示します。
        fetch();
    }
//    ListView2で追加ここまで


    //自分で作った関数です。一覧のデータを作成して表示します。
//    private void fetch() {
//        //jsonデータをサーバーから取得する通信機能です。Volleyの機能です。通信クラスのインスタンスを作成しているだけです。通信はまだしていません。
//        JsonObjectRequest request = new JsonObjectRequest(
////                "http://gashfara.com/test/json.txt" ,//jsonデータが有るサーバーのURLを指定します。
////                "http://sbhosaka.sakura.ne.jp/json.txt" ,
//                "http://khosaka.sakura.ne.jp/json.txt" ,
//                null,
//                //サーバー通信した結果、成功した時の処理をするクラスを作成しています。
//                new Response.Listener<JSONObject>() {
//                    @Override
//                    public void onResponse(JSONObject jsonObject) {
//                        //try catchでエラーを処理します。tryが必要かどうかはtryに記述している関数次第です。
//                        try {
//                            //jsonデータを下記で定義したparse関数を使いデータクラスにセットしています。
//                            List<MessageRecord> messageRecords = parse(jsonObject);
//                            //データをアダプターにセットしています。
//                            mAdapter.setMessageRecords(messageRecords);
//                        }
//                        catch(JSONException e) {
//                            //トーストを表示
//                            Toast.makeText(getApplicationContext(), "Unable to parse data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                },
//                //通信結果、エラーの時の処理クラスを作成。
//                new Response.ErrorListener() {
//                    @Override
//                    public void onErrorResponse(VolleyError volleyError) {
//                        //トーストを表示
//                        Toast.makeText(getApplicationContext(), "Unable to fetch data: " + volleyError.getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });
//        //作成した通信クラスをキュー、待ち行列にいれて適当なタイミングで通信します。
//        //VolleyApplicationはnewしていません。これはAndroidManifestで記載しているので起動時に自動的にnewされています。
//        VolleyApplication.getInstance().getRequestQueue().add(request);
//    }
//    //サーバにあるjsonデータをMessageRecordに変換します。
//    private List<MessageRecord> parse(JSONObject json) throws JSONException {
//        //空のMessageRecordデータの配列を作成
//        ArrayList<MessageRecord> records = new ArrayList<MessageRecord>();
//        //jsonデータのmessagesにあるJson配列を取得します。
//        JSONArray jsonMessages = json.getJSONArray("messages");
//        //配列の数だけ繰り返します。
//        for(int i =0; i < jsonMessages.length(); i++) {
//            //１つだけ取り出します。
//            JSONObject jsonMessage = jsonMessages.getJSONObject(i);
//            //jsonの値を取得します。
//            String title = jsonMessage.getString("comment");
//            String url = jsonMessage.getString("imageUrl");
//            //jsonMessageを新しく作ります。
//            MessageRecord record = new MessageRecord(url, title);
//            //MessageRecordの配列に追加します。
//            records.add(record);
//        }
//        return records;
//    }

    //デフォルトで作成されたメニューの関数です。未使用。
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

//        // メニューの要素を追加
//        menu.add("Normal item");
//        // メニューの要素を追加して取得
//        MenuItem actionItem = menu.add("Action Button");
//        // SHOW_AS_ACTION_IF_ROOM:余裕があれば表示
//        actionItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
//        // アイコンを設定
//        actionItem.setIcon(android.R.drawable.ic_menu_share);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        //ログアウト処理.KiiCloudにはログアウト機能はないのでAccesTokenを削除して対応。
        if (id == R.id.log_out) {
            //自動ログインのため保存されているaccess tokenを消す。
            SharedPreferences pref = getSharedPreferences(getString(R.string.save_data_name), Context.MODE_PRIVATE);
            pref.edit().clear().apply();
            //ログイン画面に遷移
            // Intent のインスタンスを取得する。getApplicationContext()でViewの自分のアクティビティーのコンテキストを取得。遷移先のアクティビティーを.classで指定
            Intent intent = new Intent(getApplicationContext(), UserActivity.class);
            // 遷移先の画面を呼び出す
            startActivity(intent);
            //戻れないようにActivityを終了します。
            finish();
            return true;
        }

        //投稿処理
        if (id == R.id.post) {
            //投稿画面に遷移
            // Intent のインスタンスを取得する。getApplicationContext()でViewの自分のアクティビティーのコンテキストを取得。遷移先のアクティビティーを.classで指定
            Intent intent = new Intent(getApplicationContext(), PostActivity.class);
            // 遷移先の画面を呼び出す
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //-----------------------------------------------------------------------------------------



//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == KiiSocialNetworkConnector.REQUEST_CODE) {
//            Kii.socialConnect(KiiSocialConnect.SocialNetwork.SOCIALNETWORK_CONNECTOR).respondAuthOnActivityResult(
//                    requestCode,
//                    resultCode,
//                    data);
//        }
//
////        Activity activity = this.getActivity();
//        Activity activity = this;
//        KiiSocialConnect connect = Kii.socialConnect(KiiSocialConnect.SocialNetwork.SOCIALNETWORK_CONNECTOR);
//
//        Bundle options = new Bundle();
//        options.putParcelable(KiiSocialNetworkConnector.PROVIDER, KiiSocialNetworkConnector.Provider.FACEBOOK);
//
//        // Login.
//        connect.logIn(activity, options, new KiiSocialCallBack() {
//            @Override
//            public void onLoginCompleted(KiiSocialConnect.SocialNetwork network, KiiUser user, Exception exception) {
//                if (exception != null) {
//                    // Error handling
//                    return;
//                }
//            }
//        });
//
//        connect = Kii.socialConnect(KiiSocialConnect.SocialNetwork.SOCIALNETWORK_CONNECTOR);
//        Bundle b = connect.getAccessTokenBundle();
//
//        // The access token.
//        String accessToken = b.getString("oauth_token");
//
//        // User id provided by the social network provider.
//        String providerUserId = b.getString("provider_user_id");
//
//        // If a new Kii user is created with the logIn method.
//        boolean kiiNewUser = b.getBoolean("kii_new_user");
//    }
    //----------------------------------------------------------------------------------------------

//    @Override
//    protected void onStart() {
//        super.onStart();
//        Session.getActiveSession().addCallback(this);
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        Session.getActiveSession().removeCallback(this);
//    }
//
//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//        Session session = Session.getActiveSession();
//        Session.saveSession(session, outState);
//
//        Kii.onSaveInstanceState(outState);
//    }
//
//    @Override
//    protected void onRestoreInstanceState(Bundle savedInstanceState) {
//        super.onRestoreInstanceState(savedInstanceState);
//        Kii.onRestoreInstanceState(savedInstanceState);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
//        if (requestCode == KiiSocialNetworkConnector.REQUEST_CODE) {
//            Kii.socialConnect(KiiSocialConnect.SocialNetwork.SOCIALNETWORK_CONNECTOR)
//                    .respondAuthOnActivityResult(requestCode, resultCode, data);
//        }
//    }
//
//    private void updateView(final Session session) {
//        if (session.isOpened()) {
//
//            // FB login succeeded. Login to Kii with obtained access token.
//            Bundle options = new Bundle();
//            String accessToken = session.getAccessToken();
//            options.putString("accessToken", accessToken);
//            options.putParcelable("provider", KiiSocialNetworkConnector.Provider.FACEBOOK);
//            KiiSocialNetworkConnector conn = (KiiSocialNetworkConnector) Kii.socialConnect(KiiSocialConnect.SocialNetwork.SOCIALNETWORK_CONNECTOR);
//            conn.logIn(this, options, new KiiSocialCallBack() {
//                @Override
//                public void onLoginCompleted(KiiSocialConnect.SocialNetwork network, KiiUser user, Exception exception) {
//                    if (exception != null) {
//                        textView.setText("Failed to Login to Kii! " + exception
//                                .getMessage());
//                        return;
//                    }
//                    textView.setText("Login to Kii! " + user.getID());
//                }
//            });
//
//            button.setText("LOGOUT");
//            button.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    Session s = Session.getActiveSession();
//                    if (!session.isClosed()) {
//                        session.closeAndClearTokenInformation();
//                    }
//                    KiiUser.logOut();
//                }
//            });
//        } else {
//            textView.setText("Login to FB");
//            button.setText("LOGIN");
//            button.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    Session session = Session.getActiveSession();
//                    if (!session.isOpened() && !session.isClosed()) {
//                        session.openForRead(new Session.OpenRequest(MainActivity.this)
//                                .setCallback(MainActivity.this));
//                    } else {
//                        Session.openActiveSession(MainActivity.this, true, MainActivity.this);
//                    }
//                }
//            });
//        }
//    }
//
//    // Session.StatusCallback
//    @Override
//    public void call(Session session, SessionState sessionState, Exception e) {
//        updateView(session);
//    }



    //---------------------------------------------------------------------------------------------

}