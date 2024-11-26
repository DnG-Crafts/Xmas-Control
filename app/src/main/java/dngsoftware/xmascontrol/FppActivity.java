package dngsoftware.xmascontrol;

import static java.net.URLEncoder.encode;
import static dngsoftware.xmascontrol.Functions.capFirst;
import static dngsoftware.xmascontrol.Functions.convertJsonArrayToList;
import static dngsoftware.xmascontrol.Functions.getRESTCommand;
import static dngsoftware.xmascontrol.Functions.postRESTCommand;
import static dngsoftware.xmascontrol.Functions.secondsToTime;
import static dngsoftware.xmascontrol.Functions.showToast;
import static dngsoftware.xmascontrol.FppCommands.fppCommand;
import static dngsoftware.xmascontrol.FppCommands.fppEffects;
import static dngsoftware.xmascontrol.FppCommands.fppStartEffect;
import static dngsoftware.xmascontrol.FppCommands.fppStatus;
import static dngsoftware.xmascontrol.FppCommands.fppStopEffect;
import static dngsoftware.xmascontrol.FppCommands.fppStopTest;
import static dngsoftware.xmascontrol.FppCommands.fppStartTestBody;
import static dngsoftware.xmascontrol.FppCommands.fppVolume;
import static dngsoftware.xmascontrol.FppCommands.fppVolumeBody;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class FppActivity extends Activity {
    private Context context = this;
    private int SelectedTest, selectedEffect;
    private String restHost = "127.0.0.1";
    private TextView tDbg;
    private TextView tVol;
    private ScheduledExecutorService scheduler;
    int maxVolume = 100;
    int currentVolume = 0;
    SeekBar vBar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        setContentView(R.layout.activity_fpp);
        context = this;

        if (extras != null) {
            final String rHost = extras.getString("HOST");
            if (rHost != null && !rHost.isEmpty()) {
                restHost = rHost;
                tDbg = findViewById(R.id.txtDbg);
                tVol = findViewById(R.id.txtVol);
                vBar = findViewById(R.id.volBar);
                vBar.setMax(maxVolume);

                vBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        tVol.setText(String.valueOf(progress));
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        setVolume(seekBar.getProgress());
                    }
                });

            } else {
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }


    void updateStatus() {
        scheduler = Executors.newScheduledThreadPool(1);
        Runnable task = () -> {
            try {
                JSONObject status = new JSONObject(getRESTCommand(restHost, fppStatus));
                JSONArray sensorArr = status.getJSONArray("sensors");
                JSONObject sensors = sensorArr.getJSONObject(0);
                int statusVol = status.getInt("volume");
                if (statusVol != currentVolume) {
                    currentVolume = statusVol;
                    vBar.setProgress(currentVolume);
                }
                String cpuTemp = sensors.getString("formatted");
                String signalPercent;
                try {
                    JSONArray wifiArr = status.getJSONArray("wifi");
                    JSONObject wifi = wifiArr.getJSONObject(0);
                    signalPercent = wifi.getString("pct") + "%";
                }catch (org.json.JSONException e)
                {
                    signalPercent = "disabled";
                }

                String advJs = status.getString("advancedView");
                JSONObject adv = new JSONObject(advJs);
                String piType = adv.getString("Variant");
                String fppVer = adv.getString("Version");
                String fppType = status.getString("mode_name");

                String statusMsg = "Platform: " + piType +
                        "\nFPP Version: " + fppVer  +
                        "\nMode: " + capFirst(fppType) +
                        "\nHost: " + restHost +
                        "\nWifi Strength: " + signalPercent +
                        "\nCPU Temp: " + cpuTemp + "°C";

                String sequence = status.getString("current_sequence");
                if (!sequence.isEmpty()) {
                    String remaining = status.getString("seconds_remaining");
                    statusMsg = statusMsg + "\n\nSequence: " + sequence +
                            "\nRemaining: " + secondsToTime(Integer.parseInt(remaining));
                }
                String finalStatusMsg = statusMsg;
                runOnUiThread(() -> tDbg.setText(finalStatusMsg));
            } catch (Exception e) {
                Log.e("FppActivity", Log.getStackTraceString(e));
            }
        };
        scheduler.scheduleWithFixedDelay(task, 0, 2, TimeUnit.SECONDS);
    }


    void sendRest(String Cmd) {
        new Thread(() -> {
            try {
                getRESTCommand(restHost, Cmd);

            } catch (Exception ignored) {
            }
        }).start();

    }


    void sendRest(String Cmd, String dat) {
        new Thread(() -> {
            try {
                postRESTCommand(restHost, Cmd, dat);
            } catch (Exception ignored) {
            }
        }).start();
    }


    public void startTest(View v) {
        try {
            String[] modes = {"Cycle", "Chase", "Solid Red", "Solid Green", "Solid Blue", "Solid White"};
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            LayoutInflater inflater = LayoutInflater.from(context);
            View dialogView = inflater.inflate(R.layout.select_dialog, null);
            builder.setView(dialogView);
            final Spinner spinner = dialogView.findViewById(R.id.spinner);
            Button okButton = dialogView.findViewById(R.id.okButton);
            Button cancelButton = dialogView.findViewById(R.id.cancelButton);
            TextView title = dialogView.findViewById(R.id.selTitle);
            title.setText(R.string.select_mode);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, modes);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setSelection(SelectedTest);
            final AlertDialog dialog = builder.create();
            okButton.setOnClickListener(v1 -> {
                String sItem = spinner.getSelectedItem().toString();
                SelectedTest = spinner.getSelectedItemPosition();
                if (!sItem.isEmpty()) {
                    String testType = "RGB Cycle", testChannels = "1-8388608", testColor = "R-G-B";
                    switch (sItem) {
                        case "Cycle":
                            testType = "RGB Cycle";
                            testChannels = "1-8388608";
                            testColor = "R-G-B";
                            break;
                        case "Chase":
                            testType = "RGB Chase";
                            testChannels = "1-8388608";
                            testColor = "R-G-B";
                            break;
                        case "Solid Red":
                            testType = "RGB Single Color";
                            testChannels = "1-8388608";
                            testColor = "#ff0000";
                            break;
                        case "Solid Green":
                            testType = "RGB Single Color";
                            testChannels = "1-8388608";
                            testColor = "#ff00";
                            break;
                        case "Solid Blue":
                            testType = "RGB Single Color";
                            testChannels = "1-8388608";
                            testColor = "#ff";
                            break;
                        case "Solid White":
                            testType = "RGB Single Color";
                            testChannels = "1-8388608";
                            testColor = "#ffffff";
                            break;
                    }
                    sendRest(fppCommand, String.format(fppStartTestBody ,testType, testChannels, testColor));
                }
                dialog.dismiss();
            });
            cancelButton.setOnClickListener(v1 -> dialog.dismiss());
            dialog.show();
        } catch (Exception ignored) {}
    }


    public void stopTest(View v) {
        sendRest(fppStopTest);
    }


    public void startEffect(View v) {
        new Thread(() -> {
            try {
                JSONArray arrLists = new JSONArray(getRESTCommand(restHost, fppEffects));
                if (arrLists.length() != 0) {
                    List<String> eLists = convertJsonArrayToList(arrLists);
                    runOnUiThread(() -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        LayoutInflater inflater = LayoutInflater.from(context);
                        View dialogView = inflater.inflate(R.layout.select_dialog, null);
                        builder.setView(dialogView);
                        final Spinner spinner = dialogView.findViewById(R.id.spinner);
                        Button okButton = dialogView.findViewById(R.id.okButton);
                        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
                        TextView title = dialogView.findViewById(R.id.selTitle);
                        title.setText(R.string.select_effect);
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, eLists);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinner.setAdapter(adapter);
                        spinner.setSelection(selectedEffect);
                        final AlertDialog dialog = builder.create();
                        okButton.setOnClickListener(v1 -> {
                            String sItem = spinner.getSelectedItem().toString();
                            selectedEffect = spinner.getSelectedItemPosition();
                            if (!sItem.isEmpty()) {
                                try {
                                    sendRest(String.format(fppStartEffect, encode(sItem, "UTF-8")));
                                } catch (Exception ignored) {}
                            }
                            dialog.dismiss();
                        });
                        cancelButton.setOnClickListener(v1 -> dialog.dismiss());
                        dialog.show();
                    });
                }
                else {
                    showToast(context, R.string.no_effects_found);
                }
            } catch (Exception ignored) {}
        }).start();
    }


    public void stopEffect(View v) {
        sendRest(fppStopEffect);
    }


    public void openPlaylists(View v) {

        Intent intent = new Intent(this, PlaylistActivity.class);
        intent.putExtra("HOST", restHost);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(intent);
    }



    public void openSequences(View v) {

        Intent intent = new Intent(this, SequenceActivity.class);
        intent.putExtra("HOST", restHost);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(intent);
    }


    public void setVolume(int volume) {
        sendRest(fppVolume, String.format(fppVolumeBody , volume));
    }

}
