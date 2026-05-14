package com.whisperyao.dsplayer;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.view.View;
import android.widget.EditText;
import com.synology.ThreadWork;
import com.whisperyao.dsplayer.playing.Player;
import dagger.android.support.DaggerAppCompatActivity;

public abstract class TestRendererActivity extends DaggerAppCompatActivity {
    protected abstract void cancelSettingRemotePlayerPassword();

    protected abstract void passSettingRemotePlayerPassword(final Player player);
    
    private void showInputDialog(final Player player) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_player);
        builder.setMessage(getString(R.string.renderer_password_title) + " : ");
        View viewInflate = getLayoutInflater().inflate(R.layout.renderer_passwd_dialog, null);
        final EditText editText = viewInflate.findViewById(R.id.input);
        builder.setView(viewInflate);
        builder.setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
            if (!editText.getText().toString().isEmpty()) {
                TestRendererActivity.this.setPassword(player, editText.getText().toString());
            } else {
                TestRendererActivity.this.cancelSettingRemotePlayerPassword();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> TestRendererActivity.this.cancelSettingRemotePlayerPassword());
        builder.setOnCancelListener(dialog -> TestRendererActivity.this.cancelSettingRemotePlayerPassword());
        builder.create().show();
    }

    protected void testPassword(final Player player) {
        new ThreadWork() {
            final ProgressDialog myDialog;
            boolean success = false;

            {
                this.myDialog = new ProgressDialog(TestRendererActivity.this);
            }

            @Override
            public void preWork() {
                this.myDialog.setMessage(TestRendererActivity.this.getResources().getString(R.string.processing));
                this.myDialog.setCancelable(false);
                this.myDialog.show();
            }

            @Override
            public void onWorking() {
                // FirebaseAnalytics.Param.SUCCESS
                this.success = new ApiRemoteController().control_testPassword(player.getUniqueId()).optBoolean("success");
            }

            @Override
            public void postWork() {
                this.myDialog.dismiss();
            }

            @Override
            public void onComplete() {
                if (this.success) {
                    TestRendererActivity.this.passSettingRemotePlayerPassword(player);
                } else {
                    TestRendererActivity.this.showInputDialog(player);
                }
            }
        }.startWork();
    }

    protected void setPassword(final Player player, final String passwd) {
        new ThreadWork() {
            final ProgressDialog myDialog;
            boolean success = false;

            {
                this.myDialog = new ProgressDialog(TestRendererActivity.this);
            }

            @Override
            public void preWork() {
                this.myDialog.setMessage(TestRendererActivity.this.getResources().getString(R.string.processing));
                this.myDialog.setCancelable(false);
                this.myDialog.show();
            }

            @Override
            public void onWorking() {
                this.success = new ApiRemoteController()
                        .control_setPassword(player.getUniqueId(), passwd)
                        // FirebaseAnalytics.Param.SUCCESS
                        .optBoolean("success");
            }

            @Override
            public void postWork() {
                this.myDialog.dismiss();
            }

            @Override
            public void onComplete() {
                if (this.success) {
                    TestRendererActivity.this.passSettingRemotePlayerPassword(player);
                } else {
                    TestRendererActivity.this.showInputDialog(player);
                }
            }
        }.startWork();
    }
}
