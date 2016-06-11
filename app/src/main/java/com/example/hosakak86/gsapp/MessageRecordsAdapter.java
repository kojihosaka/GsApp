//ListViewに１つのセルの情報(message_item.xmlとMessageRecord)を結びつけるためのクラス
package com.example.hosakak86.gsapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.kii.cloud.storage.Kii;
import com.kii.cloud.storage.KiiObject;
import com.kii.cloud.storage.callback.KiiObjectCallBack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//<MessageRecord>はデータクラスMessageRecordのArrayAdapterであることを示している。このアダプターで管理したいデータクラスを記述されば良い。
public class MessageRecordsAdapter extends ArrayAdapter<MessageRecord> {
    private ImageLoader mImageLoader;
    private Context mContext;

    //アダプターを作成する関数。コンストラクター。クラス名と同じです。
    public MessageRecordsAdapter(Context context) {
        //レイアウトのidmessage_itemのViewを親クラスに設定している
        super(context, R.layout.message_item);
        mContext = context;
        //キャッシュメモリを確保して画像を取得するクラスを作成。これを使って画像をダウンロードする。Volleyの機能
        mImageLoader = new ImageLoader(VolleyApplication.getInstance().getRequestQueue(), new BitmapLruCache());
    }
    //表示するViewを返します。これがListVewの１つのセルとして表示されます。表示されるたびに実行されます。
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        //convertViewをチェックし、Viewがないときは新しくViewを作成します。convertViewがセットされている時は未使用なのでそのまま再利用します。メモリーに優しい。
        if(convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.message_item, parent, false);
        }

        //レイアウトにある画像と文字のViewを所得します。
        NetworkImageView imageView = (NetworkImageView) convertView.findViewById(R.id.image1);
        TextView textView = (TextView) convertView.findViewById(R.id.text1);

        //webリンクを制御するプログラムはここから
        // TextView に LinkMovementMethod を登録します
        //TextViewをタップした時のイベントリスナー（タップの状況を監視するクラス）を登録します。onTouchにタップした時の処理を記述します。buttonやほかのViewも同じように記述できます。
        textView.setOnTouchListener(new ViewGroup.OnTouchListener() {
            //タップした時の処理
            @Override
            public boolean onTouch(final View view, MotionEvent event) {
                //タップしたのはTextViewなのでキャスト（型の変換）する
                TextView textView = (TextView) view;
                //リンクをタップした時に処理するクラスを作成。AndroidSDKにあるLinkMovementMethodを拡張しています。
                MutableLinkMovementMethod m = new MutableLinkMovementMethod();
                //MutableLinkMovementMethodのイベントリスナーをさらにセットしています。
                m.setOnUrlClickListener(new MutableLinkMovementMethod.OnUrlClickListener() {
                    //リンクをクリックした時の処理
                    public void onUrlClick(TextView v,Uri uri) {
                        Log.d("myurl", uri.toString());//デバッグログを出力します。
                        // Intent のインスタンスを取得する。view.getContext()でViewの自分のアクティビティーのコンテキストを取得。遷移先のアクティビティーを.classで指定
                        Intent intent = new Intent(view.getContext(), WebActivity.class);
                        
                        // 渡したいデータとキーを指定する。urlという名前でリンクの文字列を渡しています。
                        final Intent url = intent.putExtra("url", uri.toString());

                        // 遷移先の画面を呼び出す
                        view.getContext().startActivity(intent);
                        
                    }
                });
                //ここからはMutableLinkMovementMethodを使うための処理なので毎回同じ感じ。
                //リンクのチェックを行うため一時的にsetする
                textView.setMovementMethod(m);
                boolean mt = m.onTouchEvent(textView, (Spannable) textView.getText(), event);
                //チェックが終わったので解除する しないと親view(listview)に行けない
                textView.setMovementMethod(null);
                //setMovementMethodを呼ぶとフォーカスがtrueになるのでfalseにする
                textView.setFocusable(false);
                //戻り値がtrueの場合は今のviewで処理、falseの場合は親view(ListView)で処理
                return mt;
            }
        });
        //webリンクを制御するプログラムはここまで

        //表示するセルの位置からデータをMessageRecordのデータを取得します。
        MessageRecord imageRecord = getItem(position);

        //mImageLoaderを使って画像をダウンロードし、Viewにセットします。
        imageView.setImageUrl(imageRecord.getImageUrl(), mImageLoader);
        //Viewに文字をセットします。
        textView.setText(imageRecord.getComment());
        //リンクセット
        setSpannableString(textView);

        //Goodで追加ここから　
        //いいねボタンを得る
        Button buttonView = (Button) convertView.findViewById(R.id.button1);
        //ボタンの文字にいいねの数を追加します。
        buttonView.setText(getContext().getString(R.string.good)+":"+imageRecord.getGoodCount());

        //ボタンを押した時のクリックイベントを定義
        buttonView.setOnClickListener(new View.OnClickListener() {
            //クリックした時
            @Override
            public void onClick(View view) {
                //いいねボタンを得る
//                Button buttonView = (Button) view;
                ////タグからどの位置のボタンかを得る
                //int position = (Integer)buttonView.getTag();
                //MessageRecordsAdapterの位置からMessageRecordのデータを得る
                MessageRecord messageRecord = getItem(position);
                //messagesのバケット名と_idの値からKiiObjectのuri(データの場所)を得る。参考：http://documentation.kii.com/ja/starts/cloudsdk/cloudoverview/idanduri/
                Uri objUri = Uri.parse("kiicloud://buckets/" + "messages" + "/objects/" + messageRecord.getId());
                //uriから空のデータを作成
                KiiObject object = KiiObject.createByUri(objUri);
                //いいねを＋１する。
                object.set("goodCount", messageRecord.getGoodCount() + 1);
                //既存の他のデータ(_id,comment,imageUrlなど)はそのままに、goodCountだけが更新される。参考：http://documentation.kii.com/ja/guides/android/managing-data/object-storages/updating/#full_update
                object.save(new KiiObjectCallBack() {
                    //KiiCloudの更新が完了した時
                    @Override
                    public void onSaveCompleted(int token, KiiObject object, Exception exception) {
                        if (exception != null) {
                            //エラーの時
                            return;
                        }
                        //MessageRecordsAdapterの位置からMessageRecordのデータを得る
                        MessageRecord messageRecord = getItem(position);
                        //messageRecordのいいねの数を+1する。これでKiiCloudの値とListViewのデータが一致する。
                        messageRecord.setGoodCount(messageRecord.getGoodCount() + 1);
                        //データの変更を通知します。
                        notifyDataSetChanged();
                        //トーストを表示.Activityのコンテキストが必要なのでgetContext()してる。
                        Toast.makeText(getContext(), getContext().getString(R.string.good_done), Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
        //Goodで追加ここまで

        //1つのセルのViewを返します。
        return convertView;
    }
    //データをセットしなおす関数
    public void setMessageRecords(List<MessageRecord> objects) {
        //ArrayAdapterを空にする。
        clear();
        //テータの数だけMessageRecordを追加します。
        for(MessageRecord object : objects) {
            add(object);
        }
        //データの変更を通知します。
        notifyDataSetChanged();
    }

    //---------------------------------------------------------------------------------------------------

    private void setSpannableString(TextView textView) {

        String message = textView.getText().toString();

//        TextView textView = new TextView(context);
//        textView.setAutoLinkMask(Linkify.WEB_URLS);
//        textView.setText("http://www.yahoo.co.jp/");

        // リンク化対象の文字列、リンク先 URL を指定する
        Map<String, String> map = new HashMap<String, String>();
        map.put("hand", "http://google.com/");

        // SpannableString の取得
        SpannableString ss = createSpannableString(message, map);

        // SpannableString をセットし、リンクを有効化する
        textView.setText(ss);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private SpannableString createSpannableString(String message, Map<String, String> map) {

        SpannableString ss = new SpannableString(message);

        for (final Map.Entry<String, String> entry : map.entrySet()) {
            int start = 0;
            int end = 0;

            // リンク化対象の文字列の start, end を算出する
            Pattern pattern = Pattern.compile(entry.getKey());
            Matcher matcher = pattern.matcher(message);
            while (matcher.find()) {
                start = matcher.start();
                end = matcher.end();
                break;
            }

            // SpannableString にクリックイベント、パラメータをセットする
            ss.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    String url = entry.getValue();
                    Uri uri = Uri.parse(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    mContext.startActivity(intent);
                }
            }, start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }

        return ss;
    }

//---------------------------------------------------------------------------------------------------

    public static class Constants {
        public static final String Kii_APP_ID = "4c7447d9";
        public static final String KII_APP_KEY = "a48cbb93ebc6f84059f0fbe7f2083acd";
        public static final Kii.Site KII_SITE = Kii.Site.JP;
    }

}
