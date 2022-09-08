package org.libsdl.app;

import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.BaseApplication;
import com.admob.DataStoreUtils;
import com.admob.GAD;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.play.pay.Goods;
import com.play.pay.PayConsumer;
import com.play.pay.PayUtil;

import org.sean.pal95.BuildConfig;
import org.sean.pal95.R;

import java.io.File;
import java.util.Objects;


public class SettingsActivity extends AppCompatActivity {

    public static final String KEY_FEEDBACK = "key_fb";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish(); // back button
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // 震动效果
    public static boolean isFeedback() {
        return DataStoreUtils.readLongValue(BaseApplication.context, KEY_FEEDBACK, 1) == 1;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        // 专业版
        private boolean isPayPro() {
            return GAD.isNoAd();
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            CheckBoxPreference joystick = findPreference(getString(R.string.key_joystick));
            joystick.setChecked(DataStoreUtils.readLongValue(getActivity(), DataStoreUtils.SP_JOYSTICK, 1) == 1);
            joystick.setOnPreferenceChangeListener((preference, newValue) -> {
                DataStoreUtils.saveLongValue(getActivity(), DataStoreUtils.SP_JOYSTICK, (newValue instanceof Boolean && (Boolean) newValue) ? 1 : 0);
                return true;
            });

            CheckBoxPreference feedbackHaptic = findPreference(getString(R.string.feedback_haptic));
            feedbackHaptic.setChecked(DataStoreUtils.readLongValue(getActivity(), KEY_FEEDBACK, 1) == 1);
            feedbackHaptic.setOnPreferenceChangeListener((preference, newValue) -> {
                DataStoreUtils.saveLongValue(getActivity(), KEY_FEEDBACK, (newValue instanceof Boolean && (Boolean) newValue) ? 1 : 0);
                return true;
            });


            ListPreference mt = findPreference(getString(R.string.key_move_type));
            Objects.requireNonNull(mt).setValue(SDLActivity.nativeGetFlyMode() != 0 ? "cross" : "normal");
            mt.setOnPreferenceChangeListener((preference, newValue) -> {
                final boolean isFlyMode = SDLActivity.nativeGetFlyMode() != 0;
                String current = isFlyMode ? "cross" : "normal";
                if (!current.equals(newValue.toString())) {
                    SDLActivity.nativeHack(0, isFlyMode ? 0 : 1);
                }
                return true;
            });

            Preference level = findPreference(getString(R.string.key_level_up));
            Objects.requireNonNull(level).setOnPreferenceClickListener(preference -> {
                if (isPayPro() || BuildConfig.DEBUG) {
                    PayConsumer.levelUp();
                } else {
                    PayUtil.startPay(getActivity(), Goods.ITEM_LEVEL_UP);
                }
                return true;
            });


            Preference money = findPreference(getString(R.string.key_money_add));
            Objects.requireNonNull(money).setOnPreferenceClickListener(preference -> {
                if (isPayPro() || BuildConfig.DEBUG) {
                    PayConsumer.addMoney();
                } else {
                    PayUtil.startPay(getActivity(), Goods.ITEM_MONEY);
                }
                return true;
            });

            Preference noAd = findPreference(getString(R.string.key_no_ad));
            Objects.requireNonNull(noAd).setOnPreferenceClickListener(preference -> {
                if (isPayPro()) {
                    Toast.makeText(getActivity(), "已经购买,不需要再次购买！", Toast.LENGTH_LONG).show();
                } else {
                    PayUtil.startPay(getActivity(), Goods.ITEM_NO_AD);
                }
                return true;
            });

            Preference state = findPreference(getString(R.string.state_load_key));
            Objects.requireNonNull(state).setOnPreferenceChangeListener((preference, newValue) -> {
                String file = newValue.toString();
                String payed = DataStoreUtils.readLocalInfo(BaseApplication.context, PayConsumer.KEY_STATE + file);
                if (isPayPro() || BuildConfig.DEBUG || DataStoreUtils.VALUE_TRUE.equals(payed)) {
                    PayConsumer.loadState(file);
                    return true;
                } else {
                    PayUtil.startPay(getActivity(), Goods.ITEM_FLY, file);
                }
                return false;
            });

            Preference free = findPreference(getString(R.string.key_free));
            if (free != null) {
                Objects.requireNonNull(free).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        try {
                            File rpg1 = new File(SplashScreenActivity.getGameFile("1.rpg"));
                            rpg1.renameTo(new File(SplashScreenActivity.getGameFile(newValue.toString())));
                        } catch (Exception e) {
                            Toast.makeText(getActivity(), "修改存档失败:" + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                        return true;
                    }
                });
            }
        }

        @Override
        public void onResume() {
            super.onResume();
        }
    }

    public void upload() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl("gs://pal95-7e534.appspot.com")
                .child("stage").child("test2.txt");

        Uri file = Uri.fromFile(new File("path/to/images/rivers.jpg"));
        StorageReference riversRef = storageRef.child("images/" + file.getLastPathSegment());
        UploadTask uploadTask = riversRef.putFile(file);

// Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...
            }
        });
    }
}