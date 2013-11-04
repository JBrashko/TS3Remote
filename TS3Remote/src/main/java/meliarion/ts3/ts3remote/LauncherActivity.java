package meliarion.ts3.ts3remote;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

public class LauncherActivity extends Activity {
    public final static String CLIENT_IP = "Meliarion.TS3.TS3Remote.CLIENTIP";
    public final static String USE_REMOTE= "Meliarion.TS3.TS3Remote.REMOTE";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
    }

    @Override
    public void onPause()
    {super.onPause();

    }

    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void serverConnect (View view){
     EditText editText = (EditText) findViewById(R.id.edit_message);
     connect(editText.getText().toString());
    }

    public void testConnect (View view){
        connect("192.168.0.3");
    }
    private void connect(String target)
    {
        CheckBox box = (CheckBox)findViewById(R.id.useRemote);
        boolean remote = box.isChecked();
        Intent intent = new Intent(this, DisplayServerActivity.class);
        intent.putExtra(CLIENT_IP, target);
        intent.putExtra(USE_REMOTE,remote);
        startActivity(intent);
    }

}
