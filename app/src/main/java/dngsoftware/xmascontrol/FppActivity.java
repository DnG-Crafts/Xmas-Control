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
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
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
    private ImageView errIco;
    private ScheduledExecutorService scheduler;
    private int currentVolume = 0;
    private SeekBar vBar;
    private String FPPAuth = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        setContentView(R.layout.activity_fpp);
        context = this;

        if (extras != null) {
            final String rHost = extras.getString("HOST");
            if (rHost != null && !rHost.isEmpty()) {
                if (extras.containsKey("AUTH"))
                {
                    FPPAuth = extras.getString("AUTH");
                }
                restHost = rHost;
                tDbg = findViewById(R.id.txtDbg);
                tVol = findViewById(R.id.txtVol);
                vBar = findViewById(R.id.volBar);
                errIco = findViewById(R.id.errIcon);
                vBar.setMax(100);

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
                JSONObject status = new JSONObject(getRESTCommand(restHost, fppStatus, FPPAuth));
                JSONArray sensorArr = status.getJSONArray("sensors");

                if (status.has("warningInfo") || status.has("warnings")) {
                    runOnUiThread(() -> {
                       errIco.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.twotone_warning_24));
                       errIco.setVisibility(View.VISIBLE);
                    });
                }
                else {
                    runOnUiThread(() -> {
                       errIco.setVisibility(View.GONE);
                    });
                }


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
                getRESTCommand(restHost, Cmd, FPPAuth);

            } catch (Exception ignored) {
            }
        }).start();

    }


    void sendRest(String Cmd, String dat) {
        new Thread(() -> {
            try {
                postRESTCommand(restHost, Cmd, dat, FPPAuth);
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
                JSONArray arrLists = new JSONArray(getRESTCommand(restHost, fppEffects, FPPAuth));
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


    public void openErrors(View v) {
        openDialog();
    }


    public void setVolume(int volume) {
        sendRest(fppVolume, String.format(fppVolumeBody , volume));
    }


    void openDialog() {
        final Dialog dialog = new Dialog(context,android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.error_dialog);
        dialog.setCanceledOnTouchOutside(false);
        final TextView dlgTitle = dialog.findViewById(R.id.dlgtitle);
        final TextView errLog = dialog.findViewById(R.id.errorLog);
        final ImageButton btnCls = dialog.findViewById(R.id.btncls);
        dlgTitle.setText(R.string.error_log);
        dialog.setTitle(R.string.error_log);
        btnCls.setOnClickListener(v -> dialog.dismiss());
        new Thread(() -> {
            try {
                JSONObject status = new JSONObject(getRESTCommand(restHost, fppStatus, FPPAuth));
                if (status.has("warningInfo") || status.has("warnings")) {
                    StringBuilder errMsg = new StringBuilder();
                    if (status.has("warningInfo"))
                    {
                        JSONArray errorArr = status.getJSONArray("warningInfo");
                        for (int i = 0; i < errorArr.length(); i++) {
                            JSONObject error = errorArr.getJSONObject(i);
                            errMsg.append(i + 1);
                            errMsg.append(": ");
                            errMsg.append(error.getString("message"));
                            errMsg.append("\n\n");
                        }
                    }else {
                        String errorStr = status.getString("warnings");
                        errorStr = errorStr.replaceAll("[\"\\[\\]]","");
                        int i = 1;
                        String[] errorList = errorStr.split(",");
                        for (String error : errorList) {
                            errMsg.append(i);
                            errMsg.append(": ");
                            errMsg.append(error);
                            errMsg.append("\n\n");
                            i++;
                        }
                    }

                    runOnUiThread(() -> {
                        errLog.setText(errMsg.toString());
                        dialog.show();
                    });
                }
            } catch (Exception e) {
                Log.e("FppActivity", Log.getStackTraceString(e));
            }
        }).start();
    }
}
