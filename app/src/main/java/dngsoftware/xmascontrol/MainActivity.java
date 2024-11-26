package dngsoftware.xmascontrol;

import static dngsoftware.xmascontrol.Functions.checkReadImagePermission;
import static dngsoftware.xmascontrol.Functions.copyBitmap;
import static dngsoftware.xmascontrol.Functions.getCameraPhotoOrientation;
import static dngsoftware.xmascontrol.Functions.getFilePath;
import static dngsoftware.xmascontrol.Functions.getRESTCommand;
import static dngsoftware.xmascontrol.Functions.loadBitmap;
import static dngsoftware.xmascontrol.Functions.md5;
import static dngsoftware.xmascontrol.Functions.sendFalconCommand;
import static dngsoftware.xmascontrol.Functions.showToast;
import static dngsoftware.xmascontrol.FppCommands.fppInfo;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.json.JSONObject;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {
    private listItem[] listItems;
    private RecyclerView recyclerView;
    private MainAdapter recycleAdapter;
    private Context context;
    private AppDB appDb;
    private String tmpImagePath = "";
    private boolean isEditing = false;
    private TextView addMsg;
    private ImageView tmpImage;
    private ImageButton btnClear, falBtn, fppBtn;
    private ScheduledExecutorService scheduler;
    private boolean isMoving = false;

    @Override
    protected void onPause() {
        super.onPause();
        stopUpdate();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopUpdate();
    }


    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (recycleAdapter == null) {
                loadItemList();
            }else {
                updateStatus();
            }
        } catch (Exception ignored) {}
    }


    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_main);
        SetPermissions();
        BottomAppBar bottomAppBar = findViewById(R.id.bottombar);
        setSupportActionBar(bottomAppBar);
        FloatingActionButton fab = findViewById(R.id.factionbar);
        fab.setOnClickListener(v -> openDialog("", "", ""));
        falBtn = findViewById(R.id.falBtn);
        fppBtn = findViewById(R.id.fppBtn);
        falBtn.setOnClickListener(v -> openDialog("", "", ""));
        fppBtn.setOnClickListener(v -> openDialog("", "", ""));
        recyclerView = findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.scrollToPosition(0);
        recyclerView.setLayoutManager(layoutManager);

        addMsg = findViewById(R.id.lblmsg);

        AppDataBase adb = Room.databaseBuilder(context, AppDataBase.class, "app_database").build();
        appDb = adb.appDB();

    }


    private void SetPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                String[] perms = {Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.INTERNET, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED};
                int permsRequestCode = 200;
                requestPermissions(perms, permsRequestCode);
            }
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                String[] perms = {Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.INTERNET};
                int permsRequestCode = 200;
                requestPermissions(perms, permsRequestCode);
            }
        } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                String[] perms = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.INTERNET};
                int permsRequestCode = 200;
                requestPermissions(perms, permsRequestCode);
            }
        }
    }


    @SuppressLint({"ClickableViewAccessibility"})
    private void loadItemList() {
        new Thread(() -> {
            try {
                if (appDb.getItemCount() > 0) {
                    int i = 0;
                    listItems = new listItem[appDb.getItemCount()];
                    List<MainItem> list = appDb.getAllitems();
                    for (MainItem items : list) {
                        listItems[i] = new listItem();
                        listItems[i].name = items.deviceName;
                        listItems[i].url = items.deviceUrl;
                        if (items.deviceImage != null && !items.deviceImage.isEmpty()) {
                            listItems[i].ipath = items.deviceImage;
                        }
                        listItems[i].icon = AppCompatResources.getDrawable(this, R.drawable.falcon);
                        i++;
                    }

                    recycleAdapter = new MainAdapter(getBaseContext(), listItems);
                    recycleAdapter.setHasStableIds(true);
                    runOnUiThread(() -> {
                        addMsg.setVisibility(View.GONE);
                        falBtn.setVisibility(View.GONE);
                        fppBtn.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        recyclerView.removeAllViewsInLayout();
                        recyclerView.setAdapter(null);
                        recyclerView.setAdapter(recycleAdapter);
                    });

                    recycleAdapter.setSettingsClickListener((view, position) -> {
                        isMoving = true;
                        recycleAdapter.moveItem(position,position-1);
                        updateDB();
                        recyclerView.removeAllViewsInLayout();
                        isMoving = false;
                    });

                    recycleAdapter.setLongClickListener((view, position) -> {
                        TextView urlTxt = view.findViewById(R.id.itemUrl);
                        final String url = urlTxt.getText().toString();
                        TextView nameTxt = view.findViewById(R.id.itemName);
                        final String name = nameTxt.getText().toString();
                        TextView imgTxt = view.findViewById(R.id.itemTag);
                        final String imgPath = imgTxt.getText().toString();
                        final Dialog dialog = new Dialog(context);
                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        dialog.setContentView(R.layout.action_dialog);
                        dialog.setTitle(R.string.lblselact);
                        dialog.setCanceledOnTouchOutside(false);
                        final TextView dlgTitle = dialog.findViewById(R.id.dlgtitle);
                        dlgTitle.setText(R.string.lblselact);
                        final TextView txtMsg = dialog.findViewById(R.id.txtmsg);
                        txtMsg.setText(name);
                        final Button btnCls = dialog.findViewById(R.id.btncls);
                        btnCls.setOnClickListener(v -> dialog.dismiss());

                        final Button btnEdt = dialog.findViewById(R.id.btnedt);
                        btnEdt.setOnClickListener(v -> {
                            dialog.dismiss();
                            openDialog(url, name, imgPath);
                        });

                        final Button btnDel = dialog.findViewById(R.id.btndel);
                        btnDel.setOnClickListener(v -> {
                            try {
                                stopUpdate();
                                new Thread(() -> {
                                    appDb.deleteItem(appDb.getMainitem(url));
                                    runOnUiThread(this::loadItemList);
                                }).start();

                            } catch (Exception ignored) {
                            }
                            dialog.dismiss();
                        });
                        dialog.show();
                    });

                } else {
                    runOnUiThread(() -> {
                        addMsg.setVisibility(View.VISIBLE);
                        falBtn.setVisibility(View.VISIBLE);
                        fppBtn.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        recyclerView.removeAllViewsInLayout();
                        recyclerView.setAdapter(null);
                    });
                }
            } catch (Exception ignored) {
            }

            if (scheduler == null) {
                updateStatus();
            }

        }).start();
    }


    void stopUpdate() {
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    void updateStatus() {
        stopUpdate();
        scheduler = Executors.newScheduledThreadPool(1);
        Runnable task = () -> {
            try {
                if (!isMoving && appDb.getItemCount() > 0) {
                    runOnUiThread(() -> recycleAdapter.notifyDataSetChanged());
                }
            } catch (Exception ignored) {}
        };
        scheduler.scheduleWithFixedDelay(task, 0, 5, TimeUnit.SECONDS);
    }


    void openDialog(String cHost, String cName, String cImage) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.edit_dialog);
        dialog.setCanceledOnTouchOutside(false);
        final TextView dlgTitle = dialog.findViewById(R.id.dlgtitle);
        final TextView pName = dialog.findViewById(R.id.pname);
        final TextView pUrl = dialog.findViewById(R.id.purl);
        final Button btnCls = dialog.findViewById(R.id.btncls);
        final Button btnImg = dialog.findViewById(R.id.btnimg);
        final Button btnAdd = dialog.findViewById(R.id.btnadd);
        final Button btnName = dialog.findViewById(R.id.btnname);
        btnClear = dialog.findViewById(R.id.btnclr);
        tmpImage = dialog.findViewById(R.id.ivimg);
        tmpImagePath = "";
        isEditing = false;
        dlgTitle.setText(R.string.lbladdprint);
        dialog.setTitle(R.string.lbladdprint);
        if (!cHost.isEmpty()) {
            isEditing = true;
            btnAdd.setText(R.string.lblsave);
            dlgTitle.setText(R.string.lbledtprint);
            dialog.setTitle(R.string.lbledtprint);
            pUrl.setText(cHost);
        }
        if (!cName.isEmpty()) {
            pName.setText(cName);
        }
        if (!cImage.isEmpty()) {
            tmpImage.setImageURI(Uri.parse(cImage));
            tmpImagePath = cImage;
            btnClear.setVisibility(View.VISIBLE);
        }

        btnCls.setOnClickListener(v -> dialog.dismiss());

        btnClear.setOnClickListener(v -> {
            tmpImagePath = "";
            tmpImage.setImageURI(null);
            tmpImage.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.falcon));
            btnClear.setVisibility(View.GONE);
        });

        btnName.setOnClickListener(v -> new Thread(() -> {
            try {
                if (!pUrl.getText().toString().isEmpty()) {
                    try {
                        JSONObject fInfo = new JSONObject(getRESTCommand(pUrl.getText().toString(), fppInfo));
                        String fName = fInfo.getString("HostName");
                        runOnUiThread(() -> pName.setText(fName));
                    } catch (org.json.JSONException e) {
                        try {
                            JSONObject f16api = new JSONObject(sendFalconCommand(pUrl.getText().toString(),"Q","ST"));
                            JSONObject fInfo = f16api.getJSONObject("P");
                            String fName = fInfo.getString("N");
                            runOnUiThread(() -> pName.setText(fName));
                        } catch (org.json.JSONException ignored) {
                            showToast(context, R.string.lblfailedset);
                        }
                    }
                } else {
                    showToast(context, R.string.lblinphost);
                }
            } catch (Exception ignored) {
            }
        }).start());


        btnImg.setOnClickListener(v -> {
            if (checkReadImagePermission(context)) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                selectFileResult.launch(Intent.createChooser(intent, context.getText(R.string.lblselimg)));

            } else {
                AlertDialog.Builder aDB =
                        new AlertDialog.Builder(context, R.style.AlertDialog)
                                .setTitle(R.string.lblperm)
                                .setMessage(R.string.lblpermsg)
                                .setPositiveButton(R.string.lblset, (dialog1, which) -> {
                                    try {
                                        Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri);
                                        startActivity(intent);
                                    } catch (Exception e) {
                                        Intent intent = new Intent();
                                        intent.setAction(Settings.ACTION_APPLICATION_SETTINGS);
                                        startActivity(intent);
                                    }
                                    dialog1.cancel();
                                })
                                .setNegativeButton(R.string.lblcancel, (dialog1, which) -> dialog1.cancel());
                AlertDialog alertDialog = aDB.show();
                alertDialog.setCanceledOnTouchOutside(false);
            }
        });


        btnAdd.setOnClickListener(v -> {
            if (pUrl.getText().toString().isEmpty()) {
                showToast(context, R.string.lbladdhost);
                return;
            } else if (pName.getText().toString().isEmpty()) {
                showToast(context, R.string.lbladdname);
                return;
            }
            stopUpdate();
            new Thread(() -> {
                MainItem item = new MainItem();
                item.position = (appDb.getItemCount() + 1);
                item.deviceUrl = pUrl.getText().toString();
                item.deviceName = pName.getText().toString();
                item.deviceImage = tmpImagePath;
                if (isEditing) {
                    MainItem tmpItem = appDb.getMainitem(cHost);
                    if (tmpItem != null) {
                        appDb.deleteItem(tmpItem);
                    }
                }
                appDb.addItem(item);
                runOnUiThread(this::loadItemList);
            }).start();
            dialog.dismiss();
        });
        dialog.show();
    }


    ActivityResultLauncher<Intent> selectFileResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        Uri content_describer = data.getData();
                        String filename = getFilePath(context, content_describer);
                        if (filename != null && !filename.isEmpty()) {
                            new Thread(new Runnable() {
                                public void run() {
                                    tmpImagePath = context.getCacheDir().getPath() + "/" + md5(filename);
                                    Bitmap bitmap = loadBitmap(filename);
                                    copyBitmap(filename, tmpImagePath);
                                    runOnUiThread(() -> {
                                        tmpImage.setImageBitmap(bitmap);
                                        tmpImage.setRotation(getCameraPhotoOrientation(filename));
                                        btnClear.setVisibility(View.VISIBLE);
                                    });
                                }
                            }).start();
                        }
                    }
                }
            });


    private void updateDB()
    {
        new Thread(() -> {
            for (int i = 0; i < recycleAdapter.getItemCount(); i++) {
                appDb.updatePosition(i, recycleAdapter.getItem(i).url);
            }
        }).start();
    }

}